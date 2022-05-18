package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
class MultiexperimentCellTypeMarkerGenesDaoIT {
    public ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

    @Inject
    private DataSource dataSource;

    @Inject
    MultiexperimentCellTypeMarkerGenesDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        populator.setScripts(
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/inferred-cell-types-marker-genes/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/inferred-cell-types-marker-genes/scxa_cell_group_marker_genes.sql"),
                new ClassPathResource("fixtures/inferred-cell-types-marker-genes/scxa_cell_group_marker_gene_stats.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        populator.setScripts(
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @Test
    void knownCellTypeReturnsResults() {
        // We have a few cell type marker genes in our fixtures; this is one of them
        assertThat(subject.getCellTypeMarkerGenes(ImmutableSet.of("E-CURD-4"), "columella root cap cell 6"))
                .isNotEmpty();
    }

    @Test
    void unknownCellTypeReturnsEmpty() {
        assertThat(subject.getCellTypeMarkerGenes(ImmutableSet.of("E-CURD-4"), "foobar"))
                .isEmpty();
    }

    @Test
    void unknownExperimentAccessionReturnsEmpty() {
        assertThat(subject.getCellTypeMarkerGenes(ImmutableSet.of("E-FOOBAR"), "columella root cap cell 6"))
                .isEmpty();
    }
}
