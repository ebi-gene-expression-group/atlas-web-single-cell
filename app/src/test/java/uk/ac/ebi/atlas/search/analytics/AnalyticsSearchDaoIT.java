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

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_CELL_TYPE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CTW_ORGANISM_PART;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomCellId;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomOrganismPart;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

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
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/scxa_analytics.sql")
        );

        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql")
        );
        populator.execute(dataSource);
    }

    @BeforeEach
    void setup() {
        subject = new AnalyticsSearchDao(collectionProxyFactory);
    }

    @Test
    void whenEmptySetOfCellIDsProvidedReturnEmptySetOfOrganismPart() {
        var cellIDs = ImmutableSet.<String>of();

        var organismParts = subject.searchOutputFieldByInputFieldValues(
                CTW_ORGANISM_PART, CELL_ID, cellIDs);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenInvalidCellIdsProvidedReturnEmptySetOfOrganismPart() {
        var cellIDs =
                ImmutableSet.of(
                        generateRandomCellId(),
                        generateRandomCellId(),
                        generateRandomCellId()
                );

        var organismParts = subject.searchOutputFieldByInputFieldValues(
                CTW_ORGANISM_PART, CELL_ID, cellIDs);

        assertThat(organismParts).isEmpty();
    }

    @Test
    void whenValidCellIdsProvidedReturnSetOfOrganismPart() {
        var cellIDs =
                ImmutableSet.copyOf(jdbcUtils.fetchRandomListOfCells(10));

        var organismParts = subject.searchOutputFieldByInputFieldValues(
                CTW_ORGANISM_PART, CELL_ID, cellIDs);

        assertThat(organismParts.size()).isGreaterThan(0);
    }

    @Test
    void whenEmptySetOfCellIdsProvidedReturnEmptySetOfCellType() {
        var cellIDs = ImmutableSet.<String>of();

        var cellTypes = subject.searchOutputFieldByInputFieldValues(
                CTW_CELL_TYPE, CELL_ID, cellIDs);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenInvalidCellIdsProvidedReturnEmptySetOfCellType() {
        var cellIDs =
                ImmutableSet.of(
                        generateRandomCellId(),
                        generateRandomCellId(),
                        generateRandomCellId()
                );

        var cellTypes = subject.searchOutputFieldByInputFieldValues(
                CTW_CELL_TYPE, CELL_ID, cellIDs);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenValidCellIdsProvidedReturnSetOfCellTypes() {
        var cellIDs =
                ImmutableSet.copyOf(jdbcUtils.fetchRandomListOfCells(10));

        var cellTypes = subject.searchOutputFieldByInputFieldValues(
                CTW_CELL_TYPE, CELL_ID, cellIDs);

        assertThat(cellTypes.size()).isGreaterThan(0);
    }

    @Test
    void whenInvalidParametersProvidedReturnEmptySetOfOrganismParts() {
        var cellIDs = ImmutableSet.of(
                generateRandomCellId(),
                generateRandomCellId(),
                generateRandomCellId()
        );
        var species = ImmutableSet.of(
                generateRandomSpecies().getName(),
                generateRandomSpecies().getName(),
                generateRandomSpecies().getName()
        );

        var actualOrganismParts = subject.searchOrganismPartsByCellIdsAndSpecies(
                cellIDs, species);

        assertThat(actualOrganismParts).isEmpty();
    }

    @Test
    void whenValidCellIDsAndSpeciesProvidedReturnSetOfOrganismParts() {
        var experimentAccession = "E-EHCA-2";
        var cellIDs = ImmutableSet.copyOf(
                jdbcUtils.fetchRandomListOfCellsFromExperiment(experimentAccession, 10));
        var species = ImmutableSet.of(
                jdbcUtils.fetchSpeciesByExperimentAccession(experimentAccession));

        var actualOrganismParts = subject.searchOrganismPartsByCellIdsAndSpecies(cellIDs, species);

        assertThat(actualOrganismParts.size()).isGreaterThan(0);
    }

    @Test
    void whenInvalidParametersProvidedReturnEmptySetOfCellTypes() {
        var cellIDs = ImmutableSet.of(
                generateRandomCellId(),
                generateRandomCellId(),
                generateRandomCellId()
        );
        var species = ImmutableSet.of(
                generateRandomSpecies().getName(),
                generateRandomSpecies().getName(),
                generateRandomSpecies().getName()
        );
        var organismParts = ImmutableSet.of(
                generateRandomOrganismPart(),
                generateRandomOrganismPart()
        );

        var actualCellTypes = subject.searchCellTypesByCellIdsAndSpeciesAndOrganismParts(
                cellIDs, species, organismParts);

        assertThat(actualCellTypes).isEmpty();
    }

    @Test // TODO replace hardcoded values with random ones if it is possible
    void whenValidParametersProvidedReturnSetOfCellTypes() {
        var experimentAccession = "E-CURD-4";
        var species = ImmutableSet.of(
                jdbcUtils.fetchSpeciesByExperimentAccession(experimentAccession));
        // hardcoded values for cellIds and organism parts as we don't have fixtures for Solr
        var cellIDs = ImmutableSet.of(
                "SRR8206654-ATTGTTAGTAGT", "SRR8206662-ATCTCGCTCCCC", "SRR8206662-CAGGATTAAGCC");
        var organismParts = ImmutableSet.of("root");

        var actualOrganismParts = subject.searchCellTypesByCellIdsAndSpeciesAndOrganismParts(
                cellIDs, species, organismParts);

        assertThat(actualOrganismParts.size()).isGreaterThan(0);
    }
}
