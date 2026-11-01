> **Configuration update - 1 November 2026:** At the owner's request, the staging weather integrations are now enabled with a 2 TB BigQuery cap per query and a 1 GB GCS allowance per conversation turn. Billing and runtime bucket authorization are still pending. Earlier disabled/100 MB settings below describe historical checks. See [PORTAL_ASSISTED_WORKFLOWS.md](PORTAL_ASSISTED_WORKFLOWS.md).

# Grant ResQ backend access to WeatherNext

Use your Google Cloud account that owns the ResQ project. The backend already has its service-account credentials; no new key is needed.

Copy this principal:

```text
resq-auth-staging@resq-25be9.iam.gserviceaccount.com
```

## 1. Allow the backend to read both tables

1. Open [BigQuery in ResQ](https://console.cloud.google.com/bigquery?project=resq-25be9).
2. In Explorer, expand **resq-25be9** and select the **weathernext_3 dataset** (the parent of both tables).
3. Choose **Sharing → Permissions**, or **Share → Manage permissions**, then **Add principal**.
4. Paste the service-account email above.
5. Select **BigQuery Data Viewer** (`roles/bigquery.dataViewer`) and save.

Grant this role on the dataset. It covers both `weathernext_3_0_0_0p1deg` and `weathernext_3_0_0_0p05deg`. [Google's dataset access instructions](https://docs.cloud.google.com/bigquery/docs/control-access-to-resources-iam).

## 2. Allow it to run queries in ResQ

1. Open [IAM for resq-25be9](https://console.cloud.google.com/iam-admin/iam?project=resq-25be9).
2. Click **Grant access** and enter the same service-account email.
3. Select **BigQuery Job User** (`roles/bigquery.jobUser`) and save. If editing its existing entry, add this role while preserving its existing roles.

Job User belongs on the project that runs/bills queries; Data Viewer belongs on the dataset being read. [Google's role reference](https://docs.cloud.google.com/bigquery/docs/access-control).

## 3. Verify

Verified on 30 October 2026: both table metadata requests and query dry runs succeed through the backend service account. The grants are complete. Running SQL in the console as your own account does not verify service-account access.

Live query results and the remaining cost/availability checks are recorded in [the implementation checkpoint](PORTAL_IMPLEMENTATION_CHECKPOINT.md). Runtime access remains disabled pending populated-query verification and an operating budget; the standard query ceiling is 100 MB.

The separately authorized 2 TB verification was submitted on 30 October 2026 and rejected for the project's free query-byte quota (`unbilled.analysis`). The BigQuery IAM grants above are working; this is a billing restriction. See [GCS access, billing steps and the source comparison](WEATHERNEXT_GCS_ACCESS_AND_COST.md). GCS data remains in Google's external buckets, and the backend identity currently lacks their object-read/list permissions.

No Owner, Editor, or BigQuery Admin grant is needed. If a sharing control is unavailable, use an account with permission to manage that dataset's access policy.
