package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGenesDao;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotService;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.groupingBy;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class MetadataJsonSerializer {
    private final TSnePlotService tSnePlotService;
    private MarkerGenesDao markerGenesDao;
    private final TSnePlotSettingsService tsnePlotSettingsService;
    private MetadataSearchDao metadataSearchDao;
    private ExperimentTrader experimentTrader;

    public MetadataJsonSerializer(TSnePlotService tSnePlotService,
                                  MarkerGenesDao markerGenesDao,
                                  MetadataSearchDao metadataSearchDao,
                                  TSnePlotSettingsService tsnePlotSettingsService,
                                  ExperimentTrader experimentTrader) {
        this.tSnePlotService = tSnePlotService;
        this.markerGenesDao = markerGenesDao;
        this.tsnePlotSettingsService = tsnePlotSettingsService;
        this.metadataSearchDao = metadataSearchDao;
        this.experimentTrader = experimentTrader;
    }

    @Cacheable(cacheNames = "jsonCellTypeMetadata", key = "{#characteristicName, #characteristicValue}")
    public String cellTypeMetadata(String characteristicName, String characteristicValue, String accessKey) {

        //cell ids
        var cellIdsByExperimentAccession = fetchCellTypeMetadata(characteristicName, characteristicValue);

        //experiment accessions by species
        var experimentAccessionsBySpecies = cellIdsByExperimentAccession.keySet()
                .stream()
                .collect(groupingBy(experimentAccession ->
                        experimentTrader.getExperiment(experimentAccession, accessKey).getSpecies().getName()));

        //marker genes by species
        var markerGenesBySpecies = experimentAccessionsBySpecies
                .entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry ->  entry.getValue().stream()
                                .flatMap(
                                        accession -> markerGenesDao.getMarkerGenesWithAveragesPerCluster(accession,
                                                tsnePlotSettingsService.getExpectedClusters(accession).orElse(10))
                                                .stream()
                                                .map(makergene -> makergene.geneId())

                                ).collect(Collectors.toList())
                        )
                );

        var expressionByExpressionWithCellType = new HashMap<>();

        for (var species : experimentAccessionsBySpecies.keySet()) {
            var expressionBySpecies = new HashMap<>();

            for (var experimentAccession : cellIdsByExperimentAccession.keySet()) {
                var cellIdsFromCellTypeQuery = cellIdsByExperimentAccession.get(experimentAccession).keySet();
                var perpelexity = tsnePlotSettingsService.getAvailablePerplexities(experimentAccession).get(0);

                var expressionByMarkerGene = new HashMap<>();

                for (var markerGene : markerGenesBySpecies.get(species)) {
                    var expressionByCellType = new HashMap<String, ArrayList<Double>>();
                    var pointsWithExpression =
                            tSnePlotService.fetchTSnePlotWithExpression(experimentAccession, perpelexity, markerGene);

                    for (var cellExpression : pointsWithExpression) {
                        var cellId = cellExpression.name();
                        if (cellIdsFromCellTypeQuery.contains(cellId)) {
                            var cellType = cellIdsByExperimentAccession.get(experimentAccession).get(cellId);
                            var expressionAddon = cellExpression.expressionLevel();
                            var expressionList = expressionByCellType.get(cellType);
                            if (expressionList == null) {
                                var listOfExpressions = new ArrayList<Double>();
                                listOfExpressions.add(expressionAddon.get());
                                expressionByCellType.put(cellType, listOfExpressions);
                            } else if (expressionAddon.get() > 0.0) {
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
                expressionBySpecies.put(experimentAccession, expressionByMarkerGene);

            }
            expressionByExpressionWithCellType.put(species, expressionBySpecies);
        }


        return GSON.toJson(
                ImmutableMap.of(
                        "markerGeneExpressionByCellType", expressionByExpressionWithCellType));


    }

    private ImmutableMap<String, Map<String, String>>  fetchCellTypeMetadata(String characteristicName,
                                                                             String characteristicValue) {
        var metadataValuesForCells = metadataSearchDao.getCellTypeMetadata(
                characteristicName,
                characteristicValue
        );
        return ImmutableMap.copyOf(metadataValuesForCells);
    }
}
