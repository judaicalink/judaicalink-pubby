package de.fuberlin.wiwiss.pubby.dm2e;

import de.fuberlin.wiwiss.pubby.URIRedirector;

/**
 * DM2E Implementation of the resource redirect to an aggregation
 *
 * @author Kai Eckert (kai@informatik.uni-mannheim.de
 */
public class ResourceRedirector implements URIRedirector {

    public String getPageURL(String uri) {
        if (uri.contains("aggregation")) return uri.replaceFirst("aggregation","html/resourcemap");
        return uri.replaceFirst("resource","html/resourcemap");
    }
    public String getDataURL(String uri) {
        if (uri.contains("aggregation")) return uri.replaceFirst("aggregation","resourcemap");
        return uri.replaceFirst("resource","resourcemap");
    }
}
