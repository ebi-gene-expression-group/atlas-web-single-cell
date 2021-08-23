package uk.ac.ebi.atlas.experimentpage.metadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrJsonFacetBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.SelectStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;
import uk.ac.ebi.atlas.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.solr.client.solrj.SolrQuery.ORDER.asc;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACET_CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACET_FACTOR_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACTOR_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACTOR_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.search.SolrQueryResponseUtils.extractSimpleOrderedMaps;

@Component
public class CellMetadataDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(CellMetadataDao.class);

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;
    private final IdfParser idfParser;

    private final static String INFERRED_CELL_TYPE_SOLR_VALUE = "inferred_cell_type_-_ontology_labels";
    private final static String SINGLE_CELL_IDENTIFIER_SOLR_VALUE = "single_cell_identifier";
    private final static String METADATA_VALUE_TARGET_FIELD_NAME = "metadata_value";

    public CellMetadataDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory,
                           IdfParser idfParser) {
        this.singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
        this.idfParser = idfParser;
    }

    // Returns a list of available factor types for a given experiment
    public Set<String> getFactorTypes(String experimentAccession) {
        var queryBuilder = buildFactorTypeQuery(experimentAccession);

        return extractFactorTypesFromSolrQueryResults(queryBuilder);
    }

    // Returns a list of available factor types for a given cell in an experiment
    // Note: it is possible that a cell has missing factors, i.e. it has fewer factors than the experiment
    public Set<String> getFactorTypes(String experimentAccession, String cellId) {
        var queryBuilder = buildFactorTypeQuery(experimentAccession);
        queryBuilder.addQueryFieldByTerm(CELL_ID, cellId);

        return extractFactorTypesFromSolrQueryResults(queryBuilder);
    }

    // Retrieves a list of additional attributes (i.e. characteristics of interest) from the experiment idf file
    public Set<String> getCharacteristicTypes(String experimentAccession) {
        var characteristics = ImmutableSet.<String>builder();
        // We are ALWAYS interested in the inferred cell type
        if (hasInferredCellType(experimentAccession)) {
            characteristics.add(INFERRED_CELL_TYPE_SOLR_VALUE);
        }

        var idfParserOutput = idfParser.parse(experimentAccession);
        if (idfParserOutput.getMetadataFieldsOfInterest().isEmpty()) {
            return characteristics.build();
        }

        var characteristicsFromIdf = idfParserOutput.getMetadataFieldsOfInterest()
                .stream()
                .map(StringUtil::wordsToSnakeCase)
                .collect(toSet());

        characteristics.addAll(characteristicsFromIdf);

        return characteristics.build();
    }

    public ImmutableMap<String, String> getMetadataValuesForCellId(String experimentAccession,
                                                                   String cellId,
                                                                   Collection<String> factorFields,
                                                                   Collection<String> characteristicFields) {
        if (factorFields.isEmpty() && characteristicFields.isEmpty()) {
            return ImmutableMap.of();
        }

        var fields = ImmutableMap.of(CHARACTERISTIC_NAME, characteristicFields, FACTOR_NAME, factorFields);

        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CELL_ID, cellId)
                        .addQueryFieldByTerm(fields)
                        .setFieldList(Arrays.asList(CHARACTERISTIC_NAME, CHARACTERISTIC_VALUE, FACTOR_NAME, FACTOR_VALUE));

        var results = this.singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults();

        if (results.isEmpty()) {
            return ImmutableMap.of();
        }

        return results
                .stream()
                .collect(
                        toImmutableMap(
                                entry ->
                                        entry.getOrDefault(
                                                FACTOR_NAME.name(),
                                                entry.get(CHARACTERISTIC_NAME.name())).toString(),
                                entry ->
                                        entry.getOrDefault(
                                                FACTOR_VALUE.name(),
                                                entry.get(CHARACTERISTIC_VALUE.name())).toString(),
                                // If encountering the same name as factor and characteristic the last one overwrites
                                // any previous values
                                (v1, v2) -> v2));
    }

    // Given a type of metadata and an experiment accession, this method retrieves the value of that metadata for the
    // cells in an experiment
    public ImmutableMap<String, String> getMetadataValues(String experimentAccession,
                                                          String metadataType) {
        // Find matching characteristic name and factor name docs...
        var solrQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(ImmutableMap.of(
                                FACTOR_NAME, ImmutableSet.of(metadataType),
                                CHARACTERISTIC_NAME, ImmutableSet.of(metadataType)))
                        .sortBy(CELL_ID, asc)
                        .setFieldList(ImmutableSet.of(CELL_ID, FACET_CHARACTERISTIC_VALUE, FACET_FACTOR_VALUE));
        // ... all of them!
        var searchStreamBuilder =
                new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, solrQueryBuilder).returnAllDocs();
        // Rename factor/characteristic value to metadata_value
        var selectStreamBuilder =
                new SelectStreamBuilder(searchStreamBuilder)
                        .addFieldMapping(ImmutableMap.of(
                                CELL_ID.name(), CELL_ID.name(),
                                FACET_FACTOR_VALUE.name(), METADATA_VALUE_TARGET_FIELD_NAME,
                                FACET_CHARACTERISTIC_VALUE.name(), METADATA_VALUE_TARGET_FIELD_NAME));
        // Get unique values
        var uniqueStreamBuilder =
                new UniqueStreamBuilder(selectStreamBuilder, CELL_ID.name());

        try (var tupleStreamer = TupleStreamer.of(uniqueStreamBuilder.build())) {
            return tupleStreamer
                    .get()
                    .collect(toImmutableMap(
                            tuple -> tuple.getString(CELL_ID.name()),
                            // Missing values of a given metadata type is technically an invalid condensed SDRF file,
                            // but it’s a situation that we’ve encountered in the past and we want the web app to be
                            // somewhat lenient about it and show the experiment, rather than show a cryptic 400 error;
                            // instead we log an error and fill those fields with “Not available”.
                            // Please see:
                            // https://ebi-fg.slack.com/archives/C9P2QETJ9/p1624354966014500
                            // https://ebi-fg.slack.com/archives/C800ZEPPS/p1624362599003900
                            // https://www.pivotaltracker.com/story/show/177533187
                            tuple -> {
                                if (isBlank(tuple.getString(METADATA_VALUE_TARGET_FIELD_NAME))) {
                                    LOGGER.error(
                                            "Missing metadata value for {} – {}",
                                            tuple.getString(CELL_ID.name()),
                                            metadataType);
                                    return "Not available";
                                }
                                return tuple.getString(METADATA_VALUE_TARGET_FIELD_NAME);
                            }));
        }
    }

    private SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> buildFactorTypeQuery(String experimentAccession) {
        var facetBuilder =
                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                        .setFacetField(FACTOR_NAME);

        return
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addFacet(FACTOR_NAME.name(), facetBuilder)
                        .setRows(0);
    }

    private Set<String> extractFactorTypesFromSolrQueryResults(
            SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> queryBuilder) {
        var results =
                extractSimpleOrderedMaps(
                        singleCellAnalyticsCollectionProxy
                                .query(queryBuilder)
                                .getResponse()
                                .findRecursive("facets", FACTOR_NAME.name(), "buckets"));

        return results
                .stream()
                .map(x -> x.get("val").toString())
                .filter(factor -> !factor.equalsIgnoreCase(SINGLE_CELL_IDENTIFIER_SOLR_VALUE))
                .collect(Collectors.toSet());
    }

    private boolean hasInferredCellType(String experimentAccession) {
        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, INFERRED_CELL_TYPE_SOLR_VALUE);

        return !singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults().isEmpty();
    }
}
