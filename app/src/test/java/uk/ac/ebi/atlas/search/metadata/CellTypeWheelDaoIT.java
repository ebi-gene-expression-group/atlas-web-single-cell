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
    @Inject
    private CellTypeWheelDao subject;

    @Test
    void knownTermReturnsResults() {
        var results = subject.facetSearchCtwFields("leukocyte");

        assertThat(results).isNotEmpty();
        assertThat(
                results.stream()
                        .map(result -> result.get(0))
                        .distinct()
                        .collect(toImmutableList()))
                .doesNotContain("not applicable")
                .containsExactlyInAnyOrder("Homo sapiens", "Mus musculus");
    }

    @Test
    void unknownTermReturnsEmpty() {
        assertThat(subject.facetSearchCtwFields("foobar")).isEmpty();
    }

}