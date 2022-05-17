package uk.ac.ebi.atlas.experimentimport.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.experimentimport.ScxaExperimentCrud;
import uk.ac.ebi.atlas.resource.DataFileHub;

import java.util.UUID;

@Controller
@Scope("request")
@RequestMapping("/admin/experiments")
public class SingleCellExperimentAdminController extends ExperimentAdminController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCellExperimentAdminController.class);

    public SingleCellExperimentAdminController(DataFileHub dataFileHub,
                                               ScxaExperimentCrud scxaExperimentCrud) {
        super(
                new ExperimentOps(
                        new ExperimentOpLogWriter(dataFileHub),
                        new SingleCellOpsExecutionService(scxaExperimentCrud)));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "privateExperimentAccessions", allEntries = true)})
    @GetMapping(value = "/private/experiments/cache/clear")
    public String fooBar() {
        return "";
    }

    @GetMapping(value = "/{experimentAccession}")
    public void updateSingleCellExperiment(
            @PathVariable String experimentAccession,
            @RequestParam(value = "isPrivate") Boolean isPrivate,
            ScxaExperimentCrud scxaExperimentCrud) {
        UUID accessKeyUUID = scxaExperimentCrud.createExperiment(experimentAccession, isPrivate);
        LOGGER.info("Import experiment successfully, accessKeyUUID: {}", accessKeyUUID.toString());
    }
}
