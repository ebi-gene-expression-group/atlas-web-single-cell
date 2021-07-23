package uk.ac.ebi.atlas.experimentpage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.tabs.ExperimentPageContentSerializer;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

@Controller
public class ExperimentController extends HtmlExceptionHandlingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentController.class);

    private final ExperimentTrader experimentTrader;
    private final ExperimentPageContentSerializer experimentPageContentSerializer;
    private final ExperimentAttributesService experimentAttributesService;
    private final TSnePlotSettingsService tSnePlotSettingsService;

    public ExperimentController(ExperimentTrader experimentTrader,
                                ExperimentPageContentSerializer experimentPageContentSerializer,
                                ExperimentAttributesService experimentAttributesService,
                                TSnePlotSettingsService tSnePlotSettingsService) {
        this.experimentTrader = experimentTrader;
        this.experimentPageContentSerializer = experimentPageContentSerializer;
        this.experimentAttributesService = experimentAttributesService;
        this.tSnePlotSettingsService = tSnePlotSettingsService;
    }

    @RequestMapping(value = {"/experiments/{experimentAccession}", "/experiments/{experimentAccession}/**"},
                    produces = MediaType.TEXT_HTML_VALUE)
    public String showExperimentPage(Model model,
                                     @PathVariable String experimentAccession,
                                     @RequestParam(defaultValue = "") String accessKey) {
        var stopwatch = new StopWatch(this.getClass().getSimpleName());

        stopwatch.start("Get experiment from trader");
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);
        stopwatch.stop();

        stopwatch.start("Get experiment attributes");
        model.addAllAttributes(experimentAttributesService.getAttributes(experiment));
        stopwatch.stop();

        stopwatch.start("Get and serialise experiment content");
        model.addAttribute(
                "content",
                experimentPageContentSerializer.experimentPageContentForExperiment(experimentAccession, accessKey));
        stopwatch.stop();

        stopwatch.start("Get number of cells");
        model.addAttribute(
                "numberOfCells",
                tSnePlotSettingsService.getCellCount(experimentAccession));
        stopwatch.stop();

        LOGGER.debug(stopwatch.prettyPrint());
        return "experiment-page";
    }
}
