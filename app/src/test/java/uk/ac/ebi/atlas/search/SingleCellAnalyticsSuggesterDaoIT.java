package uk.ac.ebi.atlas.search;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterDao;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
class SingleCellAnalyticsSuggesterDaoIT {
    @Inject
    private AnalyticsSuggesterDao subject;

    @Test
    void canFetchSuggestionsForOrganismAndOrganismPrt() {
        assertThat(subject.fetchMetadataSuggestions("Homo", 10)).isNotEmpty();
        assertThat(subject.fetchMetadataSuggestions("skin", 10)).isNotEmpty();
    }

    @Test
    void canFetchSuggestionsForCellTypeAndDisease() {
        assertThat(subject.fetchMetadataSuggestions("B cell", 10)).isNotEmpty();
        assertThat(subject.fetchMetadataSuggestions("cancer", 10)).isNotEmpty();
    }

    @Test
    void doesNotContainDuplicateSuggestions() {
        String query = randomAlphabetic(3, 4);
        var result = subject.fetchMetadataSuggestions(query.toLowerCase(), 10).collect(toImmutableList());;
        assertThat(result).hasSameSizeAs(result.stream().distinct().collect(toImmutableList()));
    }

    @Test
    void atLeastTwoCharactersRequiredToFetchSuggestions() {
        assertThat(subject.fetchMetadataSuggestions("a", 10)).isEmpty();
    }
}
