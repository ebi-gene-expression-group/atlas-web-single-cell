package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HcaHumanExperimentDaoIT {

    @Inject
    private SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory;

    @Inject
    private HcaHumanExperimentDao subject;

    @BeforeEach
    void setUp() {
        subject = new HcaHumanExperimentDao(solrCloudCollectionProxyFactory);
    }

    @Test
    void emptyIfNoExperimentsCanBeFound() {
        assertThat(subject.fetchExperimentAccessions("organism_part", ImmutableSet.of("foobar")))
                .isEmpty();
    }

    @Test
    void notEmptyForCorrectCharacteristicType() {
        assertThat(subject.fetchExperimentAccessions("organism_part", ImmutableSet.of("skin")))
                .isNotEmpty()
                .containsExactlyInAnyOrder("E-EHCA-2");
    }

    @Test
    void notEmptyForWhenValidOnotologyIdIsPassed() {
        assertThat(subject.fetchExperimentAccessions("organism_part",
                ImmutableSet.of("UBERON_0000029")))
                .isNotEmpty()
                .containsExactlyInAnyOrder("E-EHCA-2");
    }

    @Test
    void returnAllExperimentsForEmptyCharacteristicValue() {
        assertThat(subject.fetchExperimentAccessions("organism_part", ImmutableSet.of()))
                .isNotEmpty();
    }

    @Test
    void notEmptyWhenValidOntologyAnnotationLabelIsPassed() {
        assertThat(subject.fetchExperimentAccessions("organism_part", ImmutableSet.of("zone of skin")))
                .isNotEmpty()
                .contains("E-EHCA-2");
    }

    @Test
    void returnMatchingExperimentsForMultipleCharacteristicValues() {
        assertThat(subject.fetchExperimentAccessions("organism_part", ImmutableSet.of("skin", "lymph node")))
                .isNotEmpty()
                .contains("E-EHCA-2");
    }

}