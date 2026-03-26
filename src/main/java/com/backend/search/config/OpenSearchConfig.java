package com.backend.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "opensearch")
@Data
public class OpenSearchConfig {
  private String host;
  private int port;
  private String scheme;
  private String username;
  private String password;
  private Indices indices = new Indices();

  @Data
  public static class Indices {
    private String users;
    private String posts;
  }
}
