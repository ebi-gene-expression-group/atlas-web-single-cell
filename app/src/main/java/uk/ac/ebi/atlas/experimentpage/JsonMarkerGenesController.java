package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableSet;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.markergenes.HighchartsHeatmapAdapter;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGeneService;

import java.util.Set;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class JsonMarkerGenesController extends JsonExceptionHandlingController {
    private final HighchartsHeatmapAdapter highchartsHeatmapAdapter;
    private final MarkerGeneService markerGeneService;

    public JsonMarkerGenesController(HighchartsHeatmapAdapter highchartsHeatmapAdapter,
                                     MarkerGeneService markerGeneService) {
        this.highchartsHeatmapAdapter = highchartsHeatmapAdapter;
        this.markerGeneService = markerGeneService;
    }

    @GetMapping(value = "/json/experiments/{experimentAccession}/marker-genes/clusters",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getClusterMarkerGenes(@PathVariable String experimentAccession,
                                        @RequestParam String k) {
        return GSON.toJson(
                highchartsHeatmapAdapter.getMarkerGeneHeatmapDataSortedNumerically(
                        markerGeneService.getMarkerGenesPerCluster(experimentAccession, k)
                ));
    }

    @GetMapping(value = "/json/experiments/{experimentAccession}/marker-genes/cell-types",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getCellTypeMarkerGenes(@PathVariable String experimentAccession,
                                         @RequestParam Set<String> organismPart) {
        return GSON.toJson(highchartsHeatmapAdapter.getMarkerGeneHeatmapDataSortedLexicographically(
                markerGeneService.getCellTypeMarkerGeneProfile(experimentAccession, ImmutableSet.copyOf(organismPart))
        ));
    }

    @GetMapping(value = "/json/experiments/{experimentAccession}/marker-genes-heatmap/cell-types",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getCellTypeMarkerGenesHeatmap(@PathVariable String experimentAccession,
                                                @RequestParam String cellGroupType,
                                                @RequestParam Set<String> cellType) {
        return GSON.toJson(highchartsHeatmapAdapter.getMarkerGeneHeatmapDataSortedLexicographically(
                markerGeneService.getCellTypeMarkerGeneHeatmap(experimentAccession, cellGroupType, ImmutableSet.copyOf(cellType))
        ));
    }

    @GetMapping(value = "/json/experiments/{experimentAccession}/marker-genes-heatmap/cellTypeGroups",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getCellTypeGroupsMarkerGenesHeatmap(@PathVariable String experimentAccession,
                                                      @RequestParam String cellGroupType) {
        return GSON.toJson(
                markerGeneService.getCellTypesWithMarkerGenes(experimentAccession, cellGroupType)
        );
    }
}
