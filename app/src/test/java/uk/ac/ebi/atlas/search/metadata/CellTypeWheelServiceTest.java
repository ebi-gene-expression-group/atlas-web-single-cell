package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.search.FeaturedSpeciesService;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

@ExtendWith(MockitoExtension.class)
class CellTypeWheelServiceTest {
    @Mock
    private CellTypeWheelDao cellTypeWheelDaoMock;
    @Mock
    private FeaturedSpeciesService featuredSpeciesServiceMock;

    private CellTypeWheelService subject;

    @BeforeEach
    void setUp() {
        subject = new CellTypeWheelService(cellTypeWheelDaoMock, featuredSpeciesServiceMock);
    }

    @Test
    void emptyFacetTermsProducesAnEmptyWheel() {
        var metadataSearchTerm =  randomAlphabetic(20);
        var species = generateRandomSpecies();

        when(cellTypeWheelDaoMock.facetSearchCtwFields(metadataSearchTerm, null, false))
                .thenReturn(ImmutableList.of());
        when(cellTypeWheelDaoMock.facetSearchCtwFields(metadataSearchTerm, species.getName(), false))
                .thenReturn(ImmutableList.of());

        assertThat(subject.search(metadataSearchTerm, null)).isEmpty();
        assertThat(subject.search(metadataSearchTerm, species.getName())).isEmpty();
    }

