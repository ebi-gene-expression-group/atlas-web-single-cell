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
import uk.ac.ebi.atlas.search.GeneSearchDao;
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

    @Inject
    private GeneSearchDao geneSearchDao;

    private OrganismPartSearchDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_analytics.sql"),
                new ClassPathResource("fixtures/experiment.sql")
        );

        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql")
        );
        populator.execute(dataSource);
    }

    @BeforeEach
    void setup() {
        subject = new OrganismPartSearchDao(collectionProxyFactory, geneSearchDao);
    }

    @Test
    void whenEmptySetOfGeneIDsProvidedReturnEmptyOptional() {
        ImmutableSet<String> geneIds = ImmutableSet.of();

        var organismParts = subject.searchOrganismPart(geneIds);

        assertThat(organismParts.isPresent()).isTrue();
        assertThat(organismParts.get()).isEmpty();
    }

    @Test
    void whenSetOfInvalidGeneIdsProvidedReturnSetOfOrganismPart() {
        ImmutableSet<String> geneIds =
                ImmutableSet.of("invalid-geneId-1", "invalid-geneId-2", "invalid-geneId-3");

        var organismParts = subject.searchOrganismPart(geneIds);

        assertThat(organismParts.isPresent()).isTrue();
        assertThat(organismParts.get()).isEmpty();
    }

    @Test
    void whenSetOfValidGeneIdsProvidedReturnSetOfOrganismPart() {
        final List<String> randomListOfGeneIds = jdbcUtils.fetchRandomListOfGeneIds(10);
        ImmutableSet<String> geneIds =
                ImmutableSet.copyOf(
                        new HashSet<String>(randomListOfGeneIds));

        var organismParts = subject.searchOrganismPart(geneIds);

        assertThat(organismParts.isPresent()).isTrue();
        assertThat(organismParts.get().size()).isGreaterThan(0);
    }
}
