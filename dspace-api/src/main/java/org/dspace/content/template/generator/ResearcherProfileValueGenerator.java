/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import java.sql.SQLException;
import java.util.Optional;

import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link TemplateValueGenerator} that returns a metadata
 * value with the name of researcher profile related to the current user.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResearcherProfileValueGenerator implements TemplateValueGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResearcherProfileValueGenerator.class);

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Override
    public MetadataValueVO generator(Context context, Item targetItem, Item templateItem, String extraParams) {
        return findCurrentUserProfile(context)
            .map(profile -> new MetadataValueVO(profile.getFullName(), profile.getItemId().toString()))
            .orElseGet(() -> new MetadataValueVO(""));
    }

    private Optional<ResearcherProfile> findCurrentUserProfile(Context context) {
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(researcherProfileService.findById(context, currentUser.getID()));
        } catch (SQLException | AuthorizeException e) {
            LOGGER.error(e.getMessage(), e);
            return Optional.empty();
        }

    }

}
