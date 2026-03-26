package com.backend.search.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OpenSearchClientConfig {
  private final OpenSearchConfig openSearchConfig;
  private final ObjectMapper objectMapper;

  @Bean
  public OpenSearchClient openSearchClient() {
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials(
            openSearchConfig.getUsername(), openSearchConfig.getPassword()));

    RestClient restClient =
        RestClient.builder(
                new HttpHost(
                    openSearchConfig.getHost(),
                    openSearchConfig.getPort(),
                    openSearchConfig.getScheme()))
            .setHttpClientConfigCallback(
                httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
            .setRequestConfigCallback(
                requestConfigBuilder ->
                    requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000))
            .build();

    RestClientTransport transport =
        new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    return new OpenSearchClient(transport);
  }
}
