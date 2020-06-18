package uk.ac.ebi.atlas.experimentpage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataService;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.utils.StringUtil;

@RestController
public class JsonCellMetadataController extends JsonExceptionHandlingController {
    private final CellMetadataService cellMetadataService;
    private final ExperimentTrader experimentTrader;

    public JsonCellMetadataController(ExperimentTrader experimentTrader,
                                      CellMetadataService cellMetadataService) {
        this.experimentTrader = experimentTrader;
        this.cellMetadataService = cellMetadataService;
    }

    @RequestMapping(value = "json/experiment/{experimentAccession}/cell/{cellId}/metadata",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getCellMetadata(@PathVariable String experimentAccession,
                                  @PathVariable String cellId,
                                  @RequestParam(defaultValue = "") String accessKey) {
        Experiment experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        JsonArray result = new JsonArray();

        cellMetadataService.getMetadataValues(experiment.getAccession(), cellId)
                .forEach((metadataName, metadataValue) ->
                        result.add(createMetadataJson(metadataName, metadataValue)));

        return result.toString();
    }

    private JsonObject createMetadataJson(String name, String value) {
        JsonObject result = new JsonObject();

        result.addProperty("displayName", StringUtil.snakeCaseToDisplayName(name));
        result.addProperty("value", value);

        return  result;
    }
}
