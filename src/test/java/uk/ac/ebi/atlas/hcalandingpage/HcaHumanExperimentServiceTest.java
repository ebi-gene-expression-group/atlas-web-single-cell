package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
        subject = new HcaHumanExperimentService(experimentTraderMock, hcaHumanExperimentDaoMock);
    }

    @Test
    public void ShouldGetAllHumanExperimentsWithEmptyCharacteristicValue() {
        when(hcaHumanExperimentDaoMock.fetchExperimentAccessions("organism_part", ImmutableSet.of()))
                .thenReturn(ImmutableSet.of(EXPERIMENT_ACCESSION));
        assertThat(subject.getPublicHumanExperiments("organism_part", ImmutableSet.of()))
                .isNotEmpty().hasSize(1);
    }

    @Test
    public void ShouldGetHumanExperimentsOnlyForTheGivenCharacteristicValue() {
        when(hcaHumanExperimentDaoMock.fetchExperimentAccessions("organism_part", ImmutableSet.of("skin")))
                .thenReturn(ImmutableSet.of(EXPERIMENT_ACCESSION));
        assertThat(subject.getPublicHumanExperiments("organism_part", ImmutableSet.of("skin")))
                .isNotEmpty().hasSize(1);
    }

    @Test
    public void shouldGetEmptyExperimentsIfTheGivenCharacteristicValueIsInvalid() {
        when(hcaHumanExperimentDaoMock.fetchExperimentAccessions("organism_part", ImmutableSet.of("foo")))
                .thenReturn(ImmutableSet.of());
        assertThat(subject.getPublicHumanExperiments("organism_part", ImmutableSet.of("foo")))
                .isEmpty();
    }

    @Test
    public void shouldGetHumanExperimentsForMultipleCharacteristicValues() {
        when(hcaHumanExperimentDaoMock.fetchExperimentAccessions("organism_part", ImmutableSet.of("skin", "lymph node")))
                .thenReturn(ImmutableSet.of(EXPERIMENT_ACCESSION));
        assertThat(subject.getPublicHumanExperiments("organism_part", ImmutableSet.of("skin", "lymph node")))
                .isNotEmpty().hasSize(1);
    }
}