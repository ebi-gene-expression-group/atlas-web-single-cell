package uk.ac.ebi.atlas.search.organismpart;

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
public class OrganismPartSearchServiceTest {

    @Mock
    private OrganismPartSearchDao organismPartSearchDao;

    @Mock
    private GeneSearchService geneSearchService;

    private OrganismPartSearchService subject;

    @BeforeEach
    void setup() {
        subject = new OrganismPartSearchService(organismPartSearchDao, geneSearchService);
    }

    @Test
    void whenEmptySetOfGeneIdsAndCellTypesProvidedReturnsEmptySetOfOrganismPart() {
        ImmutableSet<String> emptySetOfGeneIds = ImmutableSet.of();
        ImmutableSet<String> emptySetOfCellTypes = ImmutableSet.of();

        var organismParts = subject.search(emptySetOfGeneIds, emptySetOfCellTypes);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenNonExistentGeneIdsAndEmptySetOfCellTypesProvidedReturnsEmptySetOfOrganismPart() {
        var nonExistentGeneId = "nonExistentGeneId";
        var setOfNonExistentGeneIds = ImmutableSet.of(nonExistentGeneId);
        ImmutableSet<String> emptySetOfCellTypes = ImmutableSet.of();

        when(geneSearchService.getCellIdsFromGeneIds(setOfNonExistentGeneIds))
                .thenReturn(ImmutableSet.of());
        when(organismPartSearchDao.searchOrganismPart(ImmutableSet.of(), emptySetOfCellTypes))
                .thenReturn(ImmutableSet.of());

        var organismParts = subject.search(setOfNonExistentGeneIds, emptySetOfCellTypes);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenValidGeneIdsAndEmptySetOfCellTypesProvidedReturnsSetOfOrganismParts() {
        var existingGeneId1 = "ExistingGeneId1";
        var existingGeneId2 = "ExistingGeneId2";
        var geneIds = ImmutableSet.of(existingGeneId1, existingGeneId2);
        var existingCellId1 = "ExistingCellId1";
        var existingCellId2 = "ExistingCellId2";
        var cellIds = ImmutableSet.of(existingCellId1, existingCellId2);

        ImmutableSet<String> emptySetOfCellTypes = ImmutableSet.of();

        var expectedOrganismPart = "primary visual cortex";

        when(geneSearchService.getCellIdsFromGeneIds(geneIds))
                .thenReturn(cellIds);
        when(organismPartSearchDao.searchOrganismPart(cellIds, emptySetOfCellTypes))
                .thenReturn(ImmutableSet.of(expectedOrganismPart));

        var organismParts = subject.search(geneIds, emptySetOfCellTypes);

        assertThat(organismParts).contains(expectedOrganismPart);
    }

    @Test
    void whenValidGeneIDsAndCellTypesProvidedReturnsSetOfOrganismParts() {
        var existingGeneId1 = "ExistingGeneId1";
        var existingGeneId2 = "ExistingGeneId2";
        var geneIds = ImmutableSet.of(existingGeneId1, existingGeneId2);
        var existingCellId1 = "ExistingCellId1";
        var existingCellId2 = "ExistingCellId2";
        var cellIds = ImmutableSet.of(existingCellId1, existingCellId2);
        var existingCellType1 = "ExistingCellType1";
        var existingCellType2 = "ExistingCellType2";
        var cellTypes = ImmutableSet.of(existingCellType1, existingCellType2);

        var expectedOrganismPart = "primary visual cortex";

        when(geneSearchService.getCellIdsFromGeneIds(geneIds))
                .thenReturn(cellIds);
        when(organismPartSearchDao.searchOrganismPart(cellIds, cellTypes))
                .thenReturn(ImmutableSet.of(expectedOrganismPart));

        var organismParts = subject.search(geneIds, cellTypes);

        assertThat(organismParts).contains(expectedOrganismPart);
    }
}
