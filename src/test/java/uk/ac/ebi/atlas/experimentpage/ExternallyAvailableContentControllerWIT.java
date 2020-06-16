package uk.ac.ebi.atlas.experimentpage;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.download.ExternallyAvailableContent;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
class ExternallyAvailableContentControllerWIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternallyAvailableContentControllerWIT.class);

    @Inject
    private ExperimentTrader experimentTrader;

    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    private String endPointForExperiment(String accession, ExternallyAvailableContent.ContentType contentType) {
        return MessageFormat.format("/json/experiments/{0}/resources/{1}", accession, contentType);
    }

    private void testAllResourcesAreNonemptyAndContainValidLinks(String accession,
                                                                 ExternallyAvailableContent.ContentType contentType,
                                                                 boolean expectNonEmpty) throws Exception {
        var sb = new StringBuilder();
        this.mockMvc.perform(get(endPointForExperiment(accession, contentType)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(mvcResult -> sb.append(mvcResult.getResponse().getContentAsString()));

        List<String> urls = JsonPath.parse(sb.toString()).read("$..url");
        if (expectNonEmpty) {
            assertThat(urls).isNotEmpty();
        }

        for (var url : urls) {
             if (!url.contains("www.ebi.ac.uk")) {
                LOGGER.info(url);
                this.mockMvc.perform(get("/" + url)).andExpect(status().isOk());
            }
        }
    }

    @Test
    void shouldReturnSomeResourcesForEachExperiment() throws Exception {
        for (var experiment : experimentTrader.getPublicExperiments()) {
            testAllResourcesAreNonemptyAndContainValidLinks(
                    experiment.getAccession(), ExternallyAvailableContent.ContentType.DATA, true);
            testAllResourcesAreNonemptyAndContainValidLinks(
                    experiment.getAccession(), ExternallyAvailableContent.ContentType.SUPPLEMENTARY_INFORMATION, true);
        }

        for (var experiment :
                experimentTrader.getPublicExperiments(SINGLE_CELL_RNASEQ_MRNA_BASELINE)) {
            testAllResourcesAreNonemptyAndContainValidLinks(
                    experiment.getAccession(), ExternallyAvailableContent.ContentType.PLOTS, false);
        }
    }
}
