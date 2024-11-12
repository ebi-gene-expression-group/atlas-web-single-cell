package uk.ac.ebi.atlas.solr;

import com.google.common.collect.ImmutableSet;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // TODO: only for debugging on our CI - Please remove before merging this PR!!!!!
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCellSolrUtils.class);

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
        LOGGER.info("Solr Document list: {}", solrDocumentList.toString());
        return Arrays.stream(new Random().ints(numberOfCellTypes, 0, solrDocumentList.size()).toArray())
                .mapToObj(index -> solrDocumentList.get(index).getFieldValue(schemaFieldName).toString())
                .collect(toImmutableSet());
    }
}
