package uk.ac.ebi.atlas.search.metadata;

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

import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetadataSearchDaoIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    private MetadataSearchDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-fixture.sql"),
                new ClassPathResource("fixtures/scxa_tsne-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-delete.sql"),
                new ClassPathResource("fixtures/scxa_tsne-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        subject = new MetadataSearchDao(collectionProxyFactory);
        singleCellAnalyticsCollectionProxy = collectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    @Test
    void invalidCellTypeMetadataSearch() {
        assertThat(subject.getCellTypeMetadata("foo", "bar", "E-EHCA-2"))
                .isEmpty();
    }

    @Test
    void validCellTypeMetadataSearch() {
        assertThat(subject.getCellTypeMetadata("sex", "female", "E-EHCA-2"))
                .isNotEmpty();
    }

}