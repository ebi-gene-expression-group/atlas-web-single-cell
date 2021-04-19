package uk.ac.ebi.atlas.experiments;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;

@Controller
public class ExperimentsController extends HtmlExceptionHandlingController {
    @GetMapping(value = "/experiments", produces = "text/html;charset=UTF-8")
    public String
    getExperimentsListParameters(Model model) {
        return "experiments";
    }
}
