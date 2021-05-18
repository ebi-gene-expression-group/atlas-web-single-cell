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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomGeneSymbol;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HighchartsHeatmapAdapterTest {

    @Mock
    private BioEntityPropertyDao bioEntityPropertyDaoMock;

    private HighchartsHeatmapAdapter subject;

    @BeforeEach
    void setUp() {
        subject = new HighchartsHeatmapAdapter(bioEntityPropertyDaoMock);
    }

    @Test
    void markerGeneWithoutSymbolHasGeneIDAsName() {
        var gene1 = generateRandomEnsemblGeneId();
        var gene2 = generateRandomEnsemblGeneId();
        var gene3 = generateRandomEnsemblGeneId();

        var geneSymbol1 = generateRandomGeneSymbol();
        var geneSymbol2 = generateRandomGeneSymbol();

        var randomGeneIds = ImmutableSet.of(gene1, gene2, gene3);

        when(bioEntityPropertyDaoMock.getSymbolsForGeneIds(randomGeneIds))
                .thenReturn(
                        ImmutableMap.of(
                                gene1, geneSymbol1,
                                gene2, geneSymbol2));

        var markerGenes = ImmutableList.of(
                MarkerGene.create(gene1, "1", "1", 0.004, "1", 199, 185),
                MarkerGene.create(gene2, "1", "3", 0.0006, "2", 12, 1.11),
                MarkerGene.create(gene3, "1", "5", 0.001, "6", 1000, 10000));

        var result = subject.getMarkerGeneHeatmapDataSortedNumerically(markerGenes);
        assertThat(result).hasSize(3);

        assertThat(result).element(0).extracting("geneName").containsOnly(geneSymbol1);
        assertThat(result).element(1).extracting("geneName").containsOnly(geneSymbol2);
        assertThat(result).element(2).extracting("geneName").containsOnly(gene3);
    }

    @Test
    void cellTypeMarkerGeneWithoutSymbolHasGeneIDAsName() {
        var gene1 = generateRandomEnsemblGeneId();
        var gene2 = generateRandomEnsemblGeneId();
        var gene3 = generateRandomEnsemblGeneId();

        var geneSymbol1 = generateRandomGeneSymbol();
        var geneSymbol2 = generateRandomGeneSymbol();

        var randomGeneIds = ImmutableSet.of(gene1, gene2, gene3);

        when(bioEntityPropertyDaoMock.getSymbolsForGeneIds(randomGeneIds))
                .thenReturn(
                        ImmutableMap.of(
                                gene1, geneSymbol1,
                                gene2, geneSymbol2));

        var cellTypeMarkerGenes = ImmutableList.of(
                MarkerGene.create(gene1, "inferred cell type", "CD8-positive, alpha-beta T cell", 0.004, "T cell", 199, 185),
                MarkerGene.create(gene2, "inferred cell type", "Not available", 0.0006, "Not available", 12, 1.11),
                MarkerGene.create(gene3, "inferred cell type", "T cell", 0.001, "B cell", 1000, 10000));

        var result = subject.getMarkerGeneHeatmapDataSortedLexicographically(cellTypeMarkerGenes);

        assertThat(result).hasSize(3);
        assertThat(result).element(0).extracting("geneName").containsOnly(geneSymbol1);
        assertThat(result).element(1).extracting("geneName").containsOnly(geneSymbol2);
        assertThat(result).element(2).extracting("geneName").containsOnly(gene3);
    }
}
