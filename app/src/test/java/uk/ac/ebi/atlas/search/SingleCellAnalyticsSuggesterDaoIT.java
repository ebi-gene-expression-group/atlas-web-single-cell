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
import java.util.stream.Collectors;

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
        assertThat(subject.fetchOntologyAnnotationSuggestions("Homo", 10, ArrayUtils.toArray())).isNotEmpty();
        assertThat(subject.fetchOntologyAnnotationSuggestions("skin", 10, ArrayUtils.toArray())).isNotEmpty();
    }

    @Test
    void canFetchSuggestionsForCellTypeAndDisease() {
        assertThat(subject.fetchOntologyAnnotationSuggestions("B cell", 10, ArrayUtils.toArray())).isNotEmpty();
        assertThat(subject.fetchOntologyAnnotationSuggestions("cancer", 10, ArrayUtils.toArray())).isNotEmpty();
    }

    @Test
    void doesNotContainDuplicateSuggestions() {
        String query = randomAlphabetic(3, 4);
        var result = subject.fetchOntologyAnnotationSuggestions(query.toLowerCase(), 10, ArrayUtils.toArray())
                .collect(toImmutableList());;
        assertThat(result).hasSameSizeAs(result.stream().distinct().collect(toImmutableList()));
    }

    @Test
    void atLeastTwoCharactersRequiredToFetchSuggestions() {
        assertThat(subject.fetchOntologyAnnotationSuggestions("a", 10, ArrayUtils.toArray())).isEmpty();
    }
}
