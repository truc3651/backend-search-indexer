package com.backend.search.opensearch.handler;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.backend.search.config.OpenSearchConfig;
import com.backend.search.events.CdcEnvelope;
import com.backend.search.events.UserEvent;
import com.backend.search.kafka.EventParser;
import com.backend.search.opensearch.document.UserDocument;
import com.backend.search.opensearch.repository.OpenSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCdcEventHandler implements EventHandler {
  private final EventParser eventParser;
  private final OpenSearchRepository openSearchRepository;
  private final OpenSearchConfig openSearchConfig;

  @Override
  public void handle(String value) {
    CdcEnvelope<UserEvent> envelope = eventParser.parseCdcEvent(value, UserEvent.class);
    if (Objects.isNull(envelope)) {
      return;
    }

    if (envelope.isCreate() || envelope.isUpdate()) {
      handleUpsert(envelope.getAfter());
    } else if (envelope.isDelete()) {
      handleDelete(envelope.getBefore());
    }
  }

  private void handleUpsert(UserEvent payload) {
    if (Objects.isNull(payload) || Objects.isNull(payload.getId())) {
      return;
    }

    UserDocument document =
        UserDocument.builder()
            .id(payload.getId())
            .fullName(payload.getFullName())
            .createdAt(payload.getCreatedAt())
            .updatedAt(payload.getUpdatedAt())
            .build();
    openSearchRepository.upsert(
        openSearchConfig.getIndices().getUsers(), String.valueOf(payload.getId()), document);
    log.info("Indexed user: id={}", payload.getId());
  }

  private void handleDelete(UserEvent payload) {
    if (Objects.isNull(payload) || Objects.isNull(payload.getId())) {
      return;
    }

    openSearchRepository.delete(
        openSearchConfig.getIndices().getUsers(), String.valueOf(payload.getId()));
    log.info("Deleted user from index: id={}", payload.getId());
  }
}
