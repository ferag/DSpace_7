/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

/**
 * Defines the contract to generate custom Indexes values in a dynamic fashion for Elasticsearch.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ElasticsearchIndexTemplateValueGenerator {

    public String generator(String index);

    public String getRegex();

}