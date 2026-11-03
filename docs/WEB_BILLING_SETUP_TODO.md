# Web billing — setup to complete later

Requested deferral: the payment-provider setup can be completed at your convenience. It does not block the portal redesign.

1. Open the [ResQ RevenueCat project](https://app.revenuecat.com/projects/f7905c83/apps). Add a **Web Billing** app and connect your supported payment provider. The existing Android apps and Test Store do not accept web payments.
2. Configure ResQOps products, prices, an offering and the `resqops_pro` entitlement. Choose the actual selling prices; the backend catalogue's proposed prices are not checkout quotes.
3. Create an identified-user **Web Purchase Link** for that offering. Enable discount-code entry if offering partial discounts. Set the successful-checkout return URL to `https://www.admin.resq-app.co.ke/pro-guide` so customers return to the welcome tour. The portal checks their subscription with the backend.
4. Set the public purchase-link URL as `REVENUECAT_WEB_PURCHASE_URL` on the API service. Keep the API's RevenueCat credentials server-side. Never put secret keys in `NEXT_PUBLIC_*` variables.
5. For each partial promotion, create the matching percentage discount in RevenueCat and use the same code in the portal's platform promotion controls. RevenueCat is authoritative for paid-checkout eligibility, redemption limits, duration and final price. Enable, disable and limit the discount there too: portal controls do not create or synchronize provider discounts. A partial code never unlocks Pro before payment.
6. Test checkout, cancellation, a partial discount, webhook/provider verification, and return to the portal with a real test customer before accepting live payments. Confirm the Firebase UID is the RevenueCat customer identifier.

Until configured, the portal must explain that online checkout is not available. It must not show a successful payment or enable Pro from a browser flag. No-charge promotional access is issued separately by the backend with an expiry and a recorded redemption.

Provider documentation: [Web Purchase Links](https://www.revenuecat.com/docs/web/web-billing/web-purchase-links), [Web Billing discounts](https://www.revenuecat.com/docs/web/web-billing/discounts).
