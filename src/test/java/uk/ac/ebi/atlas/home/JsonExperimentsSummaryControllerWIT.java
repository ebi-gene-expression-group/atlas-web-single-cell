package uk.ac.ebi.atlas.home;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = BEFORE_CLASS)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
public class JsonExperimentsSummaryControllerWIT {
    @Inject
    private DataSource dataSource;

    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    private static final String ENDPOINT_URL = "/json/experiments-summary";

    @BeforeEach
    void setUp() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-fixture.sql"));
        populator.execute(dataSource);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @AfterEach
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @Test
    void hasLatestAndFeaturedExperiments() throws Exception {
        mockMvc.perform(get(ENDPOINT_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.latestExperiments").isNotEmpty())
                .andExpect(jsonPath("$.featuredExperiments").isNotEmpty());
    }
}
