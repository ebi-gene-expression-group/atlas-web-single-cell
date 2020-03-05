package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experiments.ExperimentJsonSerializer;

import java.util.List;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Component
public class HcaMetadataService {
    private final static String ONTOLOGY_IDS = "ontology_ids";
    private final static String EXPERIMENT_ACCESSIONS = "experiment_accessions";


    private final HcaMetadataTrader hcaMetadataTrader;

    public HcaMetadataService(HcaMetadataTrader hcaMetadataTrader) {
        this.hcaMetadataTrader = hcaMetadataTrader;
    }

    public List<String> getHcaOntologyIds() {
        return hcaMetadataTrader.getMetadata().get(ONTOLOGY_IDS);
    }

    public ImmutableSet<JsonObject> getHcaExperiments() {
        return hcaMetadataTrader.getHcaExperiments(hcaMetadataTrader.getMetadata().get(EXPERIMENT_ACCESSIONS))
                .stream()
                .map(ExperimentJsonSerializer::serialize)
                .collect(toImmutableSet());
    }
}
