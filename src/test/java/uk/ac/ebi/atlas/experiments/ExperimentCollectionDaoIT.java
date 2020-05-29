package uk.ac.ebi.atlas.experiments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@Transactional
@Sql("/fixtures/experiment-fixture.sql")
@Sql("/fixtures/scxa-collections-fixture.sql")
@Sql("/fixtures/scxa-experiment2collection-fixture.sql")
class ExperimentCollectionDaoIT {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    ExperimentCollectionDao subject;

    @BeforeEach
    void setUp() {
        subject = new ExperimentCollectionDao(jdbcTemplate);
    }

    @Test
    void ifNoExperimentCollectionReturnNothing() {
        assertThat(subject.getExperimentCollection("E-FOO-0000"))
                .isEmpty();
    }

    @Test
    void ifExperimentCollectionExistsReturnCollectionName() {
        assertThat(subject.getExperimentCollection("E-EHCA-2"))
                .isNotEmpty()
                .containsExactly("Human Cell Atlas");
    }

}