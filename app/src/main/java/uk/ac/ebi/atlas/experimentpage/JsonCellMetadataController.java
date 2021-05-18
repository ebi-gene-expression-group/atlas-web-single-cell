package uk.ac.ebi.atlas.experimentpage;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.metadata.CellMetadataJsonSerializer;

@RestController
public class JsonCellMetadataController extends JsonExceptionHandlingController {
    private final CellMetadataJsonSerializer cellMetadataJsonSerializer;

    public JsonCellMetadataController(CellMetadataJsonSerializer cellMetadataJsonSerializer) {
        this.cellMetadataJsonSerializer = cellMetadataJsonSerializer;
    }

    @RequestMapping(value = "json/experiment/{experimentAccession}/cell/{cellId}/metadata",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getCellMetadata(@PathVariable String experimentAccession,
                                  @PathVariable String cellId,
                                  @RequestParam(defaultValue = "") String accessKey) {
        return cellMetadataJsonSerializer.getCellMetadata(experimentAccession, cellId, accessKey);
    }
}
