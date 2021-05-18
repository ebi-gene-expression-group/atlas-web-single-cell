package uk.ac.ebi.atlas.monitoring;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
@PropertySource("classpath:configuration.properties")
public class JsonBuildVersionController extends JsonExceptionHandlingController {
    private final ImmutableMap<String, String> buildVersion;

    public JsonBuildVersionController(@Value("${build.number}") String buildNumber,
                                      @Value("${build.branch}") String buildBranch,
                                      @Value("${build.commitId}") String buildCommitId,
                                      @Value("${build.tomcatHostname}") String tomcatHostname) {
        buildVersion = ImmutableMap.of(
                "bambooBuildVersion", buildNumber,
                "gitBranch", buildBranch,
                "gitCommitID", buildCommitId,
                "tomcatHostname", tomcatHostname);
    }

    @RequestMapping(value = "/json/build",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getBuildInfo() {
        return GSON.toJson(buildVersion);
    }
}
