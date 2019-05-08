package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.trader.ScxaExperimentTrader;

import javax.inject.Inject;

import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Controller
public class ExperimentsController extends HtmlExceptionHandlingController {
    private ExperimentInfoListService experimentInfoListService;

    @Inject
    public ExperimentsController(ScxaExperimentTrader scxaExperimentTrader) {
        this.experimentInfoListService =
                new ExperimentInfoListService(scxaExperimentTrader, ImmutableList.of(
                        SINGLE_CELL_RNASEQ_MRNA_BASELINE));
    }

    @RequestMapping(value = "/experiments", method = RequestMethod.GET, produces = "text/html;charset=UTF-8")
    public String
    getExperimentsListParameters(Model model) {

        model.addAttribute("data", GSON.toJson(experimentInfoListService.getExperimentsJson().get("aaData")));

        return "experiments";
    }
}
