package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

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

    public ImmutableSet<String> getHcaOntologyIds() {
        return hcaMetadataDao
                .fetchHumanExperimentAccessionsAndAssociatedOntologyIds()
                .stream()
                .flatMap(metadataList -> metadataList
                        .stream()
                        .map(metadataMap -> {
                            var url = metadataMap.get("ontology_annotation");
                            return url.substring(url.lastIndexOf('/') + 1);
                        }))
                .collect(toImmutableSet());
    }

    public ImmutableSet<Experiment> getHcaExperiments() {
        return hcaMetadataDao
                .fetchHumanExperimentAccessionsAndAssociatedOntologyIds()
                .stream()
                .flatMap(metadataList -> metadataList
                        .stream()
                        .map(metadataMap -> metadataMap.get("experiment_accession")))
                .map(experimentTrader::getPublicExperiment)
                .collect(toImmutableSet());
    }
}
