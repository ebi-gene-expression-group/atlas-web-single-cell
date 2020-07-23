package uk.ac.ebi.atlas.search.metadata;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataDao;

import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;

@Repository
public class CellMetadataSearchDao {
    private final CellMetadataDao cellMetadataDao;
    private SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public CellMetadataSearchDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory,
                                 CellMetadataDao cellMetadataDao) {
        this.singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
        this.cellMetadataDao = cellMetadataDao;
    }

    // Given a set of characteristic name (organism part) and characteristic value (pancreas),
    // this method retrieves the value of that metadata for list of cell IDs.
    @Cacheable(cacheNames = "cellIdsMetadata", key = "{#characteristicName, #characteristicValue}")
    public List<String> getCellTypeMetadata(String characteristicName,
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
                .map(cell -> cellMetadataDao.getMetadataValuesForCellId(
                        experimentAccession,
                        cell.get(CELL_ID.name()).toString(),
                        List.of(),
                        List.of("inferred_cell_type")).get("inferred_cell_type"))
                .collect(Collectors.toList());
    }
}