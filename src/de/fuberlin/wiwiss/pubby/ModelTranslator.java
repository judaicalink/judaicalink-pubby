package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.*;

import java.io.StringWriter;

/**
 * Translates an RDF model from the dataset into one fit for publication
 * on the server by replacing URIs, adding the correct prefixes etc. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class ModelTranslator {
	private final Model model;
	private final Configuration serverConfig;
	
	public ModelTranslator(Model model, Configuration configuration) {
		this.model = model;
		this.serverConfig = configuration;
	}
	
	public Model getTranslated(MappedResource resource) {
		Model result = ModelFactory.createDefaultModel();
		result.setNsPrefixes(serverConfig.getPrefixes());
        Model prStatements = ModelFactory.createDefaultModel();
        // prStatements.setNsPrefixes(serverConfig.getPrefixes());
        StmtIterator it = model.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Resource s = stmt.getSubject();
			if (s.isURIResource()) {
				s = result.createResource(getPublicURI(s.getURI()));
			}
			Property p = result.createProperty(
					getPublicURI(stmt.getPredicate().getURI()));
			RDFNode o = stmt.getObject();
			if (o.isURIResource()) {
				o = result.createResource(
						getPublicURI(((Resource) o).getURI()));
			}
            if (s.getURI().equals(getPublicURI(resource.getDataset().getPrimaryResource(resource.getDatasetURI())))) {
                prStatements.add(s,p,o);
            }
			result.add(s, p, o);
		}
        StringWriter sw = new StringWriter();
        prStatements.write(sw, "Turtle");
        // String b64graph = getBZip2Base64(sw.toString());
        //b64graph = getBZip2Base64(b64graph);
        // String subject = getPublicURI(resource.getDataset().getWebDataResource(resource.getDatasetURI()));
        // result.add(result.createResource(subject),result.createProperty("http://libre/state"), b64graph);

        return result;
	}


	
	private String getPublicURI(String datasetURI) {
		MappedResource resource = serverConfig.getMappedResourceFromDatasetURI(datasetURI);
		if (resource == null) return datasetURI;
		return resource.getWebURI();
	}
}
