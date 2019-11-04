package uk.ac.ebi.atlas.experimentpage.tsneplot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.mvel2.integration.impl.ImmutableDefaultFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGenesDao;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataDao;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toMap;
import static uk.ac.ebi.atlas.solr.cloud.collections.SingleCellAnalyticsCollectionProxy.CELL_ID;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class TSnePlotJsonSerializer {
    private final ExperimentTrader experimentTrader;
    private final TSnePlotService tSnePlotService;
    private CellMetadataDao cellMetadataDao;
    private MarkerGenesDao markerGenesDao;
    private final TSnePlotSettingsService tsnePlotSettingsService;

    public TSnePlotJsonSerializer(ExperimentTrader experimentTrader,
                                  TSnePlotService tSnePlotService,
                                  CellMetadataDao cellMetadataDao,
                                  MarkerGenesDao markerGenesDao,
                                  TSnePlotSettingsService tsnePlotSettingsService) {
        this.experimentTrader = experimentTrader;
        this.tSnePlotService = tSnePlotService;
        this.cellMetadataDao = cellMetadataDao;
        this.markerGenesDao = markerGenesDao;
        this.tsnePlotSettingsService = tsnePlotSettingsService;
    }

    @Cacheable(cacheNames = "jsonTSnePlotWithClusters", key = "{#experimentAccession, #perplexity, #k}")
    public String tSnePlotWithClusters(String experimentAccession, int perplexity, int k, String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        return GSON.toJson(
                ImmutableMap.of(
                        "series",
                        modelForHighcharts(
                                "Cluster ",
                                tSnePlotService.fetchTSnePlotWithClusters(experiment.getAccession(), perplexity, k))));
    }

    @Cacheable(cacheNames = "jsonTSnePlotWithMetadata", key = "{#experimentAccession, #perplexity, #metadata}")
    public String tSnePlotWithMetadata(String experimentAccession, int perplexity, String metadata, String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        return GSON.toJson(
                ImmutableMap.of(
                        "series",
                        modelForHighcharts(
                                "",
                                new TreeMap<>(
                                        tSnePlotService.fetchTSnePlotWithMetadata(
                                                experiment.getAccession(), perplexity, metadata)))));
    }

    public String tSnePlotWithExpression(String experimentAccession, int perplexity, String accessKey) {
        return tSnePlotWithExpression(experimentAccession, perplexity, "", accessKey);
    }

    public String tSnePlotWithExpression(String experimentAccession, int perplexity, String geneId, String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        var pointsWithExpression =
                tSnePlotService.fetchTSnePlotWithExpression(experiment.getAccession(), perplexity, geneId);

        var max = pointsWithExpression.stream()
                .map(TSnePoint::expressionLevel)
                .filter(Optional::isPresent)
                .mapToDouble(Optional::get)
                .filter(d -> d > 0)
                .max();

        var min = pointsWithExpression.stream()
                .map(TSnePoint::expressionLevel)
                .filter(Optional::isPresent)
                .mapToDouble(Optional::get)
                .filter(d -> d > 0)
                .min();

        var unit = "CPM"; // Get units from experiment, or from request parameter if more than one is available

        var model = new HashMap<>();
        model.put("series", modelForHighcharts("Gene expression", pointsWithExpression));
        model.put("unit", unit);
        max.ifPresent(n -> model.put("max", n));
        min.ifPresent(n -> model.put("min", n));

        return GSON.toJson(model);
    }

    private ImmutableList<ImmutableMap<String, Object>> modelForHighcharts(String seriesNamePrefix,
                                                                           Map<?, ? extends Set<TSnePoint>> points) {
        return points.entrySet().stream()
                .map(entry -> ImmutableMap.of(
                        "name", seriesNamePrefix + entry.getKey().toString(),
                        "data", entry.getValue()))
                .collect(toImmutableList());
    }

    private ImmutableList<ImmutableMap<String, Object>> modelForHighcharts(String seriesName, Set<TSnePoint> points) {
        return ImmutableList.of(ImmutableMap.of("name", seriesName, "data", points));
    }

    @Cacheable(cacheNames = "jsonCellTypeMetadata", key = "{#characteristicName, #characteristicValue}")
    public String cellTypeMetadata(String characteristicName, String characteristicValue, String accessKey) {
        {
            //cell ids
            var cellIdsByExperimentAccession = tSnePlotService.fetchCellTypeMetadata(characteristicName, characteristicValue);

            //experiment accessions
            var experimentAccessionList = cellIdsByExperimentAccession.entrySet()
                    .stream().map(x -> x.getKey()).collect(Collectors.toList());

           // cell types
//            var cellTypesByExperimentAccession = cellIdsByExperimentAccession
//                    .entrySet()
//                    .stream()
//                    .collect(
//                            toMap(
//                                    Map.Entry::getKey,
//                                    entry -> entry.getValue()
//                                            .stream()
//                                            .map(cellId -> cellMetadataDao.getCellTypeForCellId(entry.getKey(), cellId.toString()))
//                                            .distinct()
//                                            .flatMap(x -> ((List) x).stream())
//                                            .collect(Collectors.toList()))
//                    );

//cell id -> cell type hashmap
            var cellIdToCellType = cellIdsByExperimentAccession
                    .entrySet()
                    .stream()
                    .collect(
                            toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue()
                                            .stream()
                                            .collect(
                                                    toMap(cellId->cellId,
                                                            cellId -> cellMetadataDao.getCellTypeForCellId(
                                                                    entry.getKey(),
                                                                    cellId.toString()).get(0))
                                            )));

            //marker genes ids
            var markerGenesByExperimentAccession = experimentAccessionList
                    .stream()
                    .collect(toMap(
                            experimentAccession -> experimentAccession,
                            experimentAccession ->  markerGenesDao.getMarkerGenesWithAveragesPerCluster(experimentAccession,
                                    tsnePlotSettingsService.getExpectedClusters(experimentAccession).get())
                                    .stream()
                                    .map(makergene -> makergene.geneId())
                                    .distinct()
                                    .collect(Collectors.toList()))

                    );

            //return tSnePlotJsonSerializer.tSnePlotWithExpression(experimentAccession, perplexity, geneId, accessKey);

//            var geneExpression = cellIdsByExperimentAccession
//                    .entrySet()
//                    .stream()
//                    .collect(
//                            toMap(
//                                    Map.Entry::getKey,
//                                    entry -> entry.getValue()
//                                            .stream()
//                                            .collect(
//                                                    toMap(
//                                                          cell -> cellMetadataDao.getCellTypeForCellId(entry.getKey(), cell.toString()),
//                                                        cell-> markerGenesByExperimentAccession.get(0).stream().map(
//                                                                markerGene ->tSnePlotWithExpression(entry.getKey(),
//                                                                        tsnePlotSettingsService.getExpectedClusters(entry.getKey()).get(),
//                                                                        markerGene, "")
//                                                        )
//                                                    ))
//                    )
//                    );

//        for(var experimentAccession : cellIdsByExperimentAccession.keySet()) {
//            var expressionByCellType = new HashMap<>();
//            for (var cellId : cellIdsByExperimentAccession.get(experimentAccession)){
//                var cellType = cellMetadataDao.getCellTypeForCellId(experimentAccession, cellId.toString()).get(0);
//                for(var markerGene:markerGenesByExperimentAccession.get(0)) {
//                    var newExpression = tSnePlotWithExpression(experimentAccession,
//                            tsnePlotSettingsService.getExpectedClusters(experimentAccession).get(),
//                            markerGene, "");
//
//                    if (expressionByCellType.containsKey(cellType)) {
//                        var expression = expressionByCellType.get(cellType);
//                        expressionByCellType.put(cellType, expression+newExpression);
//                    } else {
//                        expressionByCellType.put(cellType, newExpression);
//                    }
//
//                }
//            }
//            expressionByExpressionWithCellType.put(experimentAccession, expressionByCellType);
//        }
            var expressionByExpressionWithCellType = new HashMap<>();

            for(var experimentAccession : cellIdsByExperimentAccession.keySet()) {
                var cellIdsFromCellTypeQuery = cellIdsByExperimentAccession.get(experimentAccession);
                var perpelexity = tsnePlotSettingsService.getAvailablePerplexities(experimentAccession).get(0);
                var expressionByMarkerGene = new HashMap<>();
                for (var markerGene : markerGenesByExperimentAccession.get(experimentAccession)){
                    var expressionByCellType = new HashMap<>();
                    var pointsWithExpression =
                            tSnePlotService.fetchTSnePlotWithExpression(experimentAccession, perpelexity, markerGene);
                    for(var cellExpression:pointsWithExpression) {
                        var cellId = cellExpression.name();
                        if(cellIdsFromCellTypeQuery.contains(cellId)) {
                           // var cellType = cellMetadataDao.getCellTypeForCellId(experimentAccession, cellId).get(0);
                            var cellType = ((HashMap<String, String>)cellIdToCellType.get(experimentAccession)).get(cellId);
                            var expressionAddon = cellExpression.expressionLevel();
                            if (expressionByCellType.containsKey(cellType)) {
                                var expressionBase = expressionByCellType.get(cellType);
                                expressionByCellType.put(cellType, ((double)expressionBase + expressionAddon.get())/2);
                            } else {
                                expressionByCellType.put(cellType, expressionAddon.get());
                            }
                        }
                    }
                    expressionByMarkerGene.put(markerGene, expressionByCellType);
                }
                expressionByExpressionWithCellType.put(experimentAccession, expressionByMarkerGene);

            }

            return GSON.toJson(
                    ImmutableMap.of(
                            "markerGeneExpressionByCellType", expressionByExpressionWithCellType));
        }

    }
}
