package de.fuberlin.wiwiss.pubby.dm2e;

import de.fuberlin.wiwiss.pubby.URIRedirector;

/**
 * DM2E Implementation of the resource redirect to an aggregation
 *
 * @author Kai Eckert (kai@informatik.uni-mannheim.de
 */
public class ResourceRedirector implements URIRedirector {

    public String getRedirectedURI(String uri) {
        return uri.replaceFirst("resource","aggregation");
    }
}
