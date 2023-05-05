package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math.util.MathUtils;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataDao;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
public class CellPlotService {
    static final String MISSING_METADATA_VALUE_PLACEHOLDER = "not available";

    private final CellPlotDao cellPlotDao;
    private final CellMetadataDao cellMetadataDao;

    public CellPlotService(CellPlotDao cellPlotDao, CellMetadataDao cellMetadataDao) {
        this.cellPlotDao = cellPlotDao;
        this.cellMetadataDao = cellMetadataDao;
    }

    public List<String> cellPlotMethods(String experimentAccession) {
        return cellPlotDao.fetchCellPlotMethods(experimentAccession);
    }

    public List<String> cellPlotParameter(String experimentAccession, String method) {
        return cellPlotDao.fetchCellPlotParameter(experimentAccession, method);
    }

    public ImmutableMap<String, ImmutableSet<TSnePoint>> clusterPlotWithK(String experimentAccession,
                                                                          int k,
                                                                          String plotMethod,
                                                                          Map<String, Integer> plotParameters) {
        var points = cellPlotDao.fetchCellPlotWithK(experimentAccession, k, plotMethod, plotParameters);

        return points
                .stream()
                .collect(groupingBy(TSnePoint.Dto::clusterId,
                        LinkedHashMap::new,
                        toList()))
                .entrySet().stream()
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(pointDto ->
                                        TSnePoint.create(
                                                MathUtils.round(pointDto.x(), 2),
                                                MathUtils.round(pointDto.y(), 2),
                                                pointDto.name())
                                )
                                .collect(toImmutableSet())));
    }

    public ImmutableMap<String, ImmutableSet<TSnePoint>> clusterPlotWithMetadata(String experimentAccession,
                                                                                 String metadataCategory,
                                                                                 String plotMethod,
                                                                                 Map<String, Integer> plotParameters) {
        var metadataValuesForCells = cellMetadataDao.getMetadataValues(experimentAccession, metadataCategory);

        return ImmutableMap.copyOf(
                cellPlotDao.fetchCellPlot(experimentAccession, plotMethod, plotParameters)
                        .stream()
                        .map(
                                pointDto ->
                                        TSnePoint.create(
                                                MathUtils.round(pointDto.x(), 2),
                                                MathUtils.round(pointDto.y(), 2),
                                                pointDto.name(),
                                                StringUtils.capitalize(
                                                        metadataValuesForCells.getOrDefault(
                                                                pointDto.name(),
                                                                MISSING_METADATA_VALUE_PLACEHOLDER))))
                        .collect(groupingBy(TSnePoint::metadata, mapping(identity(), toImmutableSet()))));
    }

    public ImmutableSet<TSnePoint> expressionPlot(String experimentAccession,
                                                  String geneId,
                                                  String plotMethod,
                                                  Map<String, Integer> plotParameters) {
        return cellPlotDao.fetchCellPlotWithExpression(experimentAccession, geneId, plotMethod, plotParameters)
                .stream()
                .map(
                        pointDto ->
                                TSnePoint.create(
                                        MathUtils.round(pointDto.x(), 2),
                                        MathUtils.round(pointDto.y(), 2),
                                        pointDto.expressionLevel(),
                                        pointDto.name()))
                .collect(toImmutableSet());
    }

    public ImmutableMap<String, JsonObject> fetchDefaultPlotMethodWithParameterisation(String experimentAccession) {

        var defaultcellPlots = cellPlotDao.fetchDefaultPlotMethodWithParameterisation(experimentAccession);

        ImmutableMap.Builder<String, JsonObject> defaultPlotTypeAndOptions = new ImmutableMap.Builder<>();

        defaultcellPlots.forEach((method, options) -> defaultPlotTypeAndOptions.put(method, getMiddleElement(defaultcellPlots.get(method))));

        return defaultPlotTypeAndOptions.build();
    }

    private JsonObject getMiddleElement(List plotOptions) {

        if (plotOptions == null || plotOptions.isEmpty()) {
            return new JsonObject();
        } else {
            if (plotOptions.size() % 2 == 0) { // even number
                Object umapEvenItem = plotOptions.get((plotOptions.size() / 2 - 1));
                return (JsonObject) umapEvenItem;
            } else { //odd number
                Object umapOddItem = plotOptions.get((plotOptions.size() / 2));
                return (JsonObject) umapOddItem;
            }
        }

    }

}
