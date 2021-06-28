/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.util.UUIDUtils;

/**
 * Utility class used to go through {@link ExternalDataObject} instances
 * that have been composed by merging many sources.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class MergedExternalDataObject {
    private final ExternalDataObject externalDataObject;

    private MergedExternalDataObject(ExternalDataObject externalDataObject) {
        this.externalDataObject = externalDataObject;
    }

    public static MergedExternalDataObject from(ExternalDataObject externalDataObject) {
        return new MergedExternalDataObject(externalDataObject);
    }

    /**
     * returns whether or not inner {@link ExternalDataObject} is the result of a merge.
     *
     * @return
     */
    public boolean isMerged() {
        return externalDataObject.getId().startsWith("merged+");
    }

    /**
     * @return the UUID of the {@link org.dspace.content.DSpaceObject} that contributed the
     * creation of inner {@link ExternalDataObject}, if present.
     */
    public Optional<UUID> getDSpaceObjectUUID() {
        if (!isMerged()) {
            return Optional.empty();
        }
        String[] sources = mergeParts(externalDataObject.getSource());
        String[] ids = mergeParts(externalDataObject.getId());
        for (int i = 0; i < sources.length; i++) {
            if (StringUtils.equalsIgnoreCase("dspace", sources[i])) {
                String uuid = ids[1];
                return Optional.ofNullable(UUIDUtils.fromString(uuid));
            }
        }
        return Optional.empty();
    }

    private String[] mergeParts(String source) {
        String[] split = StringUtils.split(source, "+");
        return split;
    }
}
