package uk.ac.ebi.atlas.search.organismpart;

import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;

@Component
public class OrganismPartSearchDao {

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public OrganismPartSearchDao(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        singleCellAnalyticsCollectionProxy =
                collectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    public ImmutableSet<String> searchOrganismPart(ImmutableSet<String> cellIDs) {
//        Streaming query for getting the organism_part provided by set of cell IDs
//        unique(
//            search(scxa-analytics-v6, q=cell_id:<SET_OF_CELL_IDS>,
//            fl="ctw_organism_part",
//            sort="ctw_organism_part asc"
//            ),
//            over="ctw_organism_part"
//        )
        return getOrganismPartFromStreamQuery(
                new UniqueStreamBuilder(getStreamBuilderForOrganismPartByCellIds(cellIDs), CTW_ORGANISM_PART.name()));
    }

    private SearchStreamBuilder<SingleCellAnalyticsCollectionProxy> getStreamBuilderForOrganismPartByCellIds(
            ImmutableSet<String> cellIDs) {
        return new SearchStreamBuilder<>(
                singleCellAnalyticsCollectionProxy,
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(CELL_ID, cellIDs)
                        .setFieldList(CTW_ORGANISM_PART)
                        .sortBy(CTW_ORGANISM_PART, SolrQuery.ORDER.asc)
        ).returnAllDocs();
    }

    private ImmutableSet<String> getOrganismPartFromStreamQuery(UniqueStreamBuilder uniqueOrganismPartStreamBuilder) {
        try (TupleStreamer tupleStreamer = TupleStreamer.of(uniqueOrganismPartStreamBuilder.build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(CTW_ORGANISM_PART.name()))
                    .collect(toImmutableSet()
            );
        }
    }
}