    @Test
    void appendsExperimentAccessionsToHeadSublistsOfAllFacetResults() {
        var metadataSearchTerm = randomAlphabetic(20);

        // One species with two organism parts. The first OP has three cell types, the second has one cell type.
        // Two experiment accessions, one for each organism part.
        var species1 = generateRandomSpecies();

        var species1OrganismPart1 = randomAlphabetic(10);
        var species1OrganismPart1CellType1 = randomAlphabetic(30);
        var species1OrganismPart1CellType2 = randomAlphabetic(30);
        var species1OrganismPart1CellType3 = randomAlphabetic(30);

        var species1OrganismPart2 = randomAlphabetic(10);
        var species1OrganismPart2CellType1 = randomAlphabetic(30);

        var species1OrganismPart1ExperimentAccession = generateRandomExperimentAccession();
        var species1OrganismPart2ExperimentAccession = generateRandomExperimentAccession();

        // A second species with three organism parts. The first OP has four cell types, the second and third have
        // two cell types. Two experiment accessions, one for OPs 1 and 3, another for OP 2.
        var species2 = generateRandomSpecies();

        var species2OrganismPart1 = randomAlphabetic(10);
        var species2OrganismPart2 = randomAlphabetic(10);
        var species2OrganismPart3 = randomAlphabetic(10);
        var species2OrganismPart1CellType1 = randomAlphabetic(30);
        var species2OrganismPart1CellType2 = randomAlphabetic(30);
        var species2OrganismPart1CellType3 = randomAlphabetic(30);
        var species2OrganismPart1CellType4 = randomAlphabetic(30);

        var species2OrganismPart2CellType1 = randomAlphabetic(30);
        var species2OrganismPart2CellType2 = randomAlphabetic(30);

        var species2OrganismPart3CellType1 = randomAlphabetic(30);
        var species2OrganismPart3CellType2 = randomAlphabetic(30);

        var species2OrganismPart1And3ExperimentAccession = generateRandomExperimentAccession();
        var species2OrganismPart2ExperimentAccession = generateRandomExperimentAccession();

        var twoSpeciesCellTypesWheel =
                ImmutableList.of(
                        ImmutableList.of(
                                species1.getName(),
                                species1OrganismPart1,
                                species1OrganismPart1CellType1,
                                species1OrganismPart1ExperimentAccession
                        ),
                        ImmutableList.of(
                                species1.getName(),
                                species1OrganismPart1,
                                species1OrganismPart1CellType2,
                                species1OrganismPart1ExperimentAccession
                        ),
                        ImmutableList.of(
                                species1.getName(),
                                species1OrganismPart1,
                                species1OrganismPart1CellType3,
                                species1OrganismPart1ExperimentAccession
                        ),
                        ImmutableList.of(
                                species1.getName(),
                                species1OrganismPart2,
                                species1OrganismPart2CellType1,
                                species1OrganismPart2ExperimentAccession
                        ),
                        ImmutableList.of(
                                species2.getName(),
                                species2OrganismPart1,
                                species2OrganismPart1CellType1,
                                species2OrganismPart1And3ExperimentAccession
                        ),
                        ImmutableList.of(
                                species2.getName(),
                                species2OrganismPart1,
                                species2OrganismPart1CellType2,
                                species2OrganismPart1And3ExperimentAccession
                        ),
                        ImmutableList.of(
                                species2.getName(),
                                species2OrganismPart1,
                                species2OrganismPart1CellType3,
                                species2OrganismPart1And3ExperimentAccession
                        ),
                        ImmutableList.of(
                                species2.getName(),
                                species2OrganismPart1,
                                species2OrganismPart1CellType4,
                                species2OrganismPart1And3ExperimentAccession
                        ),
                        ImmutableList.of(
                                species2.getName(),
                                species2OrganismPart2,
                                species2OrganismPart2CellType1,
                                species2OrganismPart2ExperimentAccession
                        ),
                        ImmutableList.of(
                                species2.getName(),
                                species2OrganismPart2,
                                species2OrganismPart2CellType2,
                                species2OrganismPart2ExperimentAccession
                        ),
                        ImmutableList.of(
                                species2.getName(),
                                species2OrganismPart3,
                                species2OrganismPart3CellType1,
                                species2OrganismPart1And3ExperimentAccession
                        ),
                        ImmutableList.of(
                                species2.getName(),
                                species2OrganismPart3,
                                species2OrganismPart3CellType2,
                                species2OrganismPart1And3ExperimentAccession
                        )
                );

        when(cellTypeWheelDaoMock.facetSearchCtwFields(metadataSearchTerm, null, false))
                .thenReturn(twoSpeciesCellTypesWheel);

        assertThat(subject.search(metadataSearchTerm, null))
                .containsExactlyInAnyOrder(
                        // Species 1
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName()),
                                species1OrganismPart1ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName()),
                                species1OrganismPart2ExperimentAccession),
                        // Species 2
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName()),
                                species2OrganismPart1And3ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName()),
                                species2OrganismPart2ExperimentAccession),
                        // Species 1, organism part 1
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart1),
                                species1OrganismPart1ExperimentAccession),
                        // Species 1, organism part 2
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart2),
                                species1OrganismPart2ExperimentAccession),
                        // Species 2, organism part 1
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart1),
                                species2OrganismPart1And3ExperimentAccession),
                        // Species 2, organism part 2
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart2),
                                species2OrganismPart2ExperimentAccession),
                        // Species 2, organism part 3
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart3),
                                species2OrganismPart1And3ExperimentAccession),
                        // Species 1, organism part 1, cell types
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart1, species1OrganismPart1CellType1),
                                species1OrganismPart1ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart1, species1OrganismPart1CellType2),
                                species1OrganismPart1ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart1, species1OrganismPart1CellType3),
                                species1OrganismPart1ExperimentAccession),
                        // Species 1, organism part 2, cell types
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart2, species1OrganismPart2CellType1),
                                species1OrganismPart2ExperimentAccession),
                        // Species 2, organism part 1, cell types
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart1, species2OrganismPart1CellType1),
                                species2OrganismPart1And3ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart1, species2OrganismPart1CellType2),
                                species2OrganismPart1And3ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart1, species2OrganismPart1CellType3),
                                species2OrganismPart1And3ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart1, species2OrganismPart1CellType4),
                                species2OrganismPart1And3ExperimentAccession),
                        // Species 2, organism part 2, cell types
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart2, species2OrganismPart2CellType1),
                                species2OrganismPart2ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart2, species2OrganismPart2CellType2),
                                species2OrganismPart2ExperimentAccession),
                        // Species 2, organism part 3, cell types
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart3, species2OrganismPart3CellType1),
                                species2OrganismPart1And3ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species2.getName(), species2OrganismPart3, species2OrganismPart3CellType2),
                                species2OrganismPart1And3ExperimentAccession));
    }

    @Test
    void appendsExperimentAccessionsToHeadSublistsOfAllFacetResultsWithASpeciesArgument() {
        var metadataSearchTerm =  randomAlphabetic(20);

        // One species with two organism parts. The first OP has three cell types, the second has one cell type.
        // Two experiment accessions, one for each organism part.
        var species1 = generateRandomSpecies();

        var species1OrganismPart1 = randomAlphabetic(10);
        var species1OrganismPart1CellType1 = randomAlphabetic(30);
        var species1OrganismPart1CellType2 = randomAlphabetic(30);
        var species1OrganismPart1CellType3 = randomAlphabetic(30);

        var species1OrganismPart2 = randomAlphabetic(10);
        var species1OrganismPart2CellType1 = randomAlphabetic(30);

        var species1OrganismPart1ExperimentAccession = generateRandomExperimentAccession();
        var species1OrganismPart2ExperimentAccession = generateRandomExperimentAccession();

        var oneSpeciesCellTypesWheel =
                ImmutableList.of(
                        ImmutableList.of(
                                species1.getName(),
                                species1OrganismPart1,
                                species1OrganismPart1CellType1,
                                species1OrganismPart1ExperimentAccession
                        ),
                        ImmutableList.of(
                                species1.getName(),
                                species1OrganismPart1,
                                species1OrganismPart1CellType2,
                                species1OrganismPart1ExperimentAccession
                        ),
                        ImmutableList.of(
                                species1.getName(),
                                species1OrganismPart1,
                                species1OrganismPart1CellType3,
                                species1OrganismPart1ExperimentAccession
                        ),
                        ImmutableList.of(
                                species1.getName(),
                                species1OrganismPart2,
                                species1OrganismPart2CellType1,
                                species1OrganismPart2ExperimentAccession
                        ));

        when(cellTypeWheelDaoMock.facetSearchCtwFields(metadataSearchTerm, species1.getName(), false))
                .thenReturn(oneSpeciesCellTypesWheel);

        assertThat(subject.search(metadataSearchTerm, species1.getName()))
                .containsExactlyInAnyOrder(
                        // Species
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName()),
                                species1OrganismPart1ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName()),
                                species1OrganismPart2ExperimentAccession),
                        // Organism part 1
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart1),
                                species1OrganismPart1ExperimentAccession),
                        // Organism part 2
                        ImmutablePair.of(
                                ImmutableList.of(species1.getName(), species1OrganismPart2),
                                species1OrganismPart2ExperimentAccession),
                        // Organism part 1, cell types
                        ImmutablePair.of(
                                ImmutableList.of(
                                        species1.getName(), species1OrganismPart1, species1OrganismPart1CellType1),
                                species1OrganismPart1ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(
                                        species1.getName(), species1OrganismPart1, species1OrganismPart1CellType2),
                                species1OrganismPart1ExperimentAccession),
                        ImmutablePair.of(
                                ImmutableList.of(
                                        species1.getName(), species1OrganismPart1, species1OrganismPart1CellType3),
                                species1OrganismPart1ExperimentAccession),
                        // Organism part 2, cell types
                        ImmutablePair.of(
                                ImmutableList.of(
                                        species1.getName(), species1OrganismPart2, species1OrganismPart2CellType1),
                                species1OrganismPart2ExperimentAccession));
    }
}
