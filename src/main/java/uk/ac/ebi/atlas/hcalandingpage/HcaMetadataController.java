package uk.ac.ebi.atlas.hcalandingpage;

import com.google.gson.JsonObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class HcaMetadataController {
    private final static String SPECIES = "Homo sapiens";
    private final HcaMetadataService hcaMetadataService;

    public HcaMetadataController(HcaMetadataService hcaMetadataService) {
        this.hcaMetadataService = hcaMetadataService;
    }

    @GetMapping(value = "/json/metadata/hca",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getHCAMetadata() {
        var hcaMetadata = new JsonObject();

        hcaMetadata.add("ontology_ids", GSON.toJsonTree(hcaMetadataService.getHcaOntologyIds()));
        hcaMetadata.add("experiments", GSON.toJsonTree(hcaMetadataService.getHcaExperiments()));
        hcaMetadata.add("species", GSON.toJsonTree(SPECIES));

        return GSON.toJson(hcaMetadata);
    }
}
