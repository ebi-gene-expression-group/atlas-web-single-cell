package uk.ac.ebi.atlas.search.metadata;


import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;


@Component
public class MetadataSearchDao {

    private SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public MetadataSearchDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
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
}
