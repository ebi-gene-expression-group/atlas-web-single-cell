package uk.ac.ebi.atlas.search.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
class JsonMultiexperimentCellTypeMarkerGenesControllerWIT {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }


    @Test
    void returnsAValidJsonPayloadForAValidCellType() throws Exception {
        this.mockMvc.perform(get("/json/cell-type-marker-genes/{cellType}", "root cortex; trichoblast 9")
                .param("experimentAccession", "E-CURD-4"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].cellGroupValue", isA(String.class)))
                .andExpect(jsonPath("$[0].value", isA(Number.class)))
                .andExpect(jsonPath("$[0].cellGroupValueWhereMarker", isA(String.class)))
                .andExpect(jsonPath("$[0].pValue", isA(Number.class)));
    }

    @Test
    void returnsEmptyPayloadForAnInvalidCellType() throws Exception {
        this.mockMvc.perform(get("/json/cell-type-marker-genes/{cellType}", "fooBar")
                .param("experimentAccession", "E-CURD-4"))
                .andExpect(status().isOk());
    }
}