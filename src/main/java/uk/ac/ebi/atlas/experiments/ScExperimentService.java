package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Component
public class ScExperimentService {
    private final ExperimentTrader experimentTrader;
    private final ScExperimentDao scExperimentDao;

    public ScExperimentService(ExperimentTrader experimentTrader,
                               ScExperimentDao scExperimentDao) {
        this.experimentTrader = experimentTrader;
        this.scExperimentDao = scExperimentDao;
    }

    public ImmutableSet<Experiment> getPublicHumanExperiments(String characteristicName, String characteristicValue) {
        return scExperimentDao.fetchExperimentAccessions(characteristicName, characteristicValue)
                .stream()
                .map(experimentTrader::getPublicExperiment)
                .filter(experiment -> experiment.getSpecies().isUs())
                .collect(toImmutableSet());
    }
}
