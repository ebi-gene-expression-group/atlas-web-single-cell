package uk.ac.ebi.atlas.experimentpage.markergenes;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarkerGeneServiceIT {
    @Inject
    private DataSource dataSource;
    @Inject
    MarkerGenesDao markerGenesDao;
    @Inject
    private JdbcUtils jdbcUtils;

    MarkerGeneService subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-fixture.sql"),
                new ClassPathResource("fixtures/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats_delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        subject = new MarkerGeneServiceImpl(markerGenesDao);
    }

    @Test
    void getMarkerGeneProfileForTheValidExperimentAccession() {
        assertThat(subject.getCellTypeMarkerGeneProfile("E-HCAD-8", "skin"))
                .isNotEmpty();
    }

    @Test
    void getEmptyMarkerGeneProfileForTheInvalidExperimentAccession(){
        assertThat(subject.getCellTypeMarkerGeneProfile("FOO", "skin"))
                .isEmpty();
    }
}