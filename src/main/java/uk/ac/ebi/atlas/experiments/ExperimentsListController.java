package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class ExperimentsListController {
    private final ExperimentJsonService experimentJsonService;

    public ExperimentsListController(ExperimentJsonService experimentJsonService) {
        this.experimentJsonService = experimentJsonService;
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
}