package uk.ac.ebi.atlas.experimentpage;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotJsonSerializer;
import uk.ac.ebi.atlas.experimentpage.json.JsonExperimentController;
import uk.ac.ebi.atlas.trader.ScxaExperimentTrader;

@RestController
public class JsonTSnePlotController extends JsonExperimentController {
    private final TSnePlotJsonSerializer TSnePlotJsonSerializer;

    public JsonTSnePlotController(ScxaExperimentTrader experimentTrader,
                                  TSnePlotJsonSerializer TSnePlotJsonSerializer) {
        super(experimentTrader);
        this.TSnePlotJsonSerializer = TSnePlotJsonSerializer;
    }

    @RequestMapping(
            value = "/json/experiments/{experimentAccession}/tsneplot/{perplexity}/clusters/k/{k}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithClusters(
            @PathVariable String experimentAccession,
            @PathVariable int perplexity,
            @PathVariable int k,
            @RequestParam(defaultValue = "") String accessKey) {
        return TSnePlotJsonSerializer.tSnePlotWithClusters(experimentAccession, perplexity, k, accessKey);
    }

    @RequestMapping(
            value = "/json/experiments/{experimentAccession}/tsneplot/{perplexity}/expression",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithExpression(
            @PathVariable String experimentAccession,
            @PathVariable int perplexity,
            @RequestParam(defaultValue = "") String accessKey) {
        return TSnePlotJsonSerializer.tSnePlotWithExpression(experimentAccession, perplexity, accessKey);
    }

    @RequestMapping(
            value = "/json/experiments/{experimentAccession}/tsneplot/{perplexity}/metadata/{metadata}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithMetadata(
            @PathVariable String experimentAccession,
            @PathVariable int perplexity,
            @PathVariable String metadata,
            @RequestParam(defaultValue = "") String accessKey) {
        return TSnePlotJsonSerializer.tSnePlotWithMetadata(experimentAccession, perplexity, metadata, accessKey);
    }

    @RequestMapping(
            value = "/json/experiments/{experimentAccession}/tsneplot/{perplexity}/expression/{geneId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tSnePlotWithExpression(
            @PathVariable String experimentAccession,
            @PathVariable int perplexity,
            @PathVariable String geneId,
            @RequestParam(defaultValue = "") String accessKey) {
        return TSnePlotJsonSerializer.tSnePlotWithExpression(experimentAccession, perplexity, geneId, accessKey);
    }
}
