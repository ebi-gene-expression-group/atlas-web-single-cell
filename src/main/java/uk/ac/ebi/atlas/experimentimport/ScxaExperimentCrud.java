package uk.ac.ebi.atlas.experimentimport;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParser;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParserOutput;
import uk.ac.ebi.atlas.experimentimport.experimentdesign.ExperimentDesignFileWriterService;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class ScxaExperimentCrud extends ExperimentCrud {
    public ScxaExperimentCrud(ScxaExperimentDao scxaExperimentDao,
                              CondensedSdrfParser condensedSdrfParser,
                              IdfParser idfParser,
                              ExperimentDesignFileWriterService experimentDesignFileWriterService) {
        super(scxaExperimentDao, null, condensedSdrfParser, idfParser, experimentDesignFileWriterService);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public UUID importExperiment(String experimentAccession, boolean isPrivate) {
        checkNotNull(experimentAccession);

        var accessKey = fetchExperimentAccessKey(experimentAccession);
        var condensedSdrfParserOutput =
                condensedSdrfParser.parse(experimentAccession, ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE);
        var idfParserOutput = idfParser.parse(experimentAccession);

        var experimentDTO =
                ExperimentDto.create(
                        condensedSdrfParserOutput,
                        idfParserOutput,
                        condensedSdrfParserOutput.getSpecies(),
                        isPrivate);

        if (accessKey.isPresent()) {
            deleteExperiment(experimentAccession);
        }

        var accessKeyUuid = accessKey.map(UUID::fromString).orElseGet(UUID::randomUUID);
        experimentDao.addExperiment(experimentDTO, accessKeyUuid);

        updateWithNewExperimentDesign(condensedSdrfParserOutput.getExperimentDesign(), experimentDTO);

        return accessKeyUuid;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public void updateExperimentDesign(String experimentAccession) {
        CondensedSdrfParserOutput condensedSdrfParserOutput =
                condensedSdrfParser.parse(experimentAccession, ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE);

        updateWithNewExperimentDesign(
                condensedSdrfParserOutput.getExperimentDesign(),
                experimentDao.getExperimentAsAdmin(experimentAccession));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public void deleteExperiment(String experimentAccession) {
        super.deleteExperiment(experimentAccession);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public void makeExperimentPrivate(String experimentAccession) {
        super.makeExperimentPrivate(experimentAccession);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public void makeExperimentPublic(String experimentAccession) {
        super.makeExperimentPublic(experimentAccession);
    }
}
