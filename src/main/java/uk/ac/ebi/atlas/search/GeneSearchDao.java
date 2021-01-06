package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.SolrJsonFacetBuilder;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_NAME;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CHARACTERISTIC_VALUE;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.search.SolrQueryResponseUtils.extractSimpleOrderedMaps;
import static uk.ac.ebi.atlas.solr.cloud.search.SolrQueryResponseUtils.getValuesForFacetField;

@Component
public class GeneSearchDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneSearchDao.class);
    private static final double MARKER_GENE_P_VALUE_THRESHOLD = 0.05;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

    public GeneSearchDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                         SolrCloudCollectionProxyFactory solrCloudCollectionProxyFactory) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.singleCellAnalyticsCollectionProxy =
                solrCloudCollectionProxyFactory.create(SingleCellAnalyticsCollectionProxy.class);
    }

    private static final String SELECT_CELL_IDS_FOR_GENE_STATEMENT =
            "SELECT experiment_accession, cell_id FROM scxa_analytics AS analytics " +
                    "JOIN experiment AS experiments ON analytics.experiment_accession = experiments.accession " +
                    "WHERE gene_id=:gene_id AND private=FALSE";
    @Transactional(transactionManager = "txManager", readOnly = true)
    public Map<String, List<String>> fetchCellIds(String geneId) {
        LOGGER.debug("Fetching cell IDs for {}", geneId);

        Map<String, Object> namedParameters =
                ImmutableMap.of(
                        "gene_id", geneId);

        return namedParameterJdbcTemplate.query(
                SELECT_CELL_IDS_FOR_GENE_STATEMENT,
                namedParameters,
                (ResultSet resultSet) -> {
                    Map<String, List<String>> result = new HashMap<>();
                    while (resultSet.next()) {
                        String experimentAccession = resultSet.getString("experiment_accession");
                        String cellId = resultSet.getString("cell_id");

                        List<String> cellIds = result.getOrDefault(experimentAccession, new ArrayList<>());
                        cellIds.add(cellId);
                        result.put(experimentAccession, cellIds);
                    }
                    return result;
                }
        );
    }

    private static final String SELECT_EXPERIMENT_ACCESSIONS_FOR_MARKER_GENE_ID =
			"SELECT experiment_accession FROM scxa_cell_group AS cell_group" +
					"INNER JOIN experiment AS exp ON exp.accession = cell_group.experiment_accession" +
					"INNER JOIN scxa_cell_group_marker_genes AS marker_genes ON marker_genes.gene_id = :gene_id " +
					"WHERE private = FALSE GROUP BY experiment_accession";

    @Transactional(readOnly = true)
    public List<String> fetchExperimentAccessionsWhereGeneIsMarker(String geneId) {
        Map<String, Object> namedParameters =
                ImmutableMap.of(
                        "gene_id", geneId);

        return namedParameterJdbcTemplate.query(
				SELECT_EXPERIMENT_ACCESSIONS_FOR_MARKER_GENE_ID,
                namedParameters,
                (ResultSet resultSet) -> {
                    List<String> result = new ArrayList<>();
                    while (resultSet.next()) {
                        String experimentAccession = resultSet.getString("experiment_accession");
                        result.add(experimentAccession);
                    }
                    return result;
                }
        );
    }

    // A helper method for the query below, see GeneSearchService::fetchClusterIDWithPreferredKAndMinPForGeneID
    // In terms of design it would’ve been more consistent a cached method in GeneSearchService, but because of Spring
    // limitations, caching isn’t possible between methods within a class.
    private static final String SELECT_MIN_MARKER_PROBABILITY_GENES_STATEMENT =
            "SELECT gene_id AS gene_id, MIN(marker_probability) AS min " +
					"FROM scxa_cell_group_marker_genes AS marker_genes " +
					"INNER JOIN scxa_cell_group AS cell_group " +
					"ON cell_group.id = marker_genes.cell_group_id " +
					"WHERE cell_group.experiment_accession = :experiment_accession " +
					"GROUP BY gene_id";

    @Cacheable("minimumMarkerProbability")
    @Transactional(readOnly = true)
    public ImmutableMap<String, Double> fetchMinimumMarkerProbability(String experimentAccession) {
        var namedParameters = ImmutableMap.of("experiment_accession", experimentAccession);

        return namedParameterJdbcTemplate.query(
				SELECT_MIN_MARKER_PROBABILITY_GENES_STATEMENT,
                namedParameters,
                (ResultSet resultSet) -> {
                    var resultsBuilder = ImmutableMap.<String, Double>builder();

                    while (resultSet.next()) {
                        resultsBuilder.put(resultSet.getString("gene_id"), resultSet.getDouble("min"));
                    }

                    return resultsBuilder.build();
                });
    }

    // Retrieves cluster IDs for the preferred K value (if present), as well as for the minimum p-value. If the minimum
    // p-value is equal for multiple Ks (and a preferred K is not passed in), all K values will be returned. Originally
    // the statement contained a subquery:
    // AND (k=... OR marker_probability IN (SELECT MIN(...) )
    // While more elegant in the sense that all the heavy-lifting is made at the DB in a single statement, this
    // approach became very time-consuming, each query taking 0.5-3 seconds. “Popular” genes like human CTSB which
    // appears in 40+ experiments meant that many successive requests in the order of 0.5-3 seconds each, on aggregate
    // taking about 50 seconds. See https://www.pivotaltracker.com/story/show/173033902 for a full report and detailed
    // benchmarks.
    private static final String SELECT_PREFERRED_K_AND_MIN_P_CLUSTER_ID_FOR_GENE_STATEMENT =
			"SELECT variable as k, value as cluster_id FROM scxa_cell_group AS cell_group " +
					"INNER JOIN scxa_cell_group_marker_genes AS marker_genes " +
					"ON cell_group.id = marker_genes.cell_group_id "+
					"WHERE marker_genes.marker_probability < :threshold " +
					"AND (variable = :preferred_K OR marker_probability = :min_marker_probability) " +
					"AND gene_id = :gene_id AND experiment_accession = :experiment_accession";

    @Transactional(readOnly = true)
    public Map<Integer, List<Integer>> fetchClusterIdsWithPreferredKAndMinPForExperimentAccession(
            String geneId, String experimentAccession, int preferredK, double minMarkerProbability) {

        var namedParameters =
                ImmutableMap.of(
                        "gene_id", geneId,
                        "threshold", MARKER_GENE_P_VALUE_THRESHOLD,
                        "preferred_K", String.valueOf(preferredK),
                        "experiment_accession", experimentAccession,
                        "min_marker_probability", minMarkerProbability);

        return namedParameterJdbcTemplate.query(
                SELECT_PREFERRED_K_AND_MIN_P_CLUSTER_ID_FOR_GENE_STATEMENT,
                namedParameters,
                (ResultSet resultSet) -> {
                    Map<Integer, List<Integer>> result = new HashMap<>();

                    while (resultSet.next()) {
                        var k = Integer.valueOf(resultSet.getString("k"));
                        var clusterId = Integer.valueOf(resultSet.getString("cluster_id"));
                        var clusterIds = result.getOrDefault(k, new ArrayList<>());
                        clusterIds.add(clusterId);
                        result.put(k, clusterIds);
                    }
                    return result;
                }
        );
    }

    // Returns all the metadata values for each experiment accession, given a subset of metadata types
    public ImmutableMap<String, Map<String, List<String>>> getFacets(List<String> cellIds, String... metadataTypes) {
        var facetBuilder =
                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                        .setFacetField(EXPERIMENT_ACCESSION)
                        .addNestedFacet(
                                CHARACTERISTIC_NAME.name(),
                                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                                        .setFacetField(CHARACTERISTIC_NAME)
                                        .addNestedFacet(
                                                CHARACTERISTIC_VALUE.name(),
                                                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                                                        .setFacetField(CHARACTERISTIC_VALUE)));

        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, Arrays.asList(metadataTypes))
                        .addQueryFieldByTerm(CELL_ID, cellIds)
                        .addFacet(EXPERIMENT_ACCESSION.name(), facetBuilder)
                        .setRows(0);

        var resultsByExperiment =
                extractSimpleOrderedMaps(
                        singleCellAnalyticsCollectionProxy
                                .query(queryBuilder)
                                .getResponse()
                                .findRecursive("facets", EXPERIMENT_ACCESSION.name(), "buckets"));

        return resultsByExperiment
                .stream()
                .collect(toImmutableMap(
                        x -> x.get("val").toString(), // experiment accession
                        x -> extractSimpleOrderedMaps(
                                x.findRecursive(CHARACTERISTIC_NAME.name(), "buckets"))
                                .stream()
                                .collect(Collectors.toMap(
                                        y -> y.get("val").toString(), // metadata type, i.e. organism part, species
                                        y -> getValuesForFacetField(y, CHARACTERISTIC_VALUE.name())))));
    }
}
