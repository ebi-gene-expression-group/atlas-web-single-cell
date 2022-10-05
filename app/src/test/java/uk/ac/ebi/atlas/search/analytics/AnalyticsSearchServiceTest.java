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

        var emptySetOfOrganismParts = subject.search(emptySetOfGeneIds);

        assertThat(emptySetOfOrganismParts).isEmpty();
    }

    @Test
    void whenNonExistentGeneIdsGivenReturnEmptySetOfOrganismPart() {
        var nonExistentGeneId = "nonExistentGeneId";
        var emptySetOfGeneIds = ImmutableSet.of(nonExistentGeneId);

        when(geneSearchService.getCellIdsFromGeneIds(emptySetOfGeneIds))
                .thenReturn(ImmutableSet.of());
        when(analyticsSearchDao.searchOrganismPart(ImmutableSet.of()))
                .thenReturn(ImmutableSet.of());

        var emptySetOfOrganismParts = subject.search(emptySetOfGeneIds);

        assertThat(emptySetOfOrganismParts).isEmpty();

    }

    @Test
    void whenValidGeneIdsGivenReturnSetOfOrganismParts() {
        var existingGeneId1 = "ExistingGeneId1";
        var existingGeneId2 = "ExistingGeneId2";
        var validGeneIds = ImmutableSet.of(existingGeneId1, existingGeneId2);
        var existingCellId1 = "ExistingCellId1";
        var existingCellId2 = "ExistingCellId2";
        var validCellIds = ImmutableSet.of(existingCellId1, existingCellId2);

        var expectedOrganismPart = "primary visual cortex";

        when(geneSearchService.getCellIdsFromGeneIds(validGeneIds))
                .thenReturn(validCellIds);
        when(analyticsSearchDao.searchOrganismPart(validCellIds))
                .thenReturn(ImmutableSet.of(expectedOrganismPart));

        var actualSetOfOrganismParts = subject.search(validGeneIds);

        assertThat(actualSetOfOrganismParts).contains(expectedOrganismPart);
    }
}
