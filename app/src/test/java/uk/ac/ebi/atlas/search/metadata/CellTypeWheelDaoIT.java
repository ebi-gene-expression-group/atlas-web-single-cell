package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;

import javax.inject.Inject;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateBlankString;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellTypeWheelDaoIT {
    private final static Random RNG = ThreadLocalRandom.current();
    private final static String MULTIPLE_SPECIES_METADATA_TERM = "leukocyte";
    private final static ImmutableList<String> MULTIPLE_SPECIES = ImmutableList.of("Homo sapiens", "Mus musculus");

    @Inject
    private CellTypeWheelDao subject;

    @Test
    void knownTermReturnsResultsWithoutNotApplicable() {
        var results = subject.facetSearchCtwFields(MULTIPLE_SPECIES_METADATA_TERM, null);

        assertThat(
                results.stream()
                        .map(result -> result.get(0))
                        .distinct())
                .doesNotContain("not applicable")
                .containsExactlyInAnyOrderElementsOf(MULTIPLE_SPECIES);
    }

    @Test
    void canFilterBySpecies() {
        var speciesFilter = MULTIPLE_SPECIES.get(RNG.nextInt(MULTIPLE_SPECIES.size()));
        var resultsWithoutSpeciesFiltering =
                subject.facetSearchCtwFields(MULTIPLE_SPECIES_METADATA_TERM, generateBlankString());
        var resultsWithSpeciesFitlering =
                subject.facetSearchCtwFields(MULTIPLE_SPECIES_METADATA_TERM, speciesFilter);

        assertThat(
                resultsWithoutSpeciesFiltering.stream()
                        .map(result -> result.get(0))
                        .distinct())
                .containsExactlyInAnyOrderElementsOf(MULTIPLE_SPECIES);

        assertThat(
                resultsWithSpeciesFitlering.stream()
                        .map(result -> result.get(0))
                        .distinct())
                .containsExactly(speciesFilter);
    }

    @Test
    void isSpeciesSearchOnlyHasOneSpecies() {
        var species = MULTIPLE_SPECIES.get(RNG.nextInt(MULTIPLE_SPECIES.size()));
        var resultsWithSpeciesSearch =
                subject.speciesSearchCtwFields(species, generateBlankString());
        assertThat(
                resultsWithSpeciesSearch.stream()
                        .map(result -> result.get(0))
                        .distinct())
                .containsExactly(species);
    }

    @Test
    void unknownTermReturnsEmpty() {
        assertThat(subject.facetSearchCtwFields("foobar", generateBlankString())).isEmpty();
    }

}