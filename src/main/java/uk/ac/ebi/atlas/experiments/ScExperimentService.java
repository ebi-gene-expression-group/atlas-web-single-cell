package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;

import java.util.Comparator;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;

@Component
public class ScExperimentService {
    private final static ImmutableList<ExperimentType> EXPERIMENT_TYPE_PRECEDENCE_LIST = ImmutableList.of(
            SINGLE_CELL_RNASEQ_MRNA_BASELINE,
            RNASEQ_MRNA_BASELINE,
            PROTEOMICS_BASELINE,
            RNASEQ_MRNA_DIFFERENTIAL,
            MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
            MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
            MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL);

    private final ScExperimentTrader scExperimentTrader;

    public ScExperimentService(ScExperimentTrader scExperimentTrader) {
        this.scExperimentTrader = scExperimentTrader;
    }

    public ImmutableSet<JsonObject> getPublicHumanExperimentsJson(String characteristicName, String characteristicValue) {
        // Sort by experiment type according to the above precedence list and then by display name
        return scExperimentTrader.getPublicHumanExperiments(characteristicName, characteristicValue)
                .stream()
                .sorted(Comparator
                        .<Experiment>comparingInt(experiment ->
                                EXPERIMENT_TYPE_PRECEDENCE_LIST.indexOf(experiment.getType()))
                        .thenComparing(Experiment::getDisplayName))
                .map(ExperimentJsonSerializer::serialize)
                .collect(toImmutableSet());
    }
}
