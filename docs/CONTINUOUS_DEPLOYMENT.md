# Main-branch releases

The `Foundation contracts and web build` workflow validates every push and pull request. After validation succeeds, a push to `main` (or a manual run on `main`) deploys the exact checked-out commit to the existing Railway **staging** environment. Pull requests and other branches cannot deploy.

- Railway project: `73690bd6-2d68-4ac7-9330-b5da3ebed8f0` (ResQ / Volunteer Ops).
- Staging environment: `9de4cdd7-abad-4a36-ae1e-7577c06dc2d7`.
- Admin service: `926250e7-bb4e-4570-99f5-41611d93ede3` (`resq-admin`).
- Portal: https://www.admin.resq-app.co.ke/.

GitHub's `railway-staging` environment permits only `main` and stores a Railway **project token scoped to staging** as the encrypted `RAILWAY_TOKEN` environment secret. No personal CLI login or backend credentials belong in the workflow. Configure this secret before the first release; rotate it in Railway and GitHub together.

The verification job builds the Kotlin business module and uploads those generated files as a commit-specific artifact. The deployment job downloads that artifact, so Railway builds the frontend with the same business rules that passed verification. Railway CLI is pinned to `5.62.1`.

`scripts/deploy-railway.mjs` records the GitHub SHA in the release message and waits for the returned deployment ID to become `SUCCESS`, then checks the portal over HTTPS. An upload, a different deployment's success, or a skipped build cannot count as a successful release. Deployment links appear in the Actions job summary.

Runs for the same Git ref are serialized and an active release is not cancelled by a newer push. GitHub may replace an older pending run with the newest pending run. Failed checks prevent deployment; a failed deployment fails the workflow. After resolving a failure, rerun the failed job or manually dispatch the workflow on `main`.

This pipeline does not deploy the separate marketing website, volunteer application, or Railway production environment. Backend releases are managed by the backend repository's own workflow, with migration verification before API and forecast-worker deployment.
