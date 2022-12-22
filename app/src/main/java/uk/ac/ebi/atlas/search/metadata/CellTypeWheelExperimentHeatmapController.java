package uk.ac.ebi.atlas.search.metadata;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;

@Controller
public class CellTypeWheelExperimentHeatmapController extends HtmlExceptionHandlingController {
    @GetMapping(value = "/search/metadata/{term}", produces = "text/html;charset=UTF-8")
    public String getData (@PathVariable String term,
                           @RequestParam(name = "species", required = false) String species,
                           Model model) {
        model.addAttribute("cellTypeWheelSearchTerm", term);
        model.addAttribute("species", species);
        return "cell-type-wheel-experiment-heatmap";
    }
}
