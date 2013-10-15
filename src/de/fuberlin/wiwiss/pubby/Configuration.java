package de.fuberlin.wiwiss.pubby;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import de.fuberlin.wiwiss.pubby.vocab.CONF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * The server's configuration.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @version $Id$
 */
public class Configuration {
	private final Model model;
	private final Resource config;
	private final PrefixMapping prefixes;
	private final Collection labelProperties;
	private final Collection commentProperties;
	private final Collection imageProperties;
    private final Collection datasets;
    private final Collection externalVocabularyURLs;
    private final VocabularyCache vocabularyCache;

    private String detectedWebBase;

    private final Logger log = Logger.getLogger(getClass().getName());
	
	public Configuration(Model configurationModel) {
		model = configurationModel;
		StmtIterator it = model.listStatements(null, RDF.type, CONF.Configuration);
		if (!it.hasNext()) {
			throw new IllegalArgumentException(
					"No conf:Configuration found in configuration model");
		}
		config = it.nextStatement().getSubject();

		datasets = new ArrayList();
		it = model.listStatements(config, CONF.dataset, (RDFNode) null);
		while (it.hasNext()) {
			datasets.add(new Dataset(it.nextStatement().getResource()));
		}
		labelProperties = new ArrayList();
		it = model.listStatements(config, CONF.labelProperty, (RDFNode) null);
		while (it.hasNext()) {
			labelProperties.add(it.nextStatement().getObject().as(Property.class));
		}
		if (labelProperties.isEmpty()) {
			labelProperties.add(RDFS.label);
			labelProperties.add(DC.title);
			labelProperties.add(model.createProperty("http://xmlns.com/foaf/0.1/name"));
		}
		commentProperties = new ArrayList();
		it = model.listStatements(config, CONF.commentProperty, (RDFNode) null);
		while (it.hasNext()) {
			commentProperties.add(it.nextStatement().getObject().as(Property.class));
		}
		if (commentProperties.isEmpty()) {
			commentProperties.add(RDFS.comment);
			commentProperties.add(DC.description);
		}
		imageProperties = new ArrayList();
		it = model.listStatements(config, CONF.imageProperty, (RDFNode) null);
		while (it.hasNext()) {
			imageProperties.add(it.nextStatement().getObject().as(Property.class));
		}
		if (imageProperties.isEmpty()) {
			imageProperties.add(model.createProperty("http://xmlns.com/foaf/0.1/depiction"));
		}
        externalVocabularyURLs = new ArrayList();
        it = model.listStatements(config, CONF.loadVocabularyFromURL, (RDFNode) null);
        while (it.hasNext()) {
            externalVocabularyURLs.add(it.nextStatement().getObject().asResource().getURI());
        }


        prefixes = new PrefixMappingImpl();
		if (config.hasProperty(CONF.usePrefixesFrom)) {
			it = config.listProperties(CONF.usePrefixesFrom);
			while (it.hasNext()) {
				Statement stmt = it.nextStatement();
				String uri = stmt.getResource().getURI();
				prefixes.setNsPrefixes(FileManager.get().loadModel(uri));
			}
		} else {
			prefixes.setNsPrefixes(model);
		}
		if (prefixes.getNsURIPrefix(CONF.NS) != null) {
			prefixes.removeNsPrefix(prefixes.getNsURIPrefix(CONF.NS));
		}
        this.vocabularyCache = new VocabularyCache(this);
	}

	public MappedResource getMappedResourceFromDatasetURI(String datasetURI) {
		Iterator it = datasets.iterator();
        Dataset bestMatch = null;
        while (it.hasNext()) {
			Dataset dataset = (Dataset) it.next();
			if (dataset.isDatasetURI(datasetURI)) {
                if (bestMatch == null || bestMatch.getPriority() > dataset.getPriority()) {
                    bestMatch = dataset;
                }
			}
		}
        return bestMatch==null?null:bestMatch.getMappedResourceFromDatasetURI(datasetURI, this);
	}

	public MappedResource getMappedResourceFromRelativeWebURI(String relativeWebURI, boolean isResourceURI) {
		log.fine("Mapping resource from relative web URI: " + relativeWebURI);
        Iterator it = datasets.iterator();
        Dataset bestMatch = null;
        while (it.hasNext()) {
			Dataset dataset = (Dataset) it.next();
			MappedResource resource = dataset.getMappedResourceFromRelativeWebURI(
					relativeWebURI, isResourceURI, this);
			if (resource != null) {
				log.fine("   Potentially mapped to " + resource.getDatasetURI() + " (Dataset base: " + resource.getDataset().getDatasetBase() +")");
                if (bestMatch == null || bestMatch.getPriority() > dataset.getPriority()) {
                    bestMatch = dataset;
                }
			}
		}
        if (bestMatch==null) {
            log.fine("   Could not be mapped.");
            return null;
        } else {
            MappedResource resource = bestMatch.getMappedResourceFromRelativeWebURI(
                    relativeWebURI, isResourceURI, this);
            log.fine("   Finally mapped to " + resource.getDatasetURI() + " (Dataset base: " + resource.getDataset().getDatasetBase() +")");
            return resource;
        }

	}

    public boolean isDataURL(String relativeWebURI) {
        Iterator it = datasets.iterator();
        while (it.hasNext()) {
            Dataset dataset = (Dataset) it.next();
            if (relativeWebURI.startsWith(dataset.getWebDataPrefix()) && !dataset.isDataResource()) return true;
        }
        return false;
    }

    public boolean isPageURL(String relativeWebURI) {
        Iterator it = datasets.iterator();
        while (it.hasNext()) {
            Dataset dataset = (Dataset) it.next();
            if (relativeWebURI.startsWith(dataset.getWebPagePrefix())) return true;
        }
        return false;
    }

    public PrefixMapping getPrefixes() {
		return prefixes;
	}

	public Collection getLabelProperties() {
		return labelProperties;
	}
	
	public Collection getCommentProperties() {
		return commentProperties;
	}
	
	public Collection getImageProperties() {
		return imageProperties;
	}
	
	public String getDefaultLanguage() {
		if (!config.hasProperty(CONF.defaultLanguage)) {
			return null;
		}
		return config.getProperty(CONF.defaultLanguage).getString();
	}
	
	public MappedResource getIndexResource() {
		if (!config.hasProperty(CONF.indexResource)) {
			return null;
		}
		return getMappedResourceFromDatasetURI(
				config.getProperty(CONF.indexResource).getResource().getURI());
	}
	
	public String getProjectLink() {
		return config.getProperty(CONF.projectHomepage).getResource().getURI();
	}

    public String getProjectName() {
        return config.getProperty(CONF.projectName).getString();
    }

    public boolean showLabels() {
        if (config.hasProperty(CONF.showLabels)) {
            return config.getProperty(CONF.showLabels).getBoolean();
        } else {
            return false;
        }
    }

    public String getWebApplicationBaseURI() {
        if (config.hasProperty(CONF.webBase)) {
            return config.getProperty(CONF.webBase).getResource().getURI();
        }
        if (detectedWebBase == null) throw new RuntimeException("No web application base set in config file and detected base not available.");
		return detectedWebBase;
	}

    public String getDetectedWebBase() {
        return detectedWebBase;
    }

    public void setDetectedWebBase(String detectedWebBase) {
        this.detectedWebBase = detectedWebBase;
    }

    public VocabularyCache getVocabularyCache() {
        return vocabularyCache;
    }

    public Collection getExternalVocabularyURLs() {
        return externalVocabularyURLs;
    }
}
