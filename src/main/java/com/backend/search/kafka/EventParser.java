package com.backend.search.kafka;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.backend.search.events.CdcEnvelope;
import com.backend.search.events.CdcSource;
import com.backend.search.events.PostEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventParser {
  private final ObjectMapper objectMapper;

  public PostEvent parsePostEvent(String json) {
    try {
      return objectMapper.readValue(json, PostEvent.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse post event: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Supports both formats: 1. Direct payload: {"before": ..., "after": ..., "op": ...} 2. With
   * schema: {"schema": ..., "payload": {"before": ..., "after": ..., "op": ...}}
   */
  public <T> CdcEnvelope<T> parseCdcEvent(String json, Class<T> payloadClass) {
    if (!StringUtils.hasText(json)) {
      return null;
    }

    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;
      if (Objects.isNull(payloadNode)) {
        return null;
      }
      return parseEnvelope(payloadNode, payloadClass);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse CDC event: {}", e.getMessage());
      return null;
    }
  }

  private <T> CdcEnvelope<T> parseEnvelope(JsonNode node, Class<T> payloadClass)
      throws JsonProcessingException {
    CdcEnvelope<T> envelope = new CdcEnvelope<>();

    if (node.has("op")) {
      envelope.setOperation(node.get("op").asText());
    }
    if (node.has("ts_ms")) {
      envelope.setTimestampMs(node.get("ts_ms").asLong());
    }
    if (node.has("source")) {
      envelope.setSource(objectMapper.treeToValue(node.get("source"), CdcSource.class));
    }
    if (node.has("before") && Objects.nonNull(node.get("before"))) {
      envelope.setBefore(objectMapper.treeToValue(node.get("before"), payloadClass));
    }
    if (node.has("after") && Objects.nonNull(node.get("after"))) {
      envelope.setAfter(objectMapper.treeToValue(node.get("after"), payloadClass));
    }

    return envelope;
  }
}
