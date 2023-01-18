package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;

import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping(value="/json/cell-plots/{experimentAccession}", method= RequestMethod.GET)
public class
JsonCellPlotController extends JsonExceptionHandlingController {
    private final CellPlotJsonSerializer cellPlotJsonSerializer;
    private final CellPlotService cellPlotService;

    public JsonCellPlotController(CellPlotJsonSerializer cellPlotJsonSerializer,
                                  CellPlotService cellPlotService) {
        this.cellPlotJsonSerializer = cellPlotJsonSerializer;
        this.cellPlotService = cellPlotService;
    }

    // Our only assumption is that plot parameters are all integers, but we can further generalise if needed
    private ImmutableMap<String, Integer> parsePlotParametersFromRequestParameters(String experimentAccession,
                                                                                   String plotMethod,
                                                                                   Map<String, String> requestParams) {
        var requiredParameters =
                Optional.ofNullable(cellPlotService.cellPlotParameter(experimentAccession, plotMethod))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown plot type " + plotMethod));


        // Check that no param is missing
        var requiredParametersBuilder = ImmutableMap.<String, Integer>builder();
        for (var requiredParameter : requiredParameters) {
            if (!requestParams.containsKey(requiredParameter)) {
                throw new IllegalArgumentException("Missing parameter " + requiredParameter);
            } else {
                try {
                    requiredParametersBuilder.put(
                            requiredParameter, Integer.parseInt(requestParams.get(requiredParameter)));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid plot parameter value " +
                                    requiredParameter + "=" + requestParams.get(requiredParameter));
                }
            }
        }

        return requiredParametersBuilder.build();
    }

    @GetMapping(value = "/clusters/k/{k}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String clusterPlotK(@PathVariable String experimentAccession,
                               @PathVariable int k,
                               @RequestParam String plotMethod,
                               @RequestParam Map<String,String> requestParams) {
        return cellPlotJsonSerializer.clusterPlotWithK(
                experimentAccession,
                k,
                plotMethod,
                parsePlotParametersFromRequestParameters(experimentAccession, plotMethod, requestParams),
                requestParams.getOrDefault("accessKey", ""));
    }

    @GetMapping(value = "/clusters/metadata/{metadata}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String clusterPlotMetadata(@PathVariable String experimentAccession,
                                      @PathVariable String metadata,
                                      @RequestParam String plotMethod,
                                      @RequestParam Map<String,String> requestParams) {
        return cellPlotJsonSerializer.clusterPlotWithMetadata(
                experimentAccession,
                metadata.replaceAll(" ", "_"),
                plotMethod,
                parsePlotParametersFromRequestParameters(experimentAccession, plotMethod, requestParams),
                requestParams.getOrDefault("accessKey", ""));
    }

    @GetMapping(value = "/expression",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String expressionPlot(@PathVariable String experimentAccession,
                                 @RequestParam String plotMethod,
                                 @RequestParam Map<String,String> requestParams) {
        return cellPlotJsonSerializer.expressionPlot(
                experimentAccession,
                "",
                plotMethod,
                parsePlotParametersFromRequestParameters(experimentAccession, plotMethod, requestParams),
                requestParams.getOrDefault("accessKey", ""));
    }

    // Remember that gene IDs can have dots, e.g. Solyc09g014380.3 in E-ENAD-53
    // See also JsonBioentityInformationController.java
    @GetMapping(value = "/expression/{geneId:.+}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String expressionPlot(@PathVariable String experimentAccession,
                                 @PathVariable String geneId,
                                 @RequestParam String plotMethod,
                                 @RequestParam Map<String,String> requestParams) {
        return cellPlotJsonSerializer.expressionPlot(
                experimentAccession,
                geneId,
                plotMethod,
                parsePlotParametersFromRequestParameters(experimentAccession, plotMethod, requestParams),
                requestParams.getOrDefault("accessKey", ""));
    }
}
