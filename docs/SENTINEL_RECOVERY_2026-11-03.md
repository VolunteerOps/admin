# Ask Sentinel incident — 3 November 2026

## Confirmed causes

- The deployed database at contract `c5b6078…` has no `public.user_drafts` table. Draft creation was only in the legacy Prisma migration directory; Railway applies the canonical `migrations/app` graph. This explains the chat draft HTTP 500 responses.
- Generic form drafts used human-readable labels with spaces, which the draft API rejects. Password confirmation was also incorrectly eligible for draft saving.
- Streaming and JSON fallback requests used different idempotency keys. Provider failures triggered another request, and weather retries reused failed attempts. This could duplicate billed attempts and produce conflicts.
- Live Meta responses return HTTP 404 `model_not_found` for both configured models. This is not a missing frontend connection or a BigQuery error.

## Repairs

- Added a canonical private draft-table migration and runtime grants. All draft operations run inside authenticated, user-scoped database transactions with row-level security.
- Form draft identifiers now use a stable hash when their labels contain unsupported characters. Password/credential forms never save drafts; the server also rejects credential fields. Known local password drafts are removed when that form opens.
- Coalesced draft reads, scoped them to the signed-in user, and stopped repeated reads during rapid remounts.
- Streaming and fallback share one request key. Only a missing streaming route permits fallback. A confirmed failed model attempt gets a fresh key on manual retry; an uncertain interruption retains its key to recover an existing result. Changing the question or briefing mode gets a new key.
- Expired identity responses refresh the Firebase token once. Revoked sessions and permission denials remain enforced.
- Provider rejection logs now retain the provider request ID and hosting region, without credentials or prompts.

## Provider evidence for Meta support

The same configured key was tested with synthetic prompts only. No operational data or paid weather queries were used.

| Origin / UTC | Egress | Model | Result | Provider request ID |
|---|---|---|---|---|
| Railway Virginia, 20:31 | 152.55.180.33 / US | Spark 1.3 | 404 model_not_found | 8cdf193b-7a2f-4ed0-9763-0ea5b9bde6b0 |
| Railway Virginia, 20:31 | 152.55.180.33 / US | Wonder 1.0 | 404 model_not_found | 5d050145-d423-48dc-a887-2ce964aa78af |
| Local Kenya, 20:31 | 102.203.2.2 / KE | Spark 1.3 | 200 | 5caeaf63-43a2-4ddf-aea6-b4820f969801 |
| Local Kenya, 20:31 | 102.203.2.2 / KE | Wonder 1.0 | 200 | 195b13d4-cd5b-480f-a2cd-37271c8e4eba |
| Railway California, 20:37 | 152.55.178.77 / US | Spark 1.3 | 404 model_not_found | 0cf87bc0-60d4-459b-83cd-ba4b58b8d9fd |
| Railway California, 20:37 | 152.55.178.77 / US | Wonder 1.0 | 404 model_not_found | f691c3fb-5215-49d4-9473-fb009ef2884e |

California initially advertised nine models, then returned an empty catalog. Completion requests failed both initially and over four minutes later. The original Virginia region was restored because moving regions did not resolve the failure. No proxy, VPN or Contributor model was introduced.

Please ask Meta to investigate these request IDs and hosting IP classification/routing. Their [published geographic policy](https://dev.meta.ai/legal/geographic-use-policy/) does not list Kenya or the US as restricted territories; the observations do not establish the precise reason for denial. This report has not been sent.

## Validation and release

Local checks passed: frontend production build and TypeScript; backend build and test types; 12 provider/draft unit tests; PostgreSQL migration and schema verification; private-draft and portal integration tests; 15 distinct browser regression checks across the mission/weather suites (including expired and revoked sessions).

Staging migration `2d495b7d-51ae-4091-bf0c-4949f477e25a` completed with `MIGRATION_VERIFIED` at 20:41 UTC. Frontend release: `eee246d0-b8e8-4d61-9b84-4f3a5c346efd`. Runtime deployment verification is recorded below when complete.

The diagnostic compared credential fingerprints inside the API and via Railway's local environment injection; they matched. The failure is not explained by different keys. The public portal returned HTTP 200 after frontend release.

Final verification at 20:46 UTC:
- Backend release `3a9ad29f-6db9-400e-8c62-4e3ebc681dcc`: SUCCESS; API readiness and portal `/api/ready` proxy returned HTTP 200.
- Frontend release `eee246d0-b8e8-4d61-9b84-4f3a5c346efd`: SUCCESS.
- Running backend contains authenticated draft transactions and credential rejection. The live table has forced row-level security and the four required runtime privileges; an unscoped runtime read exposes zero rows.
- Both requested models still fail from the final Virginia container (US egress `152.55.181.196`). Catalog: empty, request `532ff0e4-950b-4867-b5bf-48ee47da1e41`. Spark: 404 `model_not_found`, request `8b992ee2-3593-4685-8c3f-1836239a2582`. Wonder: 404 `model_not_found`, request `46c739e9-8e7b-4ccc-a287-f675972c3760`.

**Remaining blocker:** Meta must resolve hosted model availability; a working end-to-end AI answer has not been verified. The application repairs are deployed from the local working trees. Their source changes remain uncommitted in the two repositories.
