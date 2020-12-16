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
import uk.ac.ebi.atlas.search.CellTypeSearchDao;

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
    private MarkerGenesDao markerGenesDao;
    @Inject
    private CellTypeSearchDao cellTypeSearchDao;
    private MarkerGeneService subject;

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
        subject = new MarkerGeneService(markerGenesDao, cellTypeSearchDao);
    }

    @Test
    void getMarkerGeneProfileForTheValidExperimentAccession() {
        assertThat(subject.getCellTypeMarkerGeneProfile("E-MTAB-5061", "http://purl.obolibrary.org/obo/UBERON_0001264"))
                .isNotEmpty();
    }

    @Test
    void getEmptyMarkerGeneProfileForTheInvalidExperimentAccession() {
        assertThat(subject.getCellTypeMarkerGeneProfile("FOO", "http://purl.obolibrary.org/obo/UBERON_0001264"))
                .isEmpty();
    }

    @Test
    void getEmptyClusterMarkerGenesForTheInvalidExperimentAccession() {
        assertThat(subject.getMarkerGenesPerCluster("FOO", "10"))
                .isEmpty();
    }

	@Test
	void getClusterMarkerGeneForTheValidExperimentAccession() {
		assertThat(subject.getMarkerGenesPerCluster("E-EHCA-2", "10"))
				.isNotEmpty();
	}

	@Test
	void getEmptyCellTypeMarkerGenesForTheInvalidExperimentAccession() {
		assertThat(subject.getCellTypeMarkerGeneProfile("FOO", "skin"))
				.isEmpty();
	}
}
