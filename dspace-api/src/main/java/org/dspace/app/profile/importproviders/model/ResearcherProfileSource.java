/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.importproviders.model;

import java.net.URI;

/**
 * The ResearcherProfileSource is a plain object used to give context to a ResearcherProfileProvider.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ResearcherProfileSource {

    private final String source;

    private final String id;

    public ResearcherProfileSource(URI source) {

        final String[] path = source.getPath().split("/");

        this.source = isDspace(path) ? "dspace" : path[path.length - 3];

        this.id = path[path.length - 1];
    }

    public ResearcherProfileSource(String source, String id) {

        this.source = source;

        this.id = id;
    }

    public ResearcherProfileSource(String id) {

        this.source = null;

        this.id = id;
    }

    //FIXME: improve way to distinguish between dspace and actually external objects
    private boolean isDspace(String[] path) {
        return "server".equals(path[path.length - 5])
                && "api".equals(path[path.length - 4])
                && "core".equals(path[path.length - 3])
                && "items".equals(path[path.length - 2]);
    }

    public String getSource() {
        return source;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return "ResearcherProfileSource(source=" + this.getSource() + ", id=" + this.getId() + ")";
    }

}
