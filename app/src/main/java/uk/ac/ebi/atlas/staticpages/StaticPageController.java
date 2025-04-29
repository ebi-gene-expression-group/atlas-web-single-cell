package uk.ac.ebi.atlas.staticpages;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;

import javax.servlet.ServletContext;

@Profile("!cli")
@Controller
public class StaticPageController extends HtmlExceptionHandlingController {
    private final ServletContextResourceLoader servletContextResourceLoader;

    public StaticPageController(ServletContext servletContext) {
        servletContextResourceLoader = new ServletContextResourceLoader(servletContext);
    }

    @RequestMapping("/{pageName}.html")
    public String getStaticPage(@PathVariable String pageName, Model model) {
        checkPageExists(String.format("classpath:/templates/thymeleaf/views/%s.html", pageName), pageName);
        model.addAttribute("title", pageName);
        return pageName;
    }

    @RequestMapping("/help.html")
    public ModelAndView getHelpPage(@RequestParam(required = false) String section) {
        var viewName = "help";
        ModelAndView mav = new ModelAndView(viewName);
        checkPageExists(String.format("classpath:/templates/thymeleaf/views/%s.html", viewName), viewName);
        mav.addObject("section", section);
        mav.addObject("title", "Help");
        return mav;
    }

    private void checkPageExists(String path, String pageName) {
        Resource resource = servletContextResourceLoader.getResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Resource " + pageName + " does not exist");
        }
    }

}
