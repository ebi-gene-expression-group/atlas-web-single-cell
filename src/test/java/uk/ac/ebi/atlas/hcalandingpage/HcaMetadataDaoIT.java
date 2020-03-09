package uk.ac.ebi.atlas.hcalandingpage;

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
class HcaMetadataDaoIT {

    @Inject
    private SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory;

    @Inject
    private HcaMetadataDao subject;

    @BeforeEach
    void setUp() {
        subject = new HcaMetadataDao(solrCloudCollectionProxyFactory);
    }

    @Test
    void returnsCorrectExperiments(){
        var result = subject.fetchHumanExperimentAccessionsAndAssociatedOntologyIds();
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