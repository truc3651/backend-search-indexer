package com.backend.search.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdcEnvelope<T> {
  private T before;
  private T after;
  private CdcSource source;

  @JsonProperty("op")
  private String operation;

  @JsonProperty("ts_ms")
  private Long timestampMs;

  public boolean isCreate() {
    return "c".equals(operation) || "r".equals(operation);
  }

  public boolean isUpdate() {
    return "u".equals(operation);
  }

  public boolean isDelete() {
    return "d".equals(operation);
  }
}
