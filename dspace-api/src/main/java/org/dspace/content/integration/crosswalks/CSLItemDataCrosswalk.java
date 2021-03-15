/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

import de.undercouch.citeproc.CSL;
import org.apache.commons.io.IOUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemExportCrosswalk} to serialize the given items
 * using the CSL processor with the configured style and producing the result in
 * the given output format.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CSLItemDataCrosswalk implements ItemExportCrosswalk {

    @Autowired
    private ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    private String mimeType;

    private String style;

    private String format;

    private String fileName;

    private String entityType;

    private CrosswalkMode crosswalkMode;

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.ITEM && isExpectedEntityType((Item) dso);
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        this.disseminate(context, Arrays.asList(dso).iterator(), out);
    }

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        DSpaceListItemDataProvider dSpaceListItemDataProvider = getDSpaceListItemDataProviderInstance();

        while (dsoIterator.hasNext()) {
            DSpaceObject dso = dsoIterator.next();

            if (!canDisseminate(context, dso)) {
                throw new CrosswalkObjectNotSupported(
                    "CSLItemDataCrosswalk can only crosswalk a " + entityType + " item.");
            }

            dSpaceListItemDataProvider.processItem((Item) dso);
        }

        if (getMIMEType() != null && getMIMEType().startsWith("application/json")) {
            print(out, dSpaceListItemDataProvider.toJson());
        } else {
            CSL citeproc = new CSL(dSpaceListItemDataProvider, getStyle());
            citeproc.setOutputFormat(format);
            citeproc.registerCitationItems(dSpaceListItemDataProvider.getIds());
            print(out, citeproc.makeBibliography().makeString());
        }

    }

    private String getStyle() throws IOException {
        return CSL.supportsStyle(style) ? style : readXmlStyleContent();
    }

    private String readXmlStyleContent() throws IOException {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File styleFile = new File(parent, style);
        if (!styleFile.exists()) {
            throw new FileNotFoundException("Could not find style in " + styleFile.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(styleFile)) {
            return IOUtils.toString(fis, Charset.defaultCharset());
        }
    }

    private void print(OutputStream out, String value) {
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.print(value);
        }
    }

    @Override
    public String getMIMEType() {
        return mimeType;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    private DSpaceListItemDataProvider getDSpaceListItemDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }

    private boolean isExpectedEntityType(Item item) {
        String relationshipType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return Objects.equals(relationshipType, entityType);
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setCrosswalkMode(CrosswalkMode crosswalkMode) {
        this.crosswalkMode = crosswalkMode;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public CrosswalkMode getCrosswalkMode() {
        return Optional.ofNullable(this.crosswalkMode).orElse(ItemExportCrosswalk.super.getCrosswalkMode());
    }

    @Override
    public Optional<String> getEntityType() {
        return Optional.ofNullable(entityType);
    }

}
