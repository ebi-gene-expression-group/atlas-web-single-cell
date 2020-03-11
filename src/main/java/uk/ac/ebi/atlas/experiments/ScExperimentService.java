package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;

@Component
public class ScExperimentService {
    private final ScExperimentTrader scExperimentTrader;

    public ScExperimentService(ScExperimentTrader scExperimentTrader) {
        this.scExperimentTrader = scExperimentTrader;
    }

    public ImmutableSet<Experiment> getPublicHumanExperiments(String characteristicName, String characteristicValue) {
        return scExperimentTrader.getPublicHumanExperiments(characteristicName, characteristicValue);
    }
}
