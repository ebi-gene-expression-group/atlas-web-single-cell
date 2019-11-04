package uk.ac.ebi.atlas.experimentpage.tsneplot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.util.MathUtils;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.tsne.TSnePoint;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataDao;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

@Component
public class TSnePlotService {
    static final String MISSING_METADATA_VALUE_PLACEHOLDER = "not available";

    private final TSnePlotDao tSnePlotDao;
    private final CellMetadataDao cellMetadataDao;

    public TSnePlotService(TSnePlotDao tSnePlotDao, CellMetadataDao cellMetadataDao) {
        this.tSnePlotDao = tSnePlotDao;
        this.cellMetadataDao = cellMetadataDao;
    }

    public ImmutableSet<TSnePoint> fetchTSnePlotWithExpression(String experimentAccession,
                                                               int perplexity,
                                                               String geneId) {
        return tSnePlotDao.fetchTSnePlotWithExpression(experimentAccession, perplexity, geneId).stream()
                .map(
                        pointDto ->
                                TSnePoint.create(
                                        MathUtils.round(pointDto.x(), 2),
                                        MathUtils.round(pointDto.y(), 2),
                                        pointDto.expressionLevel(),
                                        pointDto.name()))
                .collect(toImmutableSet());
    }

    public ImmutableMap<Integer, ImmutableSet<TSnePoint>> fetchTSnePlotWithClusters(String experimentAccession,
                                                                                    int perplexity,
                                                                                    int k) {
        var points = tSnePlotDao.fetchTSnePlotWithClusters(experimentAccession, perplexity, k);

        return points.stream()
                .collect(groupingBy(TSnePoint.Dto::clusterId))
                .entrySet().stream()
                .collect(toImmutableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(pointDto -> TSnePoint.create(pointDto.x(), pointDto.y(), pointDto.name()))
                                .collect(toImmutableSet())));
    }


    public ImmutableMap<String, ImmutableSet<TSnePoint>> fetchTSnePlotWithMetadata(String experimentAccession,
                                                                                   int perplexity,
                                                                                   String metadataCategory) {
        var pointDtos = tSnePlotDao.fetchTSnePlotForPerplexity(experimentAccession, perplexity);

        // An alternative implementation would be to get the metadata for each cell in the tSnePlotServiceDao method
        // and create TSnePoint.Dto objects with metadata values. This would require separate requests to Solr for
        // each cell ID.
        var cellIds = pointDtos
                .stream()
                .map(TSnePoint.Dto::name)
                .collect(Collectors.toList());

        var metadataValuesForCells =
                cellMetadataDao.getMetadataValueForCellIds(
                        experimentAccession,
                        metadataCategory,
                        cellIds);

        return ImmutableMap.copyOf(
                pointDtos.stream()
                        .map(
                                pointDto ->
                                        TSnePoint.create(
                                                pointDto.x(),
                                                pointDto.y(),
                                                pointDto.name(),
                                                StringUtils.capitalize(
                                                        metadataValuesForCells.getOrDefault(
                                                                pointDto.name(),
                                                                MISSING_METADATA_VALUE_PLACEHOLDER))))
                        .collect(groupingBy(TSnePoint::metadata, mapping(identity(), toImmutableSet()))));
    }
    public ImmutableMap<String, Map<String, String>>  fetchCellTypeMetadata(String characteristicName,
                                                             String characteristicValue) {
        var metadataValuesForCells = cellMetadataDao.getCellTypeMetadata(
                        characteristicName,
                        characteristicValue
                );
        return ImmutableMap.copyOf(metadataValuesForCells);
    }
}
