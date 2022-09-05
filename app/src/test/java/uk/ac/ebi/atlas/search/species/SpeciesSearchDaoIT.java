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
    void whenNoDocumentsAreFoundInBioEntitiesReturnEmptyOptional() {
        var species = subject.searchSpecies("FOOBAR", SYMBOL.name);

        assertThat(species.isPresent()).isTrue();
        assertThat(species.get()).isEmpty();
    }

    @Test
    void whenNoDocumentsAreFoundInTheIntersectionReturnEmptySet() {
        // Gorilla's gene exists in our Solr BioEntities collection, but not in our experiments
        // So, the species search by gene id should not find any results
        var gorillaGeneNotInOurExperiment = "ENSGGOG00000029327";

        var species = subject.searchSpecies(gorillaGeneNotInOurExperiment, SYMBOL.name);

        assertThat(species.isPresent()).isTrue();
        assertThat(species).hasValue(ImmutableSet.of());
    }

    @Test
    void whenGeneIdPartOfExperimentsReturnListOfSpecies() {
        var homoSapiensSymbolValueInOurExperiment = "ACRV1";
        var homoSapiensSpecies = "Homo_sapiens";

        var species = subject.searchSpecies(homoSapiensSymbolValueInOurExperiment, SYMBOL.name);

        assertThat(species.isPresent()).isTrue();
        assertThat(species.get()).contains(homoSapiensSpecies);
    }
}
