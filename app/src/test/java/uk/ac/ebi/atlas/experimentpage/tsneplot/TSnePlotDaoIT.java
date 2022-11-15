package uk.ac.ebi.atlas.experimentpage.tsneplot;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.List;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TSnePlotDaoIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private Path dataFilesPath;

    @Inject
    private Path experimentDesignDirPath;

    @Inject
    private JdbcUtils jdbcTestUtils;

    @Inject
    private TSnePlotDao subject;

    public ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

    @BeforeAll
    void populateDatabaseTables() {
        populator.setScripts(
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/scxa_analytics.sql"),
                new ClassPathResource("fixtures/scxa_coords.sql"),
                new ClassPathResource("fixtures/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        populator.setScripts(
                new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/scxa_coords-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionAndPerplexityProvider")
    void testExpression(String experimentAccession, int perplexity) {
        var geneId = jdbcTestUtils.fetchRandomGeneFromSingleCellExperiment(experimentAccession);

        assertThat(subject.fetchTSnePlotWithExpression(experimentAccession, "tsne", perplexity, geneId))
                .isNotEmpty()
                .doesNotHaveDuplicates()
                .allMatch(tSnePointDto -> tSnePointDto.expressionLevel() >= 0.0)
                .extracting("name")
                .isSubsetOf(fetchCellIds(experimentAccession));
    }

    // TODO Re-think this test with scxa_coords
    // @ParameterizedTest
    // @MethodSource("randomExperimentAccessionKAndPerplexityProvider")
    void testClustersForK(String experimentAccession, String plotType, int plotOption, int perplexity) {
        assertThat(subject.fetchTSnePlotWithClusters(experimentAccession, plotType, perplexity, String.valueOf(plotOption)))
                .isNotEmpty()
                .doesNotHaveDuplicates()
                .allMatch(tSnePointDto -> Integer.valueOf(tSnePointDto.clusterId() )<= plotOption)
                .extracting("name")
                .isSubsetOf(fetchCellIds(experimentAccession));
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionAndPerplexityProvider")
    void testClustersForPerplexity(String experimentAccession, int perplexity) {
        assertThat(subject.fetchTSnePlotForPerplexity(experimentAccession, perplexity))
                .isNotEmpty()
                .doesNotHaveDuplicates()
                .extracting("name")
                .isSubsetOf(fetchCellIds(experimentAccession));
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionProvider")
    void testPerplexities(String experimentAccession) {
        assertThat(subject.fetchPerplexities(experimentAccession))
                .isNotEmpty()
                .doesNotHaveDuplicates();
    }

    // In this test, we test the count of cells. To make a comprehensive test we count the lines of the local file to
    // match the return result by querying in the fixture.
    // If the fixture is a partition of the full dataset, then it will fail, so we load a full test dataset.
    // TODO Re-think this test with scxa_coords
    // @ParameterizedTest
    // @MethodSource("randomExperimentAccessionProvider")
    void testNumberOfCellsByExperimentAccession(String experimentAccession) {
        cleanDatabaseTables();
        //populator.setScripts(new ClassPathResource("fixtures/scxa_tsne-full.sql"));
        populator.execute(dataSource);
        var resource =
                new DataFileHub(dataFilesPath.resolve("scxa"), experimentDesignDirPath)
                        .getSingleCellExperimentFiles(experimentAccession)
                        .tSnePlotTsvs;
        var firstFile = resource.entrySet().iterator().next();
        var fileContent = firstFile.getValue().get().get();
        var fileContentLines = Math.toIntExact(fileContent.count());
        var numberOfcells = subject.fetchNumberOfCellsByExperimentAccession(experimentAccession);
        assertThat(numberOfcells)
                .isEqualTo(fileContentLines-1);
        cleanDatabaseTables();
        populateDatabaseTables();
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionProvider")
    void getTSnePlotTypesAndOptions(String experimentAccession) {
        var tsnePlotTypesAndOptions = subject.fetchTSnePlotTypesAndOptions(experimentAccession);
        assertThat(tsnePlotTypesAndOptions.get("umap")).isNotEmpty().doesNotHaveDuplicates();
        assertThat(tsnePlotTypesAndOptions.get("tsne")).isNotEmpty().doesNotHaveDuplicates();
    }

    @Test
    void getEmptyTSnePlotTypesAndOptionsWithWrongExperimentAccession() {
        assertThat(subject.fetchTSnePlotTypesAndOptions("Foo")).isEmpty();
    }

    private static final String SELECT_CELL_IDS_STATEMENT =
            "SELECT DISTINCT(cell_id) FROM scxa_analytics WHERE experiment_accession=?";
    private List<String> fetchCellIds(String experimentAccession) {
        return jdbcTemplate.queryForList(SELECT_CELL_IDS_STATEMENT, String.class, experimentAccession);
    }

    private Stream<String> randomExperimentAccessionProvider() {
        return Stream.of(jdbcTestUtils.fetchRandomExperimentAccession());
    }

    private Stream<Arguments> randomExperimentAccessionAndPerplexityProvider() {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var perplexity = jdbcTestUtils.fetchRandomPerplexityFromExperimentTSne(experimentAccession);

        return Stream.of(Arguments.of(experimentAccession, perplexity));
    }

    // TODO Re-think this provider with scxa_coords
    private Stream<Arguments> randomExperimentAccessionKAndPerplexityProvider() {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var k = jdbcTestUtils.fetchRandomKFromCellClusters(experimentAccession);
        var perplexity = jdbcTestUtils.fetchRandomPerplexityFromExperimentTSne(experimentAccession);

        return Stream.of(Arguments.of(experimentAccession, k, perplexity));
    }
}
