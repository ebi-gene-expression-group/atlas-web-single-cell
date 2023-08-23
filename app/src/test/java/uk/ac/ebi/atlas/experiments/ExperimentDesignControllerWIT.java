package uk.ac.ebi.atlas.experiments;

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

import javax.inject.Inject;
import javax.sql.DataSource;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExperimentDesignControllerWIT {

    private static final String URL = "/json/experiment-design/{experiment_accession}";

    @Inject
    private DataSource dataSource;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/scxa_exp_design_column.sql"),
                new ClassPathResource("fixtures/scxa_exp_design.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_exp_design-delete.sql"),
                new ClassPathResource("fixtures/scxa_exp_design_column-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void hasExperimentDesignDataWithoutAnyPageNoSizeParameter() throws Exception {
        mockMvc.perform(get(URL, "E-EHCA-2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.headers").isArray())
                .andExpect(jsonPath("$.headers").isNotEmpty())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.headers").contains(List.of("contrastName", "referenceOrTest", "run")))
                //default pageSize is 20 and pageNo is 1
                .andExpect(jsonPath("$.data.length()").value(20));
    }

    @Test
    void hasValidExperimentDesignDataWithPageSizeParameter() throws Exception {
        mockMvc.perform(get(URL, "E-EHCA-2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.headers").isArray())
                .andExpect(jsonPath("$.headers").isNotEmpty())
                .andExpect(jsonPath("$.headers").contains(List.of("contrastName", "referenceOrTest", "run")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.length()").value(10));
    }

    @Test
    void throwsErrorWithInValidPageSizeParameter() throws Exception {
        mockMvc.perform(get(URL, "E-EHCA-2")
                        .param("pageSize", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void throwsErrorWithInValidPageNoParameter() throws Exception {
        mockMvc.perform(get(URL, "E-EHCA-2")
                        .param("pageNo", "0"))
                .andExpect(status().isBadRequest());
    }
}