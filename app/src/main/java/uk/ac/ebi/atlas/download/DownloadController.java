package uk.ac.ebi.atlas.download;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DownloadController {

    @RequestMapping(value = "/download", produces = "text/html;charset=UTF-8")
    public String getExperimentsListParameters(Model model) {
        model.addAttribute("title", "Download ");

        return "download";
    }
}
