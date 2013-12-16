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
        else if (uri.contains("item")) return uri.replaceFirst("item","html/resourcemap");
        throw new RuntimeException("Not a valid URI for this redirect: " + uri);
    }
    public String getDataURL(String uri) {
        if (uri.contains("aggregation")) return uri.replaceFirst("aggregation","rdf/resourcemap");
        else if (uri.contains("item")) return uri.replaceFirst("item","rdf/resourcemap");
        throw new RuntimeException("Not a valid URI for this redirect: " + uri);
    }
}
