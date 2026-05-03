# RoomBay AI: Finetuning + Safety Stack

> Goal: produce safe, structured, marketplace-aware housing-support and recommendation
> responses using only authorized context, with graceful degradation for sensitive or
> missing information.

This is the operating doc for the layer of behaviour that sits **on top of RAG**.
It is opinionated on purpose so the assistant is consistent across tenants, landlords, and
admins.

---

## 1. The split: RAG vs. finetuning vs. safety filters

| Layer       | What it gives the model                                  |
|-------------|----------------------------------------------------------|
| RAG         | Facts: listing data, help docs, verification policy.     |
| Finetuning  | Behaviour: how to answer, when to refuse, output shape.  |
| Safety      | Guardrails the model is not allowed to bypass.           |

We do **not** finetune on raw RAG chunks. Memorising private data is the failure mode.
We finetune on curated `(system, user, sanitized_context, ideal_answer)` examples that
demonstrate the behaviour we want.

---

## 2. The four behaviours we train

### A. Answer style (`kind: STYLE`)
Short. Practical. Cameroon housing context. No overpromising. Clear next action.
Friendly but not casual.

### B. Structured outputs (`kind: STRUCTURED`)
For UI rendering. Strict JSON: `{ headline, bullets[1..3], confidence: high|medium|low }`
for the **similar-listing rationale** task. The frontend renders these as-is.

### C. Refusal behaviour (`kind: REFUSAL`)
The model must never reveal:
- Another user's private details (phone, email, address).
- Internal admin/review notes.
- Verification documents (other users' or any user's, except for the admin's own audit
  in admin contexts).
- Anything that helps bypass verification or commit fraud.

Refusals must be polite, brief, and offer a safe redirect (in-app messaging or
`support@roombay.com`).

### D. Graceful degradation (`kind: FALLBACK`)
When RAG returns nothing, **never invent** features or policies. Acknowledge the gap and
redirect to the right RoomBay screen or to support.

---

## 3. The four security layers

Implemented in `org.rooms.roombay.ai.safety`:

1. **`AiRetrievalPolicy`** — drops admin-only doc sources when the caller is not an admin.
2. **`AiContextSanitizer`** — strips emails, phone numbers, doc URLs, payment refs, UUIDs,
   internal-note lines, and secret-looking lines from anything the model sees.
3. **`AiOutputGuard`** — last check before the answer reaches the user. Replaces a leaked
   answer with the canonical RoomBay fallback text.
