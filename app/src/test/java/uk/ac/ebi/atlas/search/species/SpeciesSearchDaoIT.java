package uk.ac.ebi.atlas.search.species;

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
import static uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName.SYMBOL;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpeciesSearchDaoIT {
    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private SpeciesSearchDao subject;

    @BeforeEach
    void setUp() {
        subject = new SpeciesSearchDao(collectionProxyFactory);
    }

    @Test
    void whenEmptySearchTextProvidedReturnEmptyOptional() {
        var searchText = "";
        var category = SYMBOL.name;

        var actualSpecies = subject.searchSpecies(searchText, category);

        assertThat(actualSpecies).isEmpty();
    }

    @Test
    void whenNoCategoryButValidGeneIdProvidedThenReturnResult() {
        var searchText = "ACRV1";
        String category = null;
        var expectedSpecies = ImmutableSet.of("Homo_sapiens", "Mus_musculus");

        var actualSpecies = subject.searchSpecies(searchText, category);

        assertThat(actualSpecies).containsSequence(expectedSpecies);
    }

    @Test
    void whenNoDocumentsAreFoundInBioEntitiesReturnEmptyOptional() {
        var actualSpecies = subject.searchSpecies("FOOBAR", SYMBOL.name);

        assertThat(actualSpecies).isEmpty();
    }

    @Test
    void whenNoDocumentsAreFoundInTheIntersectionReturnEmptySet() {
        // Gorilla's gene exists in our Solr BioEntities collection, but not in our experiments
        // So, the species search by gene id should not find any results
        var gorillaGeneNotInOurExperiment = "ENSGGOG00000029327";

        var actualSpecies = subject.searchSpecies(gorillaGeneNotInOurExperiment, SYMBOL.name);

        assertThat(actualSpecies).isEmpty();
    }

    @Test
    void whenGeneIdAsGenericQueryPartOfExperimentsReturnListOfSpecies() {
        var homoSapiensSymbolValueInOurExperiment = "ACRV1";
        var anExpectedSpecies = "Homo_sapiens";

        var actualSpecies =
                subject.searchSpecies(homoSapiensSymbolValueInOurExperiment);

        assertThat(actualSpecies).contains(anExpectedSpecies);
    }

    @Test
    void whenGeneIdAsSymbolPartOfExperimentsReturnListOfSpecies() {
        var homoSapiensSymbolValueInOurExperiment = "ACRV1";
        var anExpectedSpecies = "Homo_sapiens";

        var actualSpecies = subject.searchSpecies(homoSapiensSymbolValueInOurExperiment, SYMBOL.name);

        assertThat(actualSpecies).contains(anExpectedSpecies);
    }
}
