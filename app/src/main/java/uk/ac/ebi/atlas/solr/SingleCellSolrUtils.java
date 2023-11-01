package uk.ac.ebi.atlas.solr;

import com.google.common.collect.ImmutableSet;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;

import java.util.Arrays;
import java.util.Random;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;

@Component
public class SingleCellSolrUtils {

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    private static final int MAX_ROWS = 10000;

    public SingleCellSolrUtils(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    public ImmutableSet<String> fetchedRandomCellTypesByCellIDs(ImmutableSet<String> cellIDs, int numberOfCellTypes) {
        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> queryBuilder = new SolrQueryBuilder<>();
        queryBuilder
                .addQueryFieldByTerm(CELL_ID, cellIDs)
                .setFieldList(CTW_CELL_TYPE)
                .setRows(MAX_ROWS);

        return getRandomCellTypesFromQueryResult(
                singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults(),
                CTW_CELL_TYPE.name(),
                numberOfCellTypes);
    }

    public ImmutableSet<String> fetchedRandomOrganismPartsByCellIDs(ImmutableSet<String> cellIDs, int numberOfOrganismParts) {
        SolrQueryBuilder<SingleCellAnalyticsCollectionProxy> queryBuilder = new SolrQueryBuilder<>();
        queryBuilder
                .addQueryFieldByTerm(CELL_ID, cellIDs)
                .setFieldList(CTW_ORGANISM_PART)
                .setRows(MAX_ROWS);

        return getRandomCellTypesFromQueryResult(
                singleCellAnalyticsCollectionProxy.query(queryBuilder).getResults(),
                CTW_ORGANISM_PART.name(),
                numberOfOrganismParts);
    }


    private ImmutableSet<String> getRandomCellTypesFromQueryResult(
            SolrDocumentList solrDocumentList,
            String schemaFieldName,
            int numberOfCellTypes) {
        return Arrays.stream(new Random().ints(numberOfCellTypes, 0, solrDocumentList.size()).toArray())
                .mapToObj(index -> solrDocumentList.get(index).getFieldValue(schemaFieldName).toString())
                .collect(toImmutableSet());
    }
}
