package uk.ac.ebi.atlas.search.metadata;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class JsonCellTypeWheelController extends JsonExceptionHandlingController {
    private final CellTypeWheelService cellTypeWheelService;
    private final HighchartsSunburstAdapter highchartsSunburstAdapter;

    public JsonCellTypeWheelController(CellTypeWheelService cellTypeWheelService,
                                       HighchartsSunburstAdapter highchartsSunburstAdapter) {
        this.cellTypeWheelService = cellTypeWheelService;
        this.highchartsSunburstAdapter = highchartsSunburstAdapter;
    }

    @GetMapping(value = "/json/cell-type-wheel/{searchTerm}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String jsonCellTypeWheel(@PathVariable String searchTerm) {
        return GSON.toJson(
                highchartsSunburstAdapter.getCellTypeWheelSunburst(
                        searchTerm, cellTypeWheelService.search(searchTerm)));
    }
}
