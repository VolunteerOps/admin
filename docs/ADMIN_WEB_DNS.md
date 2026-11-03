# Admin portal custom domain

The React/Next.js portal is available at **https://resq-admin-staging.up.railway.app**.

On 1 November 2026, `admin.resq-app.co.ke` resolved to `46.165.235.143`, rather than Railway. Railway reports `DNS_RECORD_STATUS_REQUIRES_UPDATE`; its certificate is still issuing. The application's deployment is separate from this DNS configuration.

In the DNS provider for `resq-app.co.ke`, replace the existing **admin** A record with:

| Type | Name | Target |
| --- | --- | --- |
| CNAME | `admin` | `4b6ch15z.up.railway.app` |

Do not change the root domain, marketing website or mail records. Keep the existing Railway verification record. Wait for DNS propagation and Railway certificate issuance, then open https://admin.resq-app.co.ke. No new Railway domain or service is required.

This is the only deployment step that needs the DNS account owner. The working Railway URL can be used immediately, including email/password authentication.

## Post-domain checklist (rechecked 3 November 2026)

The canonical coordinator URL is now **https://www.admin.resq-app.co.ke/**. It serves the portal (HTTP 200) and proxies `/api/*` and `/sentinel/config` to `resq-api` correctly. Adding the domain in Railway is not the whole change; each item below must also be done, then `resq-api` redeployed:

1. **Backend CORS (`resq-api` service variable `CORS_ORIGIN`)** — ✅ Done 3 November 2026. The live variable now includes `https://www.admin.resq-app.co.ke` (deployment `ae3cd2bc-0e81-48c9-bd7e-e16ad630aef6`, `SUCCESS`); preflight with the new Origin now returns `204` with `Access-Control-Allow-Origin: https://www.admin.resq-app.co.ke`. Before the fix it returned no allow headers (401 fall-through). Note the portal itself calls the API same-origin through the Next.js rewrite proxy, so this gap never blocked the portal's own requests; it blocked direct cross-origin API clients and cookie-session mutations from the new domain.
2. **Firebase authorized domains** — add `www.admin.resq-app.co.ke` in Firebase Console → Authentication → Settings → Authorized domains, so OAuth and email action links work from the new domain.
3. **Mapbox token restrictions** — allow `https://www.admin.resq-app.co.ke` on the public token if URL restrictions are enabled.
4. **Meta Model API key reference** — on `resq-api`, confirm the service variable is a reference to the shared variable (`META_MODEL_API_KEY=${{shared.META_MODEL_API_KEY}}`). A shared variable that is not referenced by the service is not injected. A missing key produces `META_NOT_CONFIGURED`, not the model-unavailable error.

See [MISSION_CHAT_AND_PRO_ACCESS.md](MISSION_CHAT_AND_PRO_ACCESS.md) for why the mission-planner chat error itself is a Meta provider failure (`model_not_found` from Railway), not a domain/CORS failure.
