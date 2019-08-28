package uk.ac.ebi.atlas.experiments;

import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

@Component
public class ScExperimentSearchService extends ExperimentSearchService {
    public ScExperimentSearchService(ExperimentTrader experimentTrader) {
        super(experimentTrader);
    }
}
