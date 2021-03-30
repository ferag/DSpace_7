/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile;

import java.util.UUID;

import org.dspace.content.Item;
import org.springframework.util.Assert;

/**
 * Object representing a CV entity.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CvEntity {

    private final Item item;

    public CvEntity(Item item) {
        Assert.notNull(item, "A CV entity requires an item");
        this.item = item;
    }

    public UUID getId() {
        return item.getID();
    }

    public Item getItem() {
        return item;
    }
}
