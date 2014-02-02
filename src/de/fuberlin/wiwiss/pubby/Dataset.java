package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.XSD;
import de.fuberlin.wiwiss.pubby.vocab.CONF;
import de.fuberlin.wiwiss.pubby.vocab.META;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The server's configuration.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Hannes Mühleisen
 * @author Olaf Hartig
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @version $Id$
 */
public class Dataset {
	private final Model model;
	private final Resource dsConfig;
	private final DataSource dataSource;
	private final Pattern datasetURIPattern;
	private final char[] fixUnescapeCharacters;
	private final Resource rdfDocumentMetadataTemplate;
	private final String metadataTemplate;
	private final static String metadataPlaceholderURIPrefix = "about:metadata:";
	private Calendar currentTime;
	private Resource currentDocRepr;
    private final List sparqlMappings;
    private URIRedirector redirector;
    private String endpoint;
    private String defaultGraph;
    private Configuration config;

    private Logger log = Logger.getLogger(getClass().getName());
	
	public Dataset(Configuration config, Resource dsConfig) {
        log.fine("Loading dataset...");
        model = dsConfig.getModel();
		this.dsConfig = dsConfig;
        this.config = config;
		if (dsConfig.hasProperty(CONF.datasetURIPattern)) {
			datasetURIPattern = Pattern.compile(
					dsConfig.getProperty(CONF.datasetURIPattern).getString());
		} else {
			datasetURIPattern = Pattern.compile(".*");
		}
		if (dsConfig.hasProperty(CONF.fixUnescapedCharacters)) {
			String chars = dsConfig.getProperty(CONF.fixUnescapedCharacters).getString();
			fixUnescapeCharacters = new char[chars.length()];
			for (int i = 0; i < chars.length(); i++) {
				fixUnescapeCharacters[i] = chars.charAt(i);
			}
		} else {
			fixUnescapeCharacters = new char[0];
		}
		if (dsConfig.hasProperty(CONF.rdfDocumentMetadata)) {
			rdfDocumentMetadataTemplate = dsConfig.getProperty(CONF.rdfDocumentMetadata).getResource();
		} else {
			rdfDocumentMetadataTemplate = null;
		}
		if (dsConfig.hasProperty(CONF.metadataTemplate)) {
			metadataTemplate = dsConfig.getProperty(CONF.metadataTemplate).getString();
		} else {
			metadataTemplate = null;
		}
		if (dsConfig.hasProperty(CONF.sparqlEndpoint)) {
			endpoint = null;
            if (dsConfig.getProperty(CONF.sparqlEndpoint).getObject().isResource()) {
                endpoint = dsConfig.getProperty(CONF.sparqlEndpoint).getResource().getURI();
            } else {
                endpoint = config.getDefaultEndpoint();
            }
            if (endpoint==null) {
                log.severe("No endpoint configured for dataset: " + getDatasetBase());
                throw new RuntimeException("No default endpoint and no dataset endpoint has been defined!");
            } else {
                log.fine("Endpoint: " + endpoint);
            }
            defaultGraph = dsConfig.hasProperty(CONF.sparqlDefaultGraph)
					? dsConfig.getProperty(CONF.sparqlDefaultGraph).getResource().getURI()
					: null;
			dataSource = new RemoteSPARQLDataSource(endpoint, defaultGraph, this);
		} else {
			Model data = ModelFactory.createDefaultModel();
			StmtIterator it = dsConfig.listProperties(CONF.loadRDF);
			while (it.hasNext()) {
				Statement stmt = it.nextStatement();
				FileManager.get().readModel(data, stmt.getResource().getURI());
			}
			dataSource = new ModelDataSource(data);
		}
        sparqlMappings = new ArrayList();
        StmtIterator it = model.listStatements(dsConfig, CONF.useSparqlMapping, (RDFNode) null);
        while (it.hasNext()) {
            sparqlMappings.add(new SparqlMapping(it.nextStatement().getResource()));
        }
        redirector = null;
        if (dsConfig.hasProperty(CONF.customRedirect)) {
            try {
                redirector = (URIRedirector) Class.forName(dsConfig.getProperty(CONF.customRedirect).getString()).newInstance();
                redirector.setConfiguration(config);
                redirector.setDataset(this);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
	}

	public boolean isDatasetURI(String uri) {
		return uri.startsWith(getDatasetBase()) 
				&& datasetURIPattern.matcher(uri.substring(getDatasetBase().length())).matches();
	}
	
	public MappedResource getMappedResourceFromDatasetURI(String datasetURI, Configuration configuration) {
		return new MappedResource(
				escapeURIDelimiters(datasetURI.substring(getDatasetBase().length())),
				datasetURI,
				configuration,
				this);
	}

	public MappedResource getMappedResourceFromRelativeWebURI(String relativeWebURI, 
			boolean isResourceURI, Configuration configuration) {
		if (isResourceURI) {
			if (!"".equals(getWebResourcePrefix())) {
				if (!relativeWebURI.startsWith(getWebResourcePrefix())) {
					return null;
				}
				relativeWebURI = relativeWebURI.substring(getWebResourcePrefix().length());
			}
		}  else {
            if (relativeWebURI.startsWith(getWebDataPrefix())) {
                relativeWebURI = relativeWebURI.substring(getWebDataPrefix().length());
            } else if (relativeWebURI.startsWith(getWebPagePrefix())) {
                relativeWebURI = relativeWebURI.substring(getWebPagePrefix().length());
            } else {
                return null;
            }

        }
		relativeWebURI = fixUnescapedCharacters(relativeWebURI);
		if (!datasetURIPattern.matcher(relativeWebURI).matches()) {
			return null;
		}
		return new MappedResource(
				relativeWebURI,
				getDatasetBase() + unescapeURIDelimiters(relativeWebURI),
				configuration,
				this);
	}

    public String getDatasetBase() {
        return dsConfig.getProperty(CONF.datasetBase).getResource().getURI();
    }
    public int getPriority() {
        return getIntConfigValue(CONF.priority, 0);
    }


    public boolean getAddSameAsStatements() {
		return getBooleanConfigValue(CONF.addSameAsStatements, false);
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}

    public String getEndpoint() {
        return endpoint;
    }

    public String getDefaultGraph() {
        return defaultGraph;
    }
	
	public boolean redirectRDFRequestsToEndpoint() {
		return getBooleanConfigValue(CONF.redirectRDFRequestsToEndpoint, false);
	}

    public String getWebResourcePrefix() {
        if (dsConfig.hasProperty(CONF.webResourcePrefix)) {
            return dsConfig.getProperty(CONF.webResourcePrefix).getString();
        }
        return "";
    }
    public String getWebDataPrefix() {
        if (dsConfig.hasProperty(CONF.webDataPrefix)) {
            return dsConfig.getProperty(CONF.webDataPrefix).getString();
        }
        return "data/";
    }

    public String getWebPagePrefix() {
        if (dsConfig.hasProperty(CONF.webPagePrefix)) {
            return dsConfig.getProperty(CONF.webPagePrefix).getString();
        }
        return "page/";
    }

    public URIRedirector getCustomRedirector() {
        return redirector;
    }

    public boolean hasCustomRedirect() {
        return redirector!=null;
    }

    public boolean isDataResource() {
        return getWebDataPrefix().equals(getWebResourcePrefix());
    }

    public void addDocumentMetadata(Model document, Resource documentResource) {
		if (rdfDocumentMetadataTemplate == null) {
			return;
		}
		StmtIterator it = rdfDocumentMetadataTemplate.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			document.add(documentResource, stmt.getPredicate(), stmt.getObject());
		}
		it = this.model.listStatements(null, null, rdfDocumentMetadataTemplate);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (stmt.getPredicate().equals(CONF.rdfDocumentMetadata)) {
				continue;
			}
			document.add(stmt.getSubject(), stmt.getPredicate(), documentResource);
		}
	}
	
