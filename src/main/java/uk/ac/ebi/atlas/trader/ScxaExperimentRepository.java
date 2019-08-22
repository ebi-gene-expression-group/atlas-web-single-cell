package uk.ac.ebi.atlas.trader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrudDao;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.trader.factory.BaselineExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.MicroarrayExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.RnaSeqDifferentialExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.SingleCellBaselineExperimentFactory;

@Repository
public class ScxaExperimentRepository implements ExperimentRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScxaExperimentRepository.class);

    private final ExperimentCrudDao experimentCrudDao;
    private final ExperimentDesignParser experimentDesignParser;
    private final IdfParser idfParser;
    private final SingleCellBaselineExperimentFactory experimentFactory;

    public ScxaExperimentRepository(ExperimentCrudDao experimentCrudDao,
                                    ExperimentDesignParser experimentDesignParser,
                                    IdfParser idfParser,
                                    SingleCellBaselineExperimentFactory experimentFactory) {
        this.experimentCrudDao = experimentCrudDao;
        this.experimentDesignParser = experimentDesignParser;
        this.idfParser = idfParser;
        this.experimentFactory = experimentFactory;
    }

    @Override
    @Cacheable(cacheNames = "experiment", sync = true)
    public Experiment getExperiment(String experimentAccession) {
        var experimentDto = experimentCrudDao.readExperiment(experimentAccession);

        if (experimentDto == null) {
            throw new ResourceNotFoundException(
                    "Experiment with accession " + experimentAccession + " could not be found");
        }

        LOGGER.info("Building experiment {}...", experimentAccession);

        return experimentFactory.create(
                experimentDto,
                experimentDesignParser.parse(experimentDto.getExperimentAccession()),
                idfParser.parse(experimentDto.getExperimentAccession()));
    }
}
