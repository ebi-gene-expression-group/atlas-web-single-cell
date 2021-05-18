package uk.ac.ebi.atlas.experimentimport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
@Sql("/fixtures/experiment-fixture.sql")
@Sql(scripts = "/fixtures/experiment-delete.sql", executionPhase = AFTER_TEST_METHOD)
class ScxaExperimentCrudIT {
    private static final Random RNG = ThreadLocalRandom.current();

    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private ScxaExperimentCrud subject;

    @Test
    void createExperiment() {
        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
        subject.deleteExperiment(experimentAccession);
        assertThat(subject.readExperiment(experimentAccession))
                .isEmpty();

        subject.createExperiment(experimentAccession, RNG.nextBoolean());
        assertThat(subject.readExperiment(experimentAccession))
                .isPresent()
                .get().hasNoNullFieldsOrProperties();
    }

    @Test
    void throwsIfExperimentFilesCannotBeFound() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> subject.createExperiment(generateRandomExperimentAccession(), RNG.nextBoolean()));
    }

    @Test
    void updatesLastUpdateIfExperimentIsAlreadyPresent() {
        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
        var experimentBeforeUpdate = subject.readExperiment(experimentAccession).orElseThrow();
        var lastUpdateBeforeUpdate = experimentBeforeUpdate.getLastUpdate();

        subject.createExperiment(experimentAccession, RNG.nextBoolean());
        assertThat(subject.readExperiment(experimentAccession)).get()
                .isEqualToIgnoringGivenFields(experimentBeforeUpdate, "lastUpdate", "isPrivate");
        assertThat(subject.readExperiment(experimentAccession).orElseThrow().getLastUpdate())
                .isAfter(lastUpdateBeforeUpdate);
    }

    @Test
    void throwIfExperimentDoesNotExistUpdateExperimentDesign() {
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> subject.updateExperimentDesign(generateRandomExperimentAccession()));
    }

    @Test
    void updateDesignCallsUpdateDesignInSuperClass() {
         var spy = spy(subject);

        var experimentAccession = jdbcUtils.fetchRandomExperimentAccession();
        spy.updateExperimentDesign(experimentAccession);

        verify(spy)
                .updateExperimentDesign(
                        any(),
                        eq(subject.readExperiment(experimentAccession).orElseThrow()));
    }
}
