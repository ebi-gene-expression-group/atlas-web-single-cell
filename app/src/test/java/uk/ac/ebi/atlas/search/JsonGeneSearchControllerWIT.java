package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.core.IsNull;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityPropertyDao;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.testutils.RandomDataTestUtils;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
                new ClassPathResource("fixtures/scxa_analytics.sql"),
                new ClassPathResource("fixtures/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats.sql")
        );

        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/scxa_cell_group_marker_gene_stats-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_marker_genes-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group_membership-delete.sql"),
                new ClassPathResource("fixtures/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/experiment-delete.sql")
        );
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
                .alwaysDo(MockMvcResultHandlers.print())
                .build();
        bioentitiesCollectionProxy = collectionProxyFactory.create(BioentitiesCollectionProxy.class);
    }

    @Test
    void unknownGene() throws Exception {
        this.mockMvc.perform(get("/json/gene-search").param("q", "FOO"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results").isEmpty())
                .andExpect(jsonPath("$.reason").value(startsWith("Gene unknown")));
    }

    @Test
    void unexpressedGene() throws Exception {
        this.mockMvc.perform(get("/json/gene-search").param("symbol", "FOX2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results").isEmpty())
                .andExpect(jsonPath("$.reason").value(startsWith("No expression found")));
    }

    @Test
    void validJsonForValidGeneId() throws Exception {
        var geneId = jdbcTestUtils.fetchRandomGene();

        this.mockMvc.perform(get("/json/gene-search").param("ensgene", geneId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].experimentAccession", isA(String.class)))
                .andExpect(jsonPath("$.results[0].markerGenes", hasSize(greaterThanOrEqualTo(0))))
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

        this.mockMvc.perform(get("/json/gene-search").param("symbol", symbol).param("species", species))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].experimentAccession", isA(String.class)))
                .andExpect(jsonPath("$.checkboxFacetGroups", contains("Marker genes", "Species")))
                .andExpect(jsonPath("$.matchingGeneId", equalTo("(" + geneId + ")")));
    }

    @Test
    void jsonPayloadContainsFacetDescription() throws Exception {
        var geneId = jdbcTestUtils.fetchRandomGene();

        this.mockMvc.perform(get("/json/gene-search").param("ensgene", geneId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.results", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.results[0].experimentAccession", isA(String.class)))
                .andExpect(jsonPath("$.checkboxFacetGroups", contains("Marker genes", "Species")));
    }

    @Test
    void speciesParamCanAppearBeforeGeneQuery() throws Exception {
        this.mockMvc.perform(get("/json/gene-search").param("species", "homo sapiens").param("symbol", "aspm"))
                .andExpect(status().isOk());
    }

    @Test
    void whenSearchForAMarkerGeneWithEmptyValueReturnsError() throws Exception {
        final String emptyGeneSearchParams = "";
        final String expectedMessage = "{\"error\":\"Error parsing query\"}\n";
        this.mockMvc.perform(get("/json/gene-search/marker-genes").param("q", emptyGeneSearchParams))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string(expectedMessage));
    }

    @Test
    void whenGeneIsAMarkerGeneSearchForItReturnsTrue() throws Exception {
        var shouldBeMarkerGene =
                jdbcTestUtils.fetchRandomMarkerGeneFromSingleCellExperiment("E-CURD-4");

        this.mockMvc.perform(get("/json/gene-search/marker-genes").param("ensgene", shouldBeMarkerGene))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string("true"));
    }

    @Test
    void whenGeneIsNotAMarkerGeneSearchForItReturnsFalse() throws Exception {
        var notAMarkerGene = RandomDataTestUtils.generateRandomEnsemblGeneId();

        this.mockMvc.perform(get("/json/gene-search/marker-genes").param("ensgene", notAMarkerGene))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string("false"));
    }

    @Test
    void whenSearchForSpeciesWithEmptyValueReturnsError() throws Exception {
        final String emptySpeciesSearchParams = "";
        final String expectedMessage = "{\"error\":\"Error parsing query\"}\n";
        this.mockMvc.perform(get("/json/gene-search/species").param("q", emptySpeciesSearchParams))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string(expectedMessage));
    }

    @Test
    void whenGeneIdIsPartOfSomeExperimentsThenReturnSetOfSpecies() throws Exception {
        var shouldBeGeneThatPartOfExperiments =
                jdbcTestUtils.fetchRandomGeneFromSingleCellExperiment("E-CURD-4");

        var expectedSpecies = "Arabidopsis_thaliana";

        this.mockMvc.perform(get("/json/gene-search/species").param("ensgene", shouldBeGeneThatPartOfExperiments))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$", containsInAnyOrder(expectedSpecies)));
    }

    @Test
    void whenGeneIdIsNotPartOfAnyExperimentsThenReturnEmptySetOfSpecies() throws Exception {
//        This is the Solr streaming expression query that is getting the list of bioentity_identifiers
//        that is not part of any experiments based on a given species (as a query parameter in the bioentities query)
//        I just selected the 1st ID and used that in my test
//        If we are going to use this query more than once, we might have to implement this in a utility method
//        Currently the `complement` streaming expression is not implemented in our code, yet, so it is a bigger effort to do it
//        complement(
//            search(bioentities-v1, q=species:solanum_lycopersicum, fl="bioentity_identifier_dv",
//                  sort="bioentity_identifier_dv asc", qt="/export"),
//            select(
//                  search(scxa-gene2experiment-v1, q=experiment_accession:E-ENAD-53, fl="bioentity_identifier",
//                          sort="bioentity_identifier asc", qt="/export"),
//                  bioentity_identifier as bioentity_identifier_dv),
//            on="bioentity_identifier_dv"
//        )

        var geneNotPartOfAnyExperiments = "ENSRNA049444660";

        this.mockMvc.perform(get("/json/gene-search/species").param("ensgene", geneNotPartOfAnyExperiments))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(equalTo(0))));
    }

    @Test
    void whenSearchForOrganismPartWithEmptyValueReturnsError() throws Exception {
        final String emptyOrganismPartSearchTerm = "";
        final String expectedMessage = "{\"error\":\"Error parsing query\"}\n";
        this.mockMvc.perform(get("/json/gene-search/organism-parts").param("q", emptyOrganismPartSearchTerm))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string(expectedMessage));
    }

    @Test
    void whenSearchTermNotExistsInDBThenOrganismPartSearchReturnsEmptySet() throws Exception {
        var geneNotPartOfAnyExperiments = "ENSRNA049444660";

        this.mockMvc.perform(get("/json/gene-search/organism-parts").param("ensgene", geneNotPartOfAnyExperiments))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(equalTo(0))));
    }

    @Test
    void whenSearchTermExistsInDBThenReturnsSetOfOrganismParts() throws Exception {
        var shouldBeGeneThatPartOfExperiments =
                jdbcTestUtils.fetchRandomGeneFromSingleCellExperiment("E-CURD-4");

        var expectedOrganismParts = "root";

        this.mockMvc.perform(get("/json/gene-search/organism-parts").param("ensgene", shouldBeGeneThatPartOfExperiments))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(equalTo(1))))
                .andExpect(jsonPath("$", containsInAnyOrder(expectedOrganismParts)));
    }

    @Test
    void whenSearchForCellTypesWithEmptyValueReturnsError() throws Exception {
        final String emptyCellTypeSearchTerm = "";
        final String expectedMessage = "{\"error\":\"Error parsing query\"}\n";
        this.mockMvc.perform(get("/json/gene-search/cell-types").param("q", emptyCellTypeSearchTerm))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string(expectedMessage));
    }

    @Test
    void whenSearchTermNotExistsInDBThenCellTypeSearchReturnsEmptySet() throws Exception {
        var geneNotPartOfAnyExperiments = "ENSRNA049444660";

        this.mockMvc.perform(get("/json/gene-search/cell-types").param("ensgene", geneNotPartOfAnyExperiments))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(equalTo(0))));
    }

    @Test
    void whenSearchTermExistsInDBThenReturnsSetOfCellType() throws Exception {
        var shouldBeGeneThatPartOfExperiments = "AT4G01480";

        // Find out which cells the gene ID is expressed in the scxa_analytics.sql fixture and then get the cell types
        // in Solr
        var expectedCellTypes =
                ImmutableSet.of(
                        "non-hair root epidermal cell 4", "protoplast", "root cortex; trichoblast 10",
                        "root cortex; trichoblast 9", "root endodermis 11", "root endodermis 12", "stele 14",
                        "trichoblast 17");

        this.mockMvc.perform(get("/json/gene-search/cell-types").param("ensgene", shouldBeGeneThatPartOfExperiments))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(equalTo(expectedCellTypes.size()))))
                .andExpect(jsonPath("$", containsInAnyOrder(expectedCellTypes.toArray())));
    }

    @Test
    void whenGivenExistingSymbolReturnedCellTypesNotContainsNullValue() throws Exception {
        var symbolValue = "CFTR";

        this.mockMvc.perform(get("/json/gene-search/cell-types").param("symbol", symbolValue))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", not(contains(IsNull.nullValue()))));
    }
}
