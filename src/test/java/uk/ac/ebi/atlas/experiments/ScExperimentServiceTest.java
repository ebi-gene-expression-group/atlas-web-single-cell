package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.atlas.testutils.MockExperiment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomOntologyId;

@RunWith(MockitoJUnitRunner.class)
public class ScExperimentServiceTest {
    private static final String EXPERIMENT_ACCESSION = generateRandomExperimentAccession();
    private static final List<String> ONTOLOGY_IDS = ImmutableList.of(
            generateRandomOntologyId(),
            generateRandomOntologyId(),
            generateRandomOntologyId());
    private static final List<String> EXPERIMENT_ACCESSIONS = ImmutableList.of(
            EXPERIMENT_ACCESSION
    );
    private static final String SPECIES = "Homo sapiens";

    @Mock
    private ScExperimentTrader scExperimentTraderMock;

    private ScExperimentService subject;

    @Before
    public void setUp() throws Exception {
        when(scExperimentTraderMock.getMetadata())
                .thenReturn(ImmutableMap.of(
                        "ontology_ids", ONTOLOGY_IDS,
                        "experiment_accessions", EXPERIMENT_ACCESSIONS
                ));
        when(scExperimentTraderMock.getPublicExperiments(EXPERIMENT_ACCESSIONS))
                .thenReturn(ImmutableSet.of(MockExperiment.createBaselineExperiment(EXPERIMENT_ACCESSION)));

        subject = new ScExperimentService(scExperimentTraderMock);
    }

    @Test
    public void sizeIsRight() {
        var result = subject.getHCAMetadataJson();

        assertThat(result.get("experiments").getAsJsonArray()).hasSize(1);
        assertThat(result.get("ontology_ids").getAsJsonArray()).hasSize(3);
        assertThat(result.get("species").getAsString()).isNotBlank();
    }

    @Test
    public void formatIsInSyncWithWhatWeExpect() {
        var result = JsonPath.parse(subject.getHCAMetadataJson().toString());

        assertThat(result.<String>read("$.experiments[0].experimentAccession"))
                .isEqualTo(EXPERIMENT_ACCESSION);
        assertThat(result.<String>read("$.ontology_ids[0]")).isNotBlank();
        assertThat(result.<String>read("$.species"))
                .isEqualTo(SPECIES);
        assertThat(subject.getHCAMetadataJson()).hasNoNullFieldsOrProperties();
    }

}