4. **`AiSafetyClassifier`** — input-side regex classifier that short-circuits clearly
   sensitive requests (asking for someone else's phone, fraud bypass, admin notes…).
   The finetuned model is taught to reproduce the same refusal copy so behaviour is
   consistent end-to-end even when the classifier has gaps.

These run on **every** `/api/ai/chat` request: classify → retrieve → role-filter →
sanitize → generate → guard → log.

---

## 4. The dataset

Table: `ai_finetune_examples` (migration `V40`). Each row is one supervised example.

Required fields:
- `kind` — `STYLE | REFUSAL | FALLBACK | STRUCTURED | RECOMMENDATION`
- `persona` — `TENANT | LANDLORD | NEUTRAL`
- `system_prompt` — the same persona system prompt the live assistant uses
- `user_message` — the user turn
- `sanitized_context` — optional retrieved chunks **already redacted**
- `ideal_assistant` — gold-standard reply (what we want the model to produce)
- `response_format` — `text` or `json_object`

Curation rules:
- Sanitize before saving (the service does this automatically on create/update).
- Don't paste raw chat logs; rewrite them as ideal answers.
- Keep refusal answers short and follow the canonical RoomBay refusal style.
- For `json_object`, the assistant turn must be valid JSON.

A starter library lives at `backend/src/main/resources/ai/finetune/roombay-seed-v1.json`.

---

## 5. Admin API

All endpoints require `ROLE_ADMIN`.

| Method | Path                                          | What it does                                     |
|--------|-----------------------------------------------|--------------------------------------------------|
| GET    | `/api/ai/admin/finetune/examples`             | List active examples                             |
| POST   | `/api/ai/admin/finetune/examples`             | Create example (sanitizer applied automatically) |
| PUT    | `/api/ai/admin/finetune/examples/{id}`        | Update example                                   |
| DELETE | `/api/ai/admin/finetune/examples/{id}`        | Soft-delete                                      |
| GET    | `/api/ai/admin/finetune/stats`                | Counts per kind + readiness hint                 |
| POST   | `/api/ai/admin/finetune/seed`                 | Idempotent seed from bundled JSON                |
| GET    | `/api/ai/admin/finetune/export`               | OpenAI chat-completions JSONL                    |
| POST   | `/api/ai/admin/finetune/eval?limitPerKind=5`  | Run examples against the current model           |

---

## 6. End-to-end finetuning workflow

1. **Seed**: `POST /api/ai/admin/finetune/seed`. Adds the bundled starter examples.
2. **Curate**: add new examples through the admin API as you observe live failures.
   Aim for ≥ 25 active examples per kind before training.
3. **Stats**: `GET /api/ai/admin/finetune/stats` to confirm the dataset is balanced.
4. **Baseline eval**: `POST /api/ai/admin/finetune/eval` and save the JSON. This is your
   "before" number for the **base** model on `validJson`, `passedNoLeak`,
   `passedRefusal`, and `avgLengthChars`.
5. **Export**: `GET /api/ai/admin/finetune/export` → `roombay-finetune.jsonl`.
6. **Train** (out of band):
   - **OpenAI**: upload the JSONL via the OpenAI dashboard / API and start a fine-tune job
     against `gpt-4o-mini` (or the latest small model). When it finishes, copy the
     `ft:gpt-4o-mini:org:...` model id.
   - **Local (Ollama)**: train a LoRA / QLoRA with your tooling of choice and `ollama
     create roombay-rationale -f Modelfile`.
7. **Wire the fine-tuned head** for the structured tasks only:
   - `OPENAI_STRUCTURED_MODEL=ft:gpt-4o-mini:...` (or `OLLAMA_STRUCTURED_MODEL=roombay-rationale`)
   - Restart the backend. The main assistant chat keeps using `OPENAI_MODEL`; the
     `chatStructuredJson` path now uses your finetune.
8. **Re-eval**: `POST /api/ai/admin/finetune/eval`. Compare to baseline. The fine-tuned
   head should improve `validJson` and length, and at minimum tie on `passedRefusal` and
   `passedNoLeak`.
9. **Promote**: when the numbers are clearly better and a manual spot-check passes, leave
   the env var set in production. To roll back, unset it.

---

## 7. Where the safety stack is used

- `AiAssistantService.chat(...)` — full classify → role-filter → sanitize → guard pipeline.
- `AiSimilarListingRationaleService` (when present) — sanitizes model output before
  building the response and falls back to the rule-based summary on PII detection.
- `AiFinetuneDatasetService` — sanitizes incoming examples on create/update so curated
  data is safe to ship to a third-party trainer.

If a future feature calls a model directly, plug `AiContextSanitizer` and `AiOutputGuard`
into that path too.

---

## 8. Eval signals worth watching

The `/eval` endpoint reports:

- `total` — examples evaluated
- `validJson` — count where `response_format=json_object` parsed cleanly
- `passedNoLeak` — count where the output guard didn't redact
- `passedRefusal` — REFUSAL examples whose model output looks like a refusal and contains
  no redacted tokens
- `avgLengthChars` — quick proxy for verbosity

A successful finetune typically:
- Pushes `validJson` close to `total` for the structured rows.
- Pushes `passedNoLeak` to `total` (no PII leakage).
- Drops `avgLengthChars` for STYLE rows (fewer rambling answers).
