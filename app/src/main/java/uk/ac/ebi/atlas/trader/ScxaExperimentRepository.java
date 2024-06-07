package uk.ac.ebi.atlas.trader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrudDao;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentimport.sdrf.SdrfParser;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.factory.SingleCellBaselineExperimentFactory;

@Repository
public class ScxaExperimentRepository implements ExperimentRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScxaExperimentRepository.class);

    private final ExperimentCrudDao experimentCrudDao;
    private final ExperimentDesignParser experimentDesignParser;
    private final IdfParser idfParser;
    private final SdrfParser sdrfParser;
    private final SingleCellBaselineExperimentFactory singleCellBaselineExperimentFactory;

    public ScxaExperimentRepository(ExperimentCrudDao experimentCrudDao,
                                    ExperimentDesignParser experimentDesignParser,
                                    IdfParser idfParser,
                                    SingleCellBaselineExperimentFactory singleCellBaselineExperimentFactory,
                                    SdrfParser sdrfParser) {
        this.experimentCrudDao = experimentCrudDao;
        this.experimentDesignParser = experimentDesignParser;
        this.idfParser = idfParser;
        this.singleCellBaselineExperimentFactory = singleCellBaselineExperimentFactory;
        this.sdrfParser = sdrfParser;
    }

    @Override
    @Cacheable(cacheNames = "experiment", sync = true)
    public Experiment getExperiment(String experimentAccession) {
        var experimentDto = experimentCrudDao.readExperiment(experimentAccession);

        if (experimentDto == null) {
            throw new ResourceNotFoundException(
                    "Experiment with accession " + experimentAccession + " could not be found");
        }

        if (!experimentDto.getExperimentType().isSingleCell()) {
            throw new IllegalArgumentException(
                    "Unable to build experiment " + experimentDto.getExperimentAccession()
                            + ": experiment type " + experimentDto.getExperimentType() + " is not supported");
        }

        LOGGER.info("Building experiment {}...", experimentAccession);

        return singleCellBaselineExperimentFactory.create(
                experimentDto,
                experimentDesignParser.parse(experimentDto.getExperimentAccession()),
                idfParser.parse(experimentDto.getExperimentAccession()),
                sdrfParser.parseSingleCellTechnologyType(experimentDto.getExperimentAccession()));
    }

    @Override
    public String getExperimentType(String experimentAccession) {
        return experimentCrudDao.getExperimentType(experimentAccession);
    }
}
