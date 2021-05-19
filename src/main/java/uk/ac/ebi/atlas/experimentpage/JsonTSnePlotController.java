package uk.ac.ebi.atlas.experimentpage;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotJsonSerializer;

@RestController
public class
JsonTSnePlotController extends JsonExceptionHandlingController {
    private final TSnePlotJsonSerializer tSnePlotJsonSerializer;

    public JsonTSnePlotController(TSnePlotJsonSerializer tSnePlotJsonSerializer) {
        this.tSnePlotJsonSerializer = tSnePlotJsonSerializer;
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}/tsneplot/{perplexity}/clusters/k/{k}",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithClusters(@PathVariable String experimentAccession,
                                       @PathVariable int perplexity,
                                       @PathVariable int k,
                                       @RequestParam(defaultValue = "") String accessKey) {
        return tSnePlotJsonSerializer.tSnePlotWithClusters(experimentAccession, perplexity, k, accessKey);
    }

    //umap projection method
    @RequestMapping(value = "/json/experiments/{experimentAccession}/tsneplot/{number}/numbers",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithUmap(@PathVariable String experimentAccession,
                                       @PathVariable int number,
                                       @RequestParam(defaultValue = "") String accessKey) {
        return tSnePlotJsonSerializer.tSnePlotWithParameterisation(experimentAccession, number, accessKey);
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}/tsneplot/{parameter}/expression",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithExpression(@PathVariable String experimentAccession,
                                         @PathVariable int parameter,
                                         @RequestParam(defaultValue = "tsne") String method,
                                         @RequestParam(defaultValue = "") String accessKey) {
        return tSnePlotJsonSerializer.tSnePlotWithExpression(experimentAccession, method, parameter, accessKey);
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}/tsneplot/{perplexity}/metadata/{metadata}",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithMetadata(@PathVariable String experimentAccession,
                                       @PathVariable int perplexity,
                                       @PathVariable String metadata,
                                       @RequestParam(defaultValue = "") String accessKey) {
        return tSnePlotJsonSerializer.tSnePlotWithMetadata(experimentAccession, perplexity, metadata, accessKey);
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}/tsneplot/{parameter}/expression/{geneId}",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithExpression(@PathVariable String experimentAccession,
                                         @PathVariable int parameter,
                                         @PathVariable String geneId,
                                         @RequestParam(defaultValue = "tsne") String method,
                                         @RequestParam(defaultValue = "") String accessKey) {
        return tSnePlotJsonSerializer.tSnePlotWithExpression(experimentAccession, method, parameter, geneId, accessKey);
    }
}
