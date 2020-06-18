package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityPropertyDao;
import uk.ac.ebi.atlas.download.FileDownloadController;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGene;
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
public class MetadataJsonService {
    private final TSnePlotService tSnePlotService;
    private MarkerGenesDao markerGenesDao;
    private final TSnePlotSettingsService tsnePlotSettingsService;
    private MetadataSearchDao metadataSearchDao;
    private ExperimentTrader experimentTrader;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadController.class);
    private final BioEntityPropertyDao bioEntityPropertyDao;

    public MetadataJsonService(TSnePlotService tSnePlotService,
                               MarkerGenesDao markerGenesDao,
                               MetadataSearchDao metadataSearchDao,
                               TSnePlotSettingsService tsnePlotSettingsService,
                               ExperimentTrader experimentTrader,
                               BioEntityPropertyDao bioEntityPropertyDao) {
        this.tSnePlotService = tSnePlotService;
        this.markerGenesDao = markerGenesDao;
        this.tsnePlotSettingsService = tsnePlotSettingsService;
        this.metadataSearchDao = metadataSearchDao;
        this.experimentTrader = experimentTrader;
        this.bioEntityPropertyDao = bioEntityPropertyDao;
    }

    public String cellTypeMetadata(String characteristicName, String characteristicValue, String accessKey) {
        //cell ids
        var cellIdsByExperimentAccession = fetchCellTypeMetadata(characteristicName, characteristicValue);

        //experiment accessions by species
        var experimentAccessionsBySpecies = cellIdsByExperimentAccession.keySet()
                .stream()
                .collect(groupingBy(experimentAccession ->
                        experimentTrader.getExperiment(experimentAccession, accessKey).getSpecies().getName()));


        var cellTypes = experimentAccessionsBySpecies.entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey, //species
                        entry -> experimentAccessionsBySpecies.get(entry.getKey())
                                .stream()
                                .collect(toMap(
                                        accession -> accession,
                                        accession -> cellIdsByExperimentAccession.get(accession).keySet()
                                                .stream()
                                                .map(cellId -> cellIdsByExperimentAccession.get(accession).get(cellId))
                                                .distinct()
                                                .collect(Collectors.toList()))
                                        )
                                )
                );

        return GSON.toJson(
                ImmutableMap.of("cellTypes", cellTypes)
        );
    }

    @Cacheable(cacheNames = "jsonCellTypeMetadata", key = "{#characteristicName, #characteristicValue}")
    public String cellTypeExpressions(String characteristicName, String characteristicValue, String accessKey) {

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
                                                .map(MarkerGene::geneId)

                                )
                                .collect(Collectors.toList())
                        )
                );

        var expressionByExpressionWithCellType = new HashMap<>();

        for (var species : experimentAccessionsBySpecies.keySet()) {
            var expressionBySpecies = new HashMap<>();

            for (var experimentAccession : experimentAccessionsBySpecies.get(species)) {
                var experimentInfo = new HashMap<>();
                var cellIdsFromCellTypeQuery = cellIdsByExperimentAccession.get(experimentAccession).keySet();
                var perpelexity = tsnePlotSettingsService.getAvailablePerplexities(experimentAccession).get(0);
                var technologyType = experimentTrader.getExperiment(experimentAccession, "").getTechnologyType();

                LOGGER.info("Cell type expression search experiment: {}", experimentAccession);

                var expressionByMarkerGene = new HashMap<>();

                var distinctMarkerGenesList = markerGenesBySpecies.get(species)
                        .stream()
                        .distinct()
                        .collect(Collectors.toList());

                for (var markerGene : distinctMarkerGenesList.subList(0, distinctMarkerGenesList.size())) {
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

                    expressionByMarkerGene.put(
                            bioEntityPropertyDao.getSymbolsForGeneIds(ImmutableSet.of(markerGene)).getOrDefault(markerGene, markerGene),
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

                experimentInfo.put("technologyType", technologyType);
                experimentInfo.put("markerGeneExpression", expressionByMarkerGene);
                expressionBySpecies.put(experimentAccession, experimentInfo);
            }
            expressionByExpressionWithCellType.put(species, expressionBySpecies);
        }


        return GSON.toJson(
                ImmutableMap.of(
                        "markerGeneExpressionByCellType", expressionByExpressionWithCellType)
                );

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
