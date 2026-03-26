package com.backend.search.opensearch.handler;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.backend.search.config.OpenSearchConfig;
import com.backend.search.events.PostEvent;
import com.backend.search.kafka.EventParser;
import com.backend.search.opensearch.document.PostDocument;
import com.backend.search.opensearch.repository.OpenSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCreatedEventHandler implements EventHandler {
  private final EventParser eventParser;
  private final OpenSearchRepository openSearchRepository;
  private final OpenSearchConfig openSearchConfig;

  @Override
  public void handle(String value) {
    PostEvent event = eventParser.parsePostEvent(value);
    if (Objects.isNull(event) || Objects.isNull(event.getPayload())) {
      return;
    }

    PostEvent.Payload payload = event.getPayload();
    if (Objects.isNull(payload.getPostId())) {
      return;
    }

    PostDocument document =
        PostDocument.builder()
            .postId(payload.getPostId())
            .content(payload.getContent())
            .visibility(payload.getVisibility())
            .createdAt(payload.getCreatedAt())
            .updatedAt(payload.getUpdatedAt())
            .build();
    openSearchRepository.upsert(
        openSearchConfig.getIndices().getPosts(), payload.getPostId(), document);
    log.info("Indexed post: id={}", payload.getPostId());
  }
}
