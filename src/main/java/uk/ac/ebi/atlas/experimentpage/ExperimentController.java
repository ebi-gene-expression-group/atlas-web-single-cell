package uk.ac.ebi.atlas.experimentpage;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.tabs.ExperimentPageContentSerializer;
import uk.ac.ebi.atlas.experimentpage.tsneplot.TSnePlotSettingsService;
import uk.ac.ebi.atlas.trader.ScxaExperimentTrader;

import javax.inject.Inject;

@Controller
public class ExperimentController extends HtmlExceptionHandlingController {
    private final ScxaExperimentTrader experimentTrader;
    private final ExperimentPageContentSerializer experimentPageContentSerializer;
    private final ExperimentAttributesService experimentAttributesService;
    private final TSnePlotSettingsService tSnePlotSettingsService;

    @Inject
    public ExperimentController(ScxaExperimentTrader experimentTrader,
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
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        model.addAllAttributes(experimentAttributesService.getAttributes(experiment));

        model.addAttribute(
                "content",
                experimentPageContentSerializer.experimentPageContentForExperiment(experimentAccession, accessKey));

        model.addAttribute(
                "numberOfCells",
                tSnePlotSettingsService.getCellCount(experimentAccession));

        return "experiment-page";
    }
}
