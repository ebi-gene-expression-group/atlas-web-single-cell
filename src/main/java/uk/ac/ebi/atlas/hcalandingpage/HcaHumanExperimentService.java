package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Service
public class HcaHumanExperimentService {
    private final ExperimentTrader experimentTrader;
    private final HcaHumanExperimentDao hcaHumanExperimentDao;

    public HcaHumanExperimentService(ExperimentTrader experimentTrader,
                                     HcaHumanExperimentDao hcaHumanExperimentDao) {
        this.experimentTrader = experimentTrader;
        this.hcaHumanExperimentDao = hcaHumanExperimentDao;
    }

    public ImmutableSet<Experiment> getPublicHumanExperiments(String characteristicName,
                                                              Set<String> characteristicValues) {

        Set<String> experimentAccessions = hcaHumanExperimentDao.fetchExperimentAccessions(characteristicName,
                                                                                            characteristicValues);
        return experimentAccessions
                .stream()
                .map(experimentTrader::getPublicExperiment)
                .filter(experiment -> experiment.getSpecies().isUs())
                .collect(toImmutableSet());
    }

}
