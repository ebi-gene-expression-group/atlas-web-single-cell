package uk.ac.ebi.atlas.experimentimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.testutils.RandomDataTestUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScxaExperimentDtoResultSetExtractorTest {

    @Mock
    private ResultSet mockResultSet;

    private Timestamp currentTime;
    private ScxaExperimentDtoResultSetExtractor subject;

    @BeforeEach
    void setUp() {
        subject = new ScxaExperimentDtoResultSetExtractor();
        currentTime = new Timestamp(System.currentTimeMillis());
    }

    @Test
    void createsValidExperimentDTO() throws SQLException {
        String experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        createMockResultSet(ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE,
                "Homo sapiens",
                "123",
                true,
                currentTime,
                "123, 124, 125",
                "10.10/abc, 12.100/abc, 13.9/abc");

        ExperimentDto result = subject.createExperimentDto(mockResultSet, experimentAccession);

        assertThat(result.getExperimentAccession()).isEqualToIgnoringCase(experimentAccession);
        assertThat(result.getExperimentType()).isEqualByComparingTo(ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE);
        assertThat(result.getSpecies()).isEqualToIgnoringCase("Homo sapiens");
        assertThat(result.getAccessKey()).isEqualToIgnoringCase("123");
        assertThat(result.isPrivate()).isTrue();
        assertThat(result.getLastUpdate()).hasSameTimeAs(currentTime);
        assertThat(result.getPubmedIds()).hasSize(3);
        assertThat(result.getDois()).hasSize(3);
    }

    @Test
    void createsExperimentDTOWithEmptyFields() throws SQLException {
        String experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        createMockResultSet(ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE,
                "Homo sapiens",
                "",
                false,
                currentTime,
                "",
                "");

        ExperimentDto result = subject.createExperimentDto(mockResultSet, experimentAccession);

        assertThat(result.getAccessKey()).isEmpty();
        assertThat(result.getPubmedIds()).isEmpty();
        assertThat(result.getDois()).isEmpty();
    }

    @Test
    void createsExperimentDTOWithNullFields() throws SQLException {
        String experimentAccession = RandomDataTestUtils.generateRandomExperimentAccession();
        createMockResultSet(ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE,
                "Homo sapiens",
                "",
                false,
                currentTime,
                null,
                null);

        ExperimentDto result = subject.createExperimentDto(mockResultSet, experimentAccession);

        assertThat(result.getPubmedIds()).isEmpty();
        assertThat(result.getDois()).isEmpty();
    }

    private void createMockResultSet(ExperimentType experimentType,
                                     String species,
                                     String accessKey,
                                     boolean isPrivate,
                                     Timestamp lastUpdate,
                                     String pubmedIds,
                                     String dois) throws SQLException {

        when(mockResultSet.getString("type")).thenReturn(experimentType.name());
        when(mockResultSet.getString("species")).thenReturn(species);
        when(mockResultSet.getString("access_key")).thenReturn(accessKey);
        when(mockResultSet.getBoolean("private")).thenReturn(isPrivate);
        when(mockResultSet.getTimestamp("last_update")).thenReturn(lastUpdate);
        when(mockResultSet.getString("pubmed_ids")).thenReturn(pubmedIds);
        when(mockResultSet.getString("dois")).thenReturn(dois);
    }
}
