package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.species.Species;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

@ExtendWith(MockitoExtension.class)
class HighchartsSunburstAdapterTest {
    @Mock
    private CellTypeWheelService cellTypeWheelServiceMock;
    private final HighchartsSunburstAdapter subject = new HighchartsSunburstAdapter();

    @Test
    void whenSearchTermFoundAndNotFilteringBySpecies_ThenReturnResult() {
        var species = generateSpecies(1);
        var speciesName = species.get(0).getName();

        when(cellTypeWheelServiceMock.search(speciesName, null))
                .thenReturn(getSearchResultForASpecies(species));

        var result = subject.getCellTypeWheelSunburst(speciesName,
                cellTypeWheelServiceMock.search(speciesName, null));
        assertThat(result.isEmpty()).isFalse();

        final ImmutableList<ImmutableMap<String, ?>> resultAsList = result.asList();
        var centreRingElement = resultAsList.get(0);
        assertThat(centreRingElement.get("parent")).isEqualTo("");
        assertThat((ImmutableSet<String>) centreRingElement.get("experimentAccessions")).isNotEmpty();

        assertRemainingRingElements(resultAsList);
    }

    @Test
    void whenSearchTermFoundAndFilteringBySpecies_ThenReturnResult() {
        var species = generateSpecies(1);
        var speciesName = species.get(0).getName();

        when(cellTypeWheelServiceMock.search(speciesName, speciesName))
                .thenReturn(getSearchResultForASpecies(species));

        var result = subject.getCellTypeWheelSunburst(speciesName,
                cellTypeWheelServiceMock.search(speciesName, speciesName));
        assertThat(result.isEmpty()).isFalse();

        final ImmutableList<ImmutableMap<String, ?>> resultAsList = result.asList();
        var centreRingElement = resultAsList.get(0);
        assertThat(centreRingElement.get("parent")).isEqualTo("");
        assertThat((ImmutableSet<String>) centreRingElement.get("experimentAccessions")).isNotEmpty();

        assertRemainingRingElements(resultAsList);
        assertAllResultsParentIsGivenSpecies(speciesName, resultAsList);
    }

    @Test
    void whenNotFilteringBySpeciesAndSearchTermIsNotFound_ThenReturnEmptyResult() {
        final String searchTermNotInDatabase = "searchTermNotInDatabase";
        when(cellTypeWheelServiceMock.search(searchTermNotInDatabase, null))
                .thenReturn(ImmutableSet.of());
        var result = subject.getCellTypeWheelSunburst(searchTermNotInDatabase,
                cellTypeWheelServiceMock.search(searchTermNotInDatabase, null));

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void whenFilteringBySpeciesButSearchTermIsNotFound_ThenReturnEmptyResult() {
        final String searchTermNotInDatabase = "searchTermNotInDatabase";
        when(cellTypeWheelServiceMock.search(searchTermNotInDatabase, null))
                .thenReturn(ImmutableSet.of());
        var result = subject.getCellTypeWheelSunburst(searchTermNotInDatabase,
                cellTypeWheelServiceMock.search(searchTermNotInDatabase, null));

        assertThat(result.isEmpty()).isTrue();
    }


    private void assertRemainingRingElements(ImmutableList<ImmutableMap<String, ?>> resultAsList) {
        resultAsList.subList(1, resultAsList.size()).forEach(element -> {
            var name = element.get("name");
            var id = element.get("id");
            var parent = element.get("parent");
            assertThat(name).isNotNull();
            assertThat(id).isNotNull();
            assertThat(parent).isNotNull();
            assertThat(name.equals(id) && id.equals(parent)).isFalse();
            assertThat(element.get("experimentAccessions")).isNotNull();
        });
    }

    private void assertAllResultsParentIsGivenSpecies(String speciesName,
                                                      ImmutableList<ImmutableMap<String, ?>> resultAsList) {
        resultAsList.forEach(element -> {
            var id = element.get("id").toString();
            var parent = element.get("parent").toString();
            assertThat(id.startsWith(speciesName)).isTrue();
            if (!parent.isBlank()) {
                assertThat(parent.startsWith(speciesName)).isTrue();
            }
        });
    }

    private List<Species> generateSpecies(int numberOfSpecies) {
        return IntStream.rangeClosed(1, numberOfSpecies)
                .mapToObj(i -> generateRandomSpecies())
                .collect(toList());
    }

    private List<String> generateOrganism(int numberOfOrganism) {
        return IntStream.rangeClosed(1, numberOfOrganism)
                .mapToObj(i -> "organism_" + randomAlphabetic(10))
                .collect(toList());
    }

    private List<String> generateCellTypes(int numberOfCellTypes) {
        return IntStream.rangeClosed(1, numberOfCellTypes)
                .mapToObj(i -> "cellType_" + randomAlphabetic(30))
                .collect(toList());
    }

    private List<String> generateAccessions(int numberOfAccessions) {
        return IntStream.rangeClosed(1, numberOfAccessions)
                .mapToObj(i -> "acc_" + generateRandomExperimentAccession())
                .collect(toList());
    }

    private ImmutableSet<ImmutablePair<ImmutableList<String>, String>> getSearchResultForASpecies(List<Species> species) {
        var organismParts = generateOrganism(2);
        var cellTypes = generateCellTypes(4);
        var accessions = generateAccessions(2);

        return // Species
                ImmutableSet.of(
                        ImmutablePair.of(
                                ImmutableList.of(species.get(0).getName()),
                                accessions.get(0)),
                        ImmutablePair.of(
                                ImmutableList.of(species.get(0).getName()),
                                accessions.get(1)),
                        // Organism part 1
                        ImmutablePair.of(
                                ImmutableList.of(species.get(0).getName(), organismParts.get(0)),
                                accessions.get(0)),
                        // Organism part 2
                        ImmutablePair.of(
                                ImmutableList.of(species.get(0).getName(), organismParts.get(1)),
                                accessions.get(1)),
                        // Organism part 1, cell types
                        ImmutablePair.of(
                                ImmutableList.of(species.get(0).getName(), organismParts.get(0), cellTypes.get(0)),
                                accessions.get(0)),
                        ImmutablePair.of(
                                ImmutableList.of(species.get(0).getName(), organismParts.get(0), cellTypes.get(1)),
                                accessions.get(0)),
                        ImmutablePair.of(
                                ImmutableList.of(species.get(0).getName(), organismParts.get(0), cellTypes.get(2)),
                                accessions.get(0)),
                        // Organism part 2, cell types
                        ImmutablePair.of(
                                ImmutableList.of(species.get(0).getName(), organismParts.get(1), cellTypes.get(3)),
                                accessions.get(1))
                );
    }
}
