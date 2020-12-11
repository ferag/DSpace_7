/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemCopyService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemCopyService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemCopyServiceImpl implements ItemCopyService {

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BundleService bundleService;

    @Autowired
    private BitstreamStorageService bitstreamStorageService;

    @Override
    public WorkspaceItem copy(Context context, Item itemToCopy, Collection collection)
        throws AuthorizeException, SQLException, IOException {

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);

        Item item = workspaceItem.getItem();
        copyMetadata(context, itemToCopy, item);
        copyBundles(context, itemToCopy, item);

        return workspaceItem;
    }

    private void copyMetadata(Context context, Item itemToCopy, Item item) throws SQLException {
        itemService.clearMetadata(context, item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (MetadataValue metadata : itemToCopy.getMetadata()) {
            itemService.addMetadata(context, item, metadata.getMetadataField(), metadata.getLanguage(),
                metadata.getValue(), metadata.getAuthority(), metadata.getConfidence());
        }
    }

    private void copyBundles(Context context, Item itemToCopy, Item item)
        throws SQLException, AuthorizeException, IOException {
        for (Bundle bundle : itemToCopy.getBundles()) {
            Bundle bundleCopy = bundleService.create(context, item, bundle.getName());

            Bitstream primaryBitstream = bundle.getPrimaryBitstream();
            if (primaryBitstream != null) {
                Bitstream primaryBitstreamCopy = bitstreamStorageService.clone(context, primaryBitstream);
                bundleCopy.setPrimaryBitstreamID(primaryBitstreamCopy);
            }

            for (Bitstream bitstream : bundle.getBitstreams()) {
                Bitstream bitstreamCopy = bitstreamStorageService.clone(context, bitstream);
                bundleService.addBitstream(context, bundleCopy, bitstreamCopy);
            }

            bundleService.update(context, bundleCopy);
        }
    }
}
