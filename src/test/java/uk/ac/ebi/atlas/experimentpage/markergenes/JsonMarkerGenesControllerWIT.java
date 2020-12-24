package uk.ac.ebi.atlas.experimentpage.markergenes;

import org.junit.jupiter.api.*;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private static final String markerGeneClusterURL = "/json/experiments/{experimentAccession}/marker-genes/clusters";
    private static final String markerGeneCellTypeURL = "/json/experiments/{experimentAccession}/marker-genes/cell-types";

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-fixture.sql"),
                new ClassPathResource("fixtures/scxa_analytics-fixture.sql"),
                new ClassPathResource("fixtures/scxa_cell_clusters-fixture.sql"),
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
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_clusters-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats-delete.sql"));
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
                .perform(get(markerGeneClusterURL, experimentAccession)
                        .param("k", k))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].cellGroupValueWhereMarker", isA(String.class)))
                .andExpect(jsonPath("$[0].x", isA(Number.class)))
                .andExpect(jsonPath("$[0].y", isA(Number.class)))
                .andExpect(jsonPath("$[0].geneName", isA(String.class)))
                .andExpect(jsonPath("$[0].value", isA(Number.class)))
                .andExpect(jsonPath("$[0].pValue", isA(Number.class)));
    }

    @Test
    void isMarkerGeneCellTypePayloadIsValidJson() throws Exception {
        this.mockMvc
                .perform(get(markerGeneCellTypeURL, "E-MTAB-5061")
                        .param("organismPart", "http://purl.obolibrary.org/obo/UBERON_0001264"))
                .andExpect(status().isOk());
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(jsonPath("$[0].cellGroupValueWhereMarker", isA(String.class)))
//                .andExpect(jsonPath("$[0].cellGroupValue", isA(String.class)))
//                .andExpect(jsonPath("$[0].x", isA(Number.class)))
//                .andExpect(jsonPath("$[0].y", isA(Number.class)))
//                .andExpect(jsonPath("$[0].geneName", isA(String.class)))
//                .andExpect(jsonPath("$[0].value", isA(Number.class)))
//                .andExpect(jsonPath("$[0].pValue", isA(Number.class)));
		// TODO: We will re enable this piece of code
    }

    @Test
    void invalidExperimentAccessionReturnsEmptyJson() throws Exception {
        this.mockMvc
                .perform(get(markerGeneCellTypeURL, "FOO")
                        .param("organismPart", "skin"))
                .andExpect(status().is2xxSuccessful());
    }
}
