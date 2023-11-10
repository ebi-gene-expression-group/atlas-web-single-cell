package uk.ac.ebi.atlas.search.celltype;

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
import uk.ac.ebi.atlas.solr.SingleCellSolrUtils;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellTypeSearchDaoIT {

    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private SingleCellSolrUtils solrUtils;

    @Inject
    private DataSource dataSource;

    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private CellTypeSearchDao subject;

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
    void setUp() {
        subject = new CellTypeSearchDao(collectionProxyFactory);
    }

    @Test
    void nonExistentValueReturnsEmptyCollection() {
        var experimentAccessions = jdbcUtils.fetchRandomExperimentAccession();
        assertThat(subject.getInferredCellTypeOntologyLabels(experimentAccessions, ImmutableSet.of("foobar")))
                .isEmpty();
        assertThat(subject.getInferredCellTypeAuthorsLabels(experimentAccessions, ImmutableSet.of("foobar")))
                .isEmpty();
    }

    @Test
    void ontologyLabelsMakeOntologicalSense() {
        // pancreas
        var cellTypesInPancreas =
                subject.getInferredCellTypeOntologyLabels(
                        "E-MTAB-5061", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0001264"));
        assertThat(cellTypesInPancreas)
                .isNotEmpty();

        // islet of Langerhans
        var cellTypesInIsletOfLangerhans =
                subject.getInferredCellTypeOntologyLabels(
                        "E-MTAB-5061", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000006"));
        assertThat(cellTypesInIsletOfLangerhans)
                .isNotEmpty();

        assertThat(cellTypesInPancreas)
                .containsAll(cellTypesInIsletOfLangerhans);
    }

    @Test
    // This is a weak test, but it’s useful to remember that there can be overlap between ontology and authors labels
    void ontologyLabelsAndAuthorMayBeDifferent() {
        var ontologyLabelsInPancreas =
                subject.getInferredCellTypeOntologyLabels(
                        "E-MTAB-5061", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0001264"));
        var authorsLabelsInPancreas =
                subject.getInferredCellTypeAuthorsLabels(
                        "E-MTAB-5061", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0001264"));

        assertThat(ontologyLabelsInPancreas)
                .doesNotContainSequence(authorsLabelsInPancreas);
        assertThat(ontologyLabelsInPancreas)
                .containsAnyElementsOf(authorsLabelsInPancreas);
    }

    @Test
    void ontologyLabelsAcceptMultipleOrganismParts(){
		var ontologyLabels =
				subject.getInferredCellTypeOntologyLabels(
						"E-MTAB-5061", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0001264","http://purl.obolibrary.org/obo/UBERON_0001987"));
		assertThat(ontologyLabels).isNotEmpty();
	}

	@Test
	void authorsLabelsAcceptMultipleOrganismParts(){
		var authorsLabels =
				subject.getInferredCellTypeAuthorsLabels(
						"E-MTAB-5061",
                        ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000006",
                                "http://purl.obolibrary.org/obo/UBERON_0001264"));
		assertThat(authorsLabels).isNotEmpty();
	}

    @Test
    void whenEmptySetOfCellIDsAndOrganismPartsProvidedReturnEmptySetOfCellTypes() {
        ImmutableSet<String> emptyCellIDs = ImmutableSet.of();
        ImmutableSet<String> emptySetOfOrganismParts = ImmutableSet.of();

        var cellTypes = subject.searchCellTypes(emptyCellIDs, emptySetOfOrganismParts);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenInvalidCellIdsAndNoOrganismPartsProvidedReturnEmptySetOfCellTypes() {
        var invalidCellIDs =
                ImmutableSet.of("invalid-cellID-1", "invalid-cellID-2", "invalid-cellID-3");
        ImmutableSet<String> emptySetOfOrganismParts = ImmutableSet.of();

        var cellTypes = subject.searchCellTypes(invalidCellIDs, emptySetOfOrganismParts);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenOnlyValidCellIdsButNoOrganismPartsProvidedReturnSetOfCellTypes() {
        var randomListOfCellIDs =
                ImmutableSet.copyOf(
                        new HashSet<>(jdbcUtils.fetchRandomListOfCells(10)));
        ImmutableSet<String> emptySetOfOrganismParts = ImmutableSet.of();

        var cellTypes = subject.searchCellTypes(randomListOfCellIDs, emptySetOfOrganismParts);

        assertThat(cellTypes.size()).isGreaterThan(0);
    }

    @Test
    void whenValidCellIdsButInvalidOrganismPartsProvidedReturnEmptySetOfCellTypes() {
        var randomListOfCellIDs =
                ImmutableSet.copyOf(
                        new HashSet<>(jdbcUtils.fetchRandomListOfCells(10)));
        ImmutableSet<String> invalidOrganismParts = ImmutableSet.of("invalid-cellType-1", "invalid-cellType-2");

        var cellTypes = subject.searchCellTypes(randomListOfCellIDs, invalidOrganismParts);

        assertThat(cellTypes).isEmpty();
    }

    @Test
    void whenValidCellIdsAndValidProvidedReturnSetOfCellTypes() {
        var randomListOfCellIDs =
                ImmutableSet.copyOf(
                        new HashSet<>(jdbcUtils.fetchRandomListOfCells(3)));
        ImmutableSet<String> organismParts = solrUtils.fetchedRandomOrganismPartsByCellIDs(
                randomListOfCellIDs, 1);

        var cellTypes = subject.searchCellTypes(randomListOfCellIDs, organismParts);

        assertThat(cellTypes.size()).isGreaterThan(0);
    }
}
