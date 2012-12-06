package de.fuberlin.wiwiss.pubby.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.fuberlin.wiwiss.pubby.Configuration;

/**
 * A catch-all servlet managing the URI space of the web application.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @version $Id$
 */
public class RootServlet extends BaseServlet {

    private Logger log = Logger.getLogger(getClass().getName());

    protected boolean doGet(String relativeURI,
                            HttpServletRequest request, HttpServletResponse response,
                            Configuration config)
            throws IOException, ServletException {

        log.fine("Relative URI served by Pubby: " + relativeURI);
        // static/ directory handled by default servlet
        if (relativeURI.startsWith("static/")) {
            getServletContext().getNamedDispatcher("default").forward(request, response);
            return true;
        }

        // If index resource is defined, redirect requests for the index page to it
        if ("".equals(relativeURI)){
            if (config.getIndexResource() != null) {
                response.sendRedirect(config.getIndexResource().getWebURI());
                return true;
            } else {
                log.fine("No index resource available in config...");
            }
        }

        // Dispatch to servlets
        if (config.isPageURL(relativeURI)) {
            getServletContext().getNamedDispatcher("PageURLServlet").forward(request, response);
        } else if (config.isDataURL(relativeURI)) {
            getServletContext().getNamedDispatcher("DataURLServlet").forward(request, response);
        } else {
            getServletContext().getNamedDispatcher("WebURIServlet").forward(request, response);
        }
        return true;
    }
}
