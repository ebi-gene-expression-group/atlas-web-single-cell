package uk.ac.ebi.atlas.trader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_NUCLEUS_RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@Sql("/fixtures/experiment-fixture.sql")
class ScxaExperimentRepositoryIT {
    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private ScxaExperimentRepository subject;

    @Test
    void throwIfExperimentCannotBeFound() {
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "experiment")).isGreaterThan(0);
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> subject.getExperiment(generateRandomExperimentAccession()));
    }

    @Test
    void singleCellBaselineRnaSeqExperiments() {
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(SINGLE_CELL_RNASEQ_MRNA_BASELINE)))
                .isInstanceOf(SingleCellBaselineExperiment.class)
                .hasNoNullFieldsOrProperties();
    }

    @Test
    void singleNucleusBaselineRnaSeqExperiments() {
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(SINGLE_NUCLEUS_RNASEQ_MRNA_BASELINE)))
          .isInstanceOf(SingleCellBaselineExperiment.class)
          .hasNoNullFieldsOrProperties();
    }
}
