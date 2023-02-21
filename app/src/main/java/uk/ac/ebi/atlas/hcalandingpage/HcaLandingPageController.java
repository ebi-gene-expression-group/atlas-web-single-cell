package uk.ac.ebi.atlas.hcalandingpage;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.experiments.ExperimentSearchService;

import java.util.HashMap;

import static uk.ac.ebi.atlas.hcalandingpage.JsonHcaLandingPageController.HCA_ACCESSION_PATTERN;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class HcaLandingPageController extends HtmlExceptionHandlingController {
    private ExperimentSearchService experimentSearchService;

    public HcaLandingPageController(ExperimentSearchService experimentSearchService) {
        this.experimentSearchService = experimentSearchService;
    }

    @GetMapping(value = "json/hca/experiments/count", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getHCAAndHomoSapiensExperiments() {
        HashMap<String, Integer> HCAAndHomoSapiensExperimentsCount = new HashMap<>();
        HCAAndHomoSapiensExperimentsCount.put("hcaExperimentsCount", experimentSearchService.searchPublicExperimentsByAccession(HCA_ACCESSION_PATTERN).size());
        HCAAndHomoSapiensExperimentsCount.put("humanExperimentsCount", experimentSearchService.searchPublicExperimentsBySpecies("Homo sapiens").size());
        return GSON.toJson(HCAAndHomoSapiensExperimentsCount);
    }
}

