package de.fuberlin.wiwiss.pubby.servlets;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ResourceDescription;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

/**
 * A servlet for serving the HTML page describing a resource.
 * Invokes a Velocity template.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @version $Id$
 */
public class PageURLServlet extends BaseURLServlet {

    private Logger log = Logger.getLogger(getClass().getName());

	public boolean doGet(MappedResource resource, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws ServletException, IOException {

        log.fine("Get RDF model for resource " + resource.getWebURI());
		Model description = getResourceDescription(resource);
		
		if (description.size() == 0) {
			return false;
		}

        // Add owl:sameAs statements referring to the original dataset URI
//        Resource r = description.getResource(resource.getWebURI());
//        if (resource.getDataset().getAddSameAsStatements()) {
//            r.addProperty(OWL.sameAs, description.createResource(resource.getDatasetURI()));
//        }


        Velocity.setProperty("velocimacro.context.localscope", Boolean.TRUE);
		
        String title=null, comment=null;
        String primaryResourceURI = resource.getDataset().getPrimaryResource(resource.getDatasetURI());
        log.fine("Primary URI: " + primaryResourceURI);
        List resourceDescriptions = new ArrayList();
		Iterator it = resource.getDataset().getPublishedResources(resource.getDatasetURI()).iterator();
        while (it.hasNext()) {
            String uri = (String) it.next();
            log.fine("Publishing description for URI: " + uri);
            MappedResource mapped = config.getMappedResourceFromDatasetURI(uri);
            Resource r = description.getResource(mapped.getWebURI());
            if (mapped.getDataset().getAddSameAsStatements()) {
                r.addProperty(OWL.sameAs, description.createResource(mapped.getDatasetURI()));
            }
            log.fine("Adding description for published resource: " + uri);
            ResourceDescription desc = new ResourceDescription(
                    mapped , description, config);
            resourceDescriptions.add(desc);
            if (mapped.getDatasetURI().equals(primaryResourceURI)) {
                title = desc.getLabel();
                comment = desc.getComment();
            }

        }



        String discoLink = "http://www4.wiwiss.fu-berlin.de/rdf_browser/?browse_uri=" +
				URLEncoder.encode(resource.getWebURI(), "utf-8");
		String tabulatorLink = "http://dig.csail.mit.edu/2005/ajar/ajaw/tab.html?uri=" +
				URLEncoder.encode(resource.getWebURI(), "utf-8");
		String openLinkLink = "http://linkeddata.uriburner.com/ode/?uri=" +
				URLEncoder.encode(resource.getWebURI(), "utf-8");
        String graphiteLink = "http://graphite.ecs.soton.ac.uk/browser/?uri=" +
                URLEncoder.encode(resource.getWebURI(), "utf-8");
        VelocityHelper template = new VelocityHelper(getServletContext(), response);
		Context context = template.getVelocityContext();
		context.put("project_name", config.getProjectName());
		context.put("project_link", config.getProjectLink());
		context.put("uri", resource.getWebURI());
		context.put("server_base", config.getWebApplicationBaseURI());
		context.put("rdf_link", resource.getDataURL());
        context.put("disco_link", discoLink);
        context.put("graphite_link", graphiteLink);
        // context.put("tabulator_link", tabulatorLink);
		// context.put("openlink_link", openLinkLink);
		context.put("sparql_endpoint", resource.getDataset().getDataSource().getEndpointURL());
		context.put("title", title);
		context.put("comment", comment);
        context.put("resources", resourceDescriptions);
        context.put("showLabels", new Boolean(config.showLabels()));
        context.put("sparql_query", getSPARQLQuery(resource));

        try {
			Model metadata = ModelFactory.createDefaultModel();
			Resource documentRepresentation = resource.getDataset().addMetadataFromTemplate( metadata, resource, getServletContext() );
			// Replaced the commented line by the following one because the
			// RDF graph we want to talk about is a specific representation
			// of the data identified by the getDataURL() URI.
			//                                       Olaf, May 28, 2010
			// context.put("metadata", metadata.getResource(resource.getDataURL()));
			context.put("metadata", documentRepresentation);

			Map nsSet = metadata.getNsPrefixMap();
			nsSet.putAll(description.getNsPrefixMap());
			context.put("prefixes", nsSet.entrySet());
			context.put("blankNodesMap", new HashMap());
		}
		catch (Exception e) {
			context.put("metadata", Boolean.FALSE);
		}
	
		template.renderXHTML("page.vm");
		return true;
	}
}
