package uk.ac.ebi.atlas.hcalandingpage;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experiments.ExperimentJsonSerializer;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class HcaMetadataController {
    private final HcaMetadataService hcaMetadataService;
    private final ExperimentJsonSerializer experimentJsonSerializer;

    public HcaMetadataController(HcaMetadataService hcaMetadataService,
                                 ExperimentJsonSerializer experimentJsonSerializer) {
        this.hcaMetadataService = hcaMetadataService;
        this.experimentJsonSerializer = experimentJsonSerializer;
    }

    @GetMapping(value = "/json/metadata/hca",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getHcaMetadata() {
        var experiments = hcaMetadataService.getHcaExperiments()
                .stream()
                .map(experimentJsonSerializer::serialize)
                .collect(toImmutableSet());
        return GSON.toJson(
                ImmutableMap.of(
                        "ontology_ids", hcaMetadataService.getHcaOntologyIds(),
                        "experiments", experiments,
                        "species", "Homo sapiens"
                ));
    }
}