	public Resource addMetadataFromTemplate(Model document, MappedResource describedResource, ServletContext context) {
		if (metadataTemplate == null) {
			return null;
		}
		
		currentTime = Calendar.getInstance();
		
		// add metadata from templates
		Model tplModel = ModelFactory.createDefaultModel();
		String tplPath = context.getRealPath("/") + "/WEB-INF/templates/" + metadataTemplate;
		FileManager.get().readModel( tplModel, tplPath, FileUtils.guessLang(tplPath,"N3") );

		// iterate over template statements to replace placeholders
		Model metadata = ModelFactory.createDefaultModel();
		currentDocRepr = metadata.createResource();
		StmtIterator it = tplModel.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Resource subj = stmt.getSubject();
			Property pred = stmt.getPredicate();
			RDFNode  obj  = stmt.getObject();
			
			try {
				if (subj.toString().contains(metadataPlaceholderURIPrefix)){
					subj = (Resource) parsePlaceholder(subj, describedResource, context);
					if (subj == null) {
						// create a unique blank node with a fixed id.
						subj = model.createResource(new AnonId(String.valueOf(stmt.getSubject().hashCode())));
					}
				}
				
				if (obj.toString().contains(metadataPlaceholderURIPrefix)){
					obj = parsePlaceholder(obj, describedResource, context);
				}
				
				// only add statements with some objects
				if (obj != null) {
					stmt = metadata.createStatement(subj,pred,obj);
					metadata.add(stmt);
				}
			} catch (Exception e) {
				// something went wrong, oops - lets better remove the offending statement
				metadata.remove(stmt);
				e.printStackTrace();
			}
		}
		
