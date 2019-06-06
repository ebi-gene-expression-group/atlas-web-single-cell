package uk.ac.ebi.atlas.experimentpage;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.content.ExperimentPageContentService2;
import uk.ac.ebi.atlas.trader.ScxaExperimentTrader;

import javax.inject.Inject;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Controller
public class ExperimentController extends HtmlExceptionHandlingController {
    private final ScxaExperimentTrader experimentTrader;
    private final ExperimentPageContentService2 experimentPageContentService2;
    private final ExperimentAttributesService experimentAttributesService;
    private final TSnePlotSettingsService tSnePlotSettingsService;

    @Inject
    public ExperimentController(ScxaExperimentTrader experimentTrader,
                                ExperimentPageContentService2 experimentPageContentService2,
                                ExperimentAttributesService experimentAttributesService,
                                TSnePlotSettingsService tSnePlotSettingsService) {
        this.experimentTrader = experimentTrader;
        this.experimentPageContentService2 = experimentPageContentService2;
        this.experimentAttributesService = experimentAttributesService;
        this.tSnePlotSettingsService = tSnePlotSettingsService;
    }

    @RequestMapping(value = {"/experiments/{experimentAccession}", "/experiments/{experimentAccession}/**"},
                    produces = "text/html;charset=UTF-8")
    public String showExperimentPage(Model model,
                                     @PathVariable String experimentAccession,
                                     @RequestParam(defaultValue = "") String accessKey) {
        var experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        model.addAllAttributes(experimentAttributesService.getAttributes(experiment));
        model.addAttribute("content", GSON.toJson(experimentPageContentService2.experimentPageContentForExperiment(experiment, accessKey)));
        model.addAttribute("numberOfCells", tSnePlotSettingsService.getCellCount(experimentAccession));

        return "experiment-page";
    }
}
