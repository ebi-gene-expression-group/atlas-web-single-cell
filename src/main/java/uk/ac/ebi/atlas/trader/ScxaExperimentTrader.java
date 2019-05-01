package uk.ac.ebi.atlas.trader;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.ExperimentDto;
import uk.ac.ebi.atlas.experimentimport.ScxaExperimentDao;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.model.experiment.singlecell.SingleCellBaselineExperiment;
import uk.ac.ebi.atlas.trader.factory.SingleCellBaselineExperimentFactory;

@Component
@NonNullByDefault
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
    protected SingleCellBaselineExperiment buildExperiment(ExperimentDto experimentDto) {
        return experimentFactory.create(
                experimentDto,
                experimentDesignParser.parse(experimentDto.getExperimentAccession()),
                idfParser.parse(experimentDto.getExperimentAccession()));
    }
}
