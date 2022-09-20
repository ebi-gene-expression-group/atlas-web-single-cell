package uk.ac.ebi.atlas.search.organismpart;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrganismPartSearchServiceTest {

    @Mock
    private OrganismPartSearchDao organismPartSearchDao;

    private OrganismPartSearchService subject;

    @BeforeEach
    void setup() {
        subject = new OrganismPartSearchService(organismPartSearchDao);
    }

    @Test
    void whenEmptySetOfGeneIdsProvidedReturnEmptyOptional() {
        Optional<ImmutableSet<String>> emptySetOfGeneIds = Optional.of(ImmutableSet.of());

        var emptySetOfOrganismParts = subject.search(emptySetOfGeneIds);

        assertThat(emptySetOfOrganismParts.isPresent()).isTrue();
        assertThat(emptySetOfOrganismParts.get()).hasSize(0);
    }

    @Test
    void whenNonExistentGeneIdsGivenReturnEmptyOptional() {
        var nonExistentGeneId = "nonExistentGeneId";
        var emptySetOfGeneIds = Optional.of(ImmutableSet.of(nonExistentGeneId));

        when(organismPartSearchDao.searchOrganismPart(emptySetOfGeneIds.get()))
                .thenReturn(Optional.of(ImmutableSet.of()));

        var emptySetOfOrganismParts = subject.search(emptySetOfGeneIds);

        assertThat(emptySetOfOrganismParts.isPresent()).isTrue();
        assertThat(emptySetOfOrganismParts.get()).hasSize(0);
    }

    @Test
    void whenValidSetOfGeneIdsGivenReturnSetOfOrganismParts() {
        var existingGeneId1 = "ExistingGeneId1";
        var existingGeneId2 = "ExistingGeneId2";
        var validGeneIds = Optional.of(ImmutableSet.of(existingGeneId1, existingGeneId2));
        var expectedOrganismPart = "primary visual cortex";

        when(organismPartSearchDao.searchOrganismPart(validGeneIds.get()))
                .thenReturn(Optional.of(ImmutableSet.of(expectedOrganismPart)));

        var actualSetOfOrganismParts = subject.search(validGeneIds);

        assertThat(actualSetOfOrganismParts.isPresent()).isTrue();
        assertThat(actualSetOfOrganismParts.get()).hasSize(1);
        assertThat(actualSetOfOrganismParts.get()).contains(expectedOrganismPart);
    }
}
