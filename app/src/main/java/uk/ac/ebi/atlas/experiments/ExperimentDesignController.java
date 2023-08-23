package uk.ac.ebi.atlas.experiments;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesignTableService;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class ExperimentDesignController extends JsonExceptionHandlingController {
    private final ExperimentDesignTableService experimentDesignService;
    private final String DEFAULT_PAGE_NO = "1";
    private final String DEFAULT_PAGE_SIZE = "20";

    public ExperimentDesignController (ExperimentDesignTableService experimentDesignService) {
        this.experimentDesignService = experimentDesignService;
    }

    @GetMapping(value = "/json/experiment-design/{experiment_accession}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getExperimentDesign(@PathVariable String experimentAccession,
                                      @RequestParam(value = "pageNo", defaultValue = DEFAULT_PAGE_NO) int pageNo,
                                      @RequestParam(value = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        if(pageNo < 1)
            throw new IllegalArgumentException("Page number must be greater than 0");
        if(pageSize < 1)
            throw new IllegalArgumentException("Page size must be greater than 0");

        return  GSON.toJson(experimentDesignService.getExperimentDesignData(experimentAccession, pageNo, pageSize));
    }
}
