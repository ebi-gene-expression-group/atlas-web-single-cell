package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataDao;
import uk.ac.ebi.atlas.testutils.RandomDataTestUtils;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CellPlotServiceTest {
    private static final int MAX_CELL_COUNT = 10000;
    private static final int MAX_K = 100;
    private static final int MAX_METADATA_VALUES = 20;
    private static final Random RNG = ThreadLocalRandom.current();

    @Mock
    private CellPlotDao cellPlotDaoMock;

    @Mock
    private CellMetadataDao cellMetadataDaoMock;

    private CellPlotService subject;

    @BeforeEach
    void setUp() {
        subject = new CellPlotService(cellPlotDaoMock, cellMetadataDaoMock);
    }

    @Test
    void clusterPlotWithK() {
        var experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        var cellCount = RNG.nextInt(MAX_CELL_COUNT) + 1;
        var k = RNG.nextInt(MAX_K) + 1;

        var points = RandomDataTestUtils.generateRandomTSnePointDtosWithClusters(cellCount, k);
        when(cellPlotDaoMock.fetchCellPlotWithK(experimentAccession, k, "umap", ImmutableMap.of()))
                .thenReturn(ImmutableList.copyOf(points));

        var result = subject.clusterPlotWithK(experimentAccession, k, "umap", ImmutableMap.of());
        assertThat(result)
                .hasSize(k);
        assertThat(result.values().stream().mapToInt(Collection::size).sum())
                .isEqualTo(cellCount);
    }

    @Test
    void clusterPlotWithMetadata() {
        var experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        var cellCount = RNG.nextInt(MAX_CELL_COUNT) + 1;

        var points = RandomDataTestUtils.generateRandomTSnePointDtos(cellCount);
        when(cellPlotDaoMock.fetchCellPlot(experimentAccession, "umap", ImmutableMap.of()))
                .thenReturn(ImmutableList.copyOf(points));

        var metadataCategory = randomAlphabetic(20);
        var metadataValues =
                IntStream.range(0, RNG.nextInt(MAX_METADATA_VALUES) + 1)
                        .boxed()
                        .map(__ -> randomAlphabetic(10))
                        .collect(toImmutableList());
        var metadataMapping = points.stream()
                .collect(toImmutableMap(
                        point -> point.name(),
                        __ -> metadataValues.get(RNG.nextInt(metadataValues.size()))));
        when(cellMetadataDaoMock.getMetadataValues(experimentAccession, metadataCategory))
                .thenReturn(metadataMapping);

        var result = subject.clusterPlotWithMetadata(experimentAccession, metadataCategory, "umap", ImmutableMap.of());
        // It would be very unlucky to have a very low number of cells and metadata values for this test to fail,
        // but if it happens then we should set a minimum value for both to make it more robust
        assertThat(result)
                .hasSameSizeAs(metadataValues);
        assertThat(result.values().stream().mapToInt(Collection::size).sum())
                .isEqualTo(cellCount);
    }

    @Test
    void expressionPlot() {
        var experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        var cellCount = RNG.nextInt(MAX_CELL_COUNT) + 1;
        var geneId = RandomDataTestUtils.generateRandomEnsemblGeneId();

        var points = RandomDataTestUtils.generateRandomTSnePointDtosWithExpression(RNG.nextInt(cellCount));
        when(cellPlotDaoMock.fetchCellPlotWithExpression(experimentAccession, geneId, "umap", ImmutableMap.of()))
                .thenReturn(ImmutableList.copyOf(points));

        var result = subject.expressionPlot(experimentAccession, geneId, "umap", ImmutableMap.of());

        assertThat(result)
                .hasSize(points.size());
    }

    @Test
    void canFetchDefaultPlotTypeWithPlotOption(){
        when(cellPlotDaoMock.fetchDefaultPlotMethodWithParameterisation("E-CURD-4"))
                .thenReturn(ImmutableMap.of("UMAP","{\"n_neighbors\": 15}"));

        assertThat(subject.fetchDefaultPlotMethodWithParameterisation("E-CURD-4"))
                .containsKeys("UMAP")
                .containsValues("{\"n_neighbors\": 15}");
    }

    @Test
    void canReturnEmptyResultIfThereIsNoDefaultPlotTypeAndOption(){
        when(cellPlotDaoMock.fetchDefaultPlotMethodWithParameterisation("fooBar"))
                .thenReturn(ImmutableMap.of());

        assertThat(subject.fetchDefaultPlotMethodWithParameterisation("fooBar"))
                .isEmpty();
    }
}
