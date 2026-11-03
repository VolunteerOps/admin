# Meta support report — Muse Spark hosted requests fail

> **Update — 3 November 2026:** The regional `model_not_found` failure is resolved in live backend checks after moving `resq-api` from Amsterdam to US East (Virginia). The same key and models now work. The report below is historical; see the resolution evidence at the end before sending it as an open incident.

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

## Fresh reproduction — 3 November 2026, ~08:14 UTC

Repeated with the currently deployed key (shared variable, 48 characters, resolves on `resq-api`):

| Source | Endpoint | Result | `x-request-id` |
|---|---|---|---|
| Local egress, deployed key | `GET /v1/models` | 200, 9 models incl. `muse-spark-1.3` | `a5a1c855-6286-4ddf-87d1-53106e622390` |
| Local egress, deployed key | `POST /v1/chat/completions` | 200 | `15ee1b32-474b-412b-bd20-f7b6acd82845` |
| Local egress, deployed key | `POST /v1/responses` | 200 | `6497d8bb-f688-48a8-a9c2-889c0a04c28a` |
| Railway egress (temporary probe service, same project/region, same shared key; deleted after test) | `GET /v1/models` | 200, empty model list | `0335c146-1d90-4063-aa1e-dd55133f26f8` |
| Railway egress | `POST /v1/chat/completions` | 404 `model_not_found` | `4bc8eaa9-e13d-4a29-8802-7e0b7cda8824` |
| Railway egress | `POST /v1/responses` | 404 `model_not_found` | `67a713d4-9a35-460b-93ac-23215076d339` |

Same key, same payloads, minutes apart: success outside Railway, `model_not_found` inside Railway. Please investigate serving/routing for these fresh request IDs.

Thank you.

---

Operator notes: This report is prepared for sending; it has not been sent. Attach the relevant Meta project/team identifier through Meta's support interface if needed. Do not attach API keys, Railway variable exports or service-account credentials. The observations do not establish a geographic restriction or a billing failure.

## Resolution evidence — 3 November 2026, 09:21–09:26 UTC

Following support's suggestion, tests ran inside the actual `resq-api` container, using its existing key. No proxy was configured. Cloudflare's trace endpoint and IPinfo independently identified the same outbound IP and country in each region.

| Check | Amsterdam (`europe-west4-drams3a`) | Virginia (`us-east4-eqdc4a`) |
|---|---|---|
| Outbound IP | `152.55.184.209` | `152.55.182.25` |
| Country / city | NL / Amsterdam | US / Ashburn |
| Model catalog | HTTP 200, empty | HTTP 200, nine models including both requested models |
| Spark 1.3 | HTTP 404 `model_not_found` | HTTP 200 |
| Wonder 1.0 | HTTP 404 `model_not_found` | HTTP 200 |

Before the move, Spark request ID: `d01e502f-fd63-4092-9f9a-f687749ac290`. After the move: `32c735d8-5fa8-4367-91a1-02162537f5be`. Wonder after the move: `826f5c2a-aa17-443d-96f2-f3a7ac68498d`. Rechecked four minutes later: both model requests still returned HTTP 200; IDs `0e7afc05-1e9b-44a8-b6b5-5c20a80c39fd` and `7060ab00-e2ea-4a18-a295-f1715285f775`.

The packaged mission diagnostic passed its required tool loop and validated the structured reply. A synthetic Eastleigh scenario also called mission requirements and live Mapbox lookup, then returned a valid coordinator-facing reply. Wonder passed a separate synthetic coverage-tool check. No mission was created, no user was impersonated, and no BigQuery or GCS weather queries ran. One longer scenario attempt produced a malformed reply (`SyntaxError`); a second attempt succeeded. That separate model-output reliability issue is not evidence of continued regional access denial.

The API remains in US East with one replica. Its existing deployment image, model IDs, key and paid-access rules were retained. Database/worker regions were not moved. API readiness, the custom-domain proxy and CORS passed after the move. Cross-region database latency is a tradeoff to monitor.

The move demonstrates region-dependent serving behavior; it does not prove that the Netherlands is prohibited. Meta's [published geographic policy](https://dev.meta.ai/legal/geographic-use-policy/) does not list the Netherlands, Kenya or the United States as restricted territories. No VPN or proxy was introduced.
