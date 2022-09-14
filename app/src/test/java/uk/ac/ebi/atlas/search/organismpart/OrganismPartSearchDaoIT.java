package uk.ac.ebi.atlas.search.organismpart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName.SYMBOL;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrganismPartSearchDaoIT {

    private OrganismPartSearchDao subject;

    @BeforeEach
    void setup() {
        subject = new OrganismPartSearchDao();
    }

    @Test
    void whenEmptySearchTextProvidedReturnEmptyOptional() {
        var queryTerm = "";
        var category = SYMBOL.name;

        var organismPart = subject.searchOrganismPart(queryTerm, category);

        assertThat(organismPart.isPresent()).isTrue();
        assertThat(organismPart.get()).isEmpty();
    }
}
