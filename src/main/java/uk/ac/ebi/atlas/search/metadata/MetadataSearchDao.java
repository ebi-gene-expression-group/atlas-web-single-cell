package uk.ac.ebi.atlas.search.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.download.FileDownloadController;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadController.class);
    private SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public MetadataSearchDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }


    private String getInferredCellTypeForCellId(String experimentAccession, String cellId) {
        var characteristicFields = "inferred_cell_type";
        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addQueryFieldByTerm(CELL_ID, cellId)
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, characteristicFields);
        var results = this.singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults();

        var inferredCellType = "";
        try {
            inferredCellType = results
                    .stream()
                    .map(entry -> entry.get(CHARACTERISTIC_VALUE.name()).toString())
                    .collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            LOGGER.debug("Invalid cell id: {}", cellId);
        }
        return inferredCellType;
    }

    // Given a set of characteristic name (organism part) and characteristic value (pancreas),
    // this method retrieves the value of that metadata for list of cell IDs.
    @Cacheable(cacheNames = "cellIdsMetadata", key = "{#characteristicName, #characteristicValue}")
    public Map<String, Map<String, String>> getCellTypeMetadata(String characteristicName,
                                                                String characteristicValue,
                                                                String experimentAccession) {

        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
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
                                                        cell -> getInferredCellTypeForCellId(
                                                                entry.getKey(),
                                                                cell.get(CELL_ID.name()).toString()
                                                        )
                                                )
                                        )
                        )
                );

    }
}