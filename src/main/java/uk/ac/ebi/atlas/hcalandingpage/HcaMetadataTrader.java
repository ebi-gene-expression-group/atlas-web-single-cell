package uk.ac.ebi.atlas.hcalandingpage;

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
public class HcaMetadataTrader {
    private final ExperimentTrader experimentTrader;
    private final HcaMetadataTraderDao hcaMetadataTraderDao;

    public HcaMetadataTrader(ExperimentTrader experimentTrader,
                             HcaMetadataTraderDao hcaMetadataTraderDao) {
        this.experimentTrader = experimentTrader;
        this.hcaMetadataTraderDao = hcaMetadataTraderDao;
    }

    public ImmutableMap<String, List<String>> getMetadata() {
        var listOfMapsOfExperimentAccessionsAndOntologyIds =
                hcaMetadataTraderDao.fetchHumanExperimentAccessionsAndAssociatedOrganismParts();

        var ontologyIds = listOfMapsOfExperimentAccessionsAndOntologyIds.stream()
                .map(metadata -> {
                    var dataMap = (HashMap) metadata.iterator().next();
                    var url = dataMap.get("ontology_annotation").toString();
                    return url.substring(url.lastIndexOf('/') + 1);
                })
                .collect(Collectors.toList());

        var experimentAccessions = listOfMapsOfExperimentAccessionsAndOntologyIds.stream()
                .map(metadata -> {
                    var dataMap = (HashMap) metadata.iterator().next();
                    return dataMap.get("experiment_accession").toString();
                })
                .collect(Collectors.toList());

        return ImmutableMap.of(
                "ontology_ids", ontologyIds,
                "experiment_accessions", experimentAccessions
        );
    }

    public ImmutableSet<Experiment> getHcaExperiments(List<String> experimentAccessions) {
        return experimentAccessions
                .stream()
                .map(experimentTrader::getPublicExperiment)
                .collect(toImmutableSet());
    }
}
