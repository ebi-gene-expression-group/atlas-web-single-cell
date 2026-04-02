package uk.ac.ebi.atlas.staticpages;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StaticPageControllerTestWIT {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void help() throws Exception {
        var viewName = "help";
        this.mockMvc
                .perform(get("/" + viewName + ".html"))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName));
    }

    @Test
    void helpContainsPrivacyNoticeLink() throws Exception {
        this.mockMvc
                .perform(get("/help.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://www.ebi.ac.uk/data-protection/privacy-notice/embl-ebi-public-website/")))
                .andExpect(content().string(containsString("Privacy Policy")));
    }

    @Test
    void invalidUrlsAreMappedToErrorView() throws Exception {
        this.mockMvc
            .perform(get("/foo.html"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error-page"));
    }
}
