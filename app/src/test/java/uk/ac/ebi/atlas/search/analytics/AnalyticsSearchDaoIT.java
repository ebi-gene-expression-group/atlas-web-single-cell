package uk.ac.ebi.atlas.search.analytics;

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
import uk.ac.ebi.atlas.testutils.RandomDataTestUtils;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnalyticsSearchDaoIT {

    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private DataSource dataSource;

    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private AnalyticsSearchDao subject;

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
        subject = new AnalyticsSearchDao(collectionProxyFactory);
    }

    @Test
    void whenEmptySetOfCellIDsProvidedReturnEmptySetOfOrganismPart() {
        ImmutableSet<String> cellIDs = ImmutableSet.of();

        var organismParts = subject.searchFieldByCellIds(CTW_ORGANISM_PART, cellIDs);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenInvalidCellIdsProvidedReturnEmptySetOfOrganismPart() {
        var cellIDs =
                ImmutableSet.of(
                        RandomDataTestUtils.generateRandomCellId(),
                        RandomDataTestUtils.generateRandomCellId(),
                        RandomDataTestUtils.generateRandomCellId()
                );

        var organismParts = subject.searchFieldByCellIds(CTW_ORGANISM_PART, cellIDs);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenValidCellIdsProvidedReturnSetOfOrganismPart() {
        var cellIDs =
                ImmutableSet.copyOf(jdbcUtils.fetchRandomListOfCells(10));

        var organismParts = subject.searchFieldByCellIds(CTW_ORGANISM_PART, cellIDs);

        assertThat(organismParts.size()).isGreaterThan(0);
    }

    @Test
    void whenEmptySetOfCellIdsProvidedReturnEmptySetOfCellType() {
        ImmutableSet<String> cellIDs = ImmutableSet.of();

        var cellTypes = subject.searchFieldByCellIds(CTW_CELL_TYPE, cellIDs);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenInvalidCellIdsProvidedReturnEmptySetOfCellType() {
        var cellIDs =
                ImmutableSet.of(
                        RandomDataTestUtils.generateRandomCellId(),
                        RandomDataTestUtils.generateRandomCellId(),
                        RandomDataTestUtils.generateRandomCellId()
                );

        var cellTypes = subject.searchFieldByCellIds(CTW_CELL_TYPE, cellIDs);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenValidCellIdsProvidedReturnSetOfCellTypes() {
        var cellIDs =
                ImmutableSet.copyOf(jdbcUtils.fetchRandomListOfCells(10));

        var cellTypes = subject.searchFieldByCellIds(CTW_CELL_TYPE, cellIDs);

        assertThat(cellTypes.size()).isGreaterThan(0);
    }
}
