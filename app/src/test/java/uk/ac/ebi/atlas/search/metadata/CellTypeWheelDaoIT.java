package uk.ac.ebi.atlas.search.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellTypeWheelDaoIT {
    private final static String MULTIPLE_SPECIES_METADATA_TERM = "leukocyte";

    @Inject
    private CellTypeWheelDao subject;

    @Test
    void knownTermReturnsResultsWithoutNotApplicable() {
        var results = subject.facetSearchCtwFields(MULTIPLE_SPECIES_METADATA_TERM);

        assertThat(
                results.stream()
                        .map(result -> result.get(0))
                        .distinct())
                .doesNotContain("not applicable")
                .containsExactlyInAnyOrder("Homo sapiens", "Mus musculus");
    }

    @Test
    void canFilterBySpecies() {
        var resultsWithoutSpeciesFiltering = subject.facetSearchCtwFields(MULTIPLE_SPECIES_METADATA_TERM);
        var resultsWithSpeciesFitlering = subject.facetSearchCtwFields(MULTIPLE_SPECIES_METADATA_TERM, "Mus musculus");

        assertThat(
                resultsWithoutSpeciesFiltering.stream()
                        .map(result -> result.get(0))
                        .distinct())
                .containsExactlyInAnyOrder("Homo sapiens", "Mus musculus");

        assertThat(
                resultsWithSpeciesFitlering.stream()
                        .map(result -> result.get(0))
                        .distinct())
                .containsExactly("Mus musculus");
    }

    @Test
    void unknownTermReturnsEmpty() {
        assertThat(subject.facetSearchCtwFields("foobar")).isEmpty();
    }

}