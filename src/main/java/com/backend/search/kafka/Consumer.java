package com.backend.search.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.backend.search.config.IndexerConfig;
import com.backend.search.opensearch.handler.EventHandler;
import com.backend.search.opensearch.handler.PostCreatedEventHandler;
import com.backend.search.opensearch.handler.PostDeletedEventHandler;
import com.backend.search.opensearch.handler.PostUpdatedEventHandler;
import com.backend.search.opensearch.handler.UserCdcEventHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Consumer {
  private final IndexerConfig indexerConfig;
  private final UserCdcEventHandler userCdcEventHandler;
  private final PostCreatedEventHandler postCreatedEventHandler;
  private final PostUpdatedEventHandler postUpdatedEventHandler;
  private final PostDeletedEventHandler postDeletedEventHandler;

  @KafkaListener(
      topics = {
        "${indexer.topics.users}",
        "${indexer.topics.post-created}",
        "${indexer.topics.post-updated}",
        "${indexer.topics.post-deleted}"
      })
  public void onMessage(ConsumerRecord<String, String> record) {
    String topic = record.topic();
    log.info(
        "Received event — topic: {}, partition: {}, offset: {}, key: {}",
        topic,
        record.partition(),
        record.offset(),
        record.key());

    EventHandler handler = resolveHandler(topic);
    if (handler == null) {
      log.warn("No handler found for topic: {}", topic);
      return;
    }

    handler.handle(record.value());

    log.info(
        "Processed event — topic: {}, partition: {}, offset: {}, key: {}",
        topic,
        record.partition(),
        record.offset(),
        record.key());
  }

  private EventHandler resolveHandler(String topic) {
    IndexerConfig.Topics topics = indexerConfig.getTopics();

    if (topic.equals(topics.getUsers())) {
      return userCdcEventHandler;
    }
    if (topic.equals(topics.getPostCreated())) {
      return postCreatedEventHandler;
    }
    if (topic.equals(topics.getPostUpdated())) {
      return postUpdatedEventHandler;
    }
    if (topic.equals(topics.getPostDeleted())) {
      return postDeletedEventHandler;
    }
    return null;
  }
}
