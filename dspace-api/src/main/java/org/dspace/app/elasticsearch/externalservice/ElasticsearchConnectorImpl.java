/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * This class deals with logic management to connect to the Elasticsearch external service
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchConnectorImpl implements ElasticsearchConnector {

    private String user;
    private String password;
    private String url;
    private int port;

    private RestHighLevelClient client;

//    public RestHighLevelClient createSimpleElasticClient() throws Exception {
//
//        try {
//            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));
//
//            SSLContextBuilder sslBuilder = SSLContexts.custom()
//                    .loadTrustMaterial(null, (x509Certificates, s) -> true);
//            final SSLContext sslContext = sslBuilder.build();
//            RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
//                    new HttpHost("localhost", 9200, "https"))
//                    .setHttpClientConfigCallback(new HttpClientConfigCallback() {
//                        @Override
//                        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
//                            return httpClientBuilder
//                                     .setSSLContext(sslContext)
//                                     .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
//                                     .setDefaultCredentialsProvider(credentialsProvider);
//                        }
//                    })
//                    .setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
//                        @Override
//                        public RequestConfig.Builder customizeRequestConfig(
//                                RequestConfig.Builder requestConfigBuilder) {
//                            return requestConfigBuilder.setConnectTimeout(5000)
//                                    .setSocketTimeout(120000);
//                        }
//                    }));
//            System.out.println("elasticsearch client created");
//            return client;
//        } catch (Exception e) {
//            System.out.println(e);
//            throw new Exception("Could not create an elasticsearch client!!");
//        }
//    }

    public RestHighLevelClient createSimpleElasticClient() throws Exception {
        try {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));
            SSLContextBuilder sslBuilder = SSLContexts.custom().loadTrustMaterial(null, (x509Certificates, s) -> true);
            final SSLContext sslContext = sslBuilder.build();
            RestHighLevelClient client = new RestHighLevelClient(RestClient
                    .builder(new HttpHost("localhost", 9200, "https"))
                    .setHttpClientConfigCallback(new HttpClientConfigCallback() {
                        @Override
                        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                            return httpClientBuilder
                                     .setSSLContext(sslContext)
                                     .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                     .setDefaultCredentialsProvider(credentialsProvider);
                        }
                    })
                    .setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                        @Override
                        public RequestConfig.Builder customizeRequestConfig(
                                RequestConfig.Builder requestConfigBuilder) {
                            return requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(120000);
                        }
                    }));
            System.out.println("elasticsearch client created");
            return client;
        } catch (Exception e) {
            System.out.println(e);
            throw new Exception("Could not create an elasticsearch client!!");
        }
    }

    @PostConstruct
    private void setup() throws Exception {
        client = createSimpleElasticClient();
    }

//    @PostConstruct
//    private void setup() {

    //        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "admin"));
//
//        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "https"))
//                   .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
//                       @Override
//                       public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
//                           return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
//                       }
//                       });
//
//        client = new RestHighLevelClient(builder);
//    }

    @Override
    public void update(String json) {}

    @Override
    public void create(String json) throws IOException {
        try {
            boolean x = client.ping(RequestOptions.DEFAULT);
            System.out.println(x);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            client.close();
        }
    }

    @Override
    public void delete(String index, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(index, id);
        DeleteResponse deleteResponse = client.delete(
                request, RequestOptions.DEFAULT);
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    public void setClient(RestHighLevelClient client) {
        this.client = client;
    }

}