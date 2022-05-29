package uk.ac.ebi.atlas.search;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterDao;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterService;
import uk.ac.ebi.atlas.species.SpeciesFactory;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsSuggesterServiceIT {
    @Inject
    private AnalyticsSuggesterDao analyticsSuggesterDao;

    @Inject
    private SpeciesFactory speciesFactory;

    private AnalyticsSuggesterService subject;

    @BeforeEach
    void setUp() {
        subject = new AnalyticsSuggesterServiceImpl(analyticsSuggesterDao,speciesFactory);
    }

    @Test
    void canFetchMetadataSuggestionsForTheOrganismPartOrOrganism() {
        assertThat(subject.fetchMetadataSuggestions("skin", ArrayUtils.toArray()))
                .isNotEmpty();
        assertThat(subject.fetchMetadataSuggestions("Homo", ArrayUtils.toArray()))
                .isNotEmpty();
    }

    @Test
    void canFetchMetadataSuggestionsForTheCellTypeOrDisease(){
        assertThat(subject.fetchMetadataSuggestions("B cell", ArrayUtils.toArray()))
                .isNotEmpty();
        assertThat(subject.fetchMetadataSuggestions("cancer", ArrayUtils.toArray()))
                .isNotEmpty();
    }
}