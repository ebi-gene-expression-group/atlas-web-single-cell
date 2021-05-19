package uk.ac.ebi.atlas.experimentpage.tsneplot;

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
public class TSnePlotJsonSerializer {
    private final ExperimentTrader experimentTrader;
    private final TSnePlotService tSnePlotService;

    public TSnePlotJsonSerializer(ExperimentTrader experimentTrader,
                                  TSnePlotService tSnePlotService) {
        this.experimentTrader = experimentTrader;
        this.tSnePlotService = tSnePlotService;
    }

    @Cacheable(cacheNames = "jsonTSnePlotWithClusters", key = "{#experimentAccession, #method, #parameter, #k}")
    public String tSnePlotWithClusters(String experimentAccession,String method, int parameter, int k, String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        return GSON.toJson(
                ImmutableMap.of(
                        "series",
                        modelForHighcharts(
                                "Cluster ",
                                tSnePlotService.fetchTSnePlotWithClusters(experiment.getAccession(), method, parameter, k))));
    }

    @Cacheable(cacheNames = "jsonTSnePlotWithMetadata", key = "{#experimentAccession, #parameter, #metadata}")
    public String tSnePlotWithMetadata(String experimentAccession, int parameter, String metadata, String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        return GSON.toJson(
                ImmutableMap.of(
                        "series",
                        modelForHighcharts(
                                "",
                                new TreeMap<>(
                                        tSnePlotService.fetchTSnePlotWithMetadata(
                                                experiment.getAccession(), parameter, metadata)))));
    }

    public String tSnePlotWithExpression(String experimentAccession, String method, int parameter, String accessKey) {
        return tSnePlotWithExpression(experimentAccession, method, parameter, "", accessKey);
    }

    public String tSnePlotWithExpression(String experimentAccession, String method, int parameter, String geneId, String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        var pointsWithExpression =
                tSnePlotService.fetchTSnePlotWithExpression(experiment.getAccession(), method, parameter, geneId);

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
}
