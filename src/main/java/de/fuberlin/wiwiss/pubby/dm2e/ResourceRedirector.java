package de.fuberlin.wiwiss.pubby.dm2e;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.Dataset;
import de.fuberlin.wiwiss.pubby.URIRedirector;

import java.util.Collections;
import java.util.logging.Logger;

/**
 * DM2E Implementation of the resource redirect to an aggregation
 *
 * @author Kai Eckert (kai@informatik.uni-mannheim.de
 */
public class ResourceRedirector implements URIRedirector {

    private final Logger log = Logger.getLogger(getClass().getName());
    private Configuration config;
    private Dataset dataset;

    public String getPageURL(String uri) {
        if (uri.contains("aggregation")) return uri.replaceFirst("aggregation","html/resourcemap") + "/" + getLatestVersion(uri);
        else if (uri.contains("item")) return uri.replaceFirst("item","html/resourcemap") + "/" + getLatestVersion(uri);
        throw new RuntimeException("Not a valid URI for this redirect: " + uri);
    }
    public String getDataURL(String uri) {
        if (uri.contains("aggregation")) return uri.replaceFirst("aggregation","rdf/resourcemap") + "/" + getLatestVersion(uri);
        else if (uri.contains("item")) return uri.replaceFirst("item","rdf/resourcemap") + "/" + getLatestVersion(uri);
        throw new RuntimeException("Not a valid URI for this redirect: " + uri);
    }

    public void setConfiguration(Configuration config) {
       this.config = config;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    private String getLatestVersion(String uri) {
        log.fine("Find latest version for URI: " + uri);
        uri = uri.substring((config.getWebApplicationBaseURI() +
                dataset.getWebResourcePrefix()).length());
        String query = "select distinct ?g ?date WHERE {\n" +
                "                        graph ?g {\n" +
                "                            <http://data.dm2e.eu/data/" + uri + "> ?p ?o .\n" +
                "?g <http://purl.org/dc/elements/1.1/date>\t?date\t\t    .\n" +
                "}\n" +
                "                    } ORDER BY DESC(?date)";
        log.fine("Query: " + query);
        QueryEngineHTTP endpoint = new QueryEngineHTTP(dataset.getEndpoint(), query);
        if (dataset.getDefaultGraph() != null) {
            endpoint.setDefaultGraphURIs(Collections.singletonList(dataset.getDefaultGraph()));
        }
        ResultSet rs = endpoint.execSelect();
        while (rs.hasNext()) {
            String graph = rs.next().get("g").toString();
            String version = graph.substring(graph.lastIndexOf("/")+1);
            String providerDataset  = uri.substring(uri.indexOf("/"),nthOccurrence(uri,'/',2)+1);

            log.fine("Graph: " + graph + " Version: " + version + " Provider/Dataset: " + providerDataset);
            if (!graph.contains(providerDataset)) {
                log.fine("Sanity check failed, provider ID and dataset ID have to be correct.");
                continue;
            }
            return version;
        }
        return "-1";
    }

    public static int nthOccurrence(String str, char c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1)
            pos = str.indexOf(c, pos+1);
        return pos;
    }
}
