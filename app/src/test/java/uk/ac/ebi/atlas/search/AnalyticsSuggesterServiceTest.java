package uk.ac.ebi.atlas.search;

import org.apache.solr.client.solrj.response.Suggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterDao;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterService;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;

import java.util.stream.Stream;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsSuggesterServiceTest {
    @Mock
    private AnalyticsSuggesterDao suggesterDaoMock;

    @Mock
    private SpeciesFactory speciesFactoryMock;

    private Species species;

    private AnalyticsSuggesterService subject;

    @BeforeEach
    void setUp() {
        species = generateRandomSpecies();

        when(speciesFactoryMock.create(species.getName()))
                .thenReturn(species);

        when(suggesterDaoMock.fetchMetadataSuggestions(anyString(), anyInt()))
                .thenReturn(Stream.of(
                        new Suggestion(randomAlphanumeric(10), 10, species.getName()),
                        new Suggestion(randomAlphanumeric(10), 20, generateRandomSpecies().getName()),
                        new Suggestion(randomAlphanumeric(10), 10, generateRandomSpecies().getName())));

        subject = new AnalyticsSuggesterServiceImpl(suggesterDaoMock, speciesFactoryMock);
    }

    @Test
    void fetchSuggestionsReturnsResultsWhenNoSpeciesIsSpecified() {
        assertThat(subject.fetchMetadataSuggestions(randomAlphanumeric(3))).hasSize(3);
        verify(suggesterDaoMock).fetchMetadataSuggestions(anyString(), anyInt());
    }

    @Test
    void allSuggestionsContainTermAndCategory() {
        assertThat(subject.fetchMetadataSuggestions(randomAlphanumeric(3)))
                .allMatch(mappedSuggestion ->
                        mappedSuggestion.containsKey("term") && mappedSuggestion.containsKey("category"))
                .hasSize(3);
    }

    @Test
    void filteringSuggestionsBySpecies() {
        assertThat(subject.fetchMetadataSuggestions(randomAlphanumeric(3), species.getName())).hasSize(1);
    }
}