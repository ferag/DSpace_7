/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.reniec;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface PeruExternalService {

    public boolean updateItem(Context context, Item item);

}
