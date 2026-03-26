## Why this exists

The social app stores users in PostgreSQL and posts in Cassandra. Neither supports full-text search well. This service bridges that gap by consuming change events from Kafka and maintaining OpenSearch indices that power user search (by full name) and post search (by content).

It sits at the end of two event pipelines:

- **User CDC pipeline**: PostgreSQL → Debezium CDC → Kafka topic `postgres.users.t_users` → **this service** → OpenSearch `users` index
- **Post event pipeline**: backend-posts-management → Kafka topics `postCreated`/`postUpdated`/`postDeleted` → **this service** → OpenSearch `posts` index

## What it does

1. Consumes 4 Kafka topics:
   - `postgres.users.t_users` — CDC events (Debezium envelope with `before`/`after`/`op` fields)
   - `postCreated` — new post published
   - `postUpdated` — post content edited
   - `postDeleted` — post removed

2. Routes each message to the appropriate handler, which transforms the event into an OpenSearch document and indexes/deletes it.

3. On failure, Spring Kafka retries with exponential backoff. After exhausting retries, the message is sent to a dead letter topic (`deadLetters`) so no data is silently lost. Parse errors (bad JSON) skip retries entirely — they will never succeed.

4. On startup, `IndexInitializer` creates the `users` and `posts` indices if they don't already exist, using the mapping definitions from `src/main/resources/opensearch/users-index.json` and `posts-index.json`.

## Tech stack

| Component | Choice | Why |
|---|---|---|
| Framework | Spring Boot 3.4.1 | Consistent with other backend services |
| Kafka | Spring Kafka with `@KafkaListener` | Synchronous consumer — no reactive overhead. Retry and DLQ handled by `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` |
| Search engine | AWS OpenSearch (via `opensearch-java` client) | Managed service, compatible with Elasticsearch APIs |
| Serialization | Jackson | Same as all other services |
| Build | Gradle, Java 17 | Same as all other services |
| Code style | Spotless (Google Java Format) | Same as all other services |

### Why not reactive?

Other services in this repo (backend-graph-projector, backend-fanout-workers) use reactor-kafka because their downstream stores (Neo4j, Redis) have native reactive drivers — the entire pipeline is non-blocking end-to-end.

Here, the OpenSearch Java client and Jackson are both synchronous. Wrapping blocking calls in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` adds complexity with zero throughput benefit — it just bounces work to a thread pool that behaves like a regular thread pool. Spring Kafka's `@KafkaListener` with `DefaultErrorHandler` gives us retry + DLQ for free with no custom code.

## How the content analyzer works

Both indices use a custom `content_analyzer`. This is a text processing pipeline that makes full-text search actually useful.

When a document is indexed, OpenSearch runs the text field through this chain:

```
raw text
  → standard tokenizer (splits on whitespace and punctuation)
    → lowercase filter (so "Hello" and "hello" match)
      → stop filter (removes noise words like "the", "a", "is", "and")
        → snowball filter (stems words: "running" → "run", "communities" → "commun")
```

**Example**: indexing the post `"She is running through the Communities"`

Produces these tokens: `["she", "run", "through", "commun"]`

So a search for `"running"`, `"run"`, or `"community"` will all match this post.

The same analyzer is used on the `users` index for `fullName`, so searching "john" matches "John", "Johnson", "Johnny".

## Index mappings

Defined as JSON files in `src/main/resources/opensearch/` and applied by `IndexInitializer` on startup.

**`users-index.json`** — 1 shard, 1 replica

| Field | Type | Notes |
|---|---|---|
| `id` | `long` | User ID from PostgreSQL |
| `fullName` | `text` (content_analyzer) | Full-text searchable |

**`posts-index.json`** — 2 shards, 1 replica

| Field | Type | Notes |
|---|---|---|
| `postId` | `keyword` | Exact match only (Cassandra partition key) |
| `content` | `text` (content_analyzer) | Full-text searchable |
| `visibility` | `keyword` | `PUBLIC`, `FRIENDS`, `ONLY_ME` — for filtering |

## Error handling

```
message consumed
  → handler processes it
    → success: offset committed
    → failure: exponential backoff retry (1s → 2s → 4s → ... up to 60s, max 10 attempts)
      → still failing: sent to DLQ topic (deadLetters)
      → non-retryable (JsonParseException, IllegalArgumentException): sent to DLQ immediately
```

## Configuration

All Kafka consumer/producer/listener settings are declared in `application.yml` — no programmatic duplication. The only Java config is `KafkaConfig` which wires up the `DefaultErrorHandler` with DLQ routing and retry backoff, since that has no YAML equivalent.

Key environment variables:

| Variable | Default | Description |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `OPENSEARCH_HOST` | `localhost` | OpenSearch endpoint |
| `OPENSEARCH_PORT` | `9200` | OpenSearch port |
| `OPENSEARCH_SCHEME` | `http` | `http` or `https` |
| `OPENSEARCH_USERNAME` | `admin` | OpenSearch credentials |
| `OPENSEARCH_PASSWORD` | `admin` | OpenSearch credentials |
| `INDEXER_CONSUMER_CONCURRENCY` | `3` | Number of concurrent Kafka listener threads |
| `INDEXER_RETRY_MAX_ATTEMPTS` | `10` | Max retry attempts before DLQ |
| `INDEXER_RETRY_INITIAL_INTERVAL` | `1000` | Initial retry delay (ms) |
| `INDEXER_RETRY_MAX_INTERVAL` | `60000` | Max retry delay (ms) |
| `INDEXER_RETRY_MULTIPLIER` | `2.0` | Backoff multiplier |
