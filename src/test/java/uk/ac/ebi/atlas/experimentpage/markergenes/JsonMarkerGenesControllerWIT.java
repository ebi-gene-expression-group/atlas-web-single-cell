package uk.ac.ebi.atlas.experimentpage.markergenes;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;


import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonMarkerGenesControllerWIT {

    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcTestUtils;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private static final String urlTemplate = "/json/experiments/{experimentAccession}/marker-genes/{k}";
    private static final String markerGeneCellTypeURL = "/experiments/{experimentAccession}/marker-genes/cell-types";

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
                //End
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
                // we replaces the functionality with new cell group tables
                new ClassPathResource("fixtures/scxa_marker_genes-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_clusters-delete.sql"),
                new ClassPathResource("fixtures/scxa_marker_gene_stats-delete.sql"),
                //End
                new ClassPathResource("fixtures/scxa_cell_group_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes_delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats_delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void payloadIsValidJson() throws Exception {
        var experimentAccession = jdbcTestUtils.fetchRandomSingleCellExperimentAccessionWithMarkerGenes();
        var k = jdbcTestUtils.fetchRandomKWithMarkerGene(experimentAccession);

        this.mockMvc
                .perform(get(urlTemplate, experimentAccession, k))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].clusterIdWhereMarker", isA(Number.class)))
                .andExpect(jsonPath("$[0].x", isA(Number.class)))
                .andExpect(jsonPath("$[0].y", isA(Number.class)))
                .andExpect(jsonPath("$[0].geneName", isA(String.class)))
                .andExpect(jsonPath("$[0].value", isA(Number.class)))
                .andExpect(jsonPath("$[0].pValue", isA(Number.class)));
    }

    @Test
    void isMarkerGeneCellTypePayloadIsValidJson() throws Exception {
        this.mockMvc
                .perform(get(markerGeneCellTypeURL, "E-EHCA-2")
                        .param("organismPart", "skin"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].cellTypeValueWhereMarker", isA(String.class)))
                .andExpect(jsonPath("$[0].cellTypeValue", isA(String.class)))
                .andExpect(jsonPath("$[0].x", isA(Number.class)))
                .andExpect(jsonPath("$[0].y", isA(Number.class)))
                .andExpect(jsonPath("$[0].geneName", isA(String.class)))
                .andExpect(jsonPath("$[0].value", isA(Number.class)))
                .andExpect(jsonPath("$[0].pValue", isA(Number.class)));
    }

    @Test
    void invalidExperimentAccessionReturnsEmptyPayload() throws Exception {
        this.mockMvc
                .perform(get(markerGeneCellTypeURL, "FOO")
                        .param("organismPart", "skin"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0))));
    }
}
