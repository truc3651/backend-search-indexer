package com.backend.search.events;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEvent {
  private Long id;
  private String fullName;
  private Instant createdAt;
  private Instant updatedAt;
}
