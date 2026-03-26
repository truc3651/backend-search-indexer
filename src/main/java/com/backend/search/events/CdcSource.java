package com.backend.search.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdcSource {
  private String version;
  private String connector;
  private String name;

  @JsonProperty("ts_ms")
  private Long timestampMs;

  private String snapshot;
  private String db;
  private String schema;
  private String table;

  @JsonProperty("txId")
  private Long transactionId;

  private Long lsn;
  private Long xmin;
}
