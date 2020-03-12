package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Component
public class HcaHumanExperimentService {
    private final ExperimentTrader experimentTrader;
    private final HcaHumanExperimentDao hcaHumanExperimentDao;

    public HcaHumanExperimentService(ExperimentTrader experimentTrader,
                                     HcaHumanExperimentDao hcaHumanExperimentDao) {
        this.experimentTrader = experimentTrader;
        this.hcaHumanExperimentDao = hcaHumanExperimentDao;
    }

    public ImmutableSet<Experiment> getPublicHumanExperiments(String characteristicName, String characteristicValue) {
        return hcaHumanExperimentDao.fetchExperimentAccessions(characteristicName, characteristicValue)
                .stream()
                .map(experimentTrader::getPublicExperiment)
                .filter(experiment -> experiment.getSpecies().isUs())
                .collect(toImmutableSet());
    }
}
