package uk.ac.ebi.atlas.metadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParserOutput;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryResponseUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_INFERRED_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACTORS;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACTOR_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.attributeNameToFieldName;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.characteristicAsSchemaField;
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
    public List<SingleCellAnalyticsSchemaField> getFactorTypes(String experimentAccession, Optional<String> cellId) {
        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .setFacetField(FACTOR_NAME)
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
                .map(SingleCellAnalyticsCollectionProxy::factorAsSchemaField) // TODO: This is no longer a schema field, rather a value. Should come up with some other mechanism
                .collect(Collectors.toList());

    }

    // Retrieves a list of additional attributes (i.e. characteristics of interest) from the experiment idf file
    public List<SingleCellAnalyticsSchemaField> getAdditionalAttributesFieldNames(String experimentAccession) {
        IdfParserOutput idfParserOutput = idfParser.parse(experimentAccession);

        if (idfParserOutput.getMetadataFieldsOfInterest().isEmpty()) {
            return emptyList();
        }

        return idfParserOutput.getMetadataFieldsOfInterest()
                .stream()
                .map(attribute -> characteristicAsSchemaField(attributeNameToFieldName(attribute)))
                .collect(toList());
    }

    // Retrieves all the available factors stored in the Solr scxa-analytics collection for a particular cell
    public List<SingleCellAnalyticsSchemaField> getFactorFieldNames(String experimentAccession, String cellId) {

        Map<String, Collection<Object>> queryResult =
                getQueryResultForMultiValueFields(experimentAccession, Optional.of(cellId), ImmutableSet.of(FACTORS));

        return queryResult.getOrDefault(FACTORS.name(), Collections.emptyList())
                .stream()
                .filter(factor -> !factor.toString().equalsIgnoreCase("single_cell_identifier"))
                .map(factor -> SingleCellAnalyticsCollectionProxy.factorAsSchemaField(factor.toString()))
                .collect(toList());
    }

    // Returns Solr query results for a list of multi-value fields of interest
    public ImmutableMap<String, Collection<Object>> getQueryResultForMultiValueFields(String experimentAccession,
                                      Optional<String> cellId,
                                      Collection<SingleCellAnalyticsSchemaField> fieldsOfInterest) {
        if (fieldsOfInterest.isEmpty()) {
            return ImmutableMap.of();
        }

        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> solrQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .setFieldList(fieldsOfInterest)
                        .setRows(1);

        cellId.ifPresent(s -> solrQueryBuilder.addQueryFieldByTerm(CELL_ID, s));

        QueryResponse queryResponse = this.singleCellAnalyticsCollectionProxy.query(solrQueryBuilder);

        if (queryResponse.getResults().isEmpty()) {
            return ImmutableMap.of();
        }

        // The map created by getFieldValuesMap has, for a reason I can’t grasp, quite a few unsupported operations.
        // Among them entrySet, which is the basis for all streaming methods in maps and forEach D:
        SolrDocument solrDocument = queryResponse.getResults().get(0);
        return solrDocument.getFieldNames().stream()
                .collect(toImmutableMap(Function.identity(), solrDocument::getFieldValues));
    }

    // Given a Solr field where metadata is stored, this method retrieves the value of that field for a cell ID.
    public Optional<String> getMetadataValueForCellId(String experimentAccession,
                                                      SingleCellAnalyticsSchemaField metadataField,
                                                      String cellId) {
        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> solrQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CELL_ID, cellId)
                        .setFieldList(metadataField);
        QueryResponse queryResponse = this.singleCellAnalyticsCollectionProxy.query(solrQueryBuilder);

        if (queryResponse.getResults().isEmpty() ||
                !queryResponse.getResults().get(0).containsKey(metadataField.name())) {
            return Optional.empty();
        }

        return Optional.of((String) queryResponse.getResults().get(0).getFirstValue(metadataField.name()));
    }

    // Given a Solr field where metadata is stored, this method retrieves the value of that field for list of cell IDs.
    public Map<String, String> getMetadataValueForCellIds(String experimentAccession,
                                                          SingleCellAnalyticsSchemaField metadataField,
                                                          List<String> cellIds) {
        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> solrQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CELL_ID, cellIds)
                        .setFieldList(ImmutableSet.of(metadataField, CELL_ID));

        QueryResponse response = singleCellAnalyticsCollectionProxy.query(solrQueryBuilder);

        return response.getResults()
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
                                entry -> (String) ((ArrayList) entry
                                                .getValue().get(0).getFieldValue(metadataField.name()))
                                                .get(0)));
    }
}
