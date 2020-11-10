/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.sunat;

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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SunatRestConnector {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(SunatRestConnector.class);

    public static final String RUC_NUMBER = "numruc";

    private String clientId;
    private String clientSecret;
    private String sunatUrl;

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
            return sendRequestToSunat(id);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private InputStream sendRequestToSunat(String id)
            throws UnsupportedEncodingException, IOException, ClientProtocolException {
        HttpPost httpPost = new HttpPost(sunatUrl);
        httpPost.setHeader("Content-type", "text/plain");
        httpPost.setHeader("Accept", "*/*");
        httpPost.setHeader("Accept-Encoding", "gzip, deflate, br");
        httpPost.setHeader("Connection", "keep-alive");

        JSONObject jsonBody = new JSONObject();
        jsonBody.put(RUC_NUMBER, id);

        StringEntity stringEntity = new StringEntity(jsonBody.toString());
        httpPost.getRequestLine();
        httpPost.setEntity(stringEntity);
        HttpResponse response = httpClient.execute(httpPost);
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

    public String getSunatUrl() {
        return sunatUrl;
    }

    public void setSunatUrl(String sunatUrl) {
        this.sunatUrl = sunatUrl;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}