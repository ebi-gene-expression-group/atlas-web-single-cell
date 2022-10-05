package uk.ac.ebi.atlas.search.analytics;

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

@Component
public class AnalyticsSearchDao {

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public AnalyticsSearchDao(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        singleCellAnalyticsCollectionProxy =
                collectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    public ImmutableSet<String> searchFieldByCellIds(
            SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField schemaField,
            ImmutableSet<String> cellIDs) {
//        Streaming query for getting the organism_part provided by set of cell IDs
//        unique(
//            search(scxa-analytics-v6, q=cell_id:<SET_OF_CELL_IDS>,
//            fl="schemaField", // could be : ctw_organism_part, ctw_cell_type
//            sort="schemaField asc"
//            ),
//            over="schemaField"
//        )
        return getSchemaFieldFromStreamQuery(
                new UniqueStreamBuilder(
                        getStreamBuilderByCellIdsForSchemaField(cellIDs, schemaField),
                        schemaField.name()
                ),
                schemaField.name()
        );
    }

    private SearchStreamBuilder<SingleCellAnalyticsCollectionProxy> getStreamBuilderByCellIdsForSchemaField(
            ImmutableSet<String> cellIDs, SingleCellAnalyticsCollectionProxy.SingleCellAnalyticsSchemaField schemaField) {
        return new SearchStreamBuilder<>(
                singleCellAnalyticsCollectionProxy,
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(CELL_ID, cellIDs)
                        .setFieldList(schemaField)
                        .sortBy(schemaField, SolrQuery.ORDER.asc)
        ).returnAllDocs();
    }

    private ImmutableSet<String> getSchemaFieldFromStreamQuery(UniqueStreamBuilder uniqueOrganismPartStreamBuilder,
            String schemaField) {
        try (TupleStreamer tupleStreamer = TupleStreamer.of(uniqueOrganismPartStreamBuilder.build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(schemaField))
                    .collect(toImmutableSet()
            );
        }
    }
}
