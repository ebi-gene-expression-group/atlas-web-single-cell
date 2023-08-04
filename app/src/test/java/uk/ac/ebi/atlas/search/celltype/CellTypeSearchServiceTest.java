package uk.ac.ebi.atlas.search.celltype;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.search.GeneSearchService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CellTypeSearchServiceTest {

    @Mock
    private CellTypeSearchDao cellTypeSearchDao;

    @Mock
    private GeneSearchService geneSearchService;

    private CellTypeSearchService subject;

    @BeforeEach
    void setup() {
        subject = new CellTypeSearchService(cellTypeSearchDao, geneSearchService);
    }

    @Test
    void whenEmptySetOfGeneIdsAndOrganismPartsProvidedReturnsEmptySetOfCellTypes() {
        ImmutableSet<String> emptySetOfGeneIds = ImmutableSet.of();
        ImmutableSet<String> emptySetOfOrganismParts = ImmutableSet.of();

        var cellTypes = subject.search(emptySetOfGeneIds, emptySetOfOrganismParts);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenNonExistentGeneIdsAndEmptySetOfOrganismPartsProvidedReturnsEmptySetOfCellTypes() {
        var nonExistentGeneId = "nonExistentGeneId";
        var setOfNonExistentGeneIds = ImmutableSet.of(nonExistentGeneId);
        ImmutableSet<String> emptySetOfOrganismParts = ImmutableSet.of();

        when(geneSearchService.getCellIdsFromGeneIds(setOfNonExistentGeneIds))
                .thenReturn(ImmutableSet.of());
        when(cellTypeSearchDao.searchCellTypes(ImmutableSet.of(), emptySetOfOrganismParts))
                .thenReturn(ImmutableSet.of());

        var cellTypes = subject.search(setOfNonExistentGeneIds, emptySetOfOrganismParts);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenValidGeneIdsAndEmptySetOfOrganismPartsProvidedReturnsSetOfCellTypes() {
        var existingGeneId1 = "ExistingGeneId1";
        var existingGeneId2 = "ExistingGeneId2";
        var geneIds = ImmutableSet.of(existingGeneId1, existingGeneId2);
        var existingCellId1 = "ExistingCellId1";
        var existingCellId2 = "ExistingCellId2";
        var cellIds = ImmutableSet.of(existingCellId1, existingCellId2);

        ImmutableSet<String> emptySetOfOrganismParts = ImmutableSet.of();

        var expectedCellType = "root cortex 7";

        when(geneSearchService.getCellIdsFromGeneIds(geneIds))
                .thenReturn(cellIds);
        when(cellTypeSearchDao.searchCellTypes(cellIds, emptySetOfOrganismParts))
                .thenReturn(ImmutableSet.of(expectedCellType));

        var cellTypes = subject.search(geneIds, emptySetOfOrganismParts);

        assertThat(cellTypes).contains(expectedCellType);
    }

    @Test
    void whenValidGeneIDsAndOrganismPartsProvidedReturnsSetOfCellTypes() {
        var existingGeneId1 = "ExistingGeneId1";
        var existingGeneId2 = "ExistingGeneId2";
        var geneIds = ImmutableSet.of(existingGeneId1, existingGeneId2);
        var existingCellId1 = "ExistingCellId1";
        var existingCellId2 = "ExistingCellId2";
        var cellIds = ImmutableSet.of(existingCellId1, existingCellId2);
        var existingCellType1 = "ExistingCellType1";
        var existingCellType2 = "ExistingCellType2";
        var organismParts = ImmutableSet.of(existingCellType1, existingCellType2);

        var expectedCellType = "root cortex 7";

        when(geneSearchService.getCellIdsFromGeneIds(geneIds))
                .thenReturn(cellIds);
        when(cellTypeSearchDao.searchCellTypes(cellIds, organismParts))
                .thenReturn(ImmutableSet.of(expectedCellType));

        var cellTypes = subject.search(geneIds, organismParts);

        assertThat(cellTypes).contains(expectedCellType);
    }
}
