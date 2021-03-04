/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.source.service;
import org.dspace.app.source.ItemSource;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service interface class for the ItemSource object.
 * The implementation of this class is responsible for all business logic
 * calls for the ItemSource object and is autowired by spring
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ItemSourceService {

    public ItemSource getItemSource(Context context, Item item);

}