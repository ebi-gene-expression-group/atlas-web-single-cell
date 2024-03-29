package uk.ac.ebi.atlas.search.species;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName.SYMBOL;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpeciesSearchServiceTest {

    @Mock
    private SpeciesSearchDao speciesSearchDao;

    private SpeciesSearchService subject;

    @BeforeEach
    void setUp() {
        subject = new SpeciesSearchService(speciesSearchDao);
    }

    @Test
    void whenEmptySearchTextProvidedReturnEmptySet() {
        var emptySearchText = "";
        var searchCategory = SYMBOL.name();

        var emptyListOfSpecies = subject.search(emptySearchText, searchCategory);

        assertThat(emptyListOfSpecies).isEmpty();
    }

    @Test
    void whenNoSearchTextNotFoundReturnEmptySet() {
        var notExistingSearchText = "NotExistingGeneId";
        var searchCategory = SYMBOL.name();

        when(speciesSearchDao.searchSpecies(notExistingSearchText, searchCategory))
                .thenReturn(ImmutableSet.of());

        var emptyListOfSpecies = subject.search(notExistingSearchText, searchCategory);

        assertThat(emptyListOfSpecies).isEmpty();
    }

    @Test
    void whenSearchTextFoundInDBReturnResult() {
        var existingGeneIdForHuman = "ExistingGeneId";
        var searchCategory = SYMBOL.name();
        var expectedSpecies = "Homo_sapiens";

        when(speciesSearchDao.searchSpecies(existingGeneIdForHuman, searchCategory))
                .thenReturn(ImmutableSet.of(expectedSpecies));

        var species = subject.search(existingGeneIdForHuman, searchCategory);

        assertThat(species).contains(expectedSpecies);
    }
}
