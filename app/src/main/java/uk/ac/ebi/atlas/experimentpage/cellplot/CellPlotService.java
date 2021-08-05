package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math.util.MathUtils;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataDao;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;

import java.util.Map;
import java.util.LinkedHashMap;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Component
public class CellPlotService {
    static final String MISSING_METADATA_VALUE_PLACEHOLDER = "not available";

    private final CellPlotDao cellPlotDao;
    private final CellMetadataDao cellMetadataDao;

    public CellPlotService(CellPlotDao cellPlotDao, CellMetadataDao cellMetadataDao) {
        this.cellPlotDao = cellPlotDao;
        this.cellMetadataDao = cellMetadataDao;
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
}
