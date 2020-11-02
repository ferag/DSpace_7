/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.renacyt;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.Logger;

/**
 * This class deals with logic management to connect to the RENACYT outdoor service
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class RenacytRestConnector {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(RenacytRestConnector.class);

    private static final String TYPE_ID = "tipoId=1";

    private static final String DNI = "numId=";

    private String clientId;
    private String clientSecret;

    private String renacytUrl;

    private HttpClient httpClient;

    @PostConstruct
    private void setup() {

        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);
        provider.setCredentials(AuthScope.ANY, credentials);

        this.httpClient = HttpClientBuilder.create()
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .setDefaultCredentialsProvider(provider)
            .build();
    }

    public InputStream get(String id) {
        try {
            return sendRequestToRenacyt(id);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private InputStream sendRequestToRenacyt(String id)
            throws UnsupportedEncodingException, IOException, ClientProtocolException {

        StringBuilder path = new StringBuilder();
        path.append("?").append(TYPE_ID).append("&").append(DNI).append(id);

        HttpGet httpGet = new HttpGet(renacytUrl + path);
        httpGet.setHeader("Content-type", "text/plain");
        httpGet.setHeader("Accept", "*/*");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
        httpGet.setHeader("Connection", "keep-alive");

        HttpResponse response = httpClient.execute(httpGet);
        return response.getEntity().getContent();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getRenacytUrl() {
        return renacytUrl;
    }

    public void setRenacytUrl(String renacytUrl) {
        this.renacytUrl = renacytUrl;
    }
}