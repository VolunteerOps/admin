# Meta support report — Muse Spark hosted requests fail

**Subject:** Muse Spark 1.3 returns model_not_found from Railway with a key that works locally

Hello Meta Model API support,

We are building ResQOps, a humanitarian volunteer-coordination platform. Our mission-planning assistant uses `muse-spark-1.3` to help coordinators prepare mission drafts. It reads controlled backend tools for mission requirements and place lookup; coordinators review and approve changes. The assistant does not independently deploy volunteers.

Our hosted backend cannot currently use this model. Please investigate model availability or routing for the request IDs below.

## Reproduction

- API base: `https://api.meta.ai/v1`
- Exact model: `muse-spark-1.3`, Standard mode.
- Runtime: Node.js on Railway, `europe-west4-drams3a`.
- Date: 1 November 2026.
- The backend's configured credential and the locally tested credential match. No key is included in this report.
- Local calls using that credential succeed, including the real mission-planning tool sequence with structured output.
- Hosted calls fail with HTTP 404 and this response:

```json
{"error":{"code":"model_not_found","type":"invalid_request_error","message":"The requested model was not found."}}
```

A hosted `GET /v1/models` initially listed `muse-spark-1.3`, yet the immediately following completion returned the error above. A subsequent catalog request returned an empty model list. Three bounded retries on fresh connections did not resolve it.

Both API formats fail even with a minimal synthetic request. At approximately **2026-11-01 19:40:48 UTC**:

| Endpoint | Result | `x-request-id` |
|---|---|---|
| `POST /v1/responses` | 404 `model_not_found` | `dcc0d12c-2562-4b4f-81d3-1751d18cbbf7` |
| `POST /v1/chat/completions` | 404 `model_not_found` | `f21232e5-b51c-405f-9611-b892debd40c5` |

Chat Completions payload:

```json
{"model":"muse-spark-1.3","messages":[{"role":"user","content":"Reply with ready."}],"max_completion_tokens":64}
```

Responses payload:

```json
{"model":"muse-spark-1.3","input":"Reply with ready.","max_output_tokens":64,"store":false}
```

Can you confirm that our team's Standard-mode access to Muse Spark 1.3 is consistent across your serving infrastructure, investigate these request IDs, and advise on any required account configuration? The same key working locally but failing on the hosted service, together with inconsistent model-list results, is preventing us from reliably connecting the assistant.

Thank you.

---

Operator notes: This report is prepared for sending; it has not been sent. Attach the relevant Meta project/team identifier through Meta's support interface if needed. Do not attach API keys, Railway variable exports or service-account credentials. The observations do not establish a geographic restriction or a billing failure.
