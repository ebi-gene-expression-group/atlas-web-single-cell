package uk.ac.ebi.atlas.configuration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class HttpOnlyCookieConfigurator implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        var cookieConfig = servletContext.getSessionCookieConfig();
        cookieConfig.setHttpOnly(true);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // No need to do anything here, as the config for cookies is set for the lifetime of the application
    }
}
