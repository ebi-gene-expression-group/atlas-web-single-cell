package uk.ac.ebi.atlas.trader;

import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.ExperimentDto;
import uk.ac.ebi.atlas.experimentimport.ScxaExperimentDao;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParserOutput;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesign;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.trader.factory.SingleCellBaselineExperimentFactory;

@Component
public class ScxaExperimentTrader extends ExperimentTrader {
    private final SingleCellBaselineExperimentFactory experimentFactory;

    public ScxaExperimentTrader(ScxaExperimentDao experimentDao,
                                SingleCellBaselineExperimentFactory experimentFactory,
                                ExperimentDesignParser experimentDesignParser,
                                IdfParser idfParser) {
        super(experimentDao, experimentDesignParser, idfParser);
        this.experimentFactory = experimentFactory;
    }

    @Override
    public SingleCellBaselineExperiment getExperiment(String experimentAccession, String accessKey) {
        ExperimentDesign experimentDesign = experimentDesignParser.parse(experimentAccession);
        IdfParserOutput idfParserOutput = idfParser.parse(experimentAccession);
        ExperimentDto experimentDto = experimentDao.findExperiment(experimentAccession, accessKey);

        return experimentFactory.create(experimentDto, experimentDesign, idfParserOutput);
    }
}
