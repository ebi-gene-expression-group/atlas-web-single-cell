package uk.ac.ebi.atlas.experimentimport;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParser;
import uk.ac.ebi.atlas.experimentimport.experimentdesign.ExperimentDesignFileWriterService;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;

import java.util.UUID;

import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;

@Component
public class ScxaExperimentCrud extends ExperimentCrud {
    private final CondensedSdrfParser condensedSdrfParser;
    private final IdfParser idfParser;

    public ScxaExperimentCrud(ExperimentCrudDao experimentCrudDao,
                              ExperimentDesignFileWriterService experimentDesignFileWriterService,
                              CondensedSdrfParser condensedSdrfParser,
                              IdfParser idfParser) {
        super(experimentCrudDao, experimentDesignFileWriterService);
        this.condensedSdrfParser = condensedSdrfParser;
        this.idfParser = idfParser;
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
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}"),
            @CacheEvict(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "cellCounts", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonCellMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithClusters", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithMetadata", allEntries = true) })
    public UUID createExperiment(String experimentAccession, boolean isPrivate) {
        var condensedSdrfParserOutput =
                condensedSdrfParser.parse(experimentAccession, SINGLE_CELL_RNASEQ_MRNA_BASELINE);
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
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}"),
            @CacheEvict(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "cellCounts", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonCellMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithClusters", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithMetadata", allEntries = true) })
    public void updateExperimentDesign(String experimentAccession) {
        var experimentDto =
                readExperiment(experimentAccession)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Experiment " + experimentAccession + " could not be found"));

        updateExperimentDesign(
                condensedSdrfParser.parse(experimentAccession, SINGLE_CELL_RNASEQ_MRNA_BASELINE).getExperimentDesign(),
                experimentDto);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}"),
            @CacheEvict(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "cellCounts", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonCellMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithClusters", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithMetadata", allEntries = true) })
    public void deleteExperiment(String experimentAccession) {
        super.deleteExperiment(experimentAccession);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonExperimentMetadata", key = "{#experimentAccession, 'tSnePlot'}"),
            @CacheEvict(cacheNames = "jsonExperimentPageTabs", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "cellCounts", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "jsonCellMetadata", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithClusters", allEntries = true),
            @CacheEvict(cacheNames = "jsonTSnePlotWithMetadata", allEntries = true) })
    public void updateExperimentPrivate(String experimentAccession, boolean isPrivate) {
        super.updateExperimentPrivate(experimentAccession, isPrivate);
    }
}
