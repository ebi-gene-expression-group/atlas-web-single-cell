package uk.ac.ebi.atlas.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OntologyAccessionsSearchServiceIT {
    @Inject
    private OntologyAccessionsSearchDao ontologyAccessionsSearchDao;

    private OntologyAccessionsSearchService subject;

    @BeforeEach
    void setUp() {
        subject = new OntologyAccessionsSearchService(ontologyAccessionsSearchDao);
    }

    @Test
    void humanPancreasExperimentReturnsAnatomogramAnnotations() {
        assertThat(subject.searchAvailableAnnotationsForOrganAnatomogram("E-MTAB-5061")).isNotEmpty();
    }

    @Test
    void nonHumanExperimentReturnsNoAnatomogramData() {
        assertThat(subject.searchAvailableAnnotationsForOrganAnatomogram("E-EHCA-2")).isEmpty();
    }
}