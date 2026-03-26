package com.backend.search.events;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostEvent {
  private String timestamp;
  private String environment;
  private Payload payload;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Payload {
    private String postId;
    private String content;
    private String visibility;
    private Instant createdAt;
    private Instant updatedAt;
  }
}
