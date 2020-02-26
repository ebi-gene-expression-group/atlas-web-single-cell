package uk.ac.ebi.atlas.experiments;

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
class ScExperimentTraderDaoIT {

    @Inject
    private SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory;

    @Inject
    private ScExperimentTraderDao subject;

    @BeforeEach
    void setUp() {
        subject = new ScExperimentTraderDao(solrCloudCollectionProxyFactory);
    }

    @Test
    void emptyIfNoExperimentsCanBeFound() {
        assertThat(subject.fetchPublicExperimentAccessions("organism_part", "foobar"))
                .isEmpty();
    }

    @Test
    void notEmptyForCorrectCharacteristicType() {
        assertThat(subject.fetchPublicExperimentAccessions("organism_part", "skin"))
                .isNotEmpty()
                .containsExactlyInAnyOrder("E-EHCA-2");
    }

    @Test
    void returnAllExperimentsForEmptyCharacteristicValue() {
        assertThat(subject.fetchPublicExperimentAccessions("organism_part", ""))
                .isNotEmpty();
    }

}