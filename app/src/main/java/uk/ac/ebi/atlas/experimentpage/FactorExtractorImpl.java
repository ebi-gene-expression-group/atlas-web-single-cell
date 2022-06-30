package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.UniqueStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.apache.solr.client.solrj.SolrQuery.ORDER.asc;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.*;

@Component
public class FactorExtractorImpl implements FactorExtractor {
    // Implements the following query to get the factors of an experiment:
    // curl --data-urlencode 'expr=
    // unique(
    //   search(scxa-analytics-v6,
    //     qt="/export",
    //     q="experiment_accession:E-MTAB-8142",
    //     fl="facet_factor_name",
    //     sort="facet_factor_name asc"),
    //   over="facet_factor_name")
    // ' "http://wp-np2-85:8983/solr/scxa-analytics-v6/stream"
    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public FactorExtractorImpl(SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.singleCellAnalyticsCollectionProxy = solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    @Override
    public ImmutableSet<String> getFactorHeaders(String experimentAccession) {
        var solrQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .sortBy(FACET_FACTOR_NAME, asc)
                        .setFieldList(FACET_FACTOR_NAME);
        var searchStreamBuilder =
                new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, solrQueryBuilder).returnAllDocs();
        var uniqueStreamBuilder =
                new UniqueStreamBuilder(searchStreamBuilder, FACET_FACTOR_NAME.name());

        try (var tupleStreamer = TupleStreamer.of(uniqueStreamBuilder.build())) {
            return tupleStreamer
                    .get()
                    .filter(tuple -> !tuple.fields.isEmpty())   // Because some documents may not have factors
                    .map(tuple -> tuple.getString(FACET_FACTOR_NAME.name()))
                    .map(factorName -> factorName.replaceAll("_", " "))
                    .collect(toImmutableSet());
        }
    }
}
