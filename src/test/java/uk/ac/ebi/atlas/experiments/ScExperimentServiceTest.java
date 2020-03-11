package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.atlas.testutils.MockExperiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@RunWith(MockitoJUnitRunner.class)
public class ScExperimentServiceTest {

    private static final String EXPERIMENT_ACCESSION = generateRandomExperimentAccession();

    @Mock
    private ScExperimentTrader scExperimentTraderMock;

    private ScExperimentService subject;

    @Before
    public void setUp() throws Exception {
        when(scExperimentTraderMock.getPublicHumanExperiments("organism_part", ""))
                .thenReturn(ImmutableSet.of(MockExperiment.createBaselineExperiment(EXPERIMENT_ACCESSION)));
        when(scExperimentTraderMock.getPublicHumanExperiments("organism_part", "skin"))
                .thenReturn(ImmutableSet.of(MockExperiment.createBaselineExperiment(EXPERIMENT_ACCESSION)));
        when(scExperimentTraderMock.getPublicHumanExperiments("organism_part", "foo"))
                .thenReturn(ImmutableSet.of());

        subject = new ScExperimentService(scExperimentTraderMock);
    }

    @Test
    public void sizeIsRightForWithDefaultValueOfParameter() {
        assertThat(
                subject.getPublicHumanExperiments("organism_part", ""))
                .hasSize(1);
    }

    @Test
    public void sizeIsRightForCorrectCharacteristicNameAndCharacteristicValue() {
        assertThat(
                subject.getPublicHumanExperiments("organism_part","skin"))
                .hasSize(1);
    }

    @Test
    public void noExperimentReturnedForInvalidCharacteristicValue() {
        assertThat(
                subject.getPublicHumanExperiments("organism_part","foo"))
                .hasSize(0);
    }
}