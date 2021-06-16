/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

/**
 * This class deals with logic management to connect to the Elasticsearch external service
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchConnectorImpl implements ElasticsearchConnector {

    private String user;
    private String password;
    private String url;

    private HttpClient httpClient;

    @PostConstruct
    @SuppressWarnings("deprecation")
    private void setup() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        // set credentials
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
        provider.setCredentials(AuthScope.ANY, credentials);

        //disable ssl verification
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        });
        SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(builder.build(),
                                               SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        this.httpClient = HttpClients.custom()
                                     .setSSLSocketFactory(sslSF)
                                     .setDefaultCredentialsProvider(provider).build();
    }

    @Override
    public HttpResponse create(String json, String index, UUID docId) throws IOException {
        try {
            HttpPost httpPost = new HttpPost(url + index + "/_doc/" + docId);
            httpPost.setHeader("Content-type", "application/json; charset=UTF-8");
            httpPost.setEntity(new StringEntity(json));
            return httpClient.execute(httpPost);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public HttpResponse update(String json, String index, UUID docId) {
        try {
            HttpPost httpPost = new HttpPost(url + index + "/_doc/" + docId);
            httpPost.setHeader("Content-type", "application/json; charset=UTF-8");
            httpPost.setEntity(new StringEntity(json));
            return httpClient.execute(httpPost);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public HttpResponse delete(String index, UUID docId) throws IOException {
        try {
            HttpDelete httpDelete = new HttpDelete(url + index + "/_doc/" + docId);
            return httpClient.execute(httpDelete);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public HttpResponse searchByIndexAndDoc(String index, UUID docId) throws IOException {
        try {
            HttpGet httpGet = new HttpGet(url + index + "/_doc/" + docId);
            return httpClient.execute(httpGet);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public HttpResponse deleteIndex(String index) throws IOException {
        try {
            HttpDelete httpDelete = new HttpDelete(url + index);
            return httpClient.execute(httpDelete);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

}