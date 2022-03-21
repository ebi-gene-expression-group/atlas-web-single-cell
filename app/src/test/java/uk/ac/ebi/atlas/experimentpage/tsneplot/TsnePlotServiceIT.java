package uk.ac.ebi.atlas.experimentpage.tsneplot;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataDao;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotService.MISSING_METADATA_VALUE_PLACEHOLDER;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TsnePlotServiceIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private TSnePlotDao tsnePlotDao;

    @Inject
    private CellMetadataDao cellMetadataDao;

    private TSnePlotService subject;

    private static final String EXPERIMENT_ACCESSION_WITH_MISSING_INFERRED_CELL_TYPE = "E-GEOD-71585";

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/202203/scxa_coords.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/202203/scxa_coords-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.subject = new TSnePlotService(tsnePlotDao, cellMetadataDao);
    }

    @Test
    void missingMetadataValuesAreReplacedWithNotAvailable() {
        int perplexity = jdbcUtils.fetchRandomPerplexityFromExperimentTSne(EXPERIMENT_ACCESSION_WITH_MISSING_INFERRED_CELL_TYPE);
        String metadataType = "inferred_cell_type_-_ontology_labels";

        assertThat(
                subject.fetchTSnePlotWithMetadata(
                        EXPERIMENT_ACCESSION_WITH_MISSING_INFERRED_CELL_TYPE,
                        perplexity,
                        metadataType))
                .containsKeys(StringUtils.capitalize(MISSING_METADATA_VALUE_PLACEHOLDER));
    }

}
