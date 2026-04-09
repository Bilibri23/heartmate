# RoomBay implementation status

**Purpose:** Single place to see what the repo implements versus open backlog work. **Authoritative product scope** is [`REVISED-SCOPE-MVP.md`](REVISED-SCOPE-MVP.md); prioritized work is [`PRODUCT-BACKLOG.md`](PRODUCT-BACKLOG.md).

**Last updated:** 2026-04-09

---

## Authoritative references

- **Scope & personas:** [`REVISED-SCOPE-MVP.md`](REVISED-SCOPE-MVP.md) (applications, leases, payments, Elasticsearch search, admin workflows).
- **Delivery backlog:** [`PRODUCT-BACKLOG.md`](PRODUCT-BACKLOG.md) (P0–P3).
- **Discovery / feed IA:** [`discovery-architecture.md`](discovery-architecture.md).
- **Security / ops gaps:** [`system-gaps-and-recommendations.md`](system-gaps-and-recommendations.md).
- **Post-deploy checklist (4 items):** [`NEXT-TODOS.md`](NEXT-TODOS.md).

---

## AI / RAG (technical anchor)

- **Vector retrieval:** PostgreSQL **pgvector** (not FAISS). Schema and repository: migration `backend/src/main/resources/db/migration/V32__create_ai_rag_tables.sql`, [`AiRagRepository.java`](../backend/src/main/java/org/rooms/roombay/ai/rag/AiRagRepository.java).
- **Embeddings & chat:** Routed via [`AiModelRouter`](../backend/src/main/java/org/rooms/roombay/ai/AiModelRouter.java) — Ollama and/or OpenAI depending on env.
- **Self-hosted-only policy:** Set `AI_PROVIDER=ollama` and omit `OPENAI_API_KEY` in environments where external AI APIs are not allowed. See [`ai-faq.md`](ai-faq.md).

---

## Implemented vs backlog (summary)

| Area | Status | Notes |
|------|--------|--------|
| Auth (JWT), roles | Implemented | Tenant role may still be `STUDENT` in API enums; UI should say “Tenant” where user-facing. |
| Listings CRUD, media, video tour fields | Implemented | Property types include studio, apartment, house, private/shared room. |
| Feed (`GET /api/feed`), For You | Implemented | See discovery doc for URL strategy. |
| Search (Elasticsearch), saved searches | Implemented | |
| Favorites | Implemented | No separate “like” metric unless added later. |
| Messaging (+ WebSocket) | Implemented | |
| Applications, leases, payments | Implemented | Core to revised MVP. |
| Roommate matching (rule-based) | Implemented | `MatchingService` weights. |
| Admin: users, listings, verifications, payments, **reports** | Implemented | `/admin/reports` + bottom nav; see P2 in backlog. |
| AI assistant (RAG + chat) | Implemented | Ingest from `docs/*.md`; pgvector storage. |
| Household (expenses/tasks) | Out of MVP scope | Routes may exist; not primary nav — see backlog. |
| P0–P2 backlog items | See [`PRODUCT-BACKLOG.md`](PRODUCT-BACKLOG.md) | Close items as shipped. |

---

## Branding

User-facing name: **RoomBay**. Java package remains `org.rooms.roombay` unless a deliberate rename project is scheduled.
