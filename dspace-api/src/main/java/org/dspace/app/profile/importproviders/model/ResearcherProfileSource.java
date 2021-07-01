/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.importproviders.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The ResearcherProfileSource is a plain object used to give context to a ResearcherProfileProvider.
 * Can hold multiple SourceId couples.
 * ex: [<orcid, orcidId>, <dni, dniId>]
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ResearcherProfileSource {

    public static class SourceId {
        private String source;
        private String id;
        public SourceId(String source, String id) {
            this.source = source;
            this.id = id;
        }
        public String getSource() {
            return source;
        }
        public void setSource(String source) {
            this.source = source;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }

        public String toString() {
            return "SourceId(source=" + this.getSource() + ", id=" + this.getId() + ")";
        }
    }

    private List<SourceId> sources = new ArrayList<SourceId>();

    public ResearcherProfileSource() {
    }

    public ResearcherProfileSource(URI sourceURI) {
        final String[] path = sourceURI.getPath().split("/");
        final String source = isDspace(path) ? "dspace" : path[path.length - 3];
        final String id = path[path.length - 1];
        this.addSource(source, id);
    }

    public ResearcherProfileSource(String source, String id) {
        this.addSource(source, id);
    }

    public List<SourceId> getSources() {
        return sources;
    }

    public void setSources(List<SourceId> sources) {
        this.sources = sources;
    }

    public void addSource(String source, String id) {
        this.getSources().add(new SourceId(source, id));
    }

    public boolean anyMatchSource(String source) {
        return this.sources.stream().anyMatch(s -> s.source.equals(source));
    }

    public Optional<SourceId> selectSource(String source) {
        return this.sources.stream().filter(s -> s.source.equals(source)).findFirst();
    }

    //FIXME: improve way to distinguish between dspace and actually external objects
    private boolean isDspace(String[] path) {
        return "server".equals(path[path.length - 5])
                && "api".equals(path[path.length - 4])
                && "core".equals(path[path.length - 3])
                && "items".equals(path[path.length - 2]);
    }

    public String toString() {
        return "ResearcherProfileSource(sources=" +
                this.getSources().stream()
                .map(s -> s.toString())
                .collect(Collectors.joining(" ")) + ")";
    }



}
