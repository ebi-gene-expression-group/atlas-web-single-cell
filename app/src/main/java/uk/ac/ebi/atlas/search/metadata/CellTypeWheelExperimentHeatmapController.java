package uk.ac.ebi.atlas.search.metadata;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;


@Controller
public class CellTypeWheelExperimentHeatmapController extends HtmlExceptionHandlingController {

    @GetMapping(value = "/search/metadata/{term}", produces = "text/html;charset=UTF-8")
    public String getData (@PathVariable String term, Model model) {
        model.addAttribute("term", term);

        return "cell-type-wheel-experiment-heatmap";

    }
}
