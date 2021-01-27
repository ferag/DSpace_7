/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * Implementation of {@link CrisLayoutSectionComponent} that gives back
 * a simple line of text, that can either be, for example, html or plain text
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CrisLayoutTextRowComponent implements CrisLayoutSectionComponent {

    private final Integer order;
    private final String contentType;
    private final String content;
    private final String style;


    public CrisLayoutTextRowComponent(String contentType, String content, String style) {
        this(0, contentType,
            content, style);
    }

    public CrisLayoutTextRowComponent(Integer order, String contentType, String content, String style) {
        this.order = order;
        this.content = content;
        this.style = style;
        this.contentType = contentType;
    }

    @Override
    public String getStyle() {
        return style;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public Integer getOrder() {
        return order;
    }
}
