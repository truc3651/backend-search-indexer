package com.backend.search.opensearch.repository;

public class OpenSearchIndexingException extends RuntimeException {
  public OpenSearchIndexingException(String message, Throwable cause) {
    super(message, cause);
  }
}
