package uk.ac.ebi.atlas.experimentpage.tsneplot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGenesDao;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toMap;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class TSnePlotJsonSerializer {
    private final ExperimentTrader experimentTrader;
    private final TSnePlotService tSnePlotService;
    private MarkerGenesDao markerGenesDao;
    private final TSnePlotSettingsService tsnePlotSettingsService;

    public TSnePlotJsonSerializer(ExperimentTrader experimentTrader,
                                  TSnePlotService tSnePlotService,
                                  MarkerGenesDao markerGenesDao,
                                  TSnePlotSettingsService tsnePlotSettingsService) {
        this.experimentTrader = experimentTrader;
        this.tSnePlotService = tSnePlotService;
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

            //marker genes ids
            var markerGenesByExperimentAccession = cellIdsByExperimentAccession.keySet()
                    .stream()
                    .collect(toMap(
                            experimentAccession -> experimentAccession,
                            experimentAccession ->  markerGenesDao.getMarkerGenesWithAveragesPerCluster(experimentAccession,
                                    tsnePlotSettingsService.getExpectedClusters(experimentAccession).orElse(10))
                                    .stream()
                                    .map(makergene -> makergene.geneId())
                                    .distinct()
                                    .collect(Collectors.toList()))

                    );

            var allMarkerGenes = markerGenesByExperimentAccession.values()
                    .stream().flatMap(x -> x.stream())
                    .distinct()
                    .collect(Collectors.toList());

            var expressionByExpressionWithCellType = new HashMap<>();

            for(var experimentAccession : cellIdsByExperimentAccession.keySet()) {
                var cellIdsFromCellTypeQuery = cellIdsByExperimentAccession.get(experimentAccession).keySet();
                var perpelexity = tsnePlotSettingsService.getAvailablePerplexities(experimentAccession).get(0);

                var expressionByMarkerGene = new HashMap<>();

                for (var markerGene : allMarkerGenes){
                    var expressionByCellType = new HashMap<String, ArrayList<Double>>();
                    var pointsWithExpression =
                            tSnePlotService.fetchTSnePlotWithExpression(experimentAccession, perpelexity, markerGene);

                    for(var cellExpression : pointsWithExpression) {
                        var cellId = cellExpression.name();
                        if(cellIdsFromCellTypeQuery.contains(cellId)) {
                            var cellType = cellIdsByExperimentAccession.get(experimentAccession).get(cellId);
                            var expressionAddon = cellExpression.expressionLevel();
                            var expressionList = expressionByCellType.get(cellType);
                            if (expressionList == null) {
                                var listOfExpressions = new ArrayList<Double>();
                                listOfExpressions.add(expressionAddon.get());
                                expressionByCellType.put(cellType, listOfExpressions);
                            }
                            else if (expressionAddon.get() > 0.0) {
                                expressionList.add(expressionAddon.get());
                            }
                        }
                    }
                    expressionByMarkerGene.put(markerGene,
                            expressionByCellType
                                    .entrySet()
                                    .stream()
                                    .collect(toMap(Map.Entry::getKey,
                                            entry -> entry.getValue().stream()
                                                    .mapToDouble(expression -> expression)
                                                    .average()
                                                    .orElse(0.0)
                                                ))
                                    );
                }
                expressionByExpressionWithCellType.put(experimentAccession, expressionByMarkerGene);

            }

            return GSON.toJson(
                    ImmutableMap.of(
                            "markerGeneExpressionByCellType", expressionByExpressionWithCellType));
        }

    }
}
