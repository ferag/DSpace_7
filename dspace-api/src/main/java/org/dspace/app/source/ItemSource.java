/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.source;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ItemSource {

    private UUID itemUuid;

    private List<Source> sources = new LinkedList<Source>();

    public UUID getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(UUID itemUuid) {
        this.itemUuid = itemUuid;
    }

    public void addSource(Source source) {
        this.sources.add(source);
    }

    public List<Source> getSources() {
        return this.sources;
    }
}