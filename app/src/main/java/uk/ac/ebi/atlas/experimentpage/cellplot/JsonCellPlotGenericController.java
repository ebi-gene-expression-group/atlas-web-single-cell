package uk.ac.ebi.atlas.experimentpage.cellplot;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
@RequestMapping(value="/json/cell-plots/generic/{experimentAccession}", method= RequestMethod.GET)
public class JsonCellPlotGenericController {
    private final CellPlotGenericDao cellPlotGenericDao;

    public JsonCellPlotGenericController(CellPlotGenericDao cellPlotGenericDao) {
        this.cellPlotGenericDao = cellPlotGenericDao;
    }

    @GetMapping(value = "/params/{plotType}",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getPlotParams(@PathVariable String plotType,
                                @PathVariable String experimentAccession) {
        var result = cellPlotGenericDao.getQueryParams(plotType, experimentAccession);

        if (result.isEmpty()) {
            throw new IllegalArgumentException("Unknown plot type " + plotType);
        } else {
          return  GSON.toJson(cellPlotGenericDao.getQueryParams(plotType, experimentAccession));
        }
    }
}
