package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(Lifecycle.PER_CLASS)
class GeneSearchDaoIT {
    private final static Random RNG = ThreadLocalRandom.current();

    @Inject
    private DataSource dataSource;

    @Inject
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Inject
    private SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory;

    @Inject
    private JdbcUtils jdbcTestUtils;

    private GeneSearchDao subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/202203/experiment.sql"),
                new ClassPathResource("fixtures/202203/scxa_analytics.sql"),
                new ClassPathResource("fixtures/202203/scxa_cell_group.sql"),
                new ClassPathResource("fixtures/202203/scxa_cell_group_membership.sql"),
                new ClassPathResource("fixtures/202203/scxa_cell_group_marker_genes.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/202203/scxa_cell_group_marker_genes-delete.sql"),
                new ClassPathResource("fixtures/202203/scxa_cell_group_membership-delete.sql"),
                new ClassPathResource("fixtures/202203/scxa_cell_group-delete.sql"),
                new ClassPathResource("fixtures/202203/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/202203/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        subject = new GeneSearchDao(namedParameterJdbcTemplate, solrCloudCollectionProxyFactory);
    }

    @ParameterizedTest
    @MethodSource("randomGeneIdProvider")
    void validGeneIdReturnsAtLeastOneCellId(String geneId) {
        assertThat(subject.fetchCellIds(geneId))
                .isNotEmpty();
    }

    // Any gene with marker_probability < 0.05
    @ParameterizedTest
    @ValueSource(strings = {"AT1G11740"})
    void validGeneIdReturnsExperimentAccessions(String geneId) {
        var result = subject.fetchExperimentAccessionsWhereGeneIsMarker(geneId);

        assertThat(result)
                .contains("E-CURD-4");
        //previous assertion is .containsOnly("E-CURD-4"), removed Only from above to fix test case
        // Alfonso needs to review this assertion, because above method returns
        // LIST of experiment accessions, not sure why we are using .containsOnly("E-CURD-4")
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOO"})
    void invalidGeneIdReturnsEmpty(String geneId) {
        assertThat(subject.fetchExperimentAccessionsWhereGeneIsMarker(geneId))
                .isEmpty();
    }

    // Look for the cell group IDs that match an experiment and its preferred K variable, then find a gene in that cell
    // group that has marker_probability < 0.05
    @ParameterizedTest
    @CsvSource({"'ENSG00000001626', 'E-GEOD-81547', 25"})
    void validExperimentAccessionReturnsClusterIDsWithPreferredKAndMinP(String geneId,
                                                                        String experimentAccession,
                                                                        Integer preferredK) {
        var minimumMarkerProbability = subject.fetchMinimumMarkerProbability(experimentAccession).get(geneId);
        var result =
                subject.fetchClusterIdsWithPreferredKAndMinPForExperimentAccession(
                        geneId, experimentAccession, preferredK, minimumMarkerProbability);

        assertThat(result)
                .isNotEmpty()
                .containsAllEntriesOf(
                        ImmutableMap.of(
                                // Hard-coded values depending on the gene,experiment we use for this test
                                25, ImmutableList.of(12, 14, 22, 8)));
    }

    @ParameterizedTest
    @CsvSource({"'ENSMUSG00000028565', 'E-EHCA-2', 24"})
    void validExperimentAccessionReturnsOnlyOneClusterIDWithBothPreferredKAndMinP(String geneId,
                                                                                  String experimentAccession,
                                                                                  Integer preferredK) {
        var result =
                subject.fetchClusterIdsWithPreferredKAndMinPForExperimentAccession(
                        geneId, experimentAccession, preferredK, 0);

        assertThat(result)
                .isNotEmpty()
                .containsAllEntriesOf(
                        ImmutableMap.of(
                                24, ImmutableList.of(11)));
    }

    @ParameterizedTest
    @MethodSource("randomCellIdsProvider")
    void getFacetsForValidCellIds(List<String> cellIds) {
        var result = subject.getFacets(cellIds, "inferred_cell_type_-_ontology_labels", "organism_part", "organism");
        assertThat(result).isNotEmpty();
    }

    @Test
    void getForEmptyListOfCellIdsReturnsEmpty() {
        var result = subject.getFacets(emptyList(), "inferred_cell_type_-_ontology_labels", "organism_part", "organism");
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("randomExperimentAccessionProvider")
    void fetchMinimumMarkerProbabilityReturnsTheMinimum(String experimentAccession) {
        var minimumMarkerProbabilities = subject.fetchMinimumMarkerProbability(experimentAccession);
        var randomGeneId =
                minimumMarkerProbabilities.keySet().asList().get(RNG.nextInt(minimumMarkerProbabilities.size()));

        var markerProbabilities = namedParameterJdbcTemplate.queryForList(
                "SELECT mg.marker_probability " +
                    "FROM scxa_cell_group_marker_genes mg " +
                        "JOIN scxa_cell_group cg " +
                        "ON mg.cell_group_id=cg.id " +
                "WHERE mg.gene_id=:gene_id AND cg.experiment_accession=:experiment_accession",
                ImmutableMap.of(
                        "gene_id", randomGeneId,
                        "experiment_accession", experimentAccession),
                Double.class);

        assertThat(markerProbabilities.stream().mapToDouble(Double::valueOf).min())
                .hasValue(minimumMarkerProbabilities.get(randomGeneId));
    }

    private Stream<String> randomGeneIdProvider() {
        return Stream.of(jdbcTestUtils.fetchRandomGene());
    }

    private Stream<String> randomExperimentAccessionProvider() {
        return Stream.of(jdbcTestUtils.fetchRandomExperimentAccession());
    }

    private Stream<List<String>> randomCellIdsProvider() {
        return Stream.of(jdbcTestUtils.fetchRandomListOfCells(ThreadLocalRandom.current().nextInt(1, 100)));
    }
}
