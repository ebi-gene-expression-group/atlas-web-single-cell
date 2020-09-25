package uk.ac.ebi.atlas.search;

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

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CellTypeSearchByOrganismPartDaoIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private CellTypeSearchByOrganismPartDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        subject = new CellTypeSearchByOrganismPartDao(collectionProxyFactory);
    }

    @Test
    void invalidCellTypeMetadataSearch() {
        assertThat(subject.getCellTypeMetadata("E-MTAB-5061", "foobar"))
                .isEmpty();
    }

    @Test
    void validCellTypeMetadataSearch() {
        // pancreas
        var cellTypesInPancreas = subject.getCellTypeMetadata("E-MTAB-5061", "http://purl.obolibrary.org/obo/UBERON_0001264");
        assertThat(cellTypesInPancreas)
                .isNotEmpty();
        // islet of Langerhans
        var cellTypesInIsletOfLangerhans = subject.getCellTypeMetadata("E-MTAB-5061", "http://purl.obolibrary.org/obo/UBERON_0000006");
        assertThat(cellTypesInIsletOfLangerhans)
                .isNotEmpty();

        assertThat(cellTypesInPancreas)
                .containsAll(cellTypesInIsletOfLangerhans);
    }
}