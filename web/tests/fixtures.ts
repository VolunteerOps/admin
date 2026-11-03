import type { Page } from "@playwright/test";
export const org = "11111111-1111-4111-8111-111111111111",
  region = "22222222-2222-4222-8222-222222222222",
  mission = "33333333-3333-4333-8333-333333333333";
const now = Math.floor(Date.now() / 1000),
  jwt = [
    { alg: "none" },
    {
      sub: "fixture-coordinator",
      user_id: "fixture-coordinator",
      aud: "resq-test",
      iss: "https://securetoken.google.com/resq-test",
      iat: now,
      auth_time: now,
      exp: now + 3600,
      email: "amina@example.test",
      email_verified: true,
      firebase: { sign_in_provider: "password" },
    },
    "signature",
  ]
    .map((x, i) =>
      i === 2 ? x : Buffer.from(JSON.stringify(x)).toString("base64url"),
    )
    .join(".");
export const missionRow = {
  id: mission,
  kind: "MISSION",
  version: 1,
  ownerId: 7,
  updatedAt: "2026-11-01T08:15:00Z",
  payload: {
    title: "Eastlands community preparedness",
    brief:
      "Coordinate the local readiness team and deliver essential supplies to the community centre.",
    meeting_point: "Eastlands community centre",
    starts_at: "2026-11-02T08:00:00Z",
    ends_at: "2026-11-02T14:00:00Z",
    capacity: 24,
    assigned_count: 18,
    status: "STAGED",
    required_skills: ["First aid", "Community response"],
    latitude: -1.28,
    longitude: 36.86,
  },
};
export const stockRow = {
  id: "44444444-4444-4444-8444-444444444444",
  kind: "INVENTORY",
  version: 2,
  ownerId: 7,
  updatedAt: "2026-11-01T08:00:00Z",
  payload: {
    title: "Emergency first aid kits",
    unit: "kits",
    location: "Nairobi central store",
    quantity: 48,
    reserved: 12,
  },
};
export function record(kind: string) {
  if (kind === "MISSION") return missionRow;
  if (kind === "INVENTORY") return stockRow;
  const names: Record<string, string> = {
    COURSE: "Community first aid",
    CAMPAIGN: "Coastal rainy-season readiness",
    APPLICATION: "Application · Eastlands readiness",
    ASSIGNMENT: "Eastlands field team",
    REPORT: "Water access at the community centre",
    HOURS: "Community preparation shift",
    CREDENTIAL: "First aid",
    MESSAGE: "Briefing and meeting point confirmed",
    BULLETIN: "Tomorrow’s regional briefing",
    STOCK_LEDGER: "First aid kit replenishment",
    EVENT: "MISSION_STAGED",
    SUPPORT: "Help reviewing the dispatch plan",
    ENROLLMENT: "First aid assessment",
  };
  const statuses: Record<string, string> = {
    COURSE: "DRAFT",
    CAMPAIGN: "ACTIVE",
    APPLICATION: "APPLIED",
    ASSIGNMENT: "ASSIGNED",
    REPORT: "SUBMITTED",
    HOURS: "PENDING",
    CREDENTIAL: "VERIFIED",
    BULLETIN: "PUBLISHED",
    SUPPORT: "OPEN",
    ENROLLMENT: "AWAITING_REVIEW",
  };
  return {
    id: "55555555-5555-4555-8555-555555555555",
    kind,
    version: 1,
    ownerId: 8,
    parentId: mission,
    updatedAt: "2026-11-01T07:40:00Z",
    payload: {
      title: names[kind],
      status: statuses[kind],
      body: "The local team has reviewed the meeting point and is preparing for tomorrow’s response.",
      skill: "First aid",
      submitted_hours: 4,
      valid_until: "2027-11-01T00:00:00Z",
      delta: 12,
      reason: "Regional supply replenishment",
      record_kind: "MISSION",
      ...(kind==="COURSE"?{capacity:20,enrolled_count:8,lessons:[{title:"First response"}],prerequisites:[]}:{}),
      ...(kind==="CAMPAIGN"?{description:"Review supplies and confirm safe community meeting points.",owner:"Coastal team",due_at:"2026-11-15T09:00:00Z",tasks:[{id:"88888888-8888-4888-8888-888888888888",title:"Review water stock",completed:false}]}:{}),
    },
  };
}
const person = {
  id: "66666666-6666-4666-8666-666666666666",
  userId: 7,
  role: "ORG_ADMIN",
  status: "ACTIVE",
  version: 1,
  regionIds: [],
  user: { fullName: "Amina Mwangi", email: "amina@example.test" },
};
export async function fixtures(
  page: Page,
  {
    signedIn = true,
    empty = false,
    denied = false,
    verified = true,
    approved = true,
  }: {
    signedIn?: boolean;
    empty?: boolean;
    denied?: boolean;
    verified?: boolean;
    approved?: boolean;
  } = {},
) {
  const mutations: any[] = [];
  let currentKind = "REPORT";
  if (signedIn)
    await page.addInitScript(
      ({ jwt, verified }) =>
        sessionStorage.setItem(
          "firebase:authUser:test-api-key:resqops-react",
          JSON.stringify({
            uid: "fixture-coordinator",
            email: "amina@example.test",
            displayName: "Amina Mwangi",
            emailVerified: verified,
            isAnonymous: false,
            providerData: [],
            stsTokenManager: {
              refreshToken: "fixture-refresh",
              accessToken: jwt,
              expirationTime: Date.now() + 3600000,
            },
            apiKey: "test-api-key",
            appName: "resqops-react",
          }),
        ),
      { jwt, verified },
    );
  await page.route("**/*", async (route) => {
    const req = route.request(),
      u = new URL(req.url()),
      p = u.pathname;
    let body: any = {},
      status = 200;
    if (p === "/sentinel/config")
      body = {
        firebase: {
          apiKey: "test-api-key",
          projectId: "resq-test",
          authDomain: "resq-test.firebaseapp.com",
          appId: "1:123:web:test",
        },
      };
    else if (p === "/sentinel/map-widget-config")
      body = { token: "", origins: [] };
    else if (u.hostname === "identitytoolkit.googleapis.com") {
      if (p.endsWith(":signInWithPassword")) {
        status = 400;
        body = { error: { code: 400, message: "INVALID_LOGIN_CREDENTIALS" } };
      } else
        body = {
          users: [
            {
              localId: "fixture-coordinator",
              email: "amina@example.test",
              displayName: "Amina Mwangi",
              emailVerified: verified,
              providerUserInfo: [],
            },
          ],
        };
    } else if (u.hostname === "securetoken.googleapis.com")
      body = {
        access_token: jwt,
        id_token: jwt,
        refresh_token: "fixture-refresh",
        expires_in: "3600",
        token_type: "Bearer",
        user_id: "fixture-coordinator",
        project_id: "resq-test",
      };
    else if (p.startsWith("/api/")) {
      if (req.method() !== "GET") {
        mutations.push({
          path: p,
          body: req.postDataJSON(),
          key: req.headers()["idempotency-key"],
        });
        body = { ok: true };
      }
      if (["/api/auth/me", "/api/auth/firebase/bootstrap"].includes(p))
        body = {
          user: { id: 7, fullName: "Amina Mwangi" },
          platform_access: "ALLOWED",
          memberships: approved
            ? [
                {
                  organization_id: org,
                  organization_name: "ResQ Kenya",
                  role: "ORG_ADMIN",
                  access: "ALLOWED",
                },
              ]
            : [],
        };
      else if (denied && p.includes("/operations/")) {
        status = 403;
        body = { error: "CAPABILITY_REQUIRED", message: "No access" };
      } else if (p.endsWith("/sentinel/regions"))
        body = [
          {
            id: region,
            name: "Nairobi East",
            forecastBounds: {
              west: 36.7,
              east: 37.1,
              south: -1.5,
              north: -1.1,
            },
            forecastPolicy: {
              version: 1,
              heat_c: 35,
              wind_mps: 15,
              hourly_rain_mm: 20,
              maximum_run_age_hours: 8,
            },
          },
        ];
      else if (p.endsWith("/overview") && !p.startsWith("/api/platform"))
        body = {
          as_of: "2026-11-01T08:20:00Z",
          counts: {
            active_missions: empty ? 0 : 3,
            staged_missions: empty ? 0 : 2,
            checked_in: empty ? 0 : 18,
            awaiting_review: empty ? 0 : 7,
          },
          missions: empty ? [] : [missionRow],
          dispatch: { paused: false },
          lanes: empty
            ? []
            : [
                {
                  key: "applications",
                  count: 4,
                  destination: "Applications",
                  action: "Review applications",
                  items: [record("APPLICATION")],
                },
                {
                  key: "reports",
                  count: 2,
                  destination: "Reports",
                  action: "Review field reports",
                  items: [record("REPORT")],
                },
                {
                  key: "hours",
                  count: 1,
                  destination: "Hours",
                  action: "Approve contributions",
                  items: [record("HOURS")],
                },
              ],
          activity: empty ? [] : [record("EVENT")],
        };
      else if (p==="/api/me/portal-preferences") body={version:0,payload:null};
      else if(p==="/api/platform/promotions") body=[];
      else if(p.endsWith('/notifications')) body={items:[],next_cursor:null};
      else if (p.endsWith("/records")) {
        currentKind = u.searchParams.get("kind") || "MISSION";
        body = {
          items: empty ? [] : [record(u.searchParams.get("kind") || "MISSION")],
          next_cursor: null,
        };
      } else if (p.includes("/records/"))
        body = p.endsWith(mission)
          ? missionRow
          : p.endsWith(stockRow.id)
            ? stockRow
            : record(currentKind);
      else if (p.endsWith("/memberships"))
        body = [
          person,
          {
            ...person,
            id: "77777777-7777-4777-8777-777777777777",
            userId: 8,
            role: "VOLUNTEER",
            user: { fullName: "Daniel Otieno", email: "daniel@example.test" },
          },
        ];
      else if (
        p.endsWith("/invitations") ||
        p.endsWith("/units") ||
        p.endsWith("/join-requests")
      )
        body = [];
      else if (p.endsWith("/directory"))
        body = { items: [], next_cursor: null };
      else if (p === "/api/me/home")
        body = {
          profile: {
            version: 1,
            displayName: "Amina Mwangi",
            biography: "Community response coordinator.",
            region: "Nairobi",
            skills: ["First aid"],
            availability: "FLEXIBLE",
            privateDetails: {},
          },
          directory: {
            version: 1,
            discoverable: true,
            publicPreview: false,
            showAvailability: true,
          },
        };
      else if (p === "/api/me/workspaces") body = { items: [] };
      else if (p === "/api/me/profile-shares" || p.endsWith("/partnerships"))
        body = [];
      else if (p === "/api/platform/overview")
        body = {
          grants: [{ userId: 7, active: true, role: "TECH_ADMIN" }],
          provider_readiness: {
            identity: "CONFIGURED_UNVERIFIED",
            email: "NOT_CONFIGURED",
          },
        };
      else if (p === "/api/me/subscription")
        body = {
          resqops_pro: true,
          source: "REVENUECAT",
          expires_at: "2026-12-01T00:00:00Z",
        };
      else if (p === "/api/me/usage")
        body = { assistant: { used: 3, limit: 20 } };
      else if (p.endsWith("/pro/sponsorships")) body = [];
      else if (p.endsWith("/forecasts/latest"))
        body = {
          status: "UNAVAILABLE",
          run: null,
          cells: [],
          assessment: null,
        };
      else if (p.endsWith("/hazards")) body = {source:"USGS",observations:[],truncated:false};
      else if (p.endsWith("/radar"))
        body = {
          generated_at: "2026-11-01T08:15:00Z",
          pins: [
            {
              id: "66666666-6666-4666-8666-666666666666", type: "BUILDING_COLLAPSE", category: "ARTIFICIAL", status: "REPORTED",
              title: "Apartment building collapses in Eastlands", latitude: -1.28, longitude: 36.82, severity: "HIGH",
              source: "GDACS", source_url: "https://example.org/report", observed_at: "2026-11-01T08:00:00Z",
              detail: "Rescue operations underway.", route: { label: "Resource planning", href: "/resource-planning" },
            },
            {
              id: "forecast-rain", type: "SEVERE_RAIN", category: "PREDICTION", status: "PREDICTED",
              title: "Severe rain indicator", latitude: -1.3, longitude: 36.9, severity: "MODERATE",
              source: "WeatherNext 3 forecast", observed_at: "2026-11-02T08:00:00Z", detail: "Upper-range rainfall above threshold.",
              route: { label: "Weather analysis", href: "/weather-analysis" },
            },
          ],
          disclaimer: "Fixture pins are not impact estimates.",
        };
      else if (p.includes("/incidents"))
        body = req.method() === "POST" && p.endsWith("/ingest") ? { created: 2, feeds: [{ name: "GDACS", status: "OK", count: 5 }] } : { items: [], next_cursor: null };
      else if (p.includes("/agent/runs")) {
        const runDetail = {
          id: "66666666-6666-4666-8666-666666666666",
          kind: "RESOURCE_PLAN",
          version: 1,
          updatedAt: "2026-11-01T08:15:00Z",
          payload: {
            title: "Eastlands staffing proposal",
            status: "DRAFT",
            model: "muse-spark-1.3",
            recommendation: { summary: "Allocate qualified responders.", allocations: [], shortages: [], decisions: [] },
            request: { question: "Plan staffing", mode: "EXECUTE", history: [{ role: "user", content: "Plan staffing for Saturday" }] },
            harness: {
              rounds: 2,
              plan: [{ title: "Read mission and candidates", status: "done" }],
              notes: ["Depot stock checked"],
              steps: [{ tool: "read_mission", ok: true, duration_ms: 12 }],
            },
          },
        };
        body =
          req.method() === "POST"
            ? { ...runDetail, id: "77777777-7777-4777-8777-777777777777", payload: { ...runDetail.payload, parent_run_id: runDetail.id } }
            : p.endsWith("/agent/runs")
              ? { items: [{ id: runDetail.id, kind: "RESOURCE_PLAN", version: 1, updatedAt: runDetail.updatedAt, title: "Eastlands staffing proposal", status: "DRAFT", model: "muse-spark-1.3", analysis_type: null, parent_run_id: null, steps: 1, plan_steps: 1, summary: "Allocate qualified responders." }] }
              : runDetail;
      } else if (p.includes("/specialised"))
        body =
          req.method() === "POST" && !p.includes("/evidence") && !p.includes("/gap")
            ? {
                id: "55555555-5555-4555-8555-555555555555",
                kind: "SPECIALISED",
                version: 1,
                updatedAt: "2026-11-01T08:15:00Z",
                payload: {
                  title: "Flood preparedness synthesis",
                  status: "DRAFT",
                  analysis_type: "FLOOD_SYNTHESIS",
                  model: "muse-wonder-1.0",
                  result: {
                    summary: "Gauge and rainfall evidence reviewed.",
                    findings: ["Gauge near watch level"],
                    recommendations: ["Check pumps"],
                    missing_inputs: ["River levels"],
                    source_ids: [],
                  },
                  flood_impact_estimate: null,
                },
              }
            : { items: [], next_cursor: null };
      else if (p.includes("/planning/"))
        body = {
          items: empty
            ? []
            : [
                {
                  ...missionRow,
                  kind: p.endsWith("weather")
                    ? "WEATHER_ANALYSIS"
                    : "RESOURCE_PLAN",
                  payload: {
                    title: p.endsWith("weather")
                      ? "Nairobi rainfall preparedness"
                      : "Eastlands staffing proposal",
                    status: "DRAFT",
                    model: p.endsWith("weather")
                      ? "Muse Wonder 1.0"
                      : "Muse Spark 1.3",
                    narrative: {
                      summary:
                        "Review readiness for possible heavy rainfall. Local flood impact remains uncertain.",
                      assumptions: ["Forecast indicators only"],
                      missing_inputs: ["Local drainage observations"],
                      preparedness: ["Review equipment locations"],
                    },
                    recommendation: {
                      summary:
                        "Allocate qualified, available responders to the planned mission.",
                      allocations: [],
                      shortages: ["Two first aid responders"],
                      decisions: ["Confirm access conditions"],
                    },
                    snapshot: {
                      mission: missionRow.payload,
                      candidates: [],
                      resources: [],
                    },
                    evidence: {points:[{longitude:36.82,latitude:-1.28}],policy_version:1},
                    source: {
                      init_time: "2026-11-01T06:00:00Z",
                      source: "WeatherNext 3",
                      source_id: "fixture-only",
                    },
                  },
                },
              ],
          next_cursor: null,
        };
      else if (p.endsWith("/cases"))
        body = {
          items: empty
            ? []
            : [
                {
                  ...missionRow,
                  kind: "ASSISTANCE_CASE",
                  payload: {
                    summary: "Community team requests transport support.",
                    severity: "HIGH",
                    status: "OPEN",
                    source: "PHONE",
                    response_due_at: "2026-11-01T09:00:00Z",
                  },
                },
              ],
        };
      else if (p.endsWith("/dispatch")) body = { paused: false, version: 0 };
      else if (p.endsWith("/metrics"))
        body = {
          mission_count: 8,
          active_missions: 3,
          completed_missions: 5,
          report_count: 12,
          approved_hours: 96,
          recorded_hours:120,
          verified_qualifications:12,
          pending_hour_reviews: 2,
        };
      else if (p === "/api/platform/organizations")
        body = [
          {
            id: org,
            displayName: "ResQ Kenya",
            slug: "resq-kenya",
            status: "ACTIVE",
            enrollmentReady: true,
            version: 2,
          },
        ];
      else if (p === "/api/organizations/" + org)
        body = {
          id: org,
          displayName: "ResQ Kenya",
          status: "ACTIVE",
          version: 2,
          enrollmentReady: true,
        };
    } else if (u.hostname === "127.0.0.1") return route.continue();
    else return route.abort();
    await route.fulfill({
      status,
      contentType: "application/json",
      headers: { "access-control-allow-origin": "*" },
      body: JSON.stringify(p.startsWith("/api/") ? wire(body) : body),
    });
  });
  return mutations;
}

function wire(value:any):any {
 if(Array.isArray(value))return value.map(wire);
 if(value&&typeof value==='object')return Object.fromEntries(Object.entries(value).map(([k,v])=>[k.replace(/[A-Z]/g,c=>'_'+c.toLowerCase()),wire(v)]));
 return value;
}
