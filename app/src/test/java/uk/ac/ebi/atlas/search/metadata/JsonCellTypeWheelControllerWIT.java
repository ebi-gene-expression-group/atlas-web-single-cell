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
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
class JsonCellTypeWheelControllerWIT {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void validSearchTermReturnsAValidPayload() throws Exception {
        var uri =
                UriComponentsBuilder
                        .fromPath("/json/cell-type-wheel/{searchTerm}")
                        .encode()
                        .buildAndExpand("pancreas")
                        .toUri();

        this.mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                // The exact value will depend on fixtures, but with E-MTAB-5061 and E-GEOD-81547 we’ll have a few
                .andExpect(jsonPath("$", hasSize(greaterThan(20))))
                .andExpect(jsonPath("$[0].name", isA(String.class)))
                .andExpect(jsonPath("$[0].id", isA(String.class)))
                .andExpect(jsonPath("$[0].value", isA(Number.class)));
    }

    @Test
    void broadQueryWithSpeciesFilterReturnsFewerExperimentsThanWithoutSpeciesFilter() throws Exception {
        var veryBroadSearchTerm = "organism part";
        var uriBuilder =
                UriComponentsBuilder
                        .fromPath("/json/cell-type-wheel/{searchTerm}")
                        .encode();

        this.mockMvc.perform(
                get(uriBuilder.buildAndExpand(veryBroadSearchTerm).toUri()))
                .andExpect(jsonPath("$[0].experimentAccessions", hasSize(6)));

        this.mockMvc.perform(
                get(uriBuilder.queryParam("species", "Mus musculus").buildAndExpand(veryBroadSearchTerm).toUri()))
                .andExpect(jsonPath("$[0].experimentAccessions", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].experimentAccessions", hasSize(lessThan(6))));

    }

    @Test
    void unknownSearchTermReturnsAnEmptyPayload() throws Exception {
        var uri =
                UriComponentsBuilder
                        .fromPath("/json/cell-type-wheel/{searchTerm}")
                        .encode()
                        .buildAndExpand("foobar")
                        .toUri();

        this.mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}