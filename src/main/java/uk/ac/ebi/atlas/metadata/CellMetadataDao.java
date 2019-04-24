package uk.ac.ebi.atlas.metadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParserOutput;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.jsonfacets.SolrJsonFacetBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
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

    public CellMetadataDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory,
                           IdfParser idfParser) {
        this.singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
        this.idfParser = idfParser;
    }

    // Returns a list of available factor types for a given experiment (and optionally, a cell ID).
    // Note: it is possible that a cell has missing factors...
    public Set<String> getFactorTypes(String experimentAccession, Optional<String> cellId) {
        SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy> facetBuilder =
                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                    .setFacetField(FACTOR_NAME);

        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .setFacets(facetBuilder)
                        .setRows(0);

        cellId.ifPresent(id -> queryBuilder.addQueryFieldByTerm(CELL_ID, id));

        List<SimpleOrderedMap> results =
                extractSimpleOrderedMaps(
                        singleCellAnalyticsCollectionProxy
                                .query(queryBuilder)
                                .getResponse()
                                .findRecursive("facets", FACTOR_NAME.name(), "buckets"));

        return results
                .stream()
                .map(x -> x.get("val").toString())
                .filter(factor -> !factor.equalsIgnoreCase("single_cell_identifier"))
                .collect(Collectors.toSet());
    }

    // Retrieves a list of additional attributes (i.e. characteristics of interest) from the experiment idf file
    public Set<String> getCharacteristicTypes(String experimentAccession) {
        IdfParserOutput idfParserOutput = idfParser.parse(experimentAccession);

        Set<String> characteristics = new HashSet<>();
        // We are ALWAYS interested in the inferred cell type
        if (hasInferredCellType(experimentAccession)) {
            characteristics.add("inferred_cell_type");
        }

        if (idfParserOutput.getMetadataFieldsOfInterest().isEmpty()) {
            return characteristics;
        }

        Set<String> characteristicsFromIdf = idfParserOutput.getMetadataFieldsOfInterest()
                .stream()
                .map(SingleCellAnalyticsCollectionProxy::attributeNameToFieldName)
                .collect(toSet());

        characteristics.addAll(characteristicsFromIdf);

        return characteristics;
    }

    public Map<String, String> getMetadataValuesForCellId(String experimentAccession,
                                                   String cellId,
                                                   Collection<String> factorFields,
                                                   Collection<String> characteristicFields) {
        if (factorFields.isEmpty() && characteristicFields.isEmpty()) {
            return ImmutableMap.of();
        }

        ImmutableMap<SingleCellAnalyticsSchemaField, Collection<String>> fields = ImmutableMap.of(
                CHARACTERISTIC_NAME, characteristicFields,
                FACTOR_NAME, factorFields);

        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CELL_ID, cellId)
                        .addQueryFieldByTerm(fields)
                        .setFieldList(Arrays.asList(CHARACTERISTIC_NAME, CHARACTERISTIC_VALUE, FACTOR_NAME, FACTOR_VALUE));

        SolrDocumentList results = this.singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults();

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
                                        entry.get(CHARACTERISTIC_VALUE.name()))).get(0).toString()
                ));

    }

    // Given a type of metadata, this method retrieves the value of that metadata for list of cell IDs.
    public Map<String, String> getMetadataValueForCellIds(String experimentAccession,
                                                             String metadataType,
                                                             Collection<String> cellIds) {
        // We need to do this because we don't know if the metadata type is a factor or a characteristic
        ImmutableMap<SingleCellAnalyticsSchemaField, Collection<String>> fields = ImmutableMap.of(
                CHARACTERISTIC_NAME, ImmutableSet.of(metadataType),
                FACTOR_NAME, ImmutableSet.of(metadataType));

        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CELL_ID, cellIds)
                        .addQueryFieldByTerm(fields)
                        .setFieldList(ImmutableSet.of(CELL_ID, CHARACTERISTIC_VALUE, FACTOR_VALUE));

        SolrDocumentList results = this.singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults();

        return results
                .stream()
                .collect(groupingBy(solrDocument -> (String) solrDocument.getFieldValue("cell_id")))
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

    private boolean hasInferredCellType(String experimentAccession) {
        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, "inferred_cell_type");

        return !singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults().isEmpty();
    }
}
