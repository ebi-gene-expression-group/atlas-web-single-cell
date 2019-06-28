package uk.ac.ebi.atlas.experimentpage;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experimentpage.json.JsonExperimentController;
import uk.ac.ebi.atlas.experimentpage.markergenes.HighchartsHeatmapAdapter;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGenesDao;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.utils.GsonProvider;

@RestController
public class JsonMarkerGenesController extends JsonExperimentController {
    private MarkerGenesDao markerGenesDao;
    private HighchartsHeatmapAdapter highchartsHeatmapAdapter;

    public JsonMarkerGenesController(ExperimentTrader experimentTrader,
                                     MarkerGenesDao markerGenesDao,
                                     HighchartsHeatmapAdapter highchartsHeatmapAdapter) {
        super(experimentTrader);
        this.markerGenesDao = markerGenesDao;
        this.highchartsHeatmapAdapter = highchartsHeatmapAdapter;
    }

    @GetMapping(value = "/json/experiments/{experimentAccession}/marker-genes/{k}",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getMarkerGenes(@PathVariable String experimentAccession,
                                 @PathVariable int k) {
        var result = markerGenesDao.getMarkerGenesWithAveragesPerCluster(experimentAccession, k);
        return GsonProvider.GSON.toJson(highchartsHeatmapAdapter.getMarkerGeneHeatmapData(result));
    }
}
