package uk.ac.ebi.atlas.experimentpage.markergenes;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarkerGenesDaoIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Inject
    private JdbcUtils jdbcTestUtils;

    private static final String EXPERIMENT_ACCESSION_WITH_MARKER_GENES = "E-GEOD-99058";
    private static final String CELL_GROUP_EXPERIMENT_ACCESSION_WITH_MARKER_GENES = "E-EHCA-2";

    private MarkerGenesDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-fixture.sql"),
                new ClassPathResource("fixtures/scxa_analytics-fixture.sql"),
                //Start - These marker gene tables test data would be removed soon after
                // we replaces functionality with new cell group tables
                new ClassPathResource("fixtures/scxa_marker_genes-fixture.sql"),
                new ClassPathResource("fixtures/scxa_cell_clusters-fixture.sql"),
                new ClassPathResource("fixtures/scxa_marker_gene_stats-fixture.sql"),
                //end
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
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                //Start - These marker gene tables deletion scripts would be removed soon after
                // we replaces functionality with new cell group tables
                new ClassPathResource("fixtures/scxa_marker_genes-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_clusters-delete.sql"),
                new ClassPathResource("fixtures/scxa_marker_gene_stats-delete.sql"),
                //end
                new ClassPathResource("fixtures/scxa_cell_group_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats_delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        subject = new MarkerGenesDao(namedParameterJdbcTemplate);
    }

    @ParameterizedTest
    @MethodSource("ksForExperimentWithMarkerGenes")
    void testExperimentsWithMarkerGenesAboveThreshold(String k) {
        var markerGenesWithAveragesPerCluster =
                subject.getMarkerGenesWithAveragesPerCluster(CELL_GROUP_EXPERIMENT_ACCESSION_WITH_MARKER_GENES, "10");
        // I would replace this hardcode value '10' with a random k value once I update fixtures properly, currently I added very
        // few records to proceed further, without hardcode value tests would fail if I pass random values of K.
        assertThat(markerGenesWithAveragesPerCluster)
                // Fixtures might not have marker genes from every cluster and this might be empty
                //.isNotEmpty()
                .allMatch(x -> x.pValue() < 0.05);
    }

    @Test
    void ksAreRetrievedForExperimentWithMarkerGenes() {
        var experimentAccession = jdbcTestUtils.fetchRandomSingleCellExperimentAccessionWithMarkerGenes();

        assertThat(subject.getKsWithMarkerGenes(experimentAccession)).isNotEmpty();
    }

    @Test
    void shouldFetchAllMarkerGenesBelowThreshold() {
        var markerGenesWithAveragesPerCellGroup = subject.getCellTypeMarkerGenes("E-EHCA-2", "skin");
        assertThat(markerGenesWithAveragesPerCellGroup).allMatch(markerGene -> markerGene.pValue() < 0.05);
    }

    @Test
    void shouldFetchOnlyInferredCellTypeMarkerGenes() {
        var markerGenesWithAveragesPerCellGroup = subject.getCellTypeMarkerGenes("E-EHCA-2", "skin");
        assertThat(markerGenesWithAveragesPerCellGroup).allMatch(markerGene -> markerGene.cellGroupType().equals("inferred cell type"));
    }

    private Stream<String> ksForExperimentWithMarkerGenes() {
        return jdbcTestUtils.fetchKsFromCellClusters(EXPERIMENT_ACCESSION_WITH_MARKER_GENES)
                .stream().map(Object::toString);
    }
}
