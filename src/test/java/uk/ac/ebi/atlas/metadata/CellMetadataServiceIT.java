package uk.ac.ebi.atlas.metadata;

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

    @Inject
    private DataSource dataSource;

    @Inject
    private CellMetadataDao cellMetadataDao;
    @Inject
    private JdbcUtils jdbcUtils;

    private CellMetadataService subject;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_experiment-fixture.sql"),
                new ClassPathResource("fixtures/scxa_analytics-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_experiment-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.subject = new CellMetadataService(cellMetadataDao);
    }


    @ParameterizedTest
    @MethodSource("experimentsWithMetadataProvider")
    void metadataForValidExperimentAccession(String experimentAccession) {
        String cellId = jdbcUtils.fetchRandomCellFromExperiment(experimentAccession);

        assertThat(subject.getMetadata(experimentAccession, cellId)).isNotEmpty();
    }

    @Test
    void metadataForInvalidExperiment() {
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(
                () -> subject.getMetadata("FOO", "FOO"))
                .withCauseInstanceOf(NoSuchFileException.class);;
    }

    private Iterable<String> experimentsWithMetadataProvider() {
        // E-GEOD-99058 does not have any metadata (factors or inferred cell type)
        return jdbcUtils.fetchPublicSingleCellExperimentAccessions()
                .stream()
                .filter(accession -> !accession.equalsIgnoreCase(EXPERIMENT_WITHOUT_METADATA_ACCESSION))
                .collect(Collectors.toSet());
    }
}
