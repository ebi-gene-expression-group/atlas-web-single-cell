package uk.ac.ebi.atlas.experimentimport;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParser;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParserOutput;
import uk.ac.ebi.atlas.experimentimport.experimentdesign.ExperimentDesignFileWriterService;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.model.experiment.ExperimentConfiguration;
import uk.ac.ebi.atlas.trader.ConfigurationTrader;

import java.util.UUID;

@Component
public class ScxaExperimentCrud extends ExperimentCrud {
    private final CondensedSdrfParser condensedSdrfParser;
    private final IdfParser idfParser;
    private final ConfigurationTrader configurationTrader;
    private final ExperimentChecker experimentChecker;

    public ScxaExperimentCrud(ExperimentCrudDao experimentCrudDao,
                              ExperimentDesignFileWriterService experimentDesignFileWriterService,
                              CondensedSdrfParser condensedSdrfParser,
                              IdfParser idfParser,
                              ConfigurationTrader configurationTrader,
                              ExperimentChecker experimentChecker) {
        super(experimentCrudDao, experimentDesignFileWriterService);
        this.condensedSdrfParser = condensedSdrfParser;
        this.idfParser = idfParser;
        this.configurationTrader = configurationTrader;
        this.experimentChecker = experimentChecker;
    }

    // Evicting all entries in jsonCellMetadata, jsonTSnePlotWithClusters and jsonTSnePlotWithMetadata is really a
    // small defeat, nothing more. We could add a private method that evicts only the appropriate entries, but soon the
    // logic is going to turn very complicated and really hard to test. Since the service in the public and fallback
    // environments is read-only, this small inconvenience will only manifest itself in the production environment,
    // where data is always changing. I think it’s a good trade-off between code simplicity and performance.
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experiment2Collections", key="#experimentAccession"),
            @CacheEvict(cacheNames = "speciesSummary", allEntries = true),
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}"),
            @CacheEvict(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "expectedClusters", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "minimumMarkerProbability", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "cellCounts", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "plotOptions", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonCellMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithClusters", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonExperimentsList", allEntries = true),
            @CacheEvict(cacheNames = "privateExperimentAccessions", allEntries = true)})
    public UUID createExperiment(String experimentAccession, boolean isPrivate) {
        var files = loadAndValidateFiles(experimentAccession);
        var condensedSdrfParserOutput = files.getRight();
        var idfParserOutput = idfParser.parse(experimentAccession);
        var accessKey = readExperiment(experimentAccession).map(ExperimentDto::getAccessKey);

        var experimentDto = new ExperimentDto(
                condensedSdrfParserOutput.getExperimentAccession(),
                condensedSdrfParserOutput.getExperimentType(),
                condensedSdrfParserOutput.getSpecies(),
                idfParserOutput.getPubmedIds(),
                idfParserOutput.getDois(),
                isPrivate,
                accessKey.orElseGet(() -> UUID.randomUUID().toString()));

        if (accessKey.isPresent()) {
            experimentCrudDao.updateExperiment(experimentDto);
        } else {
            experimentCrudDao.createExperiment(experimentDto);
            updateExperimentDesign(condensedSdrfParserOutput.getExperimentDesign(), experimentDto);
        }
        return UUID.fromString(experimentDto.getAccessKey());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experiment2Collections", key="#experimentAccession"),
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}"),
            @CacheEvict(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "expectedClusters", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "minimumMarkerProbability", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "cellCounts", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "plotOptions", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonCellMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonExperimentsList", allEntries = true),
            @CacheEvict(cacheNames = "privateExperimentAccessions", allEntries = true)})
    public void updateExperimentDesign(String experimentAccession) {
        var experimentDto =
                readExperiment(experimentAccession)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Experiment " + experimentAccession + " could not be found"));

        updateExperimentDesign(
                    condensedSdrfParser.parse(experimentAccession, experimentDto.getExperimentType())
                    .getExperimentDesign(),
                    experimentDto);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experiment2Collections", key="#experimentAccession"),
            @CacheEvict(cacheNames = "speciesSummary", allEntries = true),
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}"),
            @CacheEvict(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "expectedClusters", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "minimumMarkerProbability", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "cellCounts", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "plotOptions", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonCellMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithClusters", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithMetadata", allEntries = true),
            @CacheEvict(cacheNames = "experiment2Collections", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonExperimentsList", allEntries = true),
            @CacheEvict(cacheNames = "privateExperimentAccessions", allEntries = true)})
    public void deleteExperiment(String experimentAccession) {
        super.deleteExperiment(experimentAccession);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experiment2Collections", key="#experimentAccession"),
            @CacheEvict(cacheNames = "speciesSummary", allEntries = true),
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}"),
            @CacheEvict(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "expectedClusters", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "minimumMarkerProbability", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "cellCounts", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "plotOptions", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonCellMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithClusters", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonExperimentsList", allEntries = true),
            @CacheEvict(cacheNames = "privateExperimentAccessions", allEntries = true)})
    public void updateExperimentPrivate(String experimentAccession, boolean isPrivate) {
        super.updateExperimentPrivate(experimentAccession, isPrivate);
    }

    private Pair<ExperimentConfiguration, CondensedSdrfParserOutput> loadAndValidateFiles(String experimentAccession) {
        var experimentConfiguration = configurationTrader.getExperimentConfiguration(experimentAccession);
        experimentChecker.checkAllFiles(experimentAccession, experimentConfiguration.getExperimentType());

        var condensedSdrfParserOutput = condensedSdrfParser.parse(experimentAccession,
          experimentConfiguration.getExperimentType());

        new ExperimentFilesCrossValidator(experimentConfiguration, condensedSdrfParserOutput.getExperimentDesign()).validate();

        return Pair.of(experimentConfiguration, condensedSdrfParserOutput);
    }
}
