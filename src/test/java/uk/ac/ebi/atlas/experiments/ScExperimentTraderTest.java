package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.MockExperiment.createBaselineExperiment;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(MockitoExtension.class)
class ScExperimentTraderTest {
    @Mock
    private ScExperimentTraderDao scExperimentTraderDaoMock;

    @Mock
    private ExperimentTrader experimentTraderMock;

    private ScExperimentTrader subject;

    @BeforeEach
    void setUp() {
        subject = new ScExperimentTrader(experimentTraderMock, scExperimentTraderDaoMock);
    }

    @Test
    void returnsAllPublicExperiments() {
        var experimentAccession = generateRandomExperimentAccession();

        when(scExperimentTraderDaoMock.fetchExperimentAccessions("organism_part", ""))
                .thenReturn(ImmutableSet.of(experimentAccession));
        when(experimentTraderMock.getPublicExperiment(experimentAccession))
                .thenReturn(createBaselineExperiment(experimentAccession));

        assertThat(subject.getPublicHumanExperiments("organism_part", ""))
                .hasSize(1);
    }

}