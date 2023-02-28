package uk.ac.ebi.atlas.experiments;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesignTableService;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class ExperimentDesignController {
    private final ExperimentDesignTableService experimentDesignService;

    public ExperimentDesignController (ExperimentDesignTableService experimentDesignService) {
        this.experimentDesignService = experimentDesignService;
    }

    @GetMapping(value = "/json/experiment-design/{experiment_accession}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getExperimentDesign(@PathVariable String experiment_accession,
                                      @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                      @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return  GSON.toJson(experimentDesignService.getExperimentDesignData(experiment_accession, pageNo, pageSize));
    }
}
