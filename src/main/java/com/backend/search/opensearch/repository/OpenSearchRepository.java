package com.backend.search.opensearch.repository;

import java.util.Map;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.UpdateRequest;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OpenSearchRepository {
  private final OpenSearchClient openSearchClient;
  private final ObjectMapper objectMapper;

  // Compares ISO-8601 strings lexicographically — works because ISO-8601 is sortable
  // spotless:off
  private static final String VERSION_AWARE_UPSERT_SCRIPT =
      """
        if (ctx._source.updatedAt == null
          || params.updatedAt == null
          || params.updatedAt.compareTo(ctx._source.updatedAt) >= 0
        ) { ctx._source = params.doc; }
        else { ctx.op = 'noop'; }
      """;
  // spotless:on

  @SuppressWarnings("unchecked")
  public <T> void upsert(String indexName, String documentId, T document) {
    try {
      Map<String, Object> docMap = objectMapper.convertValue(document, Map.class);
      Object updatedAt = docMap.get("updatedAt");

      UpdateRequest<Object, Object> request =
          new UpdateRequest.Builder<>()
              .index(indexName)
              .id(documentId)
              .script(buildVersionAwareScript(docMap, updatedAt))
              .upsert(docMap)
              .build();

      openSearchClient.update(request, Object.class);
    } catch (Exception e) {
      throw new OpenSearchIndexingException(
          String.format(
              "Failed to index document — index: %s, id: %s", indexName, documentId),
          e);
    }
  }

  public void delete(String indexName, String documentId) {
    try {
      DeleteRequest request =
          new DeleteRequest.Builder().index(indexName).id(documentId).build();
      openSearchClient.delete(request);
    } catch (Exception e) {
      throw new OpenSearchIndexingException(
          String.format(
              "Failed to delete document — index: %s, id: %s", indexName, documentId),
          e);
    }
  }

  // Only replace if the incoming updatedAt >= the one already stored. Otherwise, do nothing (noop)
  // The .upsert(docMap) call handles the case where the document doesn't exist yet
  private Script buildVersionAwareScript(Map<String, Object> docMap, Object updatedAt) {
    return new Script.Builder()
        .inline(
            i ->
                i.lang("painless")
                    .source(VERSION_AWARE_UPSERT_SCRIPT)
                    .params("doc", JsonData.of(docMap))
                    .params("updatedAt", JsonData.of(updatedAt != null ? updatedAt : "")))
        .build();
  }
}
