package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class CellPlotJsonSerializer {
    private final ExperimentTrader experimentTrader;
    private final CellPlotService cellPlotService;

    public CellPlotJsonSerializer(ExperimentTrader experimentTrader,
                                  CellPlotService cellPlotService) {
        this.experimentTrader = experimentTrader;
        this.cellPlotService = cellPlotService;
    }

    @Cacheable(cacheNames = "jsonCellPlotWithK",
               key = "{#experimentAccession, #k, #plotMethod, #plotParameters}")
    public String clusterPlotWithK(String experimentAccession,
                                   int k,
                                   String plotMethod,
                                   Map<String, Integer> plotParameters,
                                   String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        return GSON.toJson(
                ImmutableMap.of(
                        "series",
                        modelForHighcharts(
                                "Cluster ",
                                cellPlotService.clusterPlotWithK(
                                        experiment.getAccession(), k, plotMethod, plotParameters))));
    }

    @Cacheable(cacheNames = "jsonCellPlotWithMetadata",
               key = "{#experimentAccession, #metadata, #plotMethod, #plotParameters}")
    public String clusterPlotWithMetadata(String experimentAccession,
                                          String metadata,
                                          String plotMethod,
                                          Map<String, Integer> plotParameters,
                                          String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        return GSON.toJson(
                ImmutableMap.of(
                        "series",
                        modelForHighcharts(
                                "",
                                new TreeMap<>(
                                        cellPlotService.clusterPlotWithMetadata(
                                                experiment.getAccession(), metadata, plotMethod, plotParameters)))));
    }

    public String expressionPlot(String experimentAccession,
                                 String geneId,
                                 String plotMethod,
                                 Map<String, Integer> plotParameters,
                                 String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        var pointsWithExpression =
                cellPlotService.expressionPlot(experiment.getAccession(), geneId, plotMethod, plotParameters);

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

    private ImmutableList<ImmutableMap<String, Object>> modelForHighcharts(String seriesName,
                                                                           Set<TSnePoint> points) {
        return ImmutableList.of(ImmutableMap.of("name", seriesName, "data", points));
    }

    public String fetchDefaultPlotMethodWithParameterisation(String experimentAccession) {
        return GSON.toJson(
                Optional.ofNullable(
                        cellPlotService.fetchDefaultPlotMethodWithParameterisation(experimentAccession)));
    }
}