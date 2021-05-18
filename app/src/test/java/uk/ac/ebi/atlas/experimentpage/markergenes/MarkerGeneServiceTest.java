package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.search.CellTypeSearchDao;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MarkerGeneServiceTest {
    @Mock
    private CellTypeSearchDao cellTypeSearchDaoMock;
    @Mock
    private MarkerGenesDao markerGenesDaoMock;

    private MarkerGeneService subject;

    @BeforeEach
    public void setUp() throws Exception {
        subject = new MarkerGeneService(markerGenesDaoMock, cellTypeSearchDaoMock);
    }

    @Test
    @DisplayName("Fetch marker gene profile expression from ontology label cell types")
    void getMarkerGeneProfileWhenOntologyLabelsHasCellTypes() {
        when(markerGenesDaoMock.getCellTypeMarkerGenes("E-EHCA-2", ImmutableSet.of("T cell", "B cell")))
                .thenReturn(mockTestData());
        when(cellTypeSearchDaoMock.getInferredCellTypeOntologyLabels("E-EHCA-2", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000006")))
                .thenReturn(ImmutableSet.of("T cell", "B cell"));
        assertThat(subject.getCellTypeMarkerGeneProfile("E-EHCA-2", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000006")))
                .isNotEmpty();
    }

    @Test
    @DisplayName("Fetch marker gene profile expression from authors label cell types")
    void getMarkerGeneProfileWhenAuthorsLabelsHasCellTypes() {
        when(markerGenesDaoMock.getCellTypeMarkerGenes("E-EHCA-2", ImmutableSet.of("T cell", "B cell")))
                .thenReturn(mockTestData());
        when(cellTypeSearchDaoMock.getInferredCellTypeAuthorsLabels("E-EHCA-2", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000006")))
                .thenReturn(ImmutableSet.of("T cell", "B cell"));
        when(cellTypeSearchDaoMock.getInferredCellTypeOntologyLabels("E-EHCA-2", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000006")))
                .thenReturn(ImmutableSet.of());
        assertThat(subject.getCellTypeMarkerGeneProfile("E-EHCA-2", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000006")))
                .isNotEmpty();
    }

    @Test
    @DisplayName("Returns empty profile if both ontology labels returns empty cell type values")
    void returnEmptyCellTypeMarkerGeneProfileIfBothOntologyAndAuthorsLabelsDoesNotHaveCellTypes() {
        when(cellTypeSearchDaoMock.getInferredCellTypeOntologyLabels("E-EHCA-2", ImmutableSet.of("skin")))
                .thenReturn(ImmutableSet.of());
        when(cellTypeSearchDaoMock.getInferredCellTypeAuthorsLabels("E-EHCA-2", ImmutableSet.of("skin")))
                .thenReturn(ImmutableSet.of());

        assertThat(subject.getCellTypeMarkerGeneProfile("E-EHCA-2", ImmutableSet.of("skin"))).isEmpty();
    }

    private List<MarkerGene> mockTestData() {
        return ImmutableList.of(
                MarkerGene.create("1", "inferred cell type", "CD4-positive, alpha-beta T cell", 0.0, "T cell", 125.258405, 563.54442), MarkerGene.create("2", "inferred cell type", "CD4-positive, alpha-beta T cell", 0.0, "T cell", 125.258405, 563.54442));
    }
}


