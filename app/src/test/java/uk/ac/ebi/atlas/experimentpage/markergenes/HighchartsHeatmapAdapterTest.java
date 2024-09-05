package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityPropertyDao;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneIds;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomGeneSymbols;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HighchartsHeatmapAdapterTest {
    private final static ThreadLocalRandom RNG = ThreadLocalRandom.current();
    public static final String EXPRESSION_UNIT = "CPM";

    @Mock
    private BioEntityPropertyDao bioEntityPropertyDaoMock;

    private HighchartsHeatmapAdapter subject;

    @BeforeEach
    void setUp() {
        subject = new HighchartsHeatmapAdapter(bioEntityPropertyDaoMock);
    }

    @Test
    void markerGeneWithoutSymbolHasGeneIDAsName() {
        var randomGeneIds = generateRandomEnsemblGeneIds(3);
        var randomGeneSymbols = generateRandomGeneSymbols(2);

        when(bioEntityPropertyDaoMock.getSymbolsForGeneIds(ImmutableSet.copyOf(randomGeneIds)))
                .thenReturn(
                        ImmutableMap.of(
                                randomGeneIds.get(0), randomGeneSymbols.get(0),
                                randomGeneIds.get(1), randomGeneSymbols.get(1)));

        var markerGenes = ImmutableList.of(
                MarkerGene.create(randomGeneIds.get(0), "1", "1", 0.004,
                        "1", 199, 185, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(1), "1", "3", 0.0006,
                        "2", 12, 1.11, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(2), "1", "5", 0.001,
                        "6", 1000, 10000, EXPRESSION_UNIT));

        var result = subject.getMarkerGeneHeatmapDataSortedNaturally(markerGenes);
        assertThat(result).hasSize(3);

        assertThat(result).element(0).extracting("geneName").containsOnly(randomGeneSymbols.get(0));
        assertThat(result).element(1).extracting("geneName").containsOnly(randomGeneSymbols.get(1));
        assertThat(result).element(2).extracting("geneName").containsOnly(randomGeneIds.get(2));
    }

    @Test
    void cellTypeMarkerGeneWithoutSymbolHasGeneIDAsName() {
        var randomGeneIds = generateRandomEnsemblGeneIds(3);
        var randomGeneSymbols = generateRandomGeneSymbols(2);

        when(bioEntityPropertyDaoMock.getSymbolsForGeneIds(ImmutableSet.copyOf(randomGeneIds)))
                .thenReturn(
                        ImmutableMap.of(
                                randomGeneIds.get(0), randomGeneSymbols.get(0),
                                randomGeneIds.get(1), randomGeneSymbols.get(1)));

        var cellTypeMarkerGenes = ImmutableList.of(
                MarkerGene.create(randomGeneIds.get(0), "inferred cell type",
                        "CD8-positive, alpha-beta T cell", 0.004,
                        "T cell", 199, 185, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(1), "inferred cell type",
                        "Not available", 0.0006,
                        "Not available", 12, 1.11, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(2), "inferred cell type",
                        "T cell", 0.001,
                        "B cell", 1000, 10000, EXPRESSION_UNIT));

        var result = subject.getMarkerGeneHeatmapDataSortedLexicographically(cellTypeMarkerGenes);

        assertThat(result).hasSize(3);
        assertThat(result).element(0).extracting("geneName").containsOnly(randomGeneSymbols.get(0));
        assertThat(result).element(1).extracting("geneName").containsOnly(randomGeneSymbols.get(1));
        assertThat(result).element(2).extracting("geneName").containsOnly(randomGeneIds.get(2));
    }

    @Test
    void mergesMultipleGeneIdsAcrossGroupsIntoOneRowWithLowestPValue() {
        var geneId = generateRandomEnsemblGeneId();

        // With integers, we can test the lexicographical and numerical ordering with the same set of test data
        var cellGroupValueWhereMarker1 = Integer.toString(RNG.nextInt());
        var cellGroupValueWhereMarker2 = Integer.toString(RNG.nextInt());
        while (cellGroupValueWhereMarker1.equals(cellGroupValueWhereMarker2)) {
            cellGroupValueWhereMarker2 = Integer.toString(RNG.nextInt());
        }

        var pValue1 = RNG.nextDouble();
        // pvalue1 is the bound, and pValue2 will be the reference marker gene
        var pValue2 = RNG.nextDouble(pValue1);

        var markerGene1 = MarkerGene.create(
                geneId,
                "inferred cell type",
                cellGroupValueWhereMarker1,
                pValue1,
                Integer.toString(RNG.nextInt()),
                RNG.nextInt(1000000),
                RNG.nextInt(1000000),
                EXPRESSION_UNIT);

        var markerGene2 = MarkerGene.create(
                geneId,
                "inferred cell type",
                cellGroupValueWhereMarker2,
                pValue2,
                Integer.toString(RNG.nextInt()),
                RNG.nextInt(1000000),
                RNG.nextInt(1000000),
                EXPRESSION_UNIT);

        when(bioEntityPropertyDaoMock.getSymbolsForGeneIds(ImmutableSet.of(geneId)))
                .thenReturn(ImmutableMap.of(geneId, geneId));

        assertThat(subject.getMarkerGeneHeatmapDataSortedLexicographically(ImmutableSet.of(markerGene1, markerGene2)))
                .extracting("y")
                .containsOnly(0);

        assertThat(subject.getMarkerGeneHeatmapDataSortedNaturally(ImmutableSet.of(markerGene1, markerGene2)))
                .extracting("y")
                .containsOnly(0);

        assertThat(subject.getMarkerGeneHeatmapDataSortedNaturally(ImmutableSet.of(markerGene1, markerGene2)))
                .extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarker2);
    }

    @Test
    void whenMarkerGeneHasNumericalClusterNames_theySortedCorrectly() {
        var randomGeneIds = generateRandomEnsemblGeneIds(3);
        var randomGeneSymbols = generateRandomGeneSymbols(3);

        var cellGroupValueWhereMarkers = new String[] {"1", "11", "9"};

        when(bioEntityPropertyDaoMock.getSymbolsForGeneIds(ImmutableSet.copyOf(randomGeneIds)))
                .thenReturn(
                        ImmutableMap.of(
                                randomGeneIds.get(0), randomGeneSymbols.get(0),
                                randomGeneIds.get(1), randomGeneSymbols.get(1),
                                randomGeneIds.get(2), randomGeneSymbols.get(2)));


        var markerGenes = ImmutableList.of(
                MarkerGene.create(randomGeneIds.get(0), "1", cellGroupValueWhereMarkers[0], 0.004,
                        "1", 199, 185, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(1), "1", cellGroupValueWhereMarkers[1], 0.0006,
                        "2", 12, 1.11, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(2), "1", cellGroupValueWhereMarkers[2], 0.001,
                        "6", 1000, 10000, EXPRESSION_UNIT));

        var result = subject.getMarkerGeneHeatmapDataSortedNaturally(markerGenes);
        assertThat(result).hasSize(3);

        assertThat(result).element(0).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[0]);
        assertThat(result).element(1).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[2]);
        assertThat(result).element(2).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[1]);
    }

    @Test
    void whenMarkerGeneHasNotOnlyNumericalClusterNames_theySortedCorrectly() {
        var randomGeneIds = generateRandomEnsemblGeneIds(4);
        var randomGeneSymbols = generateRandomGeneSymbols(4);

        var cellGroupValueWhereMarkers = new String[] {"1", "abc", "aaa", "2"};

        when(bioEntityPropertyDaoMock.getSymbolsForGeneIds(ImmutableSet.copyOf(randomGeneIds)))
                .thenReturn(
                        ImmutableMap.of(
                                randomGeneIds.get(0), randomGeneSymbols.get(0),
                                randomGeneIds.get(1), randomGeneSymbols.get(1),
                                randomGeneIds.get(2), randomGeneSymbols.get(2),
                                randomGeneIds.get(3), randomGeneSymbols.get(3)));


        var markerGenes = ImmutableList.of(
                MarkerGene.create(randomGeneIds.get(0), "1", cellGroupValueWhereMarkers[0], 0.004,
                        "1", 199, 185, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(1), "1", cellGroupValueWhereMarkers[1], 0.0006,
                        "2", 12, 1.11, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(2), "1", cellGroupValueWhereMarkers[2], 0.001,
                        "6", 234, 736, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(3), "1", cellGroupValueWhereMarkers[3], 0.001,
                        "6", 1000, 10000, EXPRESSION_UNIT));

        var result = subject.getMarkerGeneHeatmapDataSortedNaturally(markerGenes);
        assertThat(result).hasSize(4);

        assertThat(result).element(0).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[2]);
        assertThat(result).element(1).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[1]);
        assertThat(result).element(2).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[0]);
        assertThat(result).element(3).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[3]);
    }

    @Test
    void whenMarkerGeneHasOnlyStringClusterNames_theySortedCorrectly() {
        var randomGeneIds = generateRandomEnsemblGeneIds(4);
        var randomGeneSymbols = generateRandomGeneSymbols(4);

        var cellGroupValueWhereMarkers = new String[] {"x", "abc", "aaa", "y"};

        when(bioEntityPropertyDaoMock.getSymbolsForGeneIds(ImmutableSet.copyOf(randomGeneIds)))
                .thenReturn(
                        ImmutableMap.of(
                                randomGeneIds.get(0), randomGeneSymbols.get(0),
                                randomGeneIds.get(1), randomGeneSymbols.get(1),
                                randomGeneIds.get(2), randomGeneSymbols.get(2),
                                randomGeneIds.get(3), randomGeneSymbols.get(3)));


        var markerGenes = ImmutableList.of(
                MarkerGene.create(randomGeneIds.get(0), "1", cellGroupValueWhereMarkers[0], 0.004,
                        "1", 199, 185, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(1), "1", cellGroupValueWhereMarkers[1], 0.0006,
                        "2", 12, 1.11, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(2), "1", cellGroupValueWhereMarkers[2], 0.001,
                        "6", 234, 736, EXPRESSION_UNIT),
                MarkerGene.create(randomGeneIds.get(3), "1", cellGroupValueWhereMarkers[3], 0.001,
                        "6", 1000, 10000, EXPRESSION_UNIT));

        var result = subject.getMarkerGeneHeatmapDataSortedNaturally(markerGenes);
        assertThat(result).hasSize(4);

        assertThat(result).element(0).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[2]);
        assertThat(result).element(1).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[1]);
        assertThat(result).element(2).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[0]);
        assertThat(result).element(3).extracting("cellGroupValueWhereMarker")
                .containsOnly(cellGroupValueWhereMarkers[3]);
    }
}
