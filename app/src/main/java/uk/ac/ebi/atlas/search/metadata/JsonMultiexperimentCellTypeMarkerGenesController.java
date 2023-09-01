package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableSet;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.markergenes.HighchartsHeatmapAdapter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class JsonMultiexperimentCellTypeMarkerGenesController extends JsonExceptionHandlingController {
    private final HighchartsHeatmapAdapter highchartsHeatmapAdapter;
    private final MultiexperimentCellTypeMarkerGenesService multiexperimentCellTypeMarkerGenesService;

    public JsonMultiexperimentCellTypeMarkerGenesController(
            HighchartsHeatmapAdapter highchartsHeatmapAdapter,
            MultiexperimentCellTypeMarkerGenesService multiexperimentCellTypeMarkerGenesService) {
        this.highchartsHeatmapAdapter = highchartsHeatmapAdapter;
        this.multiexperimentCellTypeMarkerGenesService = multiexperimentCellTypeMarkerGenesService;
    }

    @GetMapping(value = "/json/cell-type-marker-genes/{cellType}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getCellTypeMarkerGenes(
            @PathVariable String cellType,
            @RequestParam(name = "experiment-accessions", required = false) Collection<String> experimentAccessions) {
        try {
            return GSON.toJson(
                    highchartsHeatmapAdapter.getMarkerGeneHeatmapDataSortedLexicographically(
                            experimentAccessions == null ?
                                    multiexperimentCellTypeMarkerGenesService.getCellTypeMarkerGeneProfile( URLDecoder.decode(cellType, "UTF-8")) :
                                    multiexperimentCellTypeMarkerGenesService.getCellTypeMarkerGeneProfile(
                                            ImmutableSet.copyOf(experimentAccessions), URLDecoder.decode(cellType, "UTF-8"))));
        } catch (UnsupportedEncodingException e) {
            return "Error decoding the URL: " + e.getMessage();
        }
    }
}
