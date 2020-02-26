package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class ExperimentsListController {
    private final static String CHARACTERISTIC_NAME = "organism_part";
    private final ExperimentJsonService experimentJsonService;
    private final ScExperimentService scExperimentService;

    public ExperimentsListController(ExperimentJsonService experimentJsonService,
                                     ScExperimentService scExperimentService) {
        this.experimentJsonService = experimentJsonService;
        this.scExperimentService = scExperimentService;
    }

    //Used by experiments table page
    @GetMapping(value = "/json/experiments",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getExperimentsList() {
        return GSON.toJson(
                ImmutableMap.of(
                        "experiments",
                        experimentJsonService.getPublicExperimentsJson()));
    }

    //Used by anatomogram experiments table in HCA Landing page
    @GetMapping(value = "/json/organism_part/experiments",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getOrganismPartExperimentsList(@RequestParam(defaultValue = "") String organismPart) {
        return GSON.toJson(
                ImmutableMap.of(
                        "experiments",
                        scExperimentService.getPublicExperimentsJson(CHARACTERISTIC_NAME, organismPart)));
    }
}