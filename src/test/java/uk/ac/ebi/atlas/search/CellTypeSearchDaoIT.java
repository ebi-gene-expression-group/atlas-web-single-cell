package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellTypeSearchDaoIT {
    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private CellTypeSearchDao subject;

    @BeforeEach
    void setUp() {
        subject = new CellTypeSearchDao(collectionProxyFactory);
    }

    @Test
    void nonExistentValueReturnsEmptyCollection() {
        assertThat(subject.getInferredCellTypeOntologyLabels("E-MTAB-5061", ImmutableSet.of("foobar")))
                .isEmpty();
        assertThat(subject.getInferredCellTypeAuthorsLabels("E-MTAB-5061", ImmutableSet.of("foobar")))
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
    void onotologyLabelsAndAuthorMayBeDifferent() {
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
						"E-MTAB-5061", ImmutableSet.of("http://purl.obolibrary.org/obo/UBERON_0000006","http://purl.obolibrary.org/obo/UBERON_0001264"));
		assertThat(authorsLabels).isNotEmpty();
	}
}
