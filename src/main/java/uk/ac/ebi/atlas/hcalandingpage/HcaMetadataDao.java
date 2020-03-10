package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.InnerJoinStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.ReducerStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.SelectStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.FacetStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import java.util.ArrayList;
import java.util.HashMap;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACET_CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.ONTOLOGY_ANNOTATION;

@Component
public class HcaMetadataDao {
    private final static String ORGANISM = "organism";
    private final static String ORGANISM_PART = "organism_part";
    private final static String HOMO_SAPIENS = "Homo sapiens";

    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public HcaMetadataDao(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy = solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }
    /*
     curl http://wp-np2-85:8983/solr/scxa-analytics-v3/stream -d 'expr=
innerJoin(
    unique(
        search(
        scxa-analytics,
        qt="/export",
        q="characteristic_name:organism AND characteristic_value:\"Homo sapiens\"",
        fl="experiment_accession",
        sort="experiment_accession asc"),
    over="experiment_accession"),
    select(
        reduce(
            select(
                facet(
                scxa-analytics,
                q="characteristic_name:organism_part",
                buckets="experiment_accession,facet_characteristic_value,ontology_annotation",
                bucketSorts="experiment_accession asc",
                bucketSizeLimit=1000,
                count(*)),
                experiment_accession,
                facet_characteristic_value,
                ontology_annotation),
            by="experiment_accession",
            group(sort="facet_characteristic_value asc", n="10")),
        experiment_accession,
        group),
    on="experiment_accession")
'

function below is mimicking the behavior of the above query

*/
    public ImmutableSet<ArrayList<HashMap<String, String>>> fetchHumanExperimentAccessionsAndAssociatedOntologyIds() {
        var humanExperiments =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, ORGANISM)
                        .addQueryFieldByTerm(CHARACTERISTIC_VALUE, HOMO_SAPIENS)
                        .setFieldList(EXPERIMENT_ACCESSION)
                        .sortBy(EXPERIMENT_ACCESSION, SolrQuery.ORDER.asc);

        var humanExperimentsSearchStream = new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, humanExperiments);
        var uniqueHumanExperimentsStream = new UniqueStreamBuilder(humanExperimentsSearchStream, EXPERIMENT_ACCESSION.name());

        var organismPartQuery = new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                .addQueryFieldByTerm(CHARACTERISTIC_NAME, ORGANISM_PART);
        var organismPartFacetStream = new FacetStreamBuilder<>(
                singleCellAnalyticsCollectionProxy,
                ImmutableList.of(
                        EXPERIMENT_ACCESSION,
                        FACET_CHARACTERISTIC_VALUE,
                        ONTOLOGY_ANNOTATION))
                .withQuery(organismPartQuery.build())
                .sortByAscending(EXPERIMENT_ACCESSION)
                .withCounts();

        var selectStream = new SelectStreamBuilder(
                organismPartFacetStream,
                ImmutableList.of(
                        EXPERIMENT_ACCESSION.name(),
                        FACET_CHARACTERISTIC_VALUE.name(),
                        ONTOLOGY_ANNOTATION.name()));

        var organismPartReduceStream = new ReducerStreamBuilder(
                selectStream,
                EXPERIMENT_ACCESSION.name(),
                FACET_CHARACTERISTIC_VALUE.name(),
                10);

        var organismPartSelectStream = new SelectStreamBuilder(
                organismPartReduceStream,
                ImmutableList.of(
                        EXPERIMENT_ACCESSION.name(),
                        "group"));

        var result = new InnerJoinStreamBuilder(
                uniqueHumanExperimentsStream,
                organismPartSelectStream,
                EXPERIMENT_ACCESSION.name());

        try (TupleStreamer tupleStreamer = TupleStreamer.of(result.build())) {
            return tupleStreamer.get()
                    .map(tuple -> (ArrayList<HashMap<String, String>>) tuple.get("group"))
                    .collect(toImmutableSet());
        }
    }
}