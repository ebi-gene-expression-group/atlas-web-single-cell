package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.species.Species;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
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

    @DisplayName("Make sure that 'id' and 'parent' attribute values of the CellTypeWheel result always returns different values." +
                 "They should not be the same within the object")
    @Test
    void cellTypeWheelSunburstResultObjectShouldNotContainIdAndParentAttributeValuesSame() {
        ImmutableSet<ImmutableMap<String, ?>> result = subject.getCellTypeWheelSunburst(metadataSearchTerm,cellTypeWheelServiceMock
                .search(metadataSearchTerm, species1.getName()));
        assertThat(result).hasSize(2);
        assertThat(result.asList().get(0).get("id")).isNotEqualTo(result.asList().get(0).get("parent"));
        assertThat(result.asList().get(1).get("id")).isNotEqualTo(result.asList().get(1).get("parent"));
    }
}