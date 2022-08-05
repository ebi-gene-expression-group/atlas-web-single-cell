package uk.ac.ebi.atlas.experimentpage.cellplot;

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
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellPlotGenericDaoIT {
    private static final Random RNG = ThreadLocalRandom.current();

    @Inject
    private DataSource dataSource;

/*    @Inject
    private JdbcUtils jdbcTestUtils;*/

    @Inject
    private CellPlotGenericDao subject;

    public ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

    @BeforeAll
    void populateDatabaseTables() {
        populator.setScripts(
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/scxa_analytics.sql"),
                new ClassPathResource("fixtures/scxa_coords.sql"),
                new ClassPathResource("fixtures/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership.sql"),
                new ClassPathResource("fixtures/scxa_dimension_reduction.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        populator.setScripts(
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/scxa_coords-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql"),
                new ClassPathResource("fixtures/scxa_dimension_reduction-delete.sql"));
        populator.execute(dataSource);
    }

    @Test
    void resultIsEmpty() {
        assertThat(subject.getQueryParams("UMAP", "E-CURD-4"))
                .isEmpty();

    }
}