package uk.ac.ebi.atlas.hcalandingpage;

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
public class HcaMetadataServiceTest {
    private static final String EXPERIMENT_ACCESSION = generateRandomExperimentAccession();
    private static final List<String> ONTOLOGY_IDS = ImmutableList.of(
            generateRandomOntologyId(),
            generateRandomOntologyId(),
            generateRandomOntologyId());
    private static final List<String> EXPERIMENT_ACCESSIONS = ImmutableList.of(
            EXPERIMENT_ACCESSION
    );
    @Mock
    private HcaMetadataTrader hcaMetadataTraderMock;

    private HcaMetadataService subject;

    @Before
    public void setUp() throws Exception {
        when(hcaMetadataTraderMock.getMetadata())
                .thenReturn(ImmutableMap.of(
                        "ontology_ids", ONTOLOGY_IDS,
                        "experiment_accessions", EXPERIMENT_ACCESSIONS
                ));
        when(hcaMetadataTraderMock.getHcaExperiments(EXPERIMENT_ACCESSIONS))
                .thenReturn(ImmutableSet.of(MockExperiment.createBaselineExperiment(EXPERIMENT_ACCESSION)));

        subject = new HcaMetadataService(hcaMetadataTraderMock);
    }

    @Test
    public void sizeIsRightForHcaExperiments() {
        var result = subject.getHcaExperiments();
        assertThat(result).hasSize(1);
    }

    @Test
    public void sizeIsRightForOntologyIds() {
        var result = subject.getHcaOntologyIds();
        assertThat(result).hasSize(3);
    }

    @Test
    public void experimentsAreInFormatWeExpect() {
        var result = JsonPath.parse(subject.getHcaExperiments().toString());

        assertThat(result.<String>read("$.[0].experimentAccession"))
                .isEqualTo(EXPERIMENT_ACCESSION);
    }
}