package uk.ac.ebi.atlas.experimentpage.cellplot;

import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        var requiredParameter = StringUtils.substringsBetween(requiredParameters.get(0) , "\"", "\"")[0];
        var parameterisations = requiredParameters.stream().map(parameter ->
                StringUtils.substringsBetween(parameter, " ","}")[0]).collect(Collectors.toList());
        if (!requestParams.containsKey(requiredParameter)) {
            throw new IllegalArgumentException("Missing parameter " + requiredParameter);
        }
        else if (!parameterisations.contains(requestParams.get(requiredParameter))) {
            throw new IllegalArgumentException(
                    "Invalid plot parameter value " +
                            requiredParameter + "=" + requestParams.get(requiredParameter));
        }
        else {
            requiredParametersBuilder.put(
                    requiredParameter, Integer.parseInt(requestParams.get(requiredParameter)));
        }
        return requiredParametersBuilder.build();
    }

    @GetMapping(value = "/clusters/k/{k}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Operation(
            operationId = "clustersCellPlot",
            description = "Retrieve cells from a dataset as a 2D projection bucketed by clusters as predicted by " +
                    "[Scanpy](https://scanpy.readthedocs.io); each point represents a cell in the experiment. Data " +
                    "are returned in the [Highcharts scatter plot format]" +
                    "(https://api.highcharts.com/highcharts/series.scatter.data). Each series represents a " +
                    "different cluster value. There will be as many clusters/series as `k` (see below).",
            parameters = {
                    @Parameter(
                            name = "experimentAccession",
                            in = ParameterIn.PATH,
                            description = "experiment accession of dataset",
                            example = "E-MTAB-5061"),
                    @Parameter(
                            name = "k",
                            in = ParameterIn.PATH,
                            description = "number of clusters in which the cells should be split",
                            schema = @Schema(
                                    type = "integer",
                                    example = "9"
                            )),
                    @Parameter(
                            name = "plotMethod",
                            in = ParameterIn.QUERY,
                            description = "2D projection method",
                            schema = @Schema(
                                    defaultValue = "umap",
                                    allowableValues = {"umap", "tsne"})),
                    @Parameter(
                            name = "n_neighbors",
                            in = ParameterIn.QUERY,
                            description = "number of neighbours for the UMAP projection (ignored in t-SNE plots)",
                            required = true,
                            schema = @Schema(
                                    type = "integer",
                                    example = "20")),
                    @Parameter(
                            name = "perplexity",
                            in = ParameterIn.QUERY,
                            description = "perplexity value for the t-SNE projection (ignored in UMAP plots)",
                            required = true,
                            schema = @Schema(
                                    type = "integer",
                                    example = "35"))
            })
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
    @Operation(
            operationId = "metadataCellPlot",
            description = "Retrieve cells from a dataset as a 2D projection bucketed by a metadata field; each " +
                    "point represents a cell in the experiment. Data are returned in the [Highcharts scatter plot " +
                    "format](https://api.highcharts.com/highcharts/series.scatter.data). Each series represents a " +
                    "value of the cell plots that share it for the specifed metadata field.",
            parameters = {
                    @Parameter(
                            name = "experimentAccession",
                            in = ParameterIn.PATH,
                            description = "experiment accession of dataset",
                            example = "E-MTAB-5061"),
                    @Parameter(
                            name = "metadata",
                            in = ParameterIn.PATH,
                            description = "metadata field name over which cells should be bucketed",
                            example = "inferred cell type - ontology labels"),
                    @Parameter(
                            name = "plotMethod",
                            in = ParameterIn.QUERY,
                            description = "2D projection method",
                            schema = @Schema(
                                    defaultValue = "umap",
                                    allowableValues = {"umap", "tsne" })),
                    @Parameter(
                            name = "n_neighbors",
                            in = ParameterIn.QUERY,
                            description = "number of neighbours for the UMAP projection (ignored in t-SNE plots)",
                            required = true,
                            schema = @Schema(
                                    type = "integer",
                                    example = "20")),
                    @Parameter(
                            name = "perplexity",
                            in = ParameterIn.QUERY,
                            description = "perplexity value for the t-SNE projection (ignored in UMAP plots)",
                            required = true,
                            schema = @Schema(
                                    type = "integer",
                                    example = "35"))
            })
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

    @Hidden
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
    @Operation(
            operationId = "expressionCellPlot",
            description = "Retrieve expression of a specified gene ID from a dataset as a 2D projection; each " +
                    "point represents a cell in the experiment. The data is returned in the [Highcharts scatter " +
                    "plot](https://api.highcharts.com/highcharts/series.scatter.data) format in a single series.",
            parameters = {
                    @Parameter(
                            name = "experimentAccession",
                            in = ParameterIn.PATH,
                            description = "experiment accession of dataset",
                            example = "E-MTAB-5061"),
                    @Parameter(
                            name = "geneId",
                            in = ParameterIn.PATH,
                            description = "[Ensembl](https://www.ensembl.org) gene ID to show expression of (may " +
                                    "be empty, in which case plot   will show no expression in all cells)",
                            example = "ENSG00000125798"),
                    @Parameter(
                            name = "plotMethod",
                            in = ParameterIn.QUERY,
                            description = "2D projection method",
                            schema = @Schema(
                                    type = "string",
                                    defaultValue = "umap",
                                    allowableValues = {"umap", "tsne"})),
                    @Parameter(
                            name = "n_neighbors",
                            in = ParameterIn.QUERY,
                            description = "number of neighbours for the UMAP projection (ignored in t-SNE plots)",
                            required = true,
                            schema = @Schema(
                                    type = "integer",
                                    example = "20")),
                    @Parameter(
                            name = "perplexity",
                            in = ParameterIn.QUERY,
                            description = "perplexity value for the t-SNE projection (ignored in UMAP plots)",
                            required = true,
                            schema = @Schema(
                                    type = "integer",
                                    example = "35"))
            })
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

    @GetMapping(value = "/default/plot-method")
    public String defaultPlotMethodWithParameterisation(@PathVariable String experimentAccession) {
        return cellPlotJsonSerializer.fetchDefaultPlotMethodWithParameterisation(experimentAccession);
    }
}
