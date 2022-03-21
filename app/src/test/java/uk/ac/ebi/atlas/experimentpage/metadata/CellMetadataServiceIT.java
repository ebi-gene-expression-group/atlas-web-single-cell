package uk.ac.ebi.atlas.experimentpage.metadata;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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

import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellMetadataServiceIT {
    // Ideally we would retrieve a random experiment accession, but not all experiments have metadata of interest
    // (i.e factors, inferred cell types and additional attributes in the IDF)
    private static final String EXPERIMENT_WITHOUT_METADATA_ACCESSION = "E-GEOD-99058";
    private static final ImmutableSet<String> INFERRED_CELL_TYPE_VALUES =
            ImmutableSet.of("inferred_cell_type_-_ontology_labels", "inferred_cell_type_-_authors_labels");

    @Inject
    private DataSource dataSource;

    @Inject
    private CellMetadataDao cellMetadataDao;

    @Inject
    private JdbcUtils jdbcUtils;

    private CellMetadataService subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/202203/experiment.sql"),
                new ClassPathResource("fixtures/202203/scxa_analytics.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/202203/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/202203/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.subject = new CellMetadataService(cellMetadataDao);
    }


    @ParameterizedTest
    @MethodSource("experimentsWithMetadataProvider")
    void metadataValuesForValidExperimentAccession(String experimentAccession) {
        var cellId = jdbcUtils.fetchRandomCellFromExperiment(experimentAccession);

        assertThat(subject.getMetadataValues(experimentAccession, cellId)).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("experimentsWithMetadataProvider")
    void experimentWithMetadataReturnsMetadataTypes(String experimentAccession) {
        assertThat(subject.getMetadataTypes(experimentAccession))
                .isNotEmpty()
                .anyMatch(INFERRED_CELL_TYPE_VALUES::contains);
    }

    @ParameterizedTest
    @MethodSource("experimentsWithMetadataProvider")
    void returnsMetadataValueForGivenMetadataType(String experimentAccession) {
        //assuming all experiments have inferred_cell_types as there metadata
        assertThat(INFERRED_CELL_TYPE_VALUES)
                .anyMatch(inferredCellTypeValue ->
                        !subject.getMetadataValuesForGivenType(experimentAccession, inferredCellTypeValue).isEmpty());
    }

    @Test
    void experimentWithoutMetadataReturnsEmptyMetadataTypes() {
        assertThat(subject.getMetadataTypes(EXPERIMENT_WITHOUT_METADATA_ACCESSION)).isEmpty();
    }

    @Test
    void metadataForInvalidExperiment() {
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(
                () -> subject.getMetadataValues("FOO", "FOO"))
                .withCauseInstanceOf(NoSuchFileException.class);;
    }

    private Iterable<String> experimentsWithMetadataProvider() {
        // E-GEOD-99058 does not have any metadata (factors or inferred cell type)
        return jdbcUtils.fetchPublicExperimentAccessions()
                .stream()
                .filter(accession -> !accession.equalsIgnoreCase(EXPERIMENT_WITHOUT_METADATA_ACCESSION))
                .collect(Collectors.toSet());
    }
}
