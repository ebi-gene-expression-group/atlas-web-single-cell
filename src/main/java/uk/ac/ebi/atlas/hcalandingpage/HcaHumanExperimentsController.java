package uk.ac.ebi.atlas.hcalandingpage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experiments.ExperimentJsonSerializer;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class HcaHumanExperimentsController {
    private final static String CHARACTERISTIC_NAME = "organism_part";
    private final HcaHumanExperimentService hcaHumanExperimentService;
    private final ExperimentJsonSerializer experimentJsonSerializer;

    public HcaHumanExperimentsController(HcaHumanExperimentService hcaHumanExperimentService,
                                         ExperimentJsonSerializer experimentJsonSerializer) {
        this.hcaHumanExperimentService = hcaHumanExperimentService;
        this.experimentJsonSerializer = experimentJsonSerializer;
    }

    //Used by anatomogram experiments table in HCA Landing page
    @GetMapping(value = "/json/experiments/hca/human",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getOrganismPartExperimentsList(@RequestParam(defaultValue = "") Set<String> organismPart) {
        return GSON.toJson(
                hcaHumanExperimentService
                        .getPublicHumanExperiments(CHARACTERISTIC_NAME, organismPart)
                        .stream()
                        .map(experimentJsonSerializer::serialize)
                        .collect(toImmutableSet()));
    }
}
