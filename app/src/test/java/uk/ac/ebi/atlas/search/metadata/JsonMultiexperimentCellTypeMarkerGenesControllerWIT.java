package uk.ac.ebi.atlas.search.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class JsonMultiexperimentCellTypeMarkerGenesControllerWIT {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void shouldReturnAValidJsonPayloadForAValidCellType() throws Exception {
        this.mockMvc.perform(get("/json/cell-type-marker-genes/{cellType}", "cell cycle S phase")
                        .param("experimentAccession", "E-ENAD-53"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].cellGroupValue", isA(String.class)))
                .andExpect(jsonPath("$[0].value", isA(Number.class)))
                .andExpect(jsonPath("$[0].cellGroupValueWhereMarker", isA(String.class)))
                .andExpect(jsonPath("$[0].pValue", isA(Number.class)));
    }

    @Test
    void shouldReturnAValidJsonPayloadForACellTypeContainingAForwardSlash() throws Exception {
        final String encodedCellType = URLEncoder.encode("cell cycle G2/M phase", StandardCharsets.UTF_8);
        this.mockMvc.perform(get("/json/cell-type-marker-genes/{cellType}", encodedCellType)
                        .param("experimentAccession", "E-ENAD-53"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].cellGroupValue", isA(String.class)))
                .andExpect(jsonPath("$[0].value", isA(Number.class)))
                .andExpect(jsonPath("$[0].cellGroupValueWhereMarker", isA(String.class)))
                .andExpect(jsonPath("$[0].pValue", isA(Number.class)));
    }

    @Test
    void shouldReturnEmptyPayloadForAnInvalidCellType() throws Exception {
        this.mockMvc.perform(get("/json/cell-type-marker-genes/{cellType}", "fooBar")
                .param("experimentAccession", "E-CURD-4"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", is(empty())));
    }
}
