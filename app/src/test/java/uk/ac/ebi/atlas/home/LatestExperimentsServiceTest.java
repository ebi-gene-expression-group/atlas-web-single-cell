package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.ac.ebi.atlas.experiments.ExperimentBuilder;
import uk.ac.ebi.atlas.experiments.ExperimentJsonSerializer;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LatestExperimentsServiceTest {

    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();
    @Mock
    private LatestExperimentsDao latestExperimentsDaoMock;
    @Mock
    private ExperimentTrader experimentTraderMock;
    @Mock
    private ExperimentJsonSerializer experimentJsonSerializerMock;
    private LatestExperimentsService subject;

    @BeforeEach
    void setUp() {
        subject = new LatestExperimentsService(latestExperimentsDaoMock, experimentTraderMock, experimentJsonSerializerMock);
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
        when(experimentJsonSerializerMock.serialize(experiment))
                .thenReturn(getMockSerializedExperiment(experiment));

        assertThat(subject.fetchLatestExperimentsAttributes())
                .isNotEmpty()
                .containsKeys("experimentCount", "formattedExperimentCount", "latestExperiments")
                .containsValues(experimentCount);
    }

    private JsonObject getMockSerializedExperiment(Experiment<?> experiment) {
        var jsonObject = new JsonObject();

        jsonObject.addProperty("experimentAccession", experiment.getAccession());
        jsonObject.addProperty("experimentDescription", experiment.getDescription());
        jsonObject.addProperty("species", experiment.getSpecies().getName());
        jsonObject.addProperty("kingdom", experiment.getSpecies().getKingdom());
        jsonObject.addProperty(
                "loadDate", new SimpleDateFormat("dd-MM-yyyy")
                        .format(experiment.getLoadDate()));
        jsonObject.addProperty(
                "lastUpdate", new SimpleDateFormat("dd-MM-yyyy")
                        .format(experiment.getLastUpdate()));
        jsonObject.addProperty(
                "numberOfAssays", experiment.getAnalysedAssays().size());
        jsonObject.addProperty(
                "rawExperimentType", experiment.getType().toString());
        jsonObject.addProperty(
                "experimentType", experiment.getType().isBaseline() ? "Baseline" : "Differential");
        jsonObject.add("technologyType", GSON.toJsonTree(experiment.getTechnologyType()));
        jsonObject.add(
                "experimentalFactors",
                GSON.toJsonTree(experiment.getExperimentalFactorHeaders()));
        jsonObject.add(
                "experimentProjects",
                GSON.toJsonTree(List.of()));
        return jsonObject;
    }
}
