/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.source.service;

import java.util.List;

import org.dspace.content.Item;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ItemSourceService {

    public List<String> getMatchedMetadata(Item item1, Item item2);

}