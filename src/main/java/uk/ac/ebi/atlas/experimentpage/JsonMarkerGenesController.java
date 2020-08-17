package uk.ac.ebi.atlas.experimentpage;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.markergenes.CellTypeMarkerGene;
import uk.ac.ebi.atlas.experimentpage.markergenes.HighchartsHeatmapAdapter;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGeneService;
import uk.ac.ebi.atlas.experimentpage.markergenes.MarkerGenesDao;
import uk.ac.ebi.atlas.utils.GsonProvider;

import java.util.List;

@RestController
public class JsonMarkerGenesController extends JsonExceptionHandlingController {
    private MarkerGenesDao markerGenesDao;
    private HighchartsHeatmapAdapter highchartsHeatmapAdapter;
    private MarkerGeneService markerGeneService;

    public JsonMarkerGenesController(MarkerGenesDao markerGenesDao,
                                     HighchartsHeatmapAdapter highchartsHeatmapAdapter,
                                     MarkerGeneService markerGeneService) {
        this.markerGenesDao = markerGenesDao;
        this.highchartsHeatmapAdapter = highchartsHeatmapAdapter;
        this.markerGeneService = markerGeneService;
    }

    @GetMapping(value = "/json/experiments/{experimentAccession}/marker-genes/{k}",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getMarkerGenes(@PathVariable String experimentAccession,
                                 @PathVariable int k) {
        return GsonProvider.GSON.toJson(
                highchartsHeatmapAdapter.getMarkerGeneHeatmapData(
                        markerGenesDao.getMarkerGenesWithAveragesPerCluster(experimentAccession, k)));
    }

    @GetMapping(value = "/json/experiments/{experimentAccession}/cell-type/marker-genes/{organismPart}",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getMarkerGenesProfile(@PathVariable String experimentAccession,
                                        @PathVariable String organismPart) {
        System.out.println("I am in......");
        List<CellTypeMarkerGene> markerGenesProfile = markerGeneService.getMarkerGenes(experimentAccession, organismPart);
        for (CellTypeMarkerGene markerGene : markerGenesProfile) {
            System.out.println("custerId: " + markerGene.clusterId());
        }
        System.out.println("Marker Genes Profile: " + markerGenesProfile.toString());
        System.out.println("Marker Genes Profile size: " + markerGenesProfile.size());
        return GsonProvider.GSON.toJson(highchartsHeatmapAdapter.getCellTypeMarkerGeneHeatmapData(
                markerGeneService.getMarkerGenes(experimentAccession, organismPart)
        ));
    }
}
