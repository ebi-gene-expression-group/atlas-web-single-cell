package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experiments.ExperimentJsonSerializer;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Component
public class HcaMetadataService {
    private final HcaMetadataDao hcaMetadataDao;
    private final ExperimentTrader experimentTrader;

    public HcaMetadataService(ExperimentTrader experimentTrader,
                              HcaMetadataDao hcaMetadataDao) {
        this.experimentTrader = experimentTrader;
        this.hcaMetadataDao = hcaMetadataDao;
    }

    public List<String> getHcaOntologyIds() {
        return getMetadata().get("ontology_ids");
    }

    public ImmutableSet<JsonObject> getHcaExperiments() {
        return getHcaExperiments(getMetadata().get("experiment_accessions"))
                .stream()
                .map(ExperimentJsonSerializer::serialize)
                .collect(toImmutableSet());
    }

    private ImmutableMap<String, List<String>> getMetadata() {
        var listOfMapsOfExperimentAccessionsAndOntologyIds =
                hcaMetadataDao.fetchHumanExperimentAccessionsAndAssociatedOrganismParts();

        //There is definitely a better way of parsing this
        //but after spending quite a time with streams and trying different options
        //I thought to resort back to basic loops
        //TODO: Find better way to parse following
        var ontologyIds = new ArrayList<String>();
        for (ArrayList<Object> metadata:
             listOfMapsOfExperimentAccessionsAndOntologyIds.asList()) {
            var metadataListIterator = metadata.iterator();
            while (metadataListIterator.hasNext()) {
                var dataMap = (HashMap) metadataListIterator.next();
                var url = dataMap.get("ontology_annotation").toString();
                ontologyIds.add(url.substring(url.lastIndexOf('/') + 1));
            }
        }

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

    private ImmutableSet<Experiment> getHcaExperiments(List<String> experimentAccessions) {
        return experimentAccessions
                .stream()
                .map(experimentTrader::getPublicExperiment)
                .collect(toImmutableSet());
    }
}
