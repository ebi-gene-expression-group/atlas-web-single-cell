package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Ignore;
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
	private MarkerGenesDao markerGenesDao;
	@Inject
	private CellTypeSearchDao cellTypeSearchDao;
	@Inject
	private JdbcUtils jdbcTestUtils;

	private MarkerGeneService subject;

	@BeforeAll
	void populateDatabaseTables() {
		var populator = new ResourceDatabasePopulator();
		populator.addScripts(
				new ClassPathResource("fixtures/experiment-fixture.sql"),
				new ClassPathResource("fixtures/scxa_cell_group-fixture.sql"),
				new ClassPathResource("fixtures/scxa_cell_group_membership-fixture.sql"),
				new ClassPathResource("fixtures/scxa_cell_group_marker_genes-fixture.sql"),
				new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats-fixture.sql"));
		populator.execute(dataSource);
	}

	@AfterAll
	void cleanDatabaseTables() {
		var populator = new ResourceDatabasePopulator();
		populator.addScripts(
				new ClassPathResource("fixtures/experiment-delete.sql"),
				new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
				new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
				new ClassPathResource("fixtures/scxa_cell_group_marker_genes-delete.sql"),
				new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats-delete.sql"));
		populator.execute(dataSource);
	}

	@BeforeEach
	void setUp() {
		subject = new MarkerGeneService(markerGenesDao, cellTypeSearchDao);
	}

//    TODO: Re enable this test once we receive proper test data
	@Ignore
	void getMarkerGeneProfileForTheValidExperimentAccession() {
		assertThat(subject.getCellTypeMarkerGeneProfile("E-EHCA-2", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000061")))
				.isNotEmpty();
	}

	@Test
	void getEmptyMarkerGeneProfileForTheInvalidExperimentAccession() {
		assertThat(subject.getCellTypeMarkerGeneProfile("FOO", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0001264")))
				.isEmpty();
	}

	@Test
	void getEmptyClusterMarkerGenesForTheInvalidExperimentAccession() {
		assertThat(subject.getMarkerGenesPerCluster("FOO", "10"))
				.isEmpty();
	}

	@Test
	void getCellTypeMarkerGenesForTheMultipleOrganismParts(){
		assertThat(subject.getCellTypeMarkerGeneProfile("FOO", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000061","http://purl.obolibrary.org/obo/UBERON_0001987")))
				.isEmpty();
	}

	@Test
	void getClusterMarkerGeneForTheValidExperimentAccession() {
		var experimentAccession = jdbcTestUtils.fetchRandomSingleCellExperimentAccessionWithMarkerGenes();
		var k = jdbcTestUtils.fetchRandomKWithMarkerGene(experimentAccession);
		assertThat(subject.getMarkerGenesPerCluster(experimentAccession, k))
				.isNotEmpty();
	}

	@Test
	void getEmptyCellTypeMarkerGenesForTheInvalidExperimentAccession() {
		assertThat(subject.getCellTypeMarkerGeneProfile("FOO", ImmutableSet.of("skin")))
				.isEmpty();
	}
}
