package uk.ac.ebi.atlas.search;

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

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OntologyAccessionsSearchDaoIT {
    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private OntologyAccessionsSearchDao subject;

    @BeforeEach
    void setUp() {
        subject = new OntologyAccessionsSearchDao(collectionProxyFactory);
    }


    @Test
    void foo() {
        assertThat(subject.searchOntologyAnnotations("E-MTAB-5061", "http://purl.obolibrary.org/obo/UBERON_0001264")).isNotEmpty();
    }
}