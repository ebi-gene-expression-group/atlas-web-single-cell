package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Component
public class ScExperimentTrader {
    private final ExperimentTrader experimentTrader;
    private final ScExperimentTraderDao scExperimentTraderDao;

    public ScExperimentTrader(ExperimentTrader experimentTrader,
                              ScExperimentTraderDao scExperimentTraderDao) {
        this.experimentTrader = experimentTrader;
        this.scExperimentTraderDao = scExperimentTraderDao;
    }

    public ImmutableSet<Experiment> getPublicHumanExperiments(String characteristicName, String characteristicValue) {
        return scExperimentTraderDao.fetchExperimentAccessions(characteristicName, characteristicValue)
                .stream()
                .map(experimentTrader::getPublicExperiment)
                .filter(experiment -> experiment.getSpecies().isUs())
                .collect(toImmutableSet());
    }
}
