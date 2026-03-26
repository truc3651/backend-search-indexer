package com.backend.search.opensearch.handler;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.backend.search.config.OpenSearchConfig;
import com.backend.search.events.PostEvent;
import com.backend.search.kafka.EventParser;
import com.backend.search.opensearch.repository.OpenSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostDeletedEventHandler implements EventHandler {
  private final EventParser eventParser;
  private final OpenSearchRepository openSearchRepository;
  private final OpenSearchConfig openSearchConfig;

  @Override
  public void handle(String value) {
    PostEvent event = eventParser.parsePostEvent(value);
    if (Objects.isNull(event) || Objects.isNull(event.getPayload())) {
      return;
    }

    String postId = event.getPayload().getPostId();
    if (Objects.isNull(postId)) {
      return;
    }
    openSearchRepository.delete(openSearchConfig.getIndices().getPosts(), postId);
    log.info("Deleted post from index: id={}", postId);
  }
}
