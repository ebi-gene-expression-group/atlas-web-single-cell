package uk.ac.ebi.atlas.experiments;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ExperimentsController {
    @GetMapping(value = "/experiments", produces = "text/html;charset=UTF-8")
    public String
    getExperimentsListParameters(Model model) {
        model.addAttribute("title", "Experiments");

        return "experiments";
    }
}
