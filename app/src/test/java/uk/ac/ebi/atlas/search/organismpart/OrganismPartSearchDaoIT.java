package uk.ac.ebi.atlas.search.organismpart;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrganismPartSearchDaoIT {

    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private DataSource dataSource;

    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private OrganismPartSearchDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_analytics.sql")
        );

        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_analytics-delete.sql")
        );
        populator.execute(dataSource);
    }

    @BeforeEach
    void setup() {
        subject = new OrganismPartSearchDao(collectionProxyFactory);
    }

    @Test
    void whenEmptySetOfCellIDsProvidedReturnEmptySetOfOrganismPart() {
        ImmutableSet<String> cellIDs = ImmutableSet.of();

        var organismParts = subject.searchOrganismPart(cellIDs);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenInvalidCellIdsProvidedReturnEmptySetOfOrganismPart() {
        var cellIDs =
                ImmutableSet.of("invalid-cellID-1", "invalid-cellID-2", "invalid-cellID-3");

        var organismParts = subject.searchOrganismPart(cellIDs);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenValidCellIdsProvidedReturnSetOfOrganismPart() {
        final List<String> randomListOfCellIDs = jdbcUtils.fetchRandomListOfCells(10);
        var cellIDs =
                ImmutableSet.copyOf(
                        new HashSet<>(randomListOfCellIDs));

        var organismParts = subject.searchOrganismPart(cellIDs);

        assertThat(organismParts.size()).isGreaterThan(0);
    }
}
