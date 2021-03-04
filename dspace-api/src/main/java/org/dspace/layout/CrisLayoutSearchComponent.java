/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * An implementation of {@link CrisLayoutSectionComponent} that model the Search
 * section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisLayoutSearchComponent implements CrisLayoutSectionComponent {

    private String discoveryConfigurationName;

    private String style;

    private String searchType;

    /**
     * @return the discoveryConfigurationName
     */
    public String getDiscoveryConfigurationName() {
        return discoveryConfigurationName;
    }

    /**
     * @param discoveryConfigurationName the discoveryConfigurationName to set
     */
    public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
        this.discoveryConfigurationName = discoveryConfigurationName;
    }

    @Override
    public String getStyle() {
        return this.style;
    }

    /**
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }

    public String getSearchType() {
        return searchType;
    }

    /**
     *
     * @param searchType the type of search, can be 'basic', with one input field,
     *                   or 'advanced' with many input fields combining defined filters for
     *                   discovery configuration set.
     */
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
}
