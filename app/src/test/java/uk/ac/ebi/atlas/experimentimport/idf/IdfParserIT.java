package uk.ac.ebi.atlas.experimentimport.idf;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdfParserIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private Path dataFilesPath;

    @Inject
    private Path experimentDesignDirPath;

    @Inject
    private JdbcUtils jdbcUtils;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator bulkPopulator = new ResourceDatabasePopulator();
        bulkPopulator.addScripts(new ClassPathResource("fixtures/experiment.sql"));
        bulkPopulator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @ParameterizedTest
    @MethodSource("singleCellExperimentsProvider")
    void testParserForSingleCell(String experimentAccession) {
        IdfParser idfParser = new IdfParser(new DataFileHub(dataFilesPath.resolve("scxa"), experimentDesignDirPath));
        IdfParserOutput result = idfParser.parse(experimentAccession);

        assertThat(result.getExpectedClusters()).isGreaterThanOrEqualTo(0);
        assertThat(result.getTitle()).isNotEmpty();
        assertThat(result.getPublications()).isNotNull();
    }

    private Iterable<String> singleCellExperimentsProvider() {
        return jdbcUtils.fetchPublicExperimentAccessions();
    }
}
