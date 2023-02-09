package uk.ac.ebi.atlas.search.suggester;

import org.apache.solr.client.solrj.response.Suggestion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.testutils.SpeciesUtils;

import javax.inject.Inject;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
class SearchSuggesterDaoIT {

    @Inject
    private SpeciesUtils speciesUtils;

    @Inject
    private SuggesterDao subject;

    @Test
    void idSuggestionsCanBeFetched() {
        assertThat(subject.fetchBioentityProperties("asp", 10, false))
                .isNotEmpty();
    }

    @Test
    void doesNotContainDuplicates() {
        String query = randomAlphabetic(3, 4);

        // Solr is super-weird and can return different suggestions for the same query, so we store them in results...
        var results =
                subject.fetchBioentityProperties(query.toLowerCase(), 100, false)
                        .collect(toImmutableList());
        // ... we used to compare them by doing two method calls before we found out about this
        assertThat(results)
                .hasSameSizeAs(results.stream().distinct().collect(toImmutableList()));
    }

    @Test
    void filtersOnSpecies() {
        long numberOfUnfilteredSuggestions =
                subject.fetchBioentityProperties("aspm", 10, false).count();
        long numberOfFilteredSuggestions =
                subject.fetchBioentityProperties("aspm", 10, false, speciesUtils.getHuman()).count();
        assertThat(numberOfFilteredSuggestions)
                .isGreaterThan(0)
                .isLessThan(numberOfUnfilteredSuggestions);
    }

    @Test
    void filtersOnMultipleSpecies() {
        long numberOfUnfilteredSuggestions =
                subject.fetchBioentityProperties("aspm", 10, false).count();
        long numberOfHumanFilteredSuggestions =
                subject.fetchBioentityProperties("aspm", 10, false, speciesUtils.getHuman()).count();
        long numberOfHumanAndMouseFilteredSuggestions =
                subject.fetchBioentityProperties(
                        "aspm", 10, false, speciesUtils.getHuman(), speciesUtils.getMouse()).count();

        assertThat(numberOfHumanAndMouseFilteredSuggestions)
                .isGreaterThan(0)
                .isGreaterThan(numberOfHumanFilteredSuggestions)
                .isLessThanOrEqualTo(numberOfUnfilteredSuggestions);
    }

    @Test
    void atLeastTwoCharactersRequired() {
        assertThat(subject.fetchBioentityProperties("a", 10, false))
                .isEmpty();
        assertThat(subject.fetchBioentityProperties("a", 10, false, speciesUtils.getHuman()))
                .isEmpty();

        assertThat(subject.fetchBioentityProperties("ar", 10, false))
                .isNotEmpty();
        assertThat(subject.fetchBioentityProperties("ar", 10, false, speciesUtils.getHuman()))
                .isNotEmpty();
    }

    @Test
    void closestMatchIsFirst() {
        var twoCharSymbolWithAllCaps = "AR";
        var twoCharSymbolWithLowercase = "ar";

        // Suggestion::equals is established only by term and payload, weight isn’t considered
        var bioEntitiesPropsWithAllCaps =
                subject.fetchBioentityProperties(twoCharSymbolWithAllCaps, 10, false).
                        collect(Collectors.toList());
        var bioEntitiesPropsWithAllLowercase =
                subject.fetchBioentityProperties(twoCharSymbolWithLowercase, 10, false, speciesUtils.getHuman())
                        .collect(Collectors.toList());
        var closestMatchingSuggestion = new Suggestion(twoCharSymbolWithAllCaps, 0, "symbol");
        var firstBioEntitiesSuggestionFromAllCapsQueryResult = bioEntitiesPropsWithAllCaps.get(0);
        var firstBioEntitiesSuggestionFromLowercaseQueryResult = bioEntitiesPropsWithAllLowercase.get(0);

        assertThat(bioEntitiesPropsWithAllCaps).isNotEmpty();
        assertThat(firstBioEntitiesSuggestionFromAllCapsQueryResult.getTerm())
                .isEqualToIgnoringCase(closestMatchingSuggestion.getTerm());
        assertThat(firstBioEntitiesSuggestionFromAllCapsQueryResult.getPayload())
                .isEqualToIgnoringCase(closestMatchingSuggestion.getPayload());

        assertThat(bioEntitiesPropsWithAllLowercase).isNotEmpty();
        assertThat(firstBioEntitiesSuggestionFromLowercaseQueryResult.getTerm())
                .isEqualToIgnoringCase(closestMatchingSuggestion.getTerm());
        assertThat(firstBioEntitiesSuggestionFromLowercaseQueryResult.getPayload())
                .isEqualToIgnoringCase(closestMatchingSuggestion.getPayload());
    }
}
