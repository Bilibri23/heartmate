# VPS network lockdown (P7)

Railway **egress IPs are not stable**. Locking UFW to one IP from `curl ifconfig.me` in a Railway shell will break when Railway changes egress.

## Launch default (recommended)

On Railway:

```
ROOMBAY_SEARCH_ELASTICSEARCH_ENABLED=false
```

Only Ollama is required for AI at launch. DB-backed listing search works without OpenSearch.

Expose Ollama (`11434`) with a plan for:

- **WireGuard / Tailscale** between Railway and VPS, or
- **SSH tunnel** from a sidecar (operational overhead), or
- **Reverse proxy** in front of Ollama with API key auth (Caddy/nginx)

Keep Redis disabled: `SPRING_DATA_REDIS_ENABLED=false`.

## If using UFW IP rules anyway

```bash
# On VPS — replace with current Railway egress (re-check after redeploys)
sudo ufw allow from RAILWAY_EGRESS_IP to any port 11434 proto tcp
# Repeat for 9200 only when OpenSearch is enabled and secured
```

Monitor API logs for connection refused to the VPS IP.

## When enabling OpenSearch

1. Set `ROOMBAY_SEARCH_ELASTICSEARCH_ENABLED=true`
2. Set `SPRING_ELASTICSEARCH_URIS=http://VPS_IP:9200`
3. Restrict port 9200 same as Ollama
4. Admin → Reindex listings
