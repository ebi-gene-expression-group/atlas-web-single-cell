package uk.ac.ebi.atlas.home;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.species.SpeciesProperties;
import uk.ac.ebi.atlas.species.SpeciesPropertiesTrader;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@Sql({"/fixtures/202203/experiment.sql"})
@Sql(scripts = "/fixtures/202203/experiment-delete.sql", executionPhase = AFTER_TEST_METHOD)
class JsonSpeciesSummaryControllerWIT {
    @Inject
    private SpeciesPropertiesTrader speciesPropertiesTrader;

    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    private static final String ENDPOINT_URL = "/json/species-summary";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void responseIsWellFormed() throws Exception {
        var kingdoms = speciesPropertiesTrader.getAll().stream()
                .map(SpeciesProperties::kingdom)
                .collect(toImmutableSet());

        var json = mockMvc.perform(get(ENDPOINT_URL)).andReturn().getResponse().getContentAsString();
        var jsonCtx = JsonPath.parse(json);

        var jsonKingdoms = jsonCtx.<List<String>>read("$.speciesSummary[*].kingdom");
        assertThat(jsonKingdoms).containsAnyElementsOf(kingdoms);

        var jsonCardTypes = jsonCtx.<List<String>>read("$.speciesSummary[*].cards[*].iconType");
        assertThat(jsonCardTypes).containsAnyOf("species");

        var jsonCardSources = jsonCtx.<List<String>>read("$.speciesSummary[*].cards[*].iconSrc");
        assertThat(jsonCardSources)
                .allMatch(iconSrc -> speciesPropertiesTrader.get(iconSrc) != SpeciesProperties.UNKNOWN);
    }
}
