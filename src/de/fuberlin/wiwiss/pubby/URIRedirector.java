package de.fuberlin.wiwiss.pubby;

/**
 * Interface for custom redirect mechanisms
 *
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @version $Id$
 */
public interface URIRedirector {
    public String getPageURL(String uri);
    public String getDataURL(String uri);
}
