# Mission chat and ResQOps Pro — 1 November 2026

## Root cause

`POST /organizations/:orgId/sentinel/regions/:regionId/conversations/mission` already invoked `muse-spark-1.3`. Its `agent.chat` capability check first required ResQOps Pro. The staging API had `META_MODEL_API_KEY` but no RevenueCat credentials, causing `BILLING_NOT_CONFIGURED` before any model request. The portal grouped this with unrelated setup failures and displayed a misleading generic error.

## Changes

- Kept paid access, coordinator permissions, region scope and daily usage limits.
- Connected staging API customer verification to the existing ResQ RevenueCat project (`projf7905c83`) using the ResQOps app's public v1 customer-read credential in `REVENUECAT_OPS_API_KEY`. Existing secret-key configurations retain precedence. The backend derives the customer ID from the authenticated Firebase identity; the client cannot choose an account to verify. Grant management remains privileged through RevenueCat, not the portal or the public key.
- Granted `resqops_pro` (`entlcf171013a0`) to the existing Firebase identity for `mtengo.moses@resq-app.co.ke` through 1 November 2027, 23:59:59 UTC. This is a complimentary promotional grant, not a charged or auto-renewing purchase. Expiration, revocation, refund and sandbox checks remain enforced.
- Replaced the form-like assistant with a scrollable transcript, immediate outgoing messages, thinking state, fixed composer, starter prompts, Enter-to-send, Shift+Enter, retry/edit controls, and a separate draft review. Creating a mission still requires explicit confirmation; chatting cannot assign or dispatch volunteers.
- Kept request keys across uncertain network failures to recover completed replies. Definite provider failures can start a new attempt; completed unsuccessful attempts return a recoverable message instead of appearing permanently in progress.
- Separated billing setup failures from subscription-check outages and provider failures in the plain-language error map.

## Verification

- Live RevenueCat check using the actual backend function: `VERIFIED`, `resqops_pro: true`.
- Local live synthetic Muse Spark check: `muse-spark-1.3` called `read_mission_requirements` and returned a response matching the mission draft contract. No real mission was created. This verified the local connection, not the hosted Railway connection; see the provider investigation below.
- 15 focused backend tests passed, including paid gate enforcement, valid promotional access, expired/revoked/sandbox denial, controlled model tools and conversation constraints. Backend and test type-checks passed.
- 101 portal browser checks passed. New chat checks cover optimistic messages, retries, editing failures, keyboard behavior, explicit review, scroll locking and responsive layouts. After correcting dark-mode user-message contrast, all 10 chat/error checks passed again on the final production build.
- Browser tests use fixture accounts and API responses. They do not sign in as the administrator or prove a real authenticated mission creation.

## Billing boundary

Subscription verification and the administrator's Pro grant are connected. This does not complete payment collection for new customers: the existing catalog contains Android and Test Store apps; Google Play service credentials are not configured and no web billing app is configured. Web checkout requires the project's payment-provider setup. Do not represent the complimentary grant as a paid transaction, or enable test purchases for all users to work around payment setup.

Sources: [RevenueCat customer reads](https://www.revenuecat.com/docs/api-v1/customers), [RevenueCat promotional grants](https://www.revenuecat.com/docs/api-v2/customer).

## Release

- Staging API: `c342fcbc-67fd-43dc-aca3-98606f17cf8a`.
- Staging admin: `caf49a92-6749-46b8-bcba-ed631d6ff986`.
- Both deployments passed Railway health checks. Deployed-asset browser smoke returned HTTP 200, rendered the conversation/composer with fixture API responses, and reported no browser errors. The real API health endpoint returned HTTP 200.
- Screenshots: `designs/next-portal/mission-chat-light.png`, `mission-chat-dark.png`, `mission-chat-mobile.png`.

The attempt to create `ResQOps Web` through RevenueCat was refused because no Stripe account is linked. Nothing was created by that attempt. The project owner must first use RevenueCat **account settings → Connect Stripe account**, install the RevenueCat Stripe app, and sign in to RevenueCat from its Stripe app settings. Web configurations live under **Web** in the project dashboard, separately from the mobile Apps list. [Official connection instructions](https://www.revenuecat.com/docs/web/connect-stripe-account).

## Follow-up: hosted model rejection

The subsequent screenshot shows a different failure after Pro verification. Railway receives HTTP 404, `model_not_found`, from `https://api.meta.ai/v1/chat/completions` for the documented model `muse-spark-1.3`. The original catch hid that provider distinction behind `AGENT_UNAVAILABLE`.

Evidence collected on 1 November 2026:

- The hosted request fails in under a second; this is not the mission planner's 60-second deadline.
- The configured credential matches the successful local test (compared by fingerprint without exposing the key).
- A hosted catalog check listed `muse-spark-1.3`, its immediately following completion returned 404, and the next catalog read was empty. Local catalog/completion checks succeeded with the same credential.
- A local synthetic reproduction of the Eastleigh question successfully called mission requirements and Mapbox place lookup and returned a valid reply. No mission was created, no real workspace was read, and no administrator login was simulated.
- Railway deployment `94f29ea7-85b0-4efb-9836-e3ce40ecdb2f` retried the same model on fresh connections and still received `model_not_found`. Bounded retries therefore do not yet establish recovery of the hosted assistant.

Changes retain the requested model, paid access, permissions, tool allowlist, evidence requirements and explicit mission confirmation. The backend now logs safe failure categories and allows at most three retries across a turn for this observed provider failure. It never reruns an already completed tool when retrying an HTTP request. Credentials and raw model output are not logged. The portal preserves the message, offers retry/edit, and distinguishes an unavailable model from a rejected connection.

Validation: backend type check and 13 focused tests passed. All 12 focused browser/error checks passed (11 in the initial run; the remaining assertion passed after correcting a test that matched “lease” inside “please”). The error inventory maps 138 codes across 335 backend error sites. Browser tests use fixtures and are not evidence of a successful live hosted model call.

The final hosted diagnostic also rejected minimal synthetic requests through **both** Chat Completions and Responses with HTTP 404 `model_not_found`. Changing API formats does not resolve the observed provider failure. [Prepared support report with Meta request IDs](META_MISSION_PLANNER_SUPPORT.md). No support message was sent.

The portal update is deployed as `58be129d-f47b-4911-a86a-3f9effd3bbc3`. A browser smoke test against deployed assets verified the new model-unavailable message, preserved outgoing message and fresh retry, with no browser errors; API responses in that test were fixtures. The real API health endpoint returned HTTP 200.

Temporary pre-deployment diagnostics have been removed from the service configuration. Final API deployment `349be9ec-93af-4675-a5af-bde5ad7b685b` succeeded and passed `/api/ready` with no diagnostic pre-deployment command. The operator diagnostic script remains available for deliberate testing and is not part of normal startup.

**Remaining limitation:** Meta must resolve or explain the hosted model rejection. The assistant is wired to the requested model but is not currently operational from Railway. Do not mark it operational until a Railway model/tool loop succeeds. Official references: [Meta models](https://dev.meta.ai/docs/models), [error handling](https://dev.meta.ai/docs/error-handling), [Responses API](https://dev.meta.ai/docs/protocols/responses).
