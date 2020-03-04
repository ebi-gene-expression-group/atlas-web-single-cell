package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.trader.ExperimentTrader;


import java.util.ArrayList;
import java.util.HashMap;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.MockExperiment.createBaselineExperiment;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateBlankString;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomOntologyId;

@ExtendWith(MockitoExtension.class)
class ScExperimentTraderTest {
    private final static String EXPERIMENT_ACCESSION = generateRandomExperimentAccession();

    @Mock
    private ExperimentTrader experimentTraderMock;

    @Mock
    private ScExperimentTraderDao scExperimentTraderDaoMock;

    private ScExperimentTrader subject;

    @BeforeEach
    void setUp() {
        subject = new ScExperimentTrader(experimentTraderMock, scExperimentTraderDaoMock);
    }

    @Test
    void returnsHashMapInExpectedFormat() {
        var result = new ArrayList<HashMap>();
        result.add(new HashMap<String, String>()
        {{
            put("ontology_annotation", generateRandomOntologyId());
            put("experiment_accession", EXPERIMENT_ACCESSION);
            put("facet_characteristic_value", generateBlankString());
        }});

        when(scExperimentTraderDaoMock.fetchHumanExperimentAccessionsAndAssociatedOrganismParts())
                .thenReturn(ImmutableSet.of(result));

        assertThat(subject.getMetadata())
                .hasSize(2)
                .containsKeys("ontology_ids", "experiment_accessions")
                .containsValues(ImmutableList.of(EXPERIMENT_ACCESSION));
    }

    @Test
    void returnsExperimentWithSameExperimentAccessionAsPassed(){
        var experiment = createBaselineExperiment(EXPERIMENT_ACCESSION);
        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION))
                .thenReturn(experiment);

        assertThat(subject.getPublicExperiments(ImmutableList.of(EXPERIMENT_ACCESSION)))
                .contains(experiment);
    }

}