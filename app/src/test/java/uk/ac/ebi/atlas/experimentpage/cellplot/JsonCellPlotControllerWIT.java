//package uk.ac.ebi.atlas.experimentpage.cellplot;
//
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.http.MediaType;
//import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.context.web.WebAppConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//import uk.ac.ebi.atlas.configuration.TestConfig;
//import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
//import uk.ac.ebi.atlas.testutils.JdbcUtils;
//
//import javax.inject.Inject;
//import javax.sql.DataSource;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@ExtendWith(SpringExtension.class)
//@WebAppConfiguration
//@ContextConfiguration(classes = TestConfig.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class JsonCellPlotControllerWIT {
//
//    @Inject
//    private DataSource dataSource;
//
//    @Inject
//    private JdbcUtils jdbcUtils;
//
//    @Inject
//    private TSnePlotSettingsService tSnePlotSettingsService;
//
//    @Autowired
//    private WebApplicationContext wac;
//
//    private MockMvc mockMvc;
//
//    private String accessKey;
//
//    private String endpoint_url;
//
//
//    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
//
//    @BeforeAll
//    void populateDatabaseTables() {
//        populator.setScripts(
//                new ClassPathResource("fixtures/experiment.sql"),
//                new ClassPathResource("fixtures/scxa_analytics.sql"),
//                new ClassPathResource("fixtures/scxa_coords.sql"),
//                new ClassPathResource("fixtures/scxa_cell_group.sql"),
//                new ClassPathResource("fixtures/scxa_cell_group_membership.sql"));
//        populator.execute(dataSource);
//    }
//
//    @AfterAll
//    void cleanDatabaseTables() {
//        populator.setScripts(
//                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats-delete.sql"),
//                new ClassPathResource("fixtures/scxa_cell_group_marker_genes-delete.sql"),
//                new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
//                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
//                new ClassPathResource("fixtures/scxa_coords-delete.sql"),
//                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
//                new ClassPathResource("fixtures/experiment-delete.sql"));
//        populator.execute(dataSource);
//    }
//
//    @BeforeEach
//    void setUp() {
//        var experimentAccession = jdbcUtils.fetchRandomPublicExperimentAccession();
//        jdbcUtils.updatePublicExperimentAccessionToPrivate(experimentAccession);
//        accessKey = jdbcUtils.fetchExperimentAccessKey(experimentAccession);
//
//        var k = tSnePlotSettingsService.getExpectedClusters(experimentAccession);
//        endpoint_url = "/json/cell-plots/" + experimentAccession + "/clusters/k/" + k.get();
//
//        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
//
//    }
//
//    @Test
//    void returnsPrivateExperimentWithValidAccessKey() throws Exception {
//        mockMvc.perform(get(endpoint_url + "?accessKey=" + accessKey + "&n_neighbors=5"))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(jsonPath("$.series").isArray())
//                .andExpect(jsonPath("$.series").isNotEmpty());
//    }
//
//    @Test
//    void returnsErrorWithInValidAccessKey() throws Exception {
//        mockMvc.perform(get(endpoint_url + "?accessKey=foo_bar&n_neighbors=5"))
//                .andExpect(status().isBadRequest());
//    }
//}