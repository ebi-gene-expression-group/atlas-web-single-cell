package uk.ac.ebi.atlas.experiments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@Sql("/fixtures/experiment-fixture.sql")
@Sql("/fixtures/scxa-collections-fixture.sql")
@Sql("/fixtures/scxa-experiment2collection-fixture.sql")
class ScxaExperimentCollectionsIT {

    @Inject
    private ExperimentCollectionDao experimentCollectionDao;

    private ScxaExperimentCollections subject;

    @BeforeEach
    public void setUp() throws Exception {
        subject = new ScxaExperimentCollections(experimentCollectionDao);
    }

    @Test
    void returnEmptyIfExperimentDoesntHaveCollection() {
        assertThat(subject.getExperimentCollections(generateRandomExperimentAccession()))
                .isEmpty();
    }

    @Test
    void returnCollectionNameIfExist() {
        assertThat(subject.getExperimentCollections("E-EHCA-2"))
                .isNotEmpty()
                .containsExactly("Human Cell Atlas");
    }
}