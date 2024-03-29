package uk.ac.ebi.atlas.staticpages;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.ServletContextResourceLoader;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

@Profile("!cli")
@Controller
public class StaticPageController extends HtmlExceptionHandlingController {
    private final ServletContextResourceLoader servletContextResourceLoader;

    public StaticPageController(ServletContext servletContext) {
        servletContextResourceLoader = new ServletContextResourceLoader(servletContext);
    }

    @RequestMapping("/{pageName}.html")
    public String getStaticPage(HttpServletRequest request, @PathVariable String pageName) {
        String path = String.format("/resources/html/%s.html", pageName);
        request.setAttribute("contentResource", fetchResource(path, request.getRequestURI()));
        return "static";
    }

//    @RequestMapping("/help/{pageName}.html")
//    public String getHelpPage(HttpServletRequest request, @PathVariable String pageName) {
//        String path = String.format("/resources/html/help/%s.html", pageName);
//        request.setAttribute("contentResource", fetchResource(path, request.getRequestURI()));
//        return "static";
//    }

    private Resource fetchResource(String path, String requestUrl) {
        Resource resource = servletContextResourceLoader.getResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Resource " + requestUrl + " does not exist");
        }
        return resource;
    }

}
