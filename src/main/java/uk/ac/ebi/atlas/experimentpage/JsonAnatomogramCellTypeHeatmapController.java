package uk.ac.ebi.atlas.experimentpage;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.markergenes.HighchartsHeatmapAdapter;
import uk.ac.ebi.atlas.search.OntologyAccessionsSearchService;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class JsonAnatomogramCellTypeHeatmapController extends JsonExceptionHandlingController {
    private final OntologyAccessionsSearchService ontologyAccessionsSearchService;
    private final HighchartsHeatmapAdapter highchartsHeatmapAdapter;

    public JsonAnatomogramCellTypeHeatmapController(OntologyAccessionsSearchService ontologyAccessionsSearchService,
                                                    HighchartsHeatmapAdapter highchartsHeatmapAdapter) {
        this.ontologyAccessionsSearchService = ontologyAccessionsSearchService;
        this.highchartsHeatmapAdapter = highchartsHeatmapAdapter;
    }

    @GetMapping(value = "/json/experiments/{experimentAccession}/anatomogram",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getMarkerGenes(@PathVariable String experimentAccession) {
        return GSON.toJson(
                ontologyAccessionsSearchService.searchAvailableAnnotationsForOrganAnatomogram(experimentAccession));
    }
}
