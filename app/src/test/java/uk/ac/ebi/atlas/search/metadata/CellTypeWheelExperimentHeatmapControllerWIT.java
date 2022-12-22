package uk.ac.ebi.atlas.search.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@Sql({
        "/fixtures/experiment.sql",
        "/fixtures/scxa_analytics.sql",
        "/fixtures/scxa_dimension_reduction.sql",
        "/fixtures/scxa_coords.sql",
        "/fixtures/scxa_cell_group.sql",
        "/fixtures/scxa_cell_group_membership.sql",
        "/fixtures/scxa_cell_group_marker_genes.sql",
        "/fixtures/scxa_cell_group_marker_gene_stats.sql"
})
@Sql(scripts = {
        "/fixtures/scxa_cell_group_marker_gene_stats-delete.sql",
        "/fixtures/scxa_cell_group_marker_genes-delete.sql",
        "/fixtures/scxa_cell_group_membership-delete.sql",
        "/fixtures/scxa_cell_group-delete.sql",
        "/fixtures/scxa_coords-delete.sql",
        "/fixtures/scxa_dimension_reduction-delete.sql",
        "/fixtures/scxa_analytics-delete.sql",
        "/fixtures/experiment-delete.sql"
}, executionPhase = AFTER_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellTypeWheelExperimentHeatmapControllerWIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void returnsValidModel() throws Exception {
        this.mockMvc.perform(get("/search/metadata/cancer"))
                .andExpect(status().isOk())
                .andExpect(view().name("cell-type-wheel-experiment-heatmap"));
    }
}
