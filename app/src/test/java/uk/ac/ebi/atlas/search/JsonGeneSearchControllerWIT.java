package uk.ac.ebi.atlas.search;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityPropertyDao;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName.SYMBOL;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_IDENTIFIER;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.PROPERTY_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.PROPERTY_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.SPECIES;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonGeneSearchControllerWIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcTestUtils;

    @Inject
    private BioEntityPropertyDao bioEntityPropertyDao;

    @Inject
    private SolrCloudCollectionProxyFactory collectionProxyFactory;

    @Autowired
    private WebApplicationContext wac;

    private BioentitiesCollectionProxy bioentitiesCollectionProxy;

    private MockMvc mockMvc;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment.sql"),
                new ClassPathResource("fixtures/scxa_analytics.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        bioentitiesCollectionProxy = collectionProxyFactory.create(BioentitiesCollectionProxy.class);
    }

    @Test
    void unknownGene() throws Exception {
        this.mockMvc.perform(get("/json/search").param("q", "FOO"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results").isEmpty())
                .andExpect(jsonPath("$.reason").value(startsWith("Gene unknown")));
    }

    @Test
    void unexpressedGene() throws Exception {
        this.mockMvc.perform(get("/json/search").param("symbol", "FOX2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results").isEmpty())
                .andExpect(jsonPath("$.reason").value(startsWith("No expression found")));
    }

    @Test
    void validJsonForValidGeneId() throws Exception {
        var geneId = jdbcTestUtils.fetchRandomGene();

        this.mockMvc.perform(get("/json/search").param("ensgene", geneId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].element.experimentAccession", isA(String.class)))
                .andExpect(jsonPath("$.results[0].element.markerGenes", hasSize(greaterThanOrEqualTo(0))))
                .andExpect(jsonPath("$.results[0].facets", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].facets[0].group", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets[0].value", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets[0].label", isA(String.class)))
                .andExpect(jsonPath("$.checkboxFacetGroups", contains("Marker genes", "Species")))
                .andExpect(jsonPath("$.matchingGeneId", equalTo("")));
    }

    @Test
    void ifSymbolQueryMatchesUniqueGeneIdIncludeIt() throws Exception {
        var geneId = jdbcTestUtils.fetchRandomGene();

        // Some gene IDs don’t have a symbol, e.g. ERCC-00044
        // Also, it turns out that some gene symbols like Vmn1r216 match more than one gene ID within the same species:
        // ENSMUSG00000115697 and ENSMUSG00000116057
        // We don’t want any of those pesky gene IDs!
        var matchingSymbols = bioEntityPropertyDao.fetchPropertyValuesForGeneId(geneId, SYMBOL);
        while (matchingSymbols.isEmpty() ||
                bioEntityPropertyDao.fetchGeneIdsForPropertyValue(
                        SYMBOL, matchingSymbols.iterator().next()).size() > 1) {
            geneId = jdbcTestUtils.fetchRandomGene();
            matchingSymbols = bioEntityPropertyDao.fetchPropertyValuesForGeneId(geneId, SYMBOL);
        }

        var solrQueryBuilder =
                new SolrQueryBuilder<BioentitiesCollectionProxy>()
                        .addQueryFieldByTerm(BIOENTITY_IDENTIFIER, geneId)
                        .addQueryFieldByTerm(PROPERTY_NAME, "symbol")
                        .setFieldList(PROPERTY_VALUE)
                        .setFieldList(SPECIES)
                        .setRows(1);

        var docList = bioentitiesCollectionProxy.query(solrQueryBuilder).getResults();
        var symbol = docList.get(0).getFieldValue(PROPERTY_VALUE.name()).toString();
        var species = docList.get(0).getFieldValue(SPECIES.name()).toString();

        this.mockMvc.perform(get("/json/search").param("symbol", symbol).param("species", species))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].element.experimentAccession", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].facets[0].group", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets[0].value", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets[0].label", isA(String.class)))
                .andExpect(jsonPath("$.checkboxFacetGroups", contains("Marker genes", "Species")))
                .andExpect(jsonPath("$.matchingGeneId", equalTo("(" + geneId + ")")));
    }

    @Test
    void jsonPayloadContainsFacetDescription() throws Exception {
        var geneId = jdbcTestUtils.fetchRandomGene();

        this.mockMvc.perform(get("/json/search").param("ensgene", geneId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].element.experimentAccession", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].facets[0].group", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets[0].value", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets[0].label", isA(String.class)))
                .andExpect(jsonPath("$.results[0].facets[0].description", isA(String.class)))
                .andExpect(jsonPath("$.checkboxFacetGroups", contains("Marker genes", "Species")));
    }
      
    @Test
    void speciesParamCanAppearBeforeGeneQuery() throws Exception {
        this.mockMvc.perform(get("/json/search").param("species", "homo sapiens").param("symbol", "aspm"))
                .andExpect(status().isOk());
    }
}
