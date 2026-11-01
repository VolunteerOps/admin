# ResQOps admin web

React and Next.js own the browser presentation. The Kotlin module in `../business` supplies shared admission, mission validation, transitions, stock and coverage rules. Backend authorization remains authoritative. Native Compose screens stay in `../shared`.

```powershell
npm ci
npm run business:sync
npm run dev
```

Open http://127.0.0.1:4173. For a production preview, use `npm run build` followed by `npm start`. Stop the local server before rebuilding on Windows, because the standalone directory is locked while serving.

`RESQ_BACKEND_ORIGIN` must be set **before building**. It defaults to the staging API. Only `/api/*` and the two public Sentinel configuration endpoints are proxied; `/sentinel` itself is a React page. Firebase public configuration is loaded at runtime. Provider keys, service-account keys and database credentials never belong in this application.

```powershell
npm run typecheck
npm test
```

Error copy is centralized in `src/lib/friendly-errors.ts`. With the sibling backend checkout available, run `npm run audit:errors` to refresh the code/site inventory and message map; it fails for newly discovered unmapped backend codes. Raw provider messages and validation details must never be shown directly. Use `UserFacingError` only for locally authored, reviewed instructions. Forecast requests default to automatic selection of a published forecast; see [the implementation notes](../docs/PLAIN_LANGUAGE_ERRORS_AND_FORECASTS.md).

Browser tests require a running local production preview and Microsoft Edge. They use isolated synthetic fixtures, exercise real React controls, and validate command payloads against the sibling backend's schemas. They never sign in as a real user or submit live weather jobs. Screenshots are saved in `../designs/next-portal`.

The standalone Docker image runs as the unprivileged `node` user on port 8080. `railway.toml` targets the existing staging admin service. The root repository Dockerfile also builds this Next.js app. The retired `webApp/` Compose browser application and its deployment scripts have been removed. Native Compose code remains in `../shared`; shared JS tests and Kotlin policy builds remain available through Gradle.

After changing shared Kotlin rules, run `business:sync` and commit the generated ES modules alongside their source. Deployment contexts use these generated modules and do not require an Android SDK.
