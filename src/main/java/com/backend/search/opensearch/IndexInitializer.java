package com.backend.search.opensearch;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Objects;

import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.backend.search.config.OpenSearchConfig;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexInitializer {
  private final OpenSearchClient openSearchClient;
  private final OpenSearchConfig openSearchConfig;

  @EventListener(ApplicationReadyEvent.class)
  public void initializeIndices() {
    createIndexIfNotExists(openSearchConfig.getIndices().getUsers(), "opensearch/users-index.json");
    createIndexIfNotExists(openSearchConfig.getIndices().getPosts(), "opensearch/posts-index.json");
  }

  private void createIndexIfNotExists(String indexName, String resourcePath) {
    try {
      boolean exists = openSearchClient.indices().exists(r -> r.index(indexName)).value();
      if (!exists) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (Objects.isNull(inputStream)) {
          log.error("Index definition not found: {}", resourcePath);
          return;
        }

        JsonpMapper mapper = openSearchClient._transport().jsonpMapper();
        try (JsonReader reader = Json.createReader(inputStream)) {
          JsonObject json = reader.readObject();

          CreateIndexRequest.Builder builder = new CreateIndexRequest.Builder().index(indexName);

          if (json.containsKey("settings")) {
            String settingsJson = json.getJsonObject("settings").toString();
            builder.settings(
                IndexSettings._DESERIALIZER.deserialize(
                    mapper.jsonProvider().createParser(new StringReader(settingsJson)), mapper));
          }

          if (json.containsKey("mappings")) {
            String mappingsJson = json.getJsonObject("mappings").toString();
            builder.mappings(
                org.opensearch.client.opensearch._types.mapping.TypeMapping._DESERIALIZER
                    .deserialize(
                        mapper.jsonProvider().createParser(new StringReader(mappingsJson)),
                        mapper));
          }

          openSearchClient.indices().create(builder.build());
          log.info("Created index: {}", indexName);
        }
      } else {
        log.info("Index already exists: {}", indexName);
      }
    } catch (Exception e) {
      log.error("Failed to initialize index '{}': {}", indexName, e.getMessage());
    }
  }
}
