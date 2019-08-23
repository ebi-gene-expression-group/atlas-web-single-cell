package uk.ac.ebi.atlas.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private SingleCellAnalyticsCollectionProxy singleCellAnalyticsCollectionProxy;

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

    private static final String SELECT_EXPERIMENT_ACCESSION_FOR_MARKER_GENE_ID =
            "SELECT experiment_accession FROM scxa_marker_genes AS markers " +
            "JOIN scxa_experiment AS experiments ON markers.experiment_accession = experiments.accession " +
            "WHERE private=FALSE AND gene_id=:gene_id " +
            "GROUP BY experiment_accession";
    @Transactional(readOnly = true)
    public List<String> fetchExperimentAccessionsWhereGeneIsMarker(String geneId) {
        Map<String, Object> namedParameters =
                ImmutableMap.of(
                        "gene_id", geneId);

        return namedParameterJdbcTemplate.query(
                SELECT_EXPERIMENT_ACCESSION_FOR_MARKER_GENE_ID,
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

    // Retrieves cluster IDs the preferred K value (if present), as well as for the minimum p-value. If the minimum
    // p-value is equal for multiple Ks (and a preferred K is not passed in), all K values will be returned.
    private static final String SELECT_PREFERREDK_AND_MINP_CLUSTER_ID_FOR_GENE_STATEMENT =
    "SELECT k, cluster_id FROM scxa_marker_genes AS markers " +
            "WHERE experiment_accession=:experiment_accession " +
                "AND gene_id=:gene_id " +
                "AND marker_probability<:threshold " +
                "AND (k=:preferred_K " +
                    "OR marker_probability IN ( " +
                        "SELECT MIN(marker_probability) " +
                            "FROM scxa_marker_genes " +
                            "WHERE markers.experiment_accession = :experiment_accession " +
                            "AND gene_id=:gene_id))";
    @Transactional(readOnly = true)
    public Map<Integer, List<Integer>> fetchClusterIdsWithPreferredKAndMinPForExperimentAccession(
            String geneId, String experimentAccession, int preferredK) {

        var namedParameters =
                ImmutableMap.of(
                        "gene_id", geneId,
                        "threshold", MARKER_GENE_P_VALUE_THRESHOLD,
                        "preferred_K", preferredK,
                        "experiment_accession", experimentAccession);

        return namedParameterJdbcTemplate.query(
                SELECT_PREFERREDK_AND_MINP_CLUSTER_ID_FOR_GENE_STATEMENT,
                namedParameters,
                (ResultSet resultSet) -> {
                    Map<Integer, List<Integer>> result = new HashMap<>();

                    while (resultSet.next()) {
                        Integer k = resultSet.getInt("k");
                        Integer clusterId = resultSet.getInt("cluster_id");
                        List<Integer> clusterIds = result.getOrDefault(k, new ArrayList<>());
                        clusterIds.add(clusterId);
                        result.put(k, clusterIds);
                    }

                    return result;
                }
        );
    }

    // Returns all the metadata values for each experiment accession, given a subset of metadata types
    public ImmutableMap<String, Map<String, List<String>>> getFacets(List<String> cellIds, String... metadataTypes) {
        var characteristicValueFacet =
                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                        .setFacetField(CHARACTERISTIC_VALUE)
                        .setNestedFacet(true);

        var characteristicNameFacet =
                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                        .setFacetField(CHARACTERISTIC_NAME)
                        .addSubFacets(ImmutableList.of(characteristicValueFacet))
                        .setNestedFacet(true);

        var facets =
                new SolrJsonFacetBuilder<SingleCellAnalyticsCollectionProxy>()
                        .setFacetField(EXPERIMENT_ACCESSION)
                        .addSubFacets(ImmutableList.of(characteristicNameFacet));

        var queryBuilder =
                new SolrQueryBuilder<SingleCellAnalyticsCollectionProxy>()
                        .addQueryFieldByTerm(CHARACTERISTIC_NAME, Arrays.asList(metadataTypes))
                        .addQueryFieldByTerm(CELL_ID, cellIds)
                        .setFacets(facets)
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
