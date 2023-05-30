package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.testutils.RandomDataTestUtils;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

@ExtendWith(MockitoExtension.class)
class HighchartsSunburstAdapterTest {
    @Mock
    private CellTypeWheelService cellTypeWheelServiceMock;
    private HighchartsSunburstAdapter subject;
    private String metadataSearchTerm =  randomAlphabetic(20);
    private Species species1 = generateRandomSpecies();

    @BeforeEach
    void setUp() {
        subject = new HighchartsSunburstAdapter();
        var species1OrganismPart1 = randomAlphabetic(10);
        var species1OrganismPart1CellType1 = randomAlphabetic(30);
        var species1OrganismPart1ExperimentAccession = generateRandomExperimentAccession();
        var species1OrganismPart1CellType2 = randomAlphabetic(30);
        var species1OrganismPart1CellType3 = randomAlphabetic(30);
        var species1OrganismPart2 = randomAlphabetic(10);
        var species1OrganismPart2CellType1 = randomAlphabetic(30);
        var species1OrganismPart2ExperimentAccession = generateRandomExperimentAccession();

        when(cellTypeWheelServiceMock.search(metadataSearchTerm, species1.getName()))
                .thenReturn(
                        // Species
                        ImmutableSet.of(ImmutablePair.of(
                                ImmutableList.of(species1.getName()),
                                species1OrganismPart1ExperimentAccession)),
                        ImmutableSet.of(ImmutablePair.of(
                                ImmutableList.of(species1.getName()),
                                species1OrganismPart2ExperimentAccession)),
                        // Organism part 1
                        ImmutableSet.of(ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart1),
                                species1OrganismPart1ExperimentAccession)),
                        // Organism part 2
                        ImmutableSet.of(ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart2),
                                species1OrganismPart2ExperimentAccession)),
                        // Organism part 1, cell types
                        ImmutableSet.of(ImmutablePair.of(
                                ImmutableList.of(
                                        species1.getName(), species1OrganismPart1, species1OrganismPart1CellType1),
                                species1OrganismPart1ExperimentAccession)),
                        ImmutableSet.of(ImmutablePair.of(
                                ImmutableList.of(
                                        species1.getName(), species1OrganismPart1, species1OrganismPart1CellType2),
                                species1OrganismPart1ExperimentAccession)),
                        ImmutableSet.of(ImmutablePair.of(
                                ImmutableList.of(
                                        species1.getName(), species1OrganismPart1, species1OrganismPart1CellType3),
                                species1OrganismPart1ExperimentAccession)),
                        // Organism part 2, cell types
                        ImmutableSet.of(ImmutablePair.of(
                                ImmutableList.of(
                                        species1.getName(), species1OrganismPart2, species1OrganismPart2CellType1),
                                species1OrganismPart2ExperimentAccession)));
    }

    @Test
    void responseShouldContainIdAndParenAttributesAreNonEmptyValues() {
        var result = subject.getCellTypeWheelSunburst(metadataSearchTerm, cellTypeWheelServiceMock
                .search(metadataSearchTerm, species1.getName()));
        assertFalse(result.isEmpty());
        assertNotEquals("", result.asList().get(1).get("id"));
        assertNotEquals("", result.asList().get(1).get("parent"));
    }

    @Test
    void failsIfIdAndParentFieldValuesAreSame() {
        var species = generateRandomSpecies();
        var experimentAccession1 = RandomDataTestUtils.generateRandomExperimentAccession();
        var experimentAccession2 = RandomDataTestUtils.generateRandomExperimentAccession();
        var experimentAccession3 = RandomDataTestUtils.generateRandomExperimentAccession();

        when(cellTypeWheelServiceMock.search(metadataSearchTerm, species1.getName()))
                .thenReturn(
                        ImmutableSet.of(
                                ImmutablePair.of(
                                        ImmutableList.of(species.getName()), (experimentAccession1))),
                        ImmutableSet.of(
                                ImmutablePair.of(
                                        ImmutableList.of(species.getName()), (experimentAccession2))),
                        ImmutableSet.of(
                                ImmutablePair.of(
                                        ImmutableList.of(species.getName()), (experimentAccession3))));

        var result = subject.getCellTypeWheelSunburst(metadataSearchTerm,
                cellTypeWheelServiceMock.search(metadataSearchTerm, species1.getName()));

        assertThat(result).hasSize(2);
        assertNotEquals(result.asList().get(0).get("id"), (result.asList().get(0).get("parent")));
        assertNotEquals(result.asList().get(1).get("id"), (result.asList().get(1).get("parent")));
    }
}
