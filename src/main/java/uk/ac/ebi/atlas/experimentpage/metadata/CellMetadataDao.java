package uk.ac.ebi.atlas.experimentpage.metadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.common.SolrDocument;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField;
import uk.ac.ebi.atlas.solr.cloud.search.SolrJsonFacetBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.utils.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACTOR_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACTOR_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.search.SolrQueryResponseUtils.extractSimpleOrderedMaps;

@Component
public class CellMetadataDao {
    private SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;
    private IdfParser idfParser;

    private final static String INFERRED_CELL_TYPE_SOLR_VALUE = "inferred_cell_type";
    private final static String SINGLE_CELL_IDENTIFIER_SOLR_VALUE = "single_cell_identifier";

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

    private List<String> getCellTypeForCellId(String experimentAccession, String cellId) {
        var characteristicFields = "inferred_cell_type";
        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CELL_ID, cellId)
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, characteristicFields);
        var results = this.singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults();
        return results
                .stream()
                .map(entry -> entry.get(CHARACTERISTIC_VALUE.name()).toString())
                .collect(Collectors.toList());
    }

    public Map<String, String> getMetadataValuesForCellId(String experimentAccession,
                                                          String cellId,
                                                          Collection<String> factorFields,
                                                          Collection<String> characteristicFields) {
        if (factorFields.isEmpty() && characteristicFields.isEmpty()) {
            return ImmutableMap.of();
        }

        var fields = ImmutableMap.of(CHARACTERISTIC_NAME, characteristicFields, FACTOR_NAME, factorFields);

        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
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
                        toMap(
                                entry -> ((ArrayList) entry.getOrDefault(FACTOR_NAME.name(),
                                        entry.get(CHARACTERISTIC_NAME.name()))).get(0).toString(),
                                // The factor fields in Solr are all multi-value fields, even though they technically
                                // shouldn't be. Apparently we don't expect any cell ID to have more than one factor
                                // value. This was confirmed by curators in this Slack conversation:
                                // https://ebi-fg.slack.com/archives/C800ZEPPS/p1529592962001046
                                entry -> ((ArrayList) entry.getOrDefault(FACTOR_VALUE.name(),
                                        entry.get(CHARACTERISTIC_VALUE.name()))).get(0).toString()));
    }

    // Given a type of metadata, this method retrieves the value of that metadata for list of cell IDs.
    public Map<String, String> getMetadataValueForCellIds(String experimentAccession,
                                                          String metadataType,
                                                          Collection<String> cellIds) {
        // We need to do this because we don't know if the metadata type is a factor or a characteristic
        var fields = ImmutableMap.<SingleCellAnalyticsSchemaField, Collection<String>>of(
                CHARACTERISTIC_NAME, ImmutableSet.of(metadataType),
                FACTOR_NAME, ImmutableSet.of(metadataType));

        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CELL_ID, cellIds)
                        .addQueryFieldByTerm(fields)
                        .setFieldList(ImmutableSet.of(CELL_ID, CHARACTERISTIC_VALUE, FACTOR_VALUE));

        var results = this.singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults();

        return results
                .stream()
                .collect(groupingBy(solrDocument -> (String) solrDocument.getFieldValue(CELL_ID.name())))
                .entrySet()
                .stream()
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                // The factor fields in Solr are all multi-value fields, even though they technically
                                // shouldn't be. Apparently we don't expect any cell ID to have more than one factor
                                // value. This was confirmed by curators in this Slack conversation:
                                // https://ebi-fg.slack.com/archives/C800ZEPPS/p1529592962001046
                                entry -> {
                                    SolrDocument result = entry.getValue().get(0);

                                    return ((ArrayList) result.getOrDefault(FACTOR_VALUE.name(),
                                            result.get(CHARACTERISTIC_VALUE.name()))).get(0).toString();
                                }));
    }

    // Given a type of chara, this method retrieves the value of that metadata for list of cell IDs.
    public Map<String, Map<String, String>> getCellTypeMetadata(String characteristicName,
                                                              String characteristicValue) {

        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, characteristicName)
                        .addQueryFieldByTerm(CHARACTERISTIC_VALUE, characteristicValue);

        var results = this.singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults();

        return results
                .stream()
                .collect(groupingBy(solrDocument -> (String) solrDocument.getFieldValue(EXPERIMENT_ACCESSION.name())))
                .entrySet()
                .stream()
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue()
                                        .stream()
                                        .collect(
                                                toMap(
                                                        cell -> cell.get(CELL_ID.name()).toString(),
                                                        cell -> getCellTypeForCellId(entry.getKey(),
                                                                cell.get(CELL_ID.name()).toString()).get(0)
                                                )
                                        )
                        )
                );

    }

    private SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> buildFactorTypeQuery(String experimentAccession) {
        var facetBuilder =
                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                        .setFacetField(FACTOR_NAME);

        return
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .setFacets(facetBuilder)
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
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, INFERRED_CELL_TYPE_SOLR_VALUE);

        return !singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults().isEmpty();
    }
}
