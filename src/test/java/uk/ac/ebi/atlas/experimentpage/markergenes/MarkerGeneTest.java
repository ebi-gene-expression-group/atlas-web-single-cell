package uk.ac.ebi.atlas.experimentpage.markergenes;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.controllers.BioentityNotFoundException;
import uk.ac.ebi.atlas.search.CellTypeSearchDao;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MarkerGeneTest {
    @Mock
    private CellTypeSearchDao cellTypeSearchDaoMock;
    @Mock
    private MarkerGenesDao markerGenesDaoMock;

    private MarkerGeneService subject;

    @BeforeEach
    public void setUp() throws Exception {
        when(markerGenesDaoMock.getCellTypeMarkerGenes("E-EHCA-2", ImmutableSet.of("T cell", "B cell")))
                .thenReturn(mockTestData());
        subject = new MarkerGeneService(markerGenesDaoMock, cellTypeSearchDaoMock);
    }

    @Test
    @DisplayName("Fetch marker gene profile expression from ontology label cell types")
    void getMarkerGeneProfileWhenOntologyLabelsHasCellTypes() {
        when(cellTypeSearchDaoMock.getInferredCellTypeOntologyLabels("E-EHCA-2", "skin"))
                .thenReturn(ImmutableSet.of("T cell", "B cell"));
        assertThat(subject.getCellTypeMarkerGeneProfile("E-EHCA-2", "skin"))
                .isNotEmpty();
    }

    @Test
    @DisplayName("Fetch marker gene profile expression from authors label cell types")
    void getMarkerGeneProfileWhenAuthorsLabelsHasCellTypes() {
        when(cellTypeSearchDaoMock.getInferredCellTypeAuthorsLabels("E-EHCA-2", "skin"))
                .thenReturn(ImmutableSet.of("T cell", "B cell"));
        when(cellTypeSearchDaoMock.getInferredCellTypeOntologyLabels("E-EHCA-2", "skin"))
                .thenReturn(ImmutableSet.of());
        assertThat(subject.getCellTypeMarkerGeneProfile("E-EHCA-2", "skin"))
                .isNotEmpty();
    }

    @Test()
    @DisplayName("Throws exception if both ontology labels returns empty cell type values")
    void throwsExceptionIfBothOntologyAndAuthorsLabelsDoesNotHaveCellTypes() {
        when(cellTypeSearchDaoMock.getInferredCellTypeOntologyLabels("E-EHCA-2", "skin"))
                .thenReturn(ImmutableSet.of());
        when(cellTypeSearchDaoMock.getInferredCellTypeAuthorsLabels("E-EHCA-2", "skin"))
                .thenReturn(ImmutableSet.of());
        assertThatThrownBy(() -> {
            subject.getCellTypeMarkerGeneProfile("E-EHCA-2", "skin");
        }).isInstanceOf(BioentityNotFoundException.class)
                .hasMessageContaining("OrganismOrOrganismPart: skin doesn't have annotations.");
    }

    private List<CellTypeMarkerGene> mockTestData() {
        List<CellTypeMarkerGene> CellTypeValuesMock = new ArrayList<>();
        CellTypeValuesMock.add(CellTypeMarkerGene.create("1", "inferred cell type", "CD4-positive, alpha-beta T cell", 0.0,
                "T cell", 125.258405, 563.54442));
        CellTypeValuesMock.add(CellTypeMarkerGene.create("2", "inferred cell type", "CD4-positive, alpha-beta T cell", 0.0,
                "T cell", 125.258405, 563.54442));
        return CellTypeValuesMock;
    }
}


