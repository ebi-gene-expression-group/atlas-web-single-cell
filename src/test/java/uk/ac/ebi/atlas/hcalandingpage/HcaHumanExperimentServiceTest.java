package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.testutils.MockExperiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@RunWith(MockitoJUnitRunner.class)
public class HcaHumanExperimentServiceTest {

    private static final String EXPERIMENT_ACCESSION = generateRandomExperimentAccession();

    @Mock
    private ExperimentTrader experimentTraderMock;

    @Mock
    private HcaHumanExperimentDao hcaHumanExperimentDaoMock;

    private HcaHumanExperimentService subject;

    @Before
    public void setUp() {
        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION))
                .thenReturn(MockExperiment.createBaselineExperiment(EXPERIMENT_ACCESSION));
        when(hcaHumanExperimentDaoMock.fetchExperimentAccessions("organism_part", ImmutableSet.of()))
                .thenReturn(ImmutableSet.of(EXPERIMENT_ACCESSION));
        when(hcaHumanExperimentDaoMock.fetchExperimentAccessions("organism_part", ImmutableSet.of("skin")))
                .thenReturn(ImmutableSet.of(EXPERIMENT_ACCESSION));
        when(hcaHumanExperimentDaoMock.fetchExperimentAccessions("organism_part", ImmutableSet.of("foo")))
                .thenReturn(ImmutableSet.of());

        subject = new HcaHumanExperimentService(experimentTraderMock, hcaHumanExperimentDaoMock);
    }

    @Test
    public void sizeIsRightForWithDefaultValueOfParameter() {
        assertThat(
                subject.getPublicHumanExperiments("organism_part", ImmutableSet.of()))
                .hasSize(1);
    }

    @Test
    public void sizeIsRightForCorrectCharacteristicNameAndCharacteristicValue() {
      ImmutableSet<Experiment> result =  subject.getPublicHumanExperiments("organism_part", ImmutableSet.of("skin"));
        assertThat(result).hasSize(1);
    }

    @Test
    public void noExperimentReturnedForInvalidCharacteristicValue() {
        assertThat(
                subject.getPublicHumanExperiments("organism_part",ImmutableSet.of("foo")))
                .hasSize(0);
    }

}