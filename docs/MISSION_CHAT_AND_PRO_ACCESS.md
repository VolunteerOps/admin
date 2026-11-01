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
- Live synthetic Muse Spark check: `muse-spark-1.3` called `read_mission_requirements` and returned a response matching the mission draft contract. No real mission was created.
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
