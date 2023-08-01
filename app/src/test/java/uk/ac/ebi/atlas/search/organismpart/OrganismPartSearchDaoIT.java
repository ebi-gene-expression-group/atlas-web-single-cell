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
import uk.ac.ebi.atlas.solr.SingleCellSolrUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrganismPartSearchDaoIT {

    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private SingleCellSolrUtils solrUtils;

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
    void whenEmptySetOfCellIDsAndCellTypesProvidedReturnEmptySetOfOrganismPart() {
        ImmutableSet<String> emptyCellIDs = ImmutableSet.of();
        ImmutableSet<String> emptySetOfCellTypes = ImmutableSet.of();

        var organismParts = subject.searchOrganismPart(emptyCellIDs, emptySetOfCellTypes);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenInvalidCellIdsAndNoCellTypesProvidedReturnEmptySetOfOrganismPart() {
        var invalidCellIDs =
                ImmutableSet.of("invalid-cellID-1", "invalid-cellID-2", "invalid-cellID-3");
        ImmutableSet<String> emptySetOfCellTypes = ImmutableSet.of();

        var organismParts = subject.searchOrganismPart(invalidCellIDs, emptySetOfCellTypes);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenOnlyValidCellIdsButNoCellTypesProvidedReturnSetOfOrganismPart() {
        var randomListOfCellIDs =
                ImmutableSet.copyOf(
                        new HashSet<>(jdbcUtils.fetchRandomListOfCells(10)));
        ImmutableSet<String> emptySetOfCellTypes = ImmutableSet.of();

        var organismParts = subject.searchOrganismPart(randomListOfCellIDs, emptySetOfCellTypes);

        assertThat(organismParts.size()).isGreaterThan(0);
    }

    @Test
    void whenValidCellIdsButInvalidCellTypesProvidedReturnEmptySetOfOrganismPart() {
        var randomListOfCellIDs =
                ImmutableSet.copyOf(
                        new HashSet<>(jdbcUtils.fetchRandomListOfCells(10)));
        ImmutableSet<String> invalidCellTypes = ImmutableSet.of("invalid-cellType-1", "invalid-cellType-2");

        var organismParts = subject.searchOrganismPart(randomListOfCellIDs, invalidCellTypes);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenValidCellIdsAndValidCellTypesProvidedReturnSetOfOrganismPart() {
        var randomListOfCellIDs =
                ImmutableSet.copyOf(
                        new HashSet<>(jdbcUtils.fetchRandomListOfCells(3)));
        ImmutableSet<String> cellTypes = solrUtils.fetchedRandomCellTypesByCellIDs(randomListOfCellIDs, 1);

        var organismParts = subject.searchOrganismPart(randomListOfCellIDs, cellTypes);

        assertThat(organismParts.size()).isGreaterThan(0);
    }
}
