# Elasticsearch search index (step 5.C)

The listing search index is **optional**. By default the app uses `NoOpSearchIndexClient`: outbox rows are still processed (marked as processed) but no external search engine is used.

## Enabling Elasticsearch (or OpenSearch)

1. **Start Elasticsearch** (e.g. Docker):
   ```bash
   docker run -d --name elasticsearch -p 9200:9200 -e "discovery.type=single-node" -e "xpack.security.enabled=false" docker.elastic.co/elasticsearch/elasticsearch:8.11.0
   ```

2. **Configure the backend** (e.g. in `application.properties` or `.env`):
   ```properties
   roombay.search.elasticsearch.enabled=true
   spring.elasticsearch.uris=http://localhost:9200
   # optional:
   roombay.search.elasticsearch.index-name=listings
   ```

3. **Restart the backend.** `ElasticsearchConfig` extends Spring Data Elasticsearch's `ElasticsearchConfiguration` to create the `ElasticsearchClient` bean (the `spring.elasticsearch.uris` property alone does not auto-create it). On startup, `ElasticsearchIndexMappingInitializer` creates the index with explicit mappings (`location` as `geo_point`, keyword filters, `createdAt` as date) when missing. The indexer job will create/update/delete documents in the `listings` index as outbox events are processed.

## Index shape

Documents have: `id`, `title`, `description`, `city`, `neighborhood`, `amenities`, `furnished`, `price`, `propertyType`, `location` (geo_point: lat/lon), `verified`, `featured`, `status`.

## How indexing works

- **Create/update/delete**: Listing changes write to `listing_search_outbox`; the indexer job (every 15s) processes them.
- **Admin approval**: When a listing is approved (PENDING → ACTIVE), it is re-indexed so it appears in search.
- **Empty index on startup**: If the index has 0 documents, the app enqueues all ACTIVE listings so the indexer can populate it.
- **Search fallback**: If Elasticsearch returns 0 results, the API falls back to DB search so users always see listings when they exist.
- **Manual reindex**: As admin, call `POST /api/admin/search/reindex` to enqueue all ACTIVE listings (fixes empty or stale index).

## Resetting / reindexing

To reindex all listings, you can either:

- Truncate `listing_search_outbox.processed_at` and set `processed_at = NULL` for rows you want reprocessed, or  
- Delete the `listings` index in Elasticsearch and restart the app — the bulk index runner will enqueue all ACTIVE listings.

## Testing search (boosting, fuzzy, typo tolerance, language)

When Elasticsearch is enabled, use the **`GET /api/search`** endpoint instead of `GET /api/listings` to exercise:

- **Fuzzy matching & typo tolerance**: `fuzziness: AUTO` on text fields — try `?query=aparment` or `?query=Douala` (typos) and verify results still match.
- **Boosting**: Featured listings (2x) and verified listings (1.5x) rank higher — compare order with/without featured/verified listings.
- **Recency**: `function_score` gauss decay on `createdAt` (30-day scale) in relevance mode.
- **Geo distance**: Pass `userLat`, `userLon`, and `maxDistance` (km) to filter and sort by proximity.

### Example requests

```bash
# Fuzzy/typo: search with intentional typo
curl "http://localhost:8080/api/search?query=aparment&size=5"

# French language analyzer
curl "http://localhost:8080/api/search?query=meublé&lang=fr&size=5"

# Boosting: featured/verified should appear first
curl "http://localhost:8080/api/search?query=studio&size=10"

# Filter-only (no text query)
curl "http://localhost:8080/api/search?city=Douala&minPrice=50000&maxPrice=200000"
```

When Elasticsearch is disabled, `/api/search` falls back to the database-backed search.

## Troubleshooting: "Elasticsearch returned 0 results"

1. **Check Admin Settings** → Search Index section shows "X documents in index". If 0, the index is empty.
2. **Verify Elasticsearch is running**: `docker ps` or `curl http://localhost:9200`
3. **Restart backend after starting Elasticsearch** — the client is created at startup
4. **Reindex** → Admin Settings → Reindex Search, then wait ~15–30 seconds
5. **Check backend logs** after reindex:
   - `Elasticsearch configured: localhost:9200` — client created
   - `Search indexer: processed N rows (indexed=M, failed=0)` — documents written to ES
   - `Search indexer failed for outbox id=...` — indexing errors (e.g. ES unreachable)
