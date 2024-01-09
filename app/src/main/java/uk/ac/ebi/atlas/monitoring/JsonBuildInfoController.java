package uk.ac.ebi.atlas.monitoring;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;

import java.util.Map;

@RestController
@PropertySource("classpath:resources/git.properties")
public class JsonBuildInfoController extends JsonExceptionHandlingController {
    private final ImmutableMap<String, String> buildInfo;

    public JsonBuildInfoController(@Value("${git.build.version}") String buildVersion,
                                   @Value("${git.branch}") String gitBranch,
                                   @Value("${git.commit.id}") String gitCommitId,
                                   @Value("${git.commit.message.full}") String commitMessage) {
        this.buildInfo = ImmutableMap.of(
                "Build version", buildVersion,
                "Git branch", gitBranch,
                "Latest commit ID", gitCommitId,
                "Latest commit message", commitMessage);
    }

    @RequestMapping(value = "/json/build",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> getBuildInfo() {
        return buildInfo;
    }
}
