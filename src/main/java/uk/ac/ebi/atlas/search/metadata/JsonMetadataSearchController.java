package uk.ac.ebi.atlas.search.metadata;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;

@RestController
public class JsonMetadataSearchController extends JsonExceptionHandlingController {

    private final MetadataJsonSerializer metadataJsonSerializer;

    public JsonMetadataSearchController(MetadataJsonSerializer metadataJsonSerializer) {
        this.metadataJsonSerializer = metadataJsonSerializer;
    }

    @RequestMapping(value = "/json/metadata-search/name/{characteristicName}/value/{characteristicValue}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String cellTypeMetadata(@PathVariable String characteristicName,
                                   @PathVariable String characteristicValue,
                                   @RequestParam(defaultValue = "") String accessKey) {
        return metadataJsonSerializer.cellTypeMetadata(characteristicName, characteristicValue, accessKey);
    }
}
