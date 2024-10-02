package uk.ac.ebi.atlas.search.metadata;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.markergenes.HighchartsHeatmapAdapter;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
        return GSON.toJson(
                highchartsHeatmapAdapter.getMarkerGeneHeatmapDataSortedLexicographically(
                        experimentAccessions == null ?
                                multiexperimentCellTypeMarkerGenesService.getCellTypeMarkerGeneProfile(
                                        getDecodedCellType(cellType)) :
                                multiexperimentCellTypeMarkerGenesService.getCellTypeMarkerGeneProfile(
                                        ImmutableSet.copyOf(experimentAccessions),
                                        getDecodedCellType(cellType))));
    }

    private static @NotNull String getDecodedCellType(@NotNull String cellType) {
        if (cellType == null || cellType.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cellType cannot be null or empty");
        }

        return new String(Base64.getDecoder().decode(cellType), StandardCharsets.UTF_8);
    }
}
