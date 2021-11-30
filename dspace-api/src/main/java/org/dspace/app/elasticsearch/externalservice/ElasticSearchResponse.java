/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

/**
 *
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class ElasticSearchResponse {
    private int statusCode;
    private Optional<JSONObject> body;

    public void setResponse(HttpResponse httpResponse) throws IOException {
        this.statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            this.body = Optional.of(new JSONObject(IOUtils.toString(httpResponse.getEntity().getContent(),
                StandardCharsets.UTF_8)));
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Optional<JSONObject> getBody() {
        return body;
    }
}
