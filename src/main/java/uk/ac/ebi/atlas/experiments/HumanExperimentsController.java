package uk.ac.ebi.atlas.experiments;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class HumanExperimentsController {
    private final static String CHARACTERISTIC_NAME = "organism_part";
    private final ScExperimentService scExperimentService;

    public HumanExperimentsController(ScExperimentService scExperimentService) {
        this.scExperimentService = scExperimentService;
    }

    //Used by anatomogram experiments table in HCA Landing page
    @GetMapping(value = "/json/experiments/human",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getOrganismPartExperimentsList(@RequestParam(defaultValue = "") String organismPart) {
        return GSON.toJson(
                scExperimentService
                        .getPublicHumanExperiments(CHARACTERISTIC_NAME, organismPart)
                        .stream()
                        .map(ExperimentJsonSerializer::serialize)
                        .collect(toImmutableSet()));
    }
}