		// remove blank nodes that don't have any properties
		boolean changes = true;
		while ( changes ) {
			changes = false;
			StmtIterator stmtIt = metadata.listStatements();
			List remList = new ArrayList();
			while (stmtIt.hasNext()) {
				Statement s = stmtIt.nextStatement();
				if (    s.getObject().isAnon()
				     && ! ((Resource) s.getObject().as(Resource.class)).listProperties().hasNext() ) {
					remList.add(s);
					changes = true;
				}
			}
			metadata.remove(remList);
		}

		if (document != null) {
			document.add( metadata );
		}

		return currentDocRepr;
	}
	
	private RDFNode parsePlaceholder(RDFNode phRes, MappedResource describedResource, ServletContext context) {
		String phURI = phRes.asNode().getURI();
		// get package name and placeholder name from placeholder URI
		phURI = phURI.replace(metadataPlaceholderURIPrefix, "");
		String phPackage = phURI.substring(0, phURI.indexOf(":")+1);
		String phName = phURI.replace(phPackage, "");
		phPackage = phPackage.replace(":", "");
		
		if (phPackage.equals("runtime")) {
			// <about:metadata:runtime:query> - the SPARQL Query used to get the RDF Graph
			if (phName.equals("query")) {
				RemoteSPARQLDataSource ds = (RemoteSPARQLDataSource) describedResource.getDataset().getDataSource();
				return model.createTypedLiteral(ds.getPreviousDescribeQuery());
			}
			// <about:metadata:runtime:time> - the current time
			if (phName.equals("time")) {
				return model.createTypedLiteral(currentTime);
			}
			// <about:metadata:runtime:graph> - URI of the graph
			if (phName.equals("graph")) {
				// Replaced the commented line by the following one because the
				// RDF graph we want to talk about is a specific representation
				// of the data identified by the getDataURL() URI.
				//                                       Olaf, May 28, 2010
				// return model.createResource(describedResource.getDataURL());
				return currentDocRepr;
			}
			// <about:metadata:runtime:data> - URI of the data
			if (phName.equals("data")) {
				return model.createResource(describedResource.getDataURL());
			}
			// <about:metadata:runtime:resource> - URI of the resource
			if (phName.equals("resource")) {
				return model.createResource(describedResource.getWebURI());
			}
		}
		
		// <about:metadata:config:*> - The configuration parameters
		if (phPackage.equals("config")) {
			// look for requested property in the dataset config
			Property p  = model.createProperty(CONF.NS + phName);
			if (dsConfig.hasProperty(p))
				return dsConfig.getProperty(p).getObject();
			
			// find pointer to the global configuration set...
			StmtIterator it = dsConfig.getModel().listStatements(null, CONF.dataset, dsConfig);
			Statement ptrStmt = it.nextStatement();
			if (ptrStmt == null) return null;
			
			// look in global config if nothing found so far
			Resource globalConfig = ptrStmt.getSubject();
			if (globalConfig.hasProperty(p))
				return globalConfig.getProperty(p).getObject();
		}
		
		// <about:metadata:metadata:*> - The metadata provided by users
		if (phPackage.equals("metadata")) {
			// look for requested property in the dataset config
			Property p  = model.createProperty(META.NS + phName);
			if (dsConfig.hasProperty(p))
				return dsConfig.getProperty(p).getObject();
			
			// find pointer to the global configuration set...
			StmtIterator it = dsConfig.getModel().listStatements(null, CONF.dataset, dsConfig);
			Statement ptrStmt = it.nextStatement();
			if (ptrStmt == null) return null;
			
			// look in global config if nothing found so far
			Resource globalConfig = ptrStmt.getSubject();
			if (globalConfig.hasProperty(p))
				return globalConfig.getProperty(p).getObject();
		}

		return model.createResource(new AnonId(String.valueOf(phRes.hashCode())));
	}
	
	private boolean getBooleanConfigValue(Property property, boolean defaultValue) {
		if (!dsConfig.hasProperty(property)) {
			return defaultValue;
		}
		Literal value = dsConfig.getProperty(property).getLiteral();
		if (XSD.xboolean.equals(value.getDatatype())) {
			return value.getBoolean();
		}
		return "true".equals(value.getString());
	}

    private int getIntConfigValue(Property property, int defaultValue) {
        if (!dsConfig.hasProperty(property)) {
            return defaultValue;
        }
        Literal value = dsConfig.getProperty(property).getLiteral();
        if (XSD.integer.equals(value.getDatatype())) {
            return value.getInt();
        }
        try {
            int result = Integer.parseInt(value.getString());
            return result;
        } catch (NumberFormatException nfe) {
            log.warning("Could not parse integer value: " + value + " of property: " + property.getLocalName());
            return defaultValue;
        }
    }

	private String fixUnescapedCharacters(String uri) {
		if (fixUnescapeCharacters.length == 0) {
			return uri;
		}
		StringBuffer encoded = new StringBuffer(uri.length() + 4);
		for (int charIndex = 0; charIndex < uri.length(); charIndex++) {
			boolean encodeThis = false;
			if ((int) uri.charAt(charIndex) > 127) {
				encodeThis = true;
			}
			for (int i = 0; i < fixUnescapeCharacters.length; i++) {
				if (uri.charAt(charIndex) == fixUnescapeCharacters[i]) {
					encodeThis = true;
					break;
				}
			}
			if (encodeThis) {
				encoded.append('%');
				int b = (int) uri.charAt(charIndex);
				encoded.append(Integer.toString(b, 16).toUpperCase());
			} else {
				encoded.append(uri.charAt(charIndex));
			}
		}
		return encoded.toString();
	}

	private String escapeURIDelimiters(String uri) {
		return uri.replaceAll("#", "%23").replaceAll("\\?", "%3F");
	}
	
	private String unescapeURIDelimiters(String uri) {
		return uri.replaceAll("%23", "#").replaceAll("%3F", "?");
	}

    public List getSparqlMappings() {
        return sparqlMappings;
    }

    public String getSparqlQuery(String uri) {
        Iterator it = getSparqlMappings().iterator();
        while (it.hasNext()) {
            SparqlMapping sm = (SparqlMapping) it.next();
            if (sm.isMappedURI(uri)) {
                return sm.getQuery(uri);
            }
        }
        return "DESCRIBE <" + uri + ">";
    }

    public List getPublishedResources(String uri) {
        Iterator it = getSparqlMappings().iterator();
        while (it.hasNext()) {
            SparqlMapping sm = (SparqlMapping) it.next();
            if (sm.isMappedURI(uri)) {
                return sm.getPublishedResources(uri);
            }
        }
        List result = new ArrayList();
        result.add(uri);
        return result;
    }

    public String getPrimaryResource(String uri) {
        log.fine("Detecting primary resource for URI: " + uri);
        Iterator it = getSparqlMappings().iterator();
        while (it.hasNext()) {
            SparqlMapping sm = (SparqlMapping) it.next();
            if (sm.isMappedURI(uri)) {
                return sm.getPrimaryResource(uri);
            }
        }
        return uri;
    }
    public String getWebDataResource(String uri) {
        log.fine("Detecting web data resource for URI: " + uri);
        Iterator it = getSparqlMappings().iterator();
        while (it.hasNext()) {
            SparqlMapping sm = (SparqlMapping) it.next();
            if (sm.isMappedURI(uri)) {
                return sm.getWebDataResource(uri);
            }
        }
        return uri;
    }

    public class SparqlMapping {
        private Pattern uriPattern;
        private String sparqlQuery;
        private List publishResources = new ArrayList();

        public String getPrimaryResource(String uri) {
            return uriPattern.matcher(uri).replaceAll(primaryResource);
        }
        public String getWebDataResource(String uri) {
            return uriPattern.matcher(uri).replaceAll(webDataResource);
        }

        private String primaryResource, webDataResource;

        public SparqlMapping(Resource mapping) {
            this.sparqlQuery = mapping.getProperty(CONF.sparqlQuery).getString();
            this.uriPattern = Pattern.compile(mapping.getProperty(CONF.uriPattern).getString());
            this.primaryResource = mapping.getProperty(CONF.primaryResource).getString();
            if (mapping.getProperty(CONF.webDataResource)!=null) {
                this.webDataResource = mapping.getProperty(CONF.webDataResource).getString();
            }
            StmtIterator it = mapping.listProperties(CONF.publishResources);
            while (it.hasNext()) {
                Statement stmt = it.nextStatement();
                publishResources.add(stmt.getString());
            }

        }

        public List getPublishedResources(String uri) {
            List result = new ArrayList();
            Iterator it = publishResources.iterator();
            while (it.hasNext()) {
                result.add(uriPattern.matcher(uri).replaceAll((String) it.next()));
            }
            return result;
        }

        public String getQuery(String uri) {
            return uriPattern.matcher(uri).replaceAll(sparqlQuery);
        }

        public boolean isMappedURI(String uri) {
            return uriPattern.matcher(uri).matches();
        }
    }
}
