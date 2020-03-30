package uk.ac.ebi.atlas.search.metadata;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;

@RestController
public class JsonMetadataSearchController extends JsonExceptionHandlingController {

    private final MetadataJsonService metadataJsonService;

    public JsonMetadataSearchController(MetadataJsonService metadataJsonSerializer) {
        this.metadataJsonService = metadataJsonSerializer;
    }

    @GetMapping(value = "/json/metadata-search/expression/name/{characteristicName}/value/{characteristicValue}",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String cellTypeExpressions(@PathVariable String characteristicName,
                                      @PathVariable String characteristicValue,
                                      @RequestParam(defaultValue = "") String accessKey) {
        return metadataJsonService.cellTypeExpressions(characteristicName, characteristicValue, accessKey);
    }

    @GetMapping(value = "/json/metadata-search/cell-type/name/{characteristicName}/value/{characteristicValue}",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String cellTypeMetadata(@PathVariable String characteristicName,
                                   @PathVariable String characteristicValue,
                                   @RequestParam(defaultValue = "") String accessKey) {
        return metadataJsonService.cellTypeMetadata(characteristicName, characteristicValue, accessKey);
    }
}
