/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;

import java.io.IOException;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface ElasticsearchConnector {

    public void create(String json) throws IOException;

    public void update(String json);

    public void delete(String index, String id) throws IOException;

}