package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.experiments.ExperimentBuilder;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LatestExperimentsServiceTest {

    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();
    @Mock
    private LatestExperimentsDao latestExperimentsDaoMock;
    @Mock
    private ExperimentTrader experimentTraderMock;
    private LatestExperimentsService subject;

    @BeforeEach
    void setUp() {
        subject = new LatestExperimentsService(latestExperimentsDaoMock, experimentTraderMock);
    }

    @Test
    void expectedAttributesbyLatestExperimentsAccession() {
        var experimentAcccession = generateRandomExperimentAccession();
        var experimentCount = RNG.nextLong(1, 1000);

        when(latestExperimentsDaoMock.fetchPublicExperimentsCount())
                .thenReturn(experimentCount);
        when(latestExperimentsDaoMock.fetchLatestExperimentAccessions())
                .thenReturn(ImmutableList.of(experimentAcccession));

        var experiment =
                new ExperimentBuilder.SingleCellBaselineExperimentBuilder()
                        .withExperimentAccession(experimentAcccession)
                        .build();
        when(experimentTraderMock.getPublicExperiment(experimentAcccession)).thenReturn(experiment);

        assertThat(subject.fetchLatestExperimentsAttributes())
                .isNotEmpty()
                .containsKeys("experimentCount", "formattedExperimentCount", "latestExperiments")
                .containsValues(experimentCount);
    }
}
