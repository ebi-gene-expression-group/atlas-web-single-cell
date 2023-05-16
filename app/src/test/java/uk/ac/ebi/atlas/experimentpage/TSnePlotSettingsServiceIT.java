package uk.ac.ebi.atlas.experimentpage;

import com.sun.management.UnixOperatingSystemMXBean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGenesDao;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotDao;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TSnePlotSettingsServiceIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcTestUtils;

    @Inject
    private DataFileHub dataFileHub;

    @Inject
    private IdfParser idfParser;

    @Inject
    private TSnePlotDao tSnePlotDao;

    @Inject
    private MarkerGenesDao markerGenesDao;

    private TSnePlotSettingsService subject;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/scxa_analytics.sql"),
                new ClassPathResource("fixtures/scxa_dimension_reduction.sql"),
                new ClassPathResource("fixtures/scxa_coords.sql"),
                new ClassPathResource("fixtures/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/scxa_coords-delete.sql"),
                new ClassPathResource("fixtures/scxa_dimension_reduction-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.subject = new TSnePlotSettingsService(dataFileHub, idfParser, tSnePlotDao, markerGenesDao);
    }

    @Test
    void getClustersForValidAccession() {
        List<Integer> result = subject.getAvailableKs(jdbcTestUtils.fetchRandomExperimentAccession());

        assertThat(result)
                .isNotEmpty()
                .doesNotHaveDuplicates();
    }

    @Test()
    void getClustersForInvalidAccessionThrowsException() {
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> subject.getAvailableKs("FOO"));
    }

    @Test
    void getPerplexitiesForValidAccession() {
        List<Integer> result =
                subject.getAvailablePerplexities(jdbcTestUtils.fetchRandomExperimentAccession());

        assertThat(result)
                .isNotEmpty()
                .doesNotHaveDuplicates();
    }

    // This is not a good test: the JVM may do things in the background we’re not aware of, or manage sockets or TCP
    // connections differently depending on the OS or other environment-specific factors, and can end up having more
    // open files than expected. However, because the clusters TSV file is accessed as a field without a getter, we
    // can’t mock the following call sequence and verify that TsvStreamer::close has been called:
    // dataFileHub.getSingleCellExperimentFiles(experimentAccession).clustersTsv.get().get()
    // The +1 magic number accounts for open DB connections in the build environment, in my laptop it’s +4.
    // This is the next best thing I could come up with... sorry! :(
    @Disabled // I added this annotation for now to turn this test of as we discussed this on the stand-up meeting
    @ParameterizedTest
    @MethodSource("randomSingleCellExperimentAccessionProvider")
    void filesClosed(String experimentAccession) {
        long fileDescriptorsOpenBefore = getOpenFileCount();
        subject.getAvailableKs(experimentAccession);
        subject.getExpectedClusters(experimentAccession);
        long fileDescriptorsOpenAfter = getOpenFileCount();

        assertThat(fileDescriptorsOpenAfter)
                .isEqualTo(fileDescriptorsOpenBefore);
    }

    @ParameterizedTest
    @MethodSource("randomSingleCellExperimentAccessionProvider")
    void getTSnePlotTypesAndOptionsForValidAccession(String experimentAccession, String plotMethod) {
        var tsnePlotTypesAndOptions = subject.getAvailablePlotTypesAndPlotOptions(experimentAccession);
        assertThat(tsnePlotTypesAndOptions.get(plotMethod)).isNotEmpty().doesNotHaveDuplicates();
    }

    @Test
    void getEmptyTSnePlotTypesAndOptionsForInvalidAccession() {
        assertThat(subject.getAvailablePlotTypesAndPlotOptions("Foo")).isEmpty();
    }

    // https://stackoverflow.com/questions/16360720/how-to-find-out-number-of-files-currently-open-by-java-application
    private long getOpenFileCount() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        } else {
            return -1;
        }
    }

    private Stream<Arguments> randomSingleCellExperimentAccessionProvider() {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var plotMethod = jdbcTestUtils.fetchRandomPlotMethod(experimentAccession);
        return Stream.of(Arguments.of(experimentAccession, plotMethod));
    }
}
