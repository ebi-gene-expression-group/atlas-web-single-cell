package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    public ImmutableMap<String, List<String>> getMetadata() {
        var result = scExperimentTraderDao.fetchHumanExperimentAccessionsAndAssociatedOrganismParts();

        var ontology_ids = result.stream()
                .map(metadata -> {
                    var dataMap = (HashMap) metadata.iterator().next();
                    var url = dataMap.get("ontology_annotation").toString();
                    return url.substring(url.lastIndexOf('/') + 1);
                })
                .collect(Collectors.toList());

        var experiment_accessions = result.stream()
                .map(metadata -> {
                    var dataMap = (HashMap) metadata.iterator().next();
                    return dataMap.get("experiment_accession").toString();
                })
                .collect(Collectors.toList());

        return ImmutableMap.of(
                "ontology_ids", ontology_ids,
                "experiment_accessions", experiment_accessions
        );
    }

    public ImmutableSet<Experiment> getPublicExperiments(List<String> experimentAccessions) {
        return experimentAccessions
                .stream()
                .map(experimentTrader::getPublicExperiment)
                .filter(experiment -> experiment.getSpecies().isUs())
                .collect(toImmutableSet());
    }
}
