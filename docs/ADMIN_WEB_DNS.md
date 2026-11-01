# Admin portal custom domain

The React/Next.js portal is available at **https://resq-admin-staging.up.railway.app**.

On 1 November 2026, `admin.resq-app.co.ke` resolved to `46.165.235.143`, rather than Railway. Railway reports `DNS_RECORD_STATUS_REQUIRES_UPDATE`; its certificate is still issuing. The application's deployment is separate from this DNS configuration.

In the DNS provider for `resq-app.co.ke`, replace the existing **admin** A record with:

| Type | Name | Target |
| --- | --- | --- |
| CNAME | `admin` | `4b6ch15z.up.railway.app` |

Do not change the root domain, marketing website or mail records. Keep the existing Railway verification record. Wait for DNS propagation and Railway certificate issuance, then open https://admin.resq-app.co.ke. No new Railway domain or service is required.

This is the only deployment step that needs the DNS account owner. The working Railway URL can be used immediately, including email/password authentication.
