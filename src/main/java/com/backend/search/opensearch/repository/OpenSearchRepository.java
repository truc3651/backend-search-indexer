package com.backend.search.opensearch.repository;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OpenSearchRepository {
  private final OpenSearchClient openSearchClient;

  public <T> void index(String indexName, String documentId, T document) {
    try {
      IndexRequest<T> request =
          new IndexRequest.Builder<T>().index(indexName).id(documentId).document(document).build();
      openSearchClient.index(request);
    } catch (Exception e) {
      throw new OpenSearchIndexingException(
          String.format("Failed to index document — index: %s, id: %s", indexName, documentId), e);
    }
  }

  public void delete(String indexName, String documentId) {
    try {
      DeleteRequest request = new DeleteRequest.Builder().index(indexName).id(documentId).build();
      openSearchClient.delete(request);
    } catch (Exception e) {
      throw new OpenSearchIndexingException(
          String.format("Failed to delete document — index: %s, id: %s", indexName, documentId), e);
    }
  }
}
