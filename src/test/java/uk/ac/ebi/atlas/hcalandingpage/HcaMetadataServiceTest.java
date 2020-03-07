package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.ArrayList;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.MockExperiment.createBaselineExperiment;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateBlankString;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomOntologyId;

@RunWith(MockitoJUnitRunner.class)
public class HcaMetadataServiceTest {
    private static final String EXPERIMENT_ACCESSION = generateRandomExperimentAccession();

    @Mock
    private ExperimentTrader experimentTraderMock;

    @Mock
    private HcaMetadataDao hcaMetadataDaoMock;

    private HcaMetadataService subject;

    @Before
    public void setUp() throws Exception {
        var result = new ArrayList<HashMap>();
        result.add(new HashMap<String, String>()
        {{
            put("ontology_annotation", generateRandomOntologyId());
            put("experiment_accession", EXPERIMENT_ACCESSION);
            put("facet_characteristic_value", generateBlankString());
        }});
        result.add(new HashMap<String, String>()
        {{
            put("ontology_annotation", generateRandomOntologyId());
            put("experiment_accession", EXPERIMENT_ACCESSION);
            put("facet_characteristic_value", generateBlankString());
        }});
        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION))
                .thenReturn(createBaselineExperiment(EXPERIMENT_ACCESSION)
                );
        when(hcaMetadataDaoMock.fetchHumanExperimentAccessionsAndAssociatedOrganismParts())
                .thenReturn(ImmutableSet.of(result));

        subject = new HcaMetadataService(experimentTraderMock, hcaMetadataDaoMock);
    }

    @Test
    public void sizeIsRightForHcaExperiments() {
        var result = subject.getHcaExperiments();
        assertThat(result).hasSize(1);
    }

    @Test
    public void sizeIsRightForOntologyIds() {
        var result = subject.getHcaOntologyIds();
        assertThat(result).hasSize(2);
    }

    @Test
    public void experimentsAreInFormatWeExpect() {
        var result = JsonPath.parse(subject.getHcaExperiments().toString());

        assertThat(result.<String>read("$.[0].experimentAccession"))
                .isEqualTo(EXPERIMENT_ACCESSION);
    }
}