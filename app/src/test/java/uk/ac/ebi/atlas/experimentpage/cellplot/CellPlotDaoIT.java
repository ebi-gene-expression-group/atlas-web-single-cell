package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellPlotDaoIT {
    private static final Random RNG = ThreadLocalRandom.current();

    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcTestUtils;

    @Inject
    private CellPlotDao subject;

    public ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

    @BeforeAll
    void populateDatabaseTables() {
        populator.setScripts(
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/scxa_analytics.sql"),
                new ClassPathResource("fixtures/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership.sql"),
                new ClassPathResource("fixtures/scxa_dimension_reduction.sql"),
                new ClassPathResource("fixtures/scxa_coords.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        populator.setScripts(
                new ClassPathResource("fixtures/scxa_coords-delete.sql"),
                new ClassPathResource("fixtures/scxa_dimension_reduction-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }


    @Test
    void fetchCellPlotWithK() {
        assertThat(
                subject.fetchCellPlotWithK("E-ENAD-53", 14, "umap", Map.of("n_neighbors", 15)))
                .isNotEmpty()
                .doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionPlotWithKProvider")
    void fetchCellPlotWithKWithInvalidExperimentAccessionReturnsEmpty(String experimentAccession,
                                                                      int k,
                                                                      String plotMethod,
                                                                      Map<String, Integer> parameterisation) {
        assertThat(
                subject.fetchCellPlotWithK(
                        generateRandomExperimentAccession(),
                        k,
                        plotMethod,
                        parameterisation))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionPlotWithKProvider")
    void fetchCellPlotWithKWithInvalidPlotMethodReturnsEmpty(String experimentAccession,
                                                             int k,
                                                             String plotMethod,
                                                             Map<String, Integer> parameterisation) {
        assertThat(
                subject.fetchCellPlotWithK(experimentAccession, k, randomAlphabetic(4), parameterisation))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionPlotWithKProvider")
    void fetchCellPlotWithKWithInvalidParameterisationReturnsEmpty(String experimentAccession,
                                                                   int k,
                                                                   String plotMethod,
                                                                   Map<String, Integer> parameterisation) {
        assertThat(
                subject.fetchCellPlotWithK(
                        experimentAccession,
                        k,
                        randomAlphabetic(4),
                        ImmutableMap.of(randomAlphabetic(10), RNG.nextInt())))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionPlotWithKProvider")
    void throwsIfInvalidKIsRequested(String experimentAccession,
             int k,
             String plotMethod,
             Map<String, Integer> parameterisation) {
        assertThatNullPointerException().isThrownBy(
                () -> subject.fetchCellPlotWithK(experimentAccession, RNG.nextInt(), plotMethod, parameterisation));
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionPlotWithKProvider")
    void fetchCellPlot(String experimentAccession,
                       // We can reuse the same provider but we must have a place for the unused parameter
                       int k,
                       String plotMethod,
                       Map<String, Integer> parameterisation) {
        assertThat(
                subject.fetchCellPlot(experimentAccession, plotMethod, parameterisation))
                .isNotEmpty()
                .doesNotHaveDuplicates();
    }


    @ParameterizedTest
    @MethodSource("randomExperimentAccessionPlotWithGeneIdProvider")
    void fetchExpressionPlot(String experimentAccession,
                             // We can reuse the same provider but we must have a place for the unused parameter
                             String geneId,
                             String plotMethod,
                             Map<String, Integer> parameterisation) {
        assertThat(
                subject.fetchCellPlotWithExpression(experimentAccession, geneId, plotMethod, parameterisation))
                .isNotEmpty()
                .doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionPlotWithGeneIdProvider")
    void fetchExpressionPlotWithInvalidGeneIdHasNoExpression(String experimentAccession,
                                                             String geneId,
                                                             String plotMethod,
                                                             Map<String, Integer> parameterisation) {
        assertThat(
                subject.fetchCellPlotWithExpression(
                        experimentAccession,
                        generateRandomEnsemblGeneId(),
                        plotMethod,
                        parameterisation))
                .allSatisfy(dto -> assertThat(dto).hasFieldOrPropertyWithValue("expressionLevel", 0.0));
    }

    @Test
    void fetchDefaultPlotMethodAndParameterisationForTheExistingExperiment() {
        var defaultPlotMethodResult = subject.fetchDefaultPlotMethodWithParameterisation(
                jdbcTestUtils.fetchExperimentAccessionByMaxPriority());

        assertThat(defaultPlotMethodResult)
                .isNotEmpty();
    }

    @Test
    void fetchEmptyResultsIfExperimentDoesNotHaveDefaultPlotMethod(){
        assertThat(subject.fetchDefaultPlotMethodWithParameterisation("fooBar"))
                .isEmpty();
    }

    private Stream<Arguments> randomExperimentAccessionPlotWithKProvider() {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var k = 0;
        while (k == 0) {
            try {
                k = Integer.parseInt(jdbcTestUtils.fetchRandomVariableAndValue(experimentAccession).getLeft());
            } catch (NumberFormatException e) {
                // Keep trying until we have a valid k, remember we could get inferred cell type as variable
            }
        }
        var plotMethod = jdbcTestUtils.fetchRandomPlotMethod(experimentAccession);
        var parameterisation = jdbcTestUtils.fetchRandomParameterisation(experimentAccession, plotMethod);
        return Stream.of(Arguments.of(experimentAccession, k, plotMethod, parameterisation));
    }

    private Stream<Arguments> randomExperimentAccessionPlotWithGeneIdProvider() {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var geneId = jdbcTestUtils.fetchRandomGeneFromSingleCellExperiment(experimentAccession);
        var plotMethod = jdbcTestUtils.fetchRandomPlotMethod(experimentAccession);
        var parameterisation = jdbcTestUtils.fetchRandomParameterisation(experimentAccession, plotMethod);
        return Stream.of(Arguments.of(experimentAccession, geneId, plotMethod, parameterisation));
    }
}
