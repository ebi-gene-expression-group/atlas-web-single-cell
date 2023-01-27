package uk.ac.ebi.atlas.search.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
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
        var uri =
                UriComponentsBuilder
                        .fromPath("/search/metadata/{searchTerm}")
                        .encode()
                        .buildAndExpand("cancer")
                        .toUri();

        this.mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andExpect(view().name("cell-type-wheel-experiment-heatmap"));
    }

    @Test
    void returnsValidModelWithSpeciesFilter() throws Exception {
        var uri =
                UriComponentsBuilder
                        .fromPath("/search/metadata/{searchTerm}")
                        .queryParam("species", "Mus musculus")
                        .encode()
                        .buildAndExpand("cancer")
                        .toUri();

        this.mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andExpect(view().name("cell-type-wheel-experiment-heatmap"));
    }


}
