package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;

@Repository
public class HcaHumanExperimentDao {
    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public HcaHumanExperimentDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy = solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    public ImmutableSet<String> fetchExperimentAccessions(String characteristicName,
                                                          Set<String> characteristicValues) {
        var queryBuilder = new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>();
        /* Reason for checking blank here is in case characteristicValue is empty our solrQueryBuilder
         * builds query like characteristic_value:("\*") which is different from characteristic_value:*
         * which we need. So to handle that situation if characteristicValue is empty we build query
         * characteristic_name:("organism_part") as we need all the experiments in case of empty characteristicValue
         * */

        queryBuilder.addQueryFieldByTerm(CHARACTERISTIC_NAME, characteristicName)
                .setFieldList(EXPERIMENT_ACCESSION)
                .sortBy(EXPERIMENT_ACCESSION, SolrQuery.ORDER.asc)
                .setRows(SolrQueryBuilder.DEFAULT_ROWS);

        if(!characteristicValues.isEmpty()) {
            queryBuilder.addQueryFieldByTerm(CHARACTERISTIC_VALUE, characteristicValues);
        }

        var humanExperimentsSearchStream = new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, queryBuilder);

        var uniqueHumanExperimentsStream = new UniqueStreamBuilder(humanExperimentsSearchStream, EXPERIMENT_ACCESSION.name());

        try(TupleStreamer tupleStreamer = TupleStreamer.of(uniqueHumanExperimentsStream.build())) {
            return (tupleStreamer.get()
                    .map(tuple -> (String) tuple.get(EXPERIMENT_ACCESSION.name()))
                    .collect(toImmutableSet()));
        }
    }
}
