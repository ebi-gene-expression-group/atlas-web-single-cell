package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
        assertThat(subject.getCellTypeMarkerGeneProfile("E-EHCA-2", "skin"))
                .isNotEmpty();
    }

    @Test
    void getEmptyMarkerGeneProfileForTheInvalidExperimentAccession() {
        assertThat(subject.getCellTypeMarkerGeneProfile("FOO", "skin"))
                .isEmpty();
    }

    @Test
    void getEmptyClusterMarkerGenesProfileForTheInvalidExperimentAccession() {
        assertThat(subject.getMarkerGenesPerCluster("FOO", "10"))
                .isEmpty();
    }

    @Test
    void getClusterTop5MarkerGenesForTheValidExperimentAccessionAndK() {
        ImmutableMap<String, ImmutableSet<MarkerGene>> top5MarkerGenesPerCellGroupValue = subject.getMarkerGenesPerCluster("E-EHCA-2", "10");
        assertThat(top5MarkerGenesPerCellGroupValue).containsKey("1");
        assertThat(top5MarkerGenesPerCellGroupValue.get("1"))
                .isNotEmpty()
                .allMatch(markerGene -> markerGene.cellGroupValue().equalsIgnoreCase("1"))
                .hasSize(5);
    }
}