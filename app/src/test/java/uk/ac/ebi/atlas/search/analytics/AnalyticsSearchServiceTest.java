package uk.ac.ebi.atlas.search.analytics;

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
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomCellId;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AnalyticsSearchServiceTest {

    @Mock
    private AnalyticsSearchDao analyticsSearchDao;

    @Mock
    private GeneSearchService geneSearchService;

    private AnalyticsSearchService subject;

    @BeforeEach
    void setup() {
        subject = new AnalyticsSearchService(analyticsSearchDao, geneSearchService);
    }

    @Test
    void whenEmptySetOfGeneIdsProvidedReturnEmptySetOfOrganismPart() {
        ImmutableSet<String> emptySetOfGeneIds = ImmutableSet.of();

        var emptySetOfOrganismParts = subject.searchOrganismPart(emptySetOfGeneIds);

        assertThat(emptySetOfOrganismParts).isEmpty();
    }

    @Test
    void whenNonExistentGeneIdsGivenReturnEmptySetOfOrganismPart() {
        var nonExistentGeneId = generateRandomEnsemblGeneId();
        var invalidGeneIds = ImmutableSet.of(nonExistentGeneId);

        when(geneSearchService.getCellIdsFromGeneIds(invalidGeneIds))
                .thenReturn(ImmutableSet.of());
        when(analyticsSearchDao.searchFieldByCellIds(CTW_ORGANISM_PART, ImmutableSet.of()))
                .thenReturn(ImmutableSet.of());

        var emptySetOfOrganismParts = subject.searchOrganismPart(invalidGeneIds);

        assertThat(emptySetOfOrganismParts).isEmpty();

    }

    @Test
    void whenValidGeneIdsGivenReturnSetOfOrganismParts() {
        var existingGeneId1 = generateRandomEnsemblGeneId();
        var existingGeneId2 = generateRandomEnsemblGeneId();
        var validGeneIds = ImmutableSet.of(existingGeneId1, existingGeneId2);
        var existingCellId1 = generateRandomCellId();
        var existingCellId2 = generateRandomCellId();
        var validCellIds = ImmutableSet.of(existingCellId1, existingCellId2);

        var expectedOrganismPart = "primary visual cortex";

        when(geneSearchService.getCellIdsFromGeneIds(validGeneIds))
                .thenReturn(validCellIds);
        when(analyticsSearchDao.searchFieldByCellIds(CTW_ORGANISM_PART, validCellIds))
                .thenReturn(ImmutableSet.of(expectedOrganismPart));

        var actualSetOfOrganismParts = subject.searchOrganismPart(validGeneIds);

        assertThat(actualSetOfOrganismParts).contains(expectedOrganismPart);
    }

    @Test
    void whenEmptySetOfGeneIdsProvidedReturnEmptySetOfCellType() {
        ImmutableSet<String> emptySetOfGeneIds = ImmutableSet.of();

        var emptySetOfCellType = subject.searchCellType(emptySetOfGeneIds);

        assertThat(emptySetOfCellType).isEmpty();
    }

    @Test
    void whenNonExistentGeneIdsGivenReturnEmptySetOfCellType() {
        var nonExistentGeneId = generateRandomEnsemblGeneId();
        var invalidGeneIds = ImmutableSet.of(nonExistentGeneId);

        when(geneSearchService.getCellIdsFromGeneIds(invalidGeneIds))
                .thenReturn(ImmutableSet.of());
        when(analyticsSearchDao.searchFieldByCellIds(CTW_CELL_TYPE, ImmutableSet.of()))
                .thenReturn(ImmutableSet.of());

        var emptySetOfCellType = subject.searchCellType(invalidGeneIds);

        assertThat(emptySetOfCellType).isEmpty();
    }

    @Test
    void whenValidGeneIdsGivenReturnSetOfCellType() {
        var existingGeneId1 = generateRandomEnsemblGeneId();
        var existingGeneId2 = generateRandomEnsemblGeneId();
        var validGeneIds = ImmutableSet.of(existingGeneId1, existingGeneId2);
        var existingCellId1 = generateRandomCellId();
        var existingCellId2 = generateRandomCellId();
        var validCellIds = ImmutableSet.of(existingCellId1, existingCellId2);

        var expectedCellType = "protoplast";

        when(geneSearchService.getCellIdsFromGeneIds(validGeneIds))
                .thenReturn(validCellIds);
        when(analyticsSearchDao.searchFieldByCellIds(CTW_CELL_TYPE, validCellIds))
                .thenReturn(ImmutableSet.of(expectedCellType));

        var actualSetOfCellType = subject.searchCellType(validGeneIds);

        assertThat(actualSetOfCellType).contains(expectedCellType);
    }
}
