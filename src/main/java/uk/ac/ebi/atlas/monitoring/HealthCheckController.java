package uk.ac.ebi.atlas.monitoring;

import com.google.common.collect.ImmutableSet;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experimentimport.ScxaExperimentDao;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class HealthCheckController {
    private final HealthChecker healthChecker;

    public HealthCheckController(HealthCheckService healthCheckService, ScxaExperimentDao experimentDao) {
        healthChecker = new HealthChecker(
                healthCheckService,
                experimentDao,
                ImmutableSet.of("bioentities"),
                ImmutableSet.of("scxa-analytics", "scxa-gene2experiment"));
    }

    @RequestMapping(value = "/json/health",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getHealthStatus() {
        return GSON.toJson(healthChecker.getHealthStatus());
    }
}