package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchControllerWIT {
    private static final Gson GSON = new Gson();

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void missingGeneQueryReturnsError() throws Exception {
        this.mockMvc.perform(post("/search"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void emptyGeneQueryReturnsError() throws Exception {
        this.mockMvc.perform(post("/search").param("geneQuery", ""))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void invalidGeneQueryJsonReturnsError() throws Exception {
        var randomString = randomAlphanumeric(10);

        this.mockMvc.perform(post("/search").param("geneQuery", randomString))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void geneQueryJsonWithoutTermOrCategoryReturnsError() throws Exception {
        var term = randomAlphanumeric(10);
        var category = randomAlphanumeric(10);

        this.mockMvc.perform(post("/search").param("geneQuery", GSON.toJson(ImmutableMap.of("term", term))))
                .andExpect(status().is4xxClientError());
        this.mockMvc.perform(post("/search").param("geneQuery", GSON.toJson(ImmutableMap.of("category", category))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void postRequestIsRedirectedToGetRequest() throws Exception {
        var term = randomAlphanumeric(10);
        var category = randomAlphanumeric(10);

        this.mockMvc.perform(
                post("/search")
                        .param("geneQuery", GSON.toJson(ImmutableMap.of("term", term, "category", category))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/search?*"));
    }

    @Test
    void geneQueryAndSpeciesParamsAreParsedAndAddedToGetRequest() throws Exception {
        var term = randomAlphanumeric(10);
        var category = randomAlphanumeric(10);
        var species = randomAlphanumeric(4) + " " + randomAlphanumeric(6);

        this.mockMvc.perform(
                post("/search")
                        .param("geneQuery", GSON.toJson(ImmutableMap.of("term", term, "category", category)))
                        .param("species", species))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search?" + category + "=" + term + "&species=" + species));
    }

    @Test
    void metadataAndSpeciesParamsAreParsedAndAddedToGetRequest() throws Exception {
        var term = randomAlphanumeric(10);
        var category = "metadata";
        var species = randomAlphanumeric(4) + " " + randomAlphanumeric(6);

        this.mockMvc.perform(
                        post("/search")
                                .param("geneQuery", GSON.toJson(ImmutableMap.of("term", term, "category", category)))
                                .param("species", species))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search/" + category + "/" + term + "?species=" + species));
    }

    @Test
    void otherRequestParamsAreIgnored() throws Exception {
        var term = randomAlphanumeric(10);
        var category = randomAlphanumeric(10);
        var species = randomAlphanumeric(4) + " " + randomAlphanumeric(6);
        var otherRequestParam = randomAlphanumeric(10);
        var otherRequestParamValue = randomAlphanumeric(10);

        this.mockMvc.perform(
                post("/search")
                        .param("geneQuery", GSON.toJson(ImmutableMap.of("term", term, "category", category)))
                        .param("species", species)
                        .param(otherRequestParam, otherRequestParamValue))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search?" + category + "=" + term + "&species=" + species));
    }

    @Test
    void getSearchPassesAllRequestParamsToEndpoint() throws Exception {
        var requestParam = randomAlphanumeric(10);
        var requestParamValue = randomAlphanumeric(10);

        this.mockMvc.perform(get("/search").param(requestParam, requestParamValue))
            .andExpect(model().attribute("endpoint", is("json/search?" + requestParam + "=" + requestParamValue)));
    }

    @Test
    void getSearchReturnsSearchResultsView() throws Exception {
        this.mockMvc.perform(get("/search"))
                .andExpect(view().name("gene-search-results"));
    }

    @Test
    void getSearchPutsSpeciesInTheModel() throws Exception {
        String randomString = randomAlphanumeric(10);

        this.mockMvc.perform(get("/search").param("species", randomString))
                .andExpect(model().attribute("species", is(randomString)));

    }

    @Test
    void getSearchPutsSearchTermInTheModel() throws Exception {
        String randomString = randomAlphanumeric(10);

        this.mockMvc.perform(
                get("/search")
                        .param("q", randomString)
                        .param("species", randomAlphanumeric(10)))
                .andExpect(model().attribute("geneQueryTerm", is(randomString)))
                .andExpect(model().attribute("geneQueryCategory", is("q")));
    }

    @Test
    void metadataTermIsRedirectedToMetadataCellTypeWheelPage() throws Exception {
        var term = randomAlphanumeric(10);
        var category = "metadata";
        var otherRequestParam = randomAlphanumeric(10);
        var otherRequestParamValue = randomAlphanumeric(10);

        this.mockMvc.perform(
                        post("/search")
                                .param("geneQuery", GSON.toJson(ImmutableMap.of("term", term, "category", category)))
                                .param(otherRequestParam, otherRequestParamValue))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/search/metadata/" + term));
    }
}
