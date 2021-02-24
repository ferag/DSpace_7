/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *  http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * this is an extensionof {@link CrisLayoutTopComponent} that allows display of
 * set discovery query on many columns.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class CrisLayoutMultiColumnTopComponent extends CrisLayoutTopComponent {

    private List<Column> columns = new ArrayList<>();

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public static class Column {
        private String style;
        private String metadataField;
        private String titleKey;

        public String getStyle() {
            return style;
        }

        /**
         * setter for column style (i.e. width)
         * @param style
         */
        public void setStyle(String style) {
            this.style = style;
        }

        public String getMetadataField() {
            return metadataField;
        }

        /**
         * metadata to which the value shall be displayed in column
         * @param metadataField
         */
        public void setMetadataField(String metadataField) {
            this.metadataField = metadataField;
        }

        public String getTitleKey() {
            return titleKey;
        }

        /**
         * key for the title that will be displayed. If not set metadata name will be used as title
         * @param titleKey
         */
        public void setTitleKey(String titleKey) {
            this.titleKey = titleKey;
        }
    }
}
