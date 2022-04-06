package uk.ac.ebi.atlas.resource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.resource.AtlasResource;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(transactionManager = "txManager")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataFileHubIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFileHubIT.class);

    @Inject
    private DataSource dataSource;

    @Inject
    private Path dataFilesPath;

    @Inject
    private JdbcUtils jdbcUtils;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @Test
    void findsTSnePlotFiles() {
        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
        var subject = new DataFileHub(dataFilesPath.resolve("scxa"));
        LOGGER.info("Test tsne plot files for experiment {}", experimentAccession);
        assertAtlasResourceExists(subject.getSingleCellExperimentFiles(experimentAccession).tSnePlotTsvs.values());
    }

    @Test
    void findsMarkerGeneFiles() {
        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
        var subject = new DataFileHub(dataFilesPath.resolve("scxa"));
        LOGGER.info("Test marker gene files for experiment {}", experimentAccession);
        assertAtlasResourceExists(subject.getSingleCellExperimentFiles(experimentAccession).markerGeneTsvs.values());
    }

// TODO Rethink this test since not all experiments have inferred cell type annotations
//    @Test
//    void findsCellTypeMarkerGeneFiles(@Value("${data.files.location}") String dataFilesLocation) {
//        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
//        var subject = new DataFileHub(dataFilesPath.resolve("scxa"));
//        LOGGER.info("Test cell type marker gene files for experiment {}", experimentAccession);
//        assertThat(subject.getSingleCellExperimentFiles(experimentAccession).markerGeneTsvs.values()
//                .stream().map(AtlasResource::getPath))
//                .contains(Path.of(dataFilesLocation +
//                        "/scxa/magetab/" + experimentAccession + "/" + experimentAccession +
//                        ".marker_genes_inferred_cell_type_-_ontology_labels.tsv"));
//    }

    @Test
    void findsRawFilteredCountsFiles() {
        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
        var subject = new DataFileHub(dataFilesPath.resolve("scxa"));
        LOGGER.info("Test raw filtered count files for experiment {}", experimentAccession);
        assertAtlasResourceExists(subject.getSingleCellExperimentFiles(experimentAccession).filteredCountsMatrix);
        assertAtlasResourceExists(subject.getSingleCellExperimentFiles(experimentAccession).filteredCountsGeneIdsTsv);
        assertAtlasResourceExists(subject.getSingleCellExperimentFiles(experimentAccession).filteredCountsCellIdsTsv);
    }

    @Test
    void findsNormalisedCountsFiles() {
        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
        var subject = new DataFileHub(dataFilesPath.resolve("scxa"));
        LOGGER.info("Test normalised filtered count files for experiment {}", experimentAccession);
        assertAtlasResourceExists(subject.getSingleCellExperimentFiles(experimentAccession).normalisedCountsMatrix);
        assertAtlasResourceExists(subject.getSingleCellExperimentFiles(experimentAccession).normalisedCountsGeneIdsTsv);
        assertAtlasResourceExists(subject.getSingleCellExperimentFiles(experimentAccession).normalisedCountsCellIdsTsv);
    }

    private static void assertAtlasResourceExists(AtlasResource<?> resource) {
        assertThat(resource.exists()).isTrue();
    }

    private static void assertAtlasResourceExists(Collection<? extends AtlasResource<?>> resource) {
        assertThat(resource).isNotEmpty();
        assertThat(resource).allMatch(AtlasResource::exists);
    }
}
