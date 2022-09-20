package uk.ac.ebi.atlas.search.organismpart;

import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.search.GeneSearchDao;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import java.util.HashSet;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;

@Component
public class OrganismPartSearchDao {

    private final GeneSearchDao geneSearchDao;

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public OrganismPartSearchDao(SolrCloudCollectionProxyFactory collectionProxyFactory,
                                 GeneSearchDao geneSearchDao) {
        singleCellAnalyticsCollectionProxy =
                collectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
        this.geneSearchDao = geneSearchDao;
    }

    public Optional<ImmutableSet<String>> searchOrganismPart(ImmutableSet<String> geneIds) {
        if (geneIds.isEmpty()) {
            return Optional.of(ImmutableSet.of());
        }

        var cellIDs = getCellIdsFromGeneIds(geneIds);

        if (cellIDs.isEmpty()) {
            return Optional.of(ImmutableSet.of());
        }

//        Streaming query for getting the organism_part provided by set of cell IDs
//        unique(
//            search(scxa-analytics-v6, q=cell_id:<SET_OF_CELL_IDS>,
//            fl="ctw_organism_part",
//            sort="ctw_organism_part asc"
//            ),
//            over="ctw_organism_part"
//        )
        var searchAnalyticsQueryBuilder =
                getAnalyticsQueryBuilderByCellIds(cellIDs);
        var uniqueOrganismPartStreamBuilder =
                new UniqueStreamBuilder(searchAnalyticsQueryBuilder, CTW_ORGANISM_PART.name());

        return getOrganismPartFromStreamQuery(uniqueOrganismPartStreamBuilder);
    }

    @NotNull
    private HashSet<String> getCellIdsFromGeneIds(ImmutableSet<String> geneIds) {
        var cellIds = new HashSet<String>();
        for (var geneId : geneIds) {
            var cellIdsByGeneID = geneSearchDao.fetchCellIds(geneId);
            cellIdsByGeneID.values().forEach(cellIds::addAll);
        }
        return cellIds;
    }

    private SearchStreamBuilder<SingleCellAnalyticsCollectionProxy> getAnalyticsQueryBuilderByCellIds(
            HashSet<String> cellIDs) {
        return new SearchStreamBuilder<>(
                singleCellAnalyticsCollectionProxy,
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(CELL_ID, cellIDs)
                        .setFieldList(CTW_ORGANISM_PART)
                        .sortBy(CTW_ORGANISM_PART, SolrQuery.ORDER.asc)
        ).returnAllDocs();
    }

    private Optional<ImmutableSet<String>> getOrganismPartFromStreamQuery(UniqueStreamBuilder uniqueOrganismPartStreamBuilder) {
        try (TupleStreamer tupleStreamer = TupleStreamer.of(uniqueOrganismPartStreamBuilder.build())) {
            return Optional.of(tupleStreamer.get()
                    .map(tuple -> tuple.getString(CTW_ORGANISM_PART.name()))
                    .collect(toImmutableSet())
            );
        }
    }
}
