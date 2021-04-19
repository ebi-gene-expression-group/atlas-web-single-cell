package uk.ac.ebi.atlas.home;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
// This test is a quite the mystery: running it together with JsonExperimentsSummaryControllerWIT would make the latter
// fail because it will re-use the DB in whatever state is in here. If we add the experiments fixture here it will pass
// and if we don’t it will fail. Moreover, adding the fixture in JsonExperimentsSummaryControllerWIT will have no
// effect, and trying to clean the experiment table with experiment-delete.sql here in either @AfterAll or with
// @Sql(..., executionPhase = AFTER_TEST_METHOD) seems also not to work. For whatever reason, this test cripples the DB
// and only @DirtiesContext solves the issue. Future Atlas developers: I hope you can explain to me what’s going on
// here!
@DirtiesContext
class HomeControllerWIT {
    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void homeReturnsValidModel() throws Exception {
        this.mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    void homeModelHaveAssayInfo() throws Exception {
        this.mockMvc.perform(get("/home"))
                .andExpect(model().attributeExists("numberOfCells"));
    }
}
