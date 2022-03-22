package uk.ac.ebi.atlas.experimentpage;

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

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ALL TESTS IGNORED BECAUSE THE SUBJECT IS GOING TO BE DEPRECATED
//@ExtendWith(SpringExtension.class)
//@WebAppConfiguration
//@ContextConfiguration(classes = TestConfig.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonTSnePlotControllerWIT {
    //@Inject
    private DataSource dataSource;

    //@Inject
    private JdbcUtils jdbcTestUtils;

    //@Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    //@BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/scxa_coords.sql"),
                new ClassPathResource("fixtures/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership.sql"),
                new ClassPathResource("fixtures/scxa_analytics.sql"));
        populator.execute(dataSource);
    }

    //@AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/scxa_coords-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    //@BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    //@Test
    void validJsonForExpressedGeneId() throws Exception {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var geneId = jdbcTestUtils.fetchRandomGeneFromSingleCellExperiment(experimentAccession);
        // If our fixtures contained full experiments we could use any random perplexity with
        // fetchRandomPerplexityFromExperimentTSne(experimentAccession), but since we have a subset of all the rows, we
        // need to restrict this value to the perplexities actually available for the particular gene we choose.
        var perplexity = jdbcTestUtils.fetchRandomPerplexityFromExperimentTSne(experimentAccession, geneId);

        this.mockMvc
                .perform(get(
                        "/json/experiments/" + experimentAccession + "/tsneplot/" + perplexity +
                        "/expression/" + geneId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.min", isA(Number.class)))
                .andExpect(jsonPath("$.min", is(greaterThan(0.0))))
                .andExpect(jsonPath("$.max", isA(Number.class)))
                .andExpect(jsonPath("$.max", is(greaterThan(0.0))))
                .andExpect(jsonPath("$.unit", is(oneOf("CPM"))))
                .andExpect(jsonPath("$.series", hasSize(greaterThan(0))));
    }

    //@Test
    void validJsonForInvalidGeneId() throws Exception {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var perplexity = jdbcTestUtils.fetchRandomPerplexityFromExperimentTSne(experimentAccession);

        this.mockMvc
                .perform(get(
                        "/json/experiments/" + experimentAccession + "/tsneplot/" + perplexity + "/expression/FOOBAR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.min").doesNotExist())
                .andExpect(jsonPath("$.max").doesNotExist())
                .andExpect(jsonPath("$.unit", is(oneOf("CPM"))))
                .andExpect(jsonPath("$.series", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.series..expressionLevel", everyItem(is(0.0))));
    }

    //@Test
    void noExpressionForEmptyGeneId() throws Exception {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var perplexity = jdbcTestUtils.fetchRandomPerplexityFromExperimentTSne(experimentAccession);

        this.mockMvc
                .perform(get("/json/experiments/" + experimentAccession + "/tsneplot/" + perplexity + "/expression/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.min").doesNotExist())
                .andExpect(jsonPath("$.max").doesNotExist())
                .andExpect(jsonPath("$.unit", is(oneOf("CPM"))))
                .andExpect(jsonPath("$.series", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.series..expressionLevel", everyItem(is(0.0))));
    }

    //@Test
    void validJsonForValidK() throws Exception {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var k = jdbcTestUtils.fetchRandomKFromCellClusters(experimentAccession);
        var perplexity = jdbcTestUtils.fetchRandomPerplexityFromExperimentTSne(experimentAccession);

        this.mockMvc
                .perform(get(
                        "/json/experiments/" + experimentAccession + "/tsneplot/" + perplexity +
                        "/clusters/variable/" + k))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                // With full experiments this test could be even better:
                // .andExpect(jsonPath("$.series", hasSize(k)))
                // .andExpect(jsonPath("$.series[" + Integer.toString(RNG.nextInt(0, k)) + "].data").isNotEmpty());
                .andExpect(jsonPath("$.series", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.series[0].data").isNotEmpty());
    }

    //@Test
    void validJsonForInvalidK() throws Exception {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var perplexity = jdbcTestUtils.fetchRandomPerplexityFromExperimentTSne(experimentAccession);

        this.mockMvc
                .perform(get(
                        "/json/experiments/" + experimentAccession + "/tsneplot/" + perplexity +
                        "/clusters/variable/9000"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.series", hasSize(1)));
    }

    //@Test
    void defaultMethodInExpressionRequestsWithoutAGeneIdIsUmap() throws Exception {
        var experimentAccession = jdbcTestUtils.fetchRandomExperimentAccession();
        var nNeighbors = jdbcTestUtils.fetchRandomNeighboursFromExperimentUmap(experimentAccession);

        var expected =
                this.mockMvc
                        .perform(get(
                                "/json/experiments/" + experimentAccession + "/tsneplot/" + nNeighbors + "/expression")
                                .param("method", "umap"))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();

        this.mockMvc
                .perform(get("/json/experiments/" + experimentAccession + "/tsneplot/" + nNeighbors + "/expression"))
                .andExpect(content().bytes(expected));
    }
}
