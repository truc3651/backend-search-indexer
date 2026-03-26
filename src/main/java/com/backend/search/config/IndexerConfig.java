package com.backend.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "indexer")
@Data
public class IndexerConfig {
  private Topics topics = new Topics();
  private Retry retry = new Retry();

  @Data
  public static class Topics {
    private String users;
    private String postCreated;
    private String postUpdated;
    private String postDeleted;
    private String deadLetter;
  }

  @Data
  public static class Retry {
    private int maxAttempts;
    private long initialIntervalMs;
    private long maxIntervalMs;
    private double multiplier;
  }
}
