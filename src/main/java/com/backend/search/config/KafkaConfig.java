package com.backend.search.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import com.fasterxml.jackson.core.JsonParseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {
  private final IndexerConfig indexerConfig;

  @Bean
  public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, exception) -> {
              log.error(
                  "Sending to DLQ — topic: {}, partition: {}, offset: {}, key: {}, error: {}",
                  record.topic(),
                  record.partition(),
                  record.offset(),
                  record.key(),
                  exception.getMessage());
              return new TopicPartition(
                  indexerConfig.getTopics().getDeadLetter(), record.partition());
            });

    IndexerConfig.Retry retryConfig = indexerConfig.getRetry();
    ExponentialBackOff backOff = new ExponentialBackOff();
    backOff.setInitialInterval(retryConfig.getInitialIntervalMs());
    backOff.setMaxInterval(retryConfig.getMaxIntervalMs());
    backOff.setMultiplier(retryConfig.getMultiplier());
    backOff.setMaxElapsedTime(retryConfig.getMaxIntervalMs() * retryConfig.getMaxAttempts());

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
    errorHandler.addNotRetryableExceptions(
        IllegalArgumentException.class, JsonParseException.class);

    return errorHandler;
  }
}
