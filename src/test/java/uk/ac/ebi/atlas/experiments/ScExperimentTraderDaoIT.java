package uk.ac.ebi.atlas.experiments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@Transactional
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
    void returnsCorrectExperiments(){
        var result = subject.fetchHumanExperimentAccessionsAndAssociatedOrganismParts();
        var experiment_accessions = result.stream()
                .map(metadata -> {
                    var dataMap = (HashMap) metadata.iterator().next();
                    return dataMap.get("experiment_accession").toString();
                })
                .collect(Collectors.toList());
        assertThat(experiment_accessions)
                .contains("E-GEOD-81547", "E-MTAB-5061");
    }
}