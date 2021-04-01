/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.dspace;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Optional;
import java.util.UUID;

import org.dspace.external.model.ExternalDataObject;
import org.junit.Test;

/**
 * Unit tests for {@link MergedExternalDataObject}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class MergedExternalDataObjectTest {

    @Test
    public void notMerged() {
        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource("source");
        externalDataObject.setId("id");

        boolean merged = MergedExternalDataObject.from(externalDataObject).isMerged();

        assertThat(merged, is(false));
    }

    @Test
    public void mergedWithDspaceSource() {

        UUID dspaceSourceId = UUID.randomUUID();

        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource("merged::orcid+dspace");
        externalDataObject.setId("merged::0000-1111-2222-3333+" + dspaceSourceId.toString());

        MergedExternalDataObject mergedExternalDataObject = MergedExternalDataObject.from(externalDataObject);

        assertThat(mergedExternalDataObject.isMerged(), is(true));
        assertThat(mergedExternalDataObject.getDSpaceObjectUUID(), is(Optional.of(dspaceSourceId)));
    }

    @Test
    public void mergedWithDspaceSourceButInvalidId() {

        String dspaceSourceId = "invalid";

        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource("merged::orcid+dspace");
        externalDataObject.setId("merged::0000-1111-2222-3333+" + dspaceSourceId);

        MergedExternalDataObject mergedExternalDataObject = MergedExternalDataObject.from(externalDataObject);

        assertThat(mergedExternalDataObject.isMerged(), is(true));
        assertThat(mergedExternalDataObject.getDSpaceObjectUUID(), is(Optional.empty()));
    }

    @Test
    public void mergedWithoutDspaceSource() {

        ExternalDataObject externalDataObject = new ExternalDataObject();
        externalDataObject.setSource("merged::orcid+reniec");
        externalDataObject.setId("merged::0000-1111-2222-3333+123456");

        MergedExternalDataObject mergedExternalDataObject = MergedExternalDataObject.from(externalDataObject);

        assertThat(mergedExternalDataObject.isMerged(), is(true));
        assertThat(mergedExternalDataObject.getDSpaceObjectUUID(), is(Optional.empty()));
    }
}