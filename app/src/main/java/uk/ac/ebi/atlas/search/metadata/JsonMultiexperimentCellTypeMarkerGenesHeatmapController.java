package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableSet;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.markergenes.HighchartsHeatmapAdapter;

import java.util.Collection;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class JsonMultiexperimentCellTypeMarkerGenesHeatmapController extends JsonExceptionHandlingController {
    private final HighchartsHeatmapAdapter highchartsHeatmapAdapter;
    private final MultiexperimentCellTypeMarkerGenesHeatmapService multiexperimentCellTypeMarkerGenesHeatmapService;

    public JsonMultiexperimentCellTypeMarkerGenesHeatmapController(
            HighchartsHeatmapAdapter highchartsHeatmapAdapter,
            MultiexperimentCellTypeMarkerGenesHeatmapService multiexperimentCellTypeMarkerGenesHeatmapService) {
        this.highchartsHeatmapAdapter = highchartsHeatmapAdapter;
        this.multiexperimentCellTypeMarkerGenesHeatmapService = multiexperimentCellTypeMarkerGenesHeatmapService;
    }

    @GetMapping(value = "/json/cell-type-marker-genes/{cellType}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getCellTypeMarkerGenes(
            @PathVariable String cellType,
            @RequestParam(required = false) Collection<String> experimentAccessions) {
        return GSON.toJson(
                highchartsHeatmapAdapter.getMarkerGeneHeatmapDataSortedLexicographically(
                        experimentAccessions == null ?
                                multiexperimentCellTypeMarkerGenesHeatmapService.getCellTypeMarkerGeneProfile(cellType) :
                                multiexperimentCellTypeMarkerGenesHeatmapService.getCellTypeMarkerGeneProfile(
                                        ImmutableSet.copyOf(experimentAccessions), cellType)));
    }
}
