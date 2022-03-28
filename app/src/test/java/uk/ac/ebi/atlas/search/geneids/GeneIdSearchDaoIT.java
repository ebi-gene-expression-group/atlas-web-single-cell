package uk.ac.ebi.atlas.search.geneids;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.SearchStreamBuilder;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.trader.ExperimentTraderDao;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName.ENSGENE;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_IDENTIFIER;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.PROPERTY_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.PROPERTY_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.SPECIES;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeneIdSearchDaoIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcTestUtils;

    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    private BioentitiesCollectionProxy bioentitiesCollectionProxy;

    @Inject
    private ExperimentTraderDao experimentTraderDao;

    private GeneIdSearchDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_analytics.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        subject = new GeneIdSearchDao(collectionProxyFactory, experimentTraderDao);
        bioentitiesCollectionProxy = collectionProxyFactory.create(BioentitiesCollectionProxy.class);
    }

    @Test
    void ifNoDocumentsAreFoundInBioentititesReturnEmptyOptional() {
        assertThat(subject.searchGeneIds("FOOBAR", ENSGENE.name))
                .isEmpty();
    }

    @Test
    void ifNoDocumentsAreFoundInTheIntersectionReturnEmptySet() {
        var speciesNotCovered = "Oryza_sativa";

        var queryBuilder = new SolrQueryBuilder<BioentitiesCollectionProxy>();
        queryBuilder
                .addQueryFieldByTerm(SPECIES, speciesNotCovered)
                .sortBy(BIOENTITY_IDENTIFIER, SolrQuery.ORDER.asc)
                .setFieldList(BIOENTITY_IDENTIFIER);

        try (var tupleStreamer =
                     TupleStreamer.of(new SearchStreamBuilder<>(bioentitiesCollectionProxy, queryBuilder).build())) {
            var geneId =
                    tupleStreamer.get()
                            .findAny()
                            .map(tuples -> tuples.getString(BIOENTITY_IDENTIFIER.name()))
                            .orElseThrow(RuntimeException::new);

            assertThat(subject.searchGeneIds(geneId, ENSGENE.name))
                    .hasValue(ImmutableSet.of());

        }
    }

    @ParameterizedTest
    @MethodSource("randomGeneIdProvider")
    void geneIdWithEnsgeneReturnsSelf(String geneId) {
        assertThat(subject.searchGeneIds(geneId, ENSGENE.name))
                .contains(ImmutableSet.of(geneId));
    }

    @ParameterizedTest
    @MethodSource("randomGeneIdProvider")
    void geneIdIsFoundSearchingItsOwnProperties(String geneId) {
        var queryBuilder = new SolrQueryBuilder<BioentitiesCollectionProxy>();
        queryBuilder
                .addQueryFieldByTerm(BIOENTITY_IDENTIFIER, geneId)
                .sortBy(PROPERTY_NAME, SolrQuery.ORDER.asc)
                .setFieldList(ImmutableSet.of(PROPERTY_VALUE, PROPERTY_NAME));

        try (var tupleStreamer =
                     TupleStreamer.of(new SearchStreamBuilder<>(bioentitiesCollectionProxy, queryBuilder).build())) {

            // We pick a random property, otherwise we risk flooding Solr with a pile of requests and getting a timeout
            var anyProperty =
                    tupleStreamer.get()
                            .map(tuple ->
                                    Pair.of(tuple.getString(PROPERTY_VALUE.name()),
                                            tuple.getString(PROPERTY_NAME.name())))
                            .findAny()
                            .orElseThrow(RuntimeException::new);

            assertThat(subject.searchGeneIds(anyProperty.getLeft(), anyProperty.getRight()))
                    .hasValueSatisfying(results ->
                            assertThat(results).contains(geneId));
        }
    }

    @Test
    void filterBySpecies() {
        var multiSpeciesPropertyValue = "GO:0005622";
        var propertyName = "go";

        assertThat(subject.searchGeneIds(multiSpeciesPropertyValue, propertyName, "Homo_sapiens"))
                .hasValueSatisfying(results ->
                        assertThat(results).allMatch(geneId -> geneId.matches("ENSG\\d+")));

        assertThat(subject.searchGeneIds(multiSpeciesPropertyValue, propertyName, "Glycine_max"))
                .hasValueSatisfying(results ->
                            assertThat(results).allMatch(geneId -> geneId.matches("GLYMA_.{9}")));
    }

    private Stream<String> randomGeneIdProvider() {
        return Stream.of(jdbcTestUtils.fetchRandomGene());
    }
}
