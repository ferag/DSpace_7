/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.authority.service.AuthorityValueService.GENERATE;
import static org.dspace.authority.service.AuthorityValueService.REFERENCE;

import java.sql.SQLException;

import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.PostShadowCopyCreationAction;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link PostShadowCopyCreationAction} that add the prefix will be
 * generated::SHADOW:: to all the authorities set in the item's metadata.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ReplaceShadowCopyAuthorityAction implements PostShadowCopyCreationAction {

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @Override
    public void process(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException {
        for (MetadataValue metadataValue : workspaceItem.getItem().getMetadata()) {
            String authority = metadataValue.getAuthority();
            if (isNotBlank(authority) && notStartsWithWillBePrefix(authority) && isItemAuthority(metadataValue)) {
                metadataValue.setAuthority(AuthorityValueService.REFERENCE + "SHADOW::" + authority);
            }
        }
    }

    private boolean notStartsWithWillBePrefix(String authority) {
        return !authority.startsWith(REFERENCE) && !authority.startsWith(GENERATE);
    }

    private boolean isItemAuthority(MetadataValue value) {
        return choiceAuthorityService.isItemAuthority(value.getMetadataField().toString('_'));
    }

}
