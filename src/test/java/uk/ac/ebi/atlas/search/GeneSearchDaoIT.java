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
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(Lifecycle.PER_CLASS)
class GeneSearchDaoIT {
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
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-fixture.sql"),
                new ClassPathResource("fixtures/scxa_analytics-fixture.sql"),
                new ClassPathResource("fixtures/scxa_marker_genes-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-delete.sql"),
                new ClassPathResource("fixtures/scxa_analytics-delete.sql"),
                new ClassPathResource("fixtures/scxa_marker_genes-delete.sql"));
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

    @ParameterizedTest
    @ValueSource(strings = {"AT1G02640"})
    void validGeneIdReturnsExperimentAccessions(String geneId) {
        List<String> result = subject.fetchExperimentAccessionsWhereGeneIsMarker(geneId);

        assertThat(result)
                .containsOnly("E-CURD-4");
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOO"})
    void invalidGeneIdReturnsEmpty(String geneId) {
        assertThat(subject.fetchExperimentAccessionsWhereGeneIsMarker(geneId))
                .isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"'ENSMUSG00000005360', 'E-GEOD-99058', 6"})
    void validExperimentAccessionReturnsClusterIDsWithPreferredKAndMinP(String geneId,
                                                                        String experimentAccession,
                                                                        Integer preferredK){
        Map<Integer, List<Integer>> result =
                subject.fetchClusterIdsWithPreferredKAndMinPForExperimentAccession(
                        geneId, experimentAccession, preferredK, 0);

        assertThat(result)
                .isNotEmpty()
                .containsAllEntriesOf(
                        ImmutableMap.of(
                                6, ImmutableList.of(3),
                                4, ImmutableList.of(2)));
    }

    @ParameterizedTest
    @CsvSource({"'ENSMUSG00000005360', 'E-GEOD-99058', 6"})
    void validExperimentAccessionReturnsOnlyOneClusterIDWithBothPreferredKAndMinP(String geneId,
                                                                                  String experimentAccession,
                                                                                  Integer preferredK){
        Map<Integer, List<Integer>> result =
                subject.fetchClusterIdsWithPreferredKAndMinPForExperimentAccession(
                        geneId, experimentAccession, preferredK, 0);

        assertThat(result)
                .isNotEmpty()
                .containsAllEntriesOf(
                        ImmutableMap.of(
                                6, ImmutableList.of(3)));
    }

    @ParameterizedTest
    @MethodSource("randomCellIdsProvider")
    void getFacetsForValidCellIds(List<String> cellIds) {
        Map<String, Map<String, List<String>>> result =
                subject.getFacets(
                        cellIds,
                        "inferred_cell_type", "organism_part", "organism");

        assertThat(result).isNotEmpty();
    }

    @Test
    void getForEmptyListOfCellIdsReturnsEmpty() {
        Map<String, Map<String, List<String>>> result =
                subject.getFacets(
                        emptyList(),
                        "inferred_cell_type", "organism_part", "organism");

        assertThat(result).isEmpty();
    }

    private Stream<String> randomGeneIdProvider() {
        return Stream.of(jdbcTestUtils.fetchRandomGene());
    }

    private Stream<List<String>> randomCellIdsProvider() {
        return Stream.of(jdbcTestUtils.fetchRandomListOfCells(ThreadLocalRandom.current().nextInt(1, 100)));
    }
}
