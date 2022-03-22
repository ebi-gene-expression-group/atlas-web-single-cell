package uk.ac.ebi.atlas.bioentity.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
@Sql("/fixtures/202203/scxa_analytics.sql")
@Sql(value = "/fixtures/202203/scxa_analytics-delete.sql", executionPhase = AFTER_TEST_METHOD)
class ExpressedBioentityFinderImplIT {
    @Inject
    JdbcUtils jdbcUtils;

    @Inject
    private ExpressedBioentityFinderImpl subject;

    @Test
    void unknownGeneIdIsNotExpressed() {
        assertThat(subject.bioentityIsExpressedInAtLeastOneExperiment(generateRandomEnsemblGeneId()))
                .isFalse();
    }

    @Test
    void expressedGeneIdIsExpressed() {
        var geneId = jdbcUtils.fetchRandomGene();

        assertThat(subject.bioentityIsExpressedInAtLeastOneExperiment(geneId))
                .isTrue();
    }
}
