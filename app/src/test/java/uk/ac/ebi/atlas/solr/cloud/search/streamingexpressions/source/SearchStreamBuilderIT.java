package uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;

import javax.inject.Inject;
import java.io.UncheckedIOException;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.solr.client.solrj.SolrQuery.ORDER.asc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.FACET_CHARACTERISTIC_NAME;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
// Some tests in this class are executed against the scxa-analytics collection, although the subject class is located
// in atlas-web-core
class SearchStreamBuilderIT {
    private static final int MAX_NUM_ROWS = 100;

    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    @BeforeEach
    void setUp() {
        singleCellAnalyticsCollectionProxy = collectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    @Test
    void returnAllRowsOverridesRowsParameterInSolrQuery() {
        var numRows = ThreadLocalRandom.current().nextInt(MAX_NUM_ROWS);
        var solrQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, "E-EHCA-2")
                        .setFieldList(ImmutableSet.of(EXPERIMENT_ACCESSION, CELL_ID, FACET_CHARACTERISTIC_NAME))
                        .setRows(numRows)
                        .sortBy(CELL_ID, asc);

        var searchStreamBuilder = new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, solrQueryBuilder);
        // SolrJ 8.7 still exhibits this weird behaviour: https://issues.apache.org/jira/browse/SOLR-12510
        // Rather than disabling the test, let’s skip the assumption that rows works as a sane person would expect
        // try (var tupleStreamer = TupleStreamer.of(searchStreamBuilder.build())) {
        //     assertThat(tupleStreamer.get().count()).isEqualTo(numRows);
        // }
        var allDocsSearchStreamBuilder =
                new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, solrQueryBuilder).returnAllDocs();

        try (var tupleStreamer = TupleStreamer.of(searchStreamBuilder.build());
             var allDocsStreamer = TupleStreamer.of(allDocsSearchStreamBuilder.build())) {
            assertThat(allDocsStreamer.get().count()).isGreaterThan(tupleStreamer.get().count());
        }
    }

    @Test
    void throwsIfFieldListContainsMultivaluedFieldWithoutDocValuesWhenReturningAllFields() {
        var numRows = ThreadLocalRandom.current().nextInt(MAX_NUM_ROWS);
        var solrQueryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(EXPERIMENT_ACCESSION, "E-EHCA-2")
                        .setFieldList(ImmutableSet.of(EXPERIMENT_ACCESSION, CHARACTERISTIC_NAME))
                        .setRows(numRows)
                        .sortBy(EXPERIMENT_ACCESSION, asc);

        var searchStreamBuilder = new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, solrQueryBuilder);
        // No exceptions down here...
        TupleStreamer.of(searchStreamBuilder.build());

        var allDocsSearchStreamBuilder =
                new SearchStreamBuilder<>(singleCellAnalyticsCollectionProxy, solrQueryBuilder).returnAllDocs();
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> TupleStreamer.of(allDocsSearchStreamBuilder.build()));
    }
}
