package uk.ac.ebi.atlas.download;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;

@Controller
public class DownloadController extends HtmlExceptionHandlingController {

    @RequestMapping(value = "/download", produces = "text/html;charset=UTF-8")
    public String getExperimentsListParameters(Model model) {
        model.addAttribute("mainTitle", "Download ");

        return "download";
    }
}
