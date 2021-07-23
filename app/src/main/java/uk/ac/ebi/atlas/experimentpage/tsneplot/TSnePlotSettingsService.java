package uk.ac.ebi.atlas.experimentpage.tsneplot;

import com.google.gson.JsonObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.commons.readers.TsvStreamer;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParserOutput;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGenesDao;
import uk.ac.ebi.atlas.resource.DataFileHub;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TSnePlotSettingsService {
    private final DataFileHub dataFileHub;
    private final IdfParser idfParser;
    private final TSnePlotDao tSnePlotDao;
    private final MarkerGenesDao markerGenesDao;

    public TSnePlotSettingsService(DataFileHub dataFileHub,
                                   IdfParser idfParser,
                                   TSnePlotDao tSnePlotDao,
                                   MarkerGenesDao markerGenesDao) {
        this.dataFileHub = dataFileHub;
        this.idfParser = idfParser;
        this.tSnePlotDao = tSnePlotDao;
        this.markerGenesDao = markerGenesDao;
    }

    public List<Integer> getAvailableKs(String experimentAccession) {
        try (TsvStreamer clustersTsvStreamer =
                     dataFileHub.getSingleCellExperimentFiles(experimentAccession).clustersTsv.get()) {
            return clustersTsvStreamer.get()
                    .skip(1)
                    .map(line -> Integer.parseInt(line[1]))
                    .collect(Collectors.toList());
        }
    }

    public List<String> getKsWithMarkerGenes(String experimentAccession) {
        return markerGenesDao.getKsWithMarkerGenes(experimentAccession);
    }

    public List<Integer> getAvailablePerplexities(String experimentAccession) {
        return tSnePlotDao.fetchPerplexities(experimentAccession);
    }

    @Cacheable("expectedClusters")
    public Optional<Integer> getExpectedClusters(String experimentAccession) {
        IdfParserOutput idfParserOutput = idfParser.parse(experimentAccession);

        // Only add preferred cluster property if it exists in the idf file and it is one of the available k values
        if (idfParserOutput.getExpectedClusters() != 0 &&
                getAvailableKs(experimentAccession).contains(idfParserOutput.getExpectedClusters())) {
            return Optional.of(idfParserOutput.getExpectedClusters());
        } else {
            try (TsvStreamer clustersTsvStreamer =
                         dataFileHub.getSingleCellExperimentFiles(experimentAccession).clustersTsv.get()) {
                return clustersTsvStreamer.get()
                        .skip(1)
                        .filter(line -> line[0].equalsIgnoreCase("true"))
                        .map(line -> Integer.parseInt(line[1]))
                        .findFirst();
            }
        }
    }

    public Map<String, List<JsonObject>> getAvailablePlotTypesAndPlotOptions(String experimentAccession){
        return tSnePlotDao.fetchTSnePlotTypesAndOptions(experimentAccession);
    }
}
