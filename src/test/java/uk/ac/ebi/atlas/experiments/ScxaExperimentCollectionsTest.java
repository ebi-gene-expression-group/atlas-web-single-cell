package uk.ac.ebi.atlas.experiments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(MockitoExtension.class)
class ScxaExperimentCollectionsTest {

    @Mock
    private ExperimentCollectionDao experimentCollectionDaoMock;

    private ScxaExperimentCollections subject;

    @BeforeEach
    public void setUp() throws Exception {
        subject = new ScxaExperimentCollections(experimentCollectionDaoMock);
    }

    @Test
    void returnEmptyIfExperimentDoesntHaveCollection() {
        when(experimentCollectionDaoMock.getExperimentCollection("F-BAR-0000"))
                .thenReturn(List.of());
        assertThat(subject.getExperimentCollections("F-BAR-0000"))
                .isEmpty();
    }

    @Test
    void returnCollectionNameIfExist() {
        var experimentAccession = generateRandomExperimentAccession();
        when(experimentCollectionDaoMock.getExperimentCollection(experimentAccession))
                .thenReturn(List.of("Human Cell Atlas"));
        assertThat(subject.getExperimentCollections(experimentAccession))
                .isNotEmpty()
                .containsExactly("Human Cell Atlas");
    }

    @Test
    void returnMultipleCollectionNameIfExist() {
        var experimentAccession = generateRandomExperimentAccession();
        when(experimentCollectionDaoMock.getExperimentCollection(experimentAccession))
                .thenReturn(List.of("Human Cell Atlas", "Malaria Cell Atlas"));
        assertThat(subject.getExperimentCollections(experimentAccession))
                .isNotEmpty()
                .containsExactly("Human Cell Atlas", "Malaria Cell Atlas");
    }
}