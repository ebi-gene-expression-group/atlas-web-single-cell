package uk.ac.ebi.atlas.experimentpage.tsneplot;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math.util.MathUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataDao;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;
import uk.ac.ebi.atlas.testutils.RandomDataTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TSnePlotServiceTest {
    private static final int NUMBER_OF_CELLS = 10000;

    @Mock
    private TSnePlotDao tSnePlotDaoMock;
    @Mock
    private CellMetadataDao cellMetadataDaoMock;

    private TSnePlotService subject;

    @BeforeEach
    void setUp() {
        subject = new TSnePlotService(tSnePlotDaoMock, cellMetadataDaoMock);
    }

    @Test
    @DisplayName("Points retrieved by the DAO class are assigned the right cluster")
    void testFetchPlotWithClusters() {
        var experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        var perplexities = new int[]{1, 5, 10, 15, 20};
        var perplexity = perplexities[ThreadLocalRandom.current().nextInt(0, perplexities.length)];
        var k = ThreadLocalRandom.current().nextInt(5, 20);

        var randomPointDtos = RandomDataTestUtils.generateRandomTSnePointDtosWithClusters(NUMBER_OF_CELLS, k);
        when(tSnePlotDaoMock.fetchTSnePlotWithClusters(experimentAccession, perplexity, k))
                .thenReturn(ImmutableList.copyOf(randomPointDtos));

        var results = subject.fetchTSnePlotWithClusters(experimentAccession, perplexity, k);
        for(TSnePoint.Dto tSnePointDto : randomPointDtos) {
            assertThat(results.get(tSnePointDto.clusterId()))
                    .contains(TSnePoint.create(MathUtils.round(tSnePointDto.x(), 2),
                            MathUtils.round(tSnePointDto.y(), 2),
                            tSnePointDto.name()));
        }

        assertThat(results)
                .containsOnlyKeys(
                        randomPointDtos.stream()
                                .collect(groupingBy(TSnePoint.Dto::clusterId))
                                .keySet()
                                .toArray(new Integer[0]));

        assertThat(results.values().stream().flatMap(Set::stream).collect(toSet()))
                .containsExactlyInAnyOrder(
                        randomPointDtos.stream()
                                .map(dto -> TSnePoint.create(MathUtils.round(dto.x(), 2), MathUtils.round(dto.y(), 2),
                                        dto.name()))
                                .toArray(TSnePoint[]::new)
                )
                .extracting("expressionLevel")
                .allMatch(expressionLevel -> expressionLevel == Optional.empty());


    }

    @Test
    @DisplayName("Points retrieved by the DAO class are correctly grouped according to metadata values")
    void testFetchPlotWithMetadata() {
        var experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        var perplexities = new int[]{1, 5, 10, 15, 20};
        var perplexity = perplexities[ThreadLocalRandom.current().nextInt(0, perplexities.length)];
        var metadataCategory = "characteristic_inferred_cell_type";
        var metadataValues = ImmutableList.of("neuron", "stem cell", "B cell");

        var randomPointDtos =
                RandomDataTestUtils.generateRandomTSnePointDtos(NUMBER_OF_CELLS)
                    .stream()
                    .map(pointDto ->
                            TSnePoint.Dto.create(
                                    MathUtils.round(pointDto.x(), 2),
                                    MathUtils.round(pointDto.y(), 2),
                                    pointDto.name()))
                    .collect(toImmutableSet());

        when(tSnePlotDaoMock.fetchTSnePlotForPerplexity(eq(experimentAccession), eq(perplexity)))
                .thenReturn(randomPointDtos.asList());

        // Extract list of cell IDs from t-SNE points
        var cellIds = randomPointDtos
                .stream()
                .map(TSnePoint.Dto::name)
                .collect(toImmutableList());

        assertThat(cellIds).doesNotHaveDuplicates();

        // Assign random metadata value to each cell ID
        var cellMetadata = cellIds
                .stream()
                .collect(toImmutableMap(
                        Function.identity(),
                        value -> metadataValues.get(ThreadLocalRandom.current().nextInt(0, metadataValues.size()))));

        when(
                cellMetadataDaoMock.getMetadataValueForCellIds(eq(experimentAccession), anyString()))
                .thenReturn(cellMetadata);

        var results = subject.fetchTSnePlotWithMetadata(experimentAccession, perplexity, metadataCategory);
        assertThat(results)
                .containsOnlyKeys(metadataValues.stream().map(StringUtils::capitalize).toArray(String[]::new));
    }

    @Test
    @DisplayName("Points DTOs retrieved by the DAO class are correctly transformed to their non-DTO counterparts")
    void testFetchPlotWithExpressionLevels() {
        var experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        var perplexities = new int[]{1, 5, 10, 15, 20};
        var perplexity = perplexities[ThreadLocalRandom.current().nextInt(0, perplexities.length)];
        var geneId = RandomDataTestUtils.generateRandomEnsemblGeneId();

        var randomPointDtos = RandomDataTestUtils.generateRandomTSnePointDtosWithExpression(NUMBER_OF_CELLS);
        when(tSnePlotDaoMock.fetchTSnePlotWithExpression(experimentAccession, perplexity, geneId))
                .thenReturn(ImmutableList.copyOf(randomPointDtos));

        var results = subject.fetchTSnePlotWithExpression(experimentAccession, perplexity, geneId);
        assertThat(results)
                .containsExactlyInAnyOrder(
                        randomPointDtos.stream()
                                .map(dto -> TSnePoint.create(
                                        MathUtils.round(dto.x(), 2),
                                        MathUtils.round(dto.y(), 2),
                                        dto.expressionLevel(),
                                        dto.name()))
                                .toArray(TSnePoint[]::new));
    }

}
