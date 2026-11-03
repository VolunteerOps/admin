import { test, expect } from "@playwright/test";
import { fixtures, record } from "./fixtures";
import { preferencesSchema } from "../../../backend/src/modules/portal/portal.schema";

test("report downloads use the selected region and interval", async ({
  page,
}) => {
  await fixtures(page);
  const requested: string[] = [];
  await page.route("**/operations/exports/*?*", (r) => {
    const u = new URL(r.request().url());
    requested.push(u.pathname);
    expect(u.searchParams.get("from")).toBeTruthy();
    expect(u.searchParams.get("to")).toBeTruthy();
    expect(r.request().headers().authorization).toMatch(/^Bearer /);
    return r.fulfill({
      contentType: "text/csv",
      body: "record_id,status\r\nfixture,APPROVED\r\n",
    });
  });
  await page.goto("/insights");
  for (const name of ["Export volunteer hours", "Export mission summary"]) {
    const download = page.waitForEvent("download");
    await page.getByRole("button", { name, exact: true }).click();
    expect((await download).suggestedFilename()).toMatch(/^resq-.*\.csv$/);
  }
  expect(requested.map((p) => p.split("/").at(-1))).toEqual([
    "HOURS",
    "MISSION",
  ]);
});

test("notifications record a personal read without removing the activity", async ({
  page,
}) => {
  await fixtures(page);
  let read = false;
  await page.route("**/api/**/notifications", (r) =>
    r.fulfill({
      json: {
        items: [
          {
            id: "77777777-7777-4777-8777-777777777777",
            read,
            created_at: new Date().toISOString(),
            payload: { action: "CREATE_CAMPAIGN", record_kind: "CAMPAIGN" },
          },
        ],
        next_cursor: null,
      },
    }),
  );
  await page.route("**/notifications/*/read", (r) => {
    read = true;
    return r.fulfill({ json: { read: true } });
  });
  await page.goto("/notifications");
  await page.getByRole("button", { name: "Mark as read" }).click();
  await expect(page.getByRole("button", { name: "Mark as read" })).toHaveCount(
    0,
  );
  await expect(
    page.getByRole("link", { name: "Review campaign" }),
  ).toBeVisible();
  await page.getByRole("checkbox", { name: "Unread only" }).check();
  await expect(
    page.getByRole("heading", { name: "You’re up to date" }),
  ).toBeVisible();
  expect(read).toBe(true);
});

test("a promotion is previewed, redeemed on confirmation, and the guide persists only valid preferences", async ({
  page,
}) => {
  await fixtures(page);
  let activated = false,
    redemptions = 0,
    saved: any;
  await page.route("**/api/me/subscription", (r) =>
    r.fulfill({ json: { status: "VERIFIED", resqops_pro: activated } }),
  );
  await page.route("**/api/billing/promotions/preview", (r) =>
    r.fulfill({
      json: {
        code: "WELCOME",
        percent: 100,
        days: 30,
        requires_checkout: false,
      },
    }),
  );
  await page.route("**/api/billing/promotions/redeem", (r) => {
    redemptions++;
    activated = true;
    return r.fulfill({
      json: { activated: true, expires_at: "2026-12-04T00:00:00Z" },
    });
  });
  const payload = {
    name: "Amina Mwangi",
    role: "Coordinator",
    organization: "ResQ Kenya",
    coverage: "Nairobi",
    briefings: true,
    reminders: true,
    step: 4,
    completed: true,
    pro_tour_completed: false,
  };
  await page.route("**/api/me/portal-preferences", async (r) => {
    if (r.request().method() === "PUT") {
      saved = r.request().postDataJSON();
      expect(preferencesSchema.safeParse(saved).success).toBe(true);
    }
    await r.fulfill({ json: { version: 2, payload: saved || payload } });
  });
  await page.goto("/pro");
  await page.getByRole("button", { name: "Review upgrade" }).click();
  await page.getByLabel("Have a promotion code?").fill("WELCOME");
  await page.getByRole("button", { name: "Apply", exact: true }).click();
  await expect(
    page.getByText("Your code covers 30 days of Pro. Activate it below."),
  ).toBeVisible();
  expect(redemptions).toBe(0);
  await page
    .getByRole("button", { name: "Activate promotional access" })
    .click();
  await expect(page.locator(".guide-copy")).toBeVisible();
  expect(redemptions).toBe(1);
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByRole("button", { name: "Finish guide" }).click();
  await expect(
    page.getByRole("link", { name: "Open Ask Sentinel", exact: true }),
  ).toBeVisible();
  expect(saved.pro_tour_completed).toBe(true);
  expect(saved.expected_version).toBe(2);
});

test("partial discounts never grant access when checkout has not been connected", async ({
  page,
}) => {
  await fixtures(page);
  let redeemed = false;
  await page.route("**/api/me/subscription", (r) =>
    r.fulfill({ json: { status: "VERIFIED", resqops_pro: false } }),
  );
  await page.route("**/api/billing/promotions/preview", (r) =>
    r.fulfill({
      json: {
        code: "HALFPRICE",
        percent: 50,
        days: 30,
        requires_checkout: true,
      },
    }),
  );
  await page.route("**/api/billing/promotions/redeem", (r) => {
    redeemed = true;
    return r.fulfill({ status: 409, json: { error: "CHECKOUT_REQUIRED" } });
  });
  await page.route("**/api/billing/web-checkout", (r) => {
    expect(r.request().postDataJSON()).toEqual({ code: "HALFPRICE" });
    return r.fulfill({ json: { configured: false, url: null } });
  });
  await page.goto("/pro");
  await page.getByRole("button", { name: "Review upgrade" }).click();
  await page.getByLabel("Have a promotion code?").fill("HALFPRICE");
  await page.getByRole("button", { name: "Apply", exact: true }).click();
  await expect(
    page.getByText(
      "50% off. Final eligibility and price are confirmed at checkout.",
    ),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Continue to secure checkout" })
    .click();
  await expect(
    page.getByText(/Online checkout is not available yet/),
  ).toBeVisible();
  expect(redeemed).toBe(false);
  await expect(page.locator(".guide-copy")).toHaveCount(0);
});

test("account setup saves all four steps and notification preferences with version checks", async ({
  page,
}) => {
  await fixtures(page);
  let version = 0;
  let payload: any = null;
  const saves: any[] = [];
  await page.route("**/api/me/portal-preferences", async (r) => {
    if (r.request().method() === "PUT") {
      const body = r.request().postDataJSON();
      expect(preferencesSchema.safeParse(body).success).toBe(true);
      expect(body.expected_version).toBe(version);
      saves.push(body);
      const { expected_version, ...fields } = body;
      payload = fields;
      version++;
    }
    await r.fulfill({ json: { version, payload } });
  });
  await page.goto("/setup");
  await page.getByLabel("Full name").fill("Amina Mwangi");
  await page.getByRole("button", { name: "Save and continue" }).click();
  await expect(page.getByLabel("How will you join?")).toBeVisible();
  await page.getByRole("button", { name: "Save and continue" }).click();
  await page
    .getByLabel("Preferred city, district or region")
    .fill("Mombasa, Kenya");
  await page.getByRole("button", { name: "Save and continue" }).click();
  await page.getByRole("checkbox", { name: /Preparedness updates/ }).uncheck();
  await page.getByRole("button", { name: "Finish setup" }).click();
  await expect(
    page.getByRole("heading", { name: "A good place to start." }),
  ).toBeVisible();
  expect(saves).toHaveLength(4);
  expect(payload).toMatchObject({
    completed: true,
    step: 4,
    coverage: "Mombasa, Kenya",
    briefings: false,
    reminders: true,
  });
});

test("campaign checklist uses the newest version and an archived campaign cannot be changed", async ({
  page,
}) => {
  await fixtures(page);
  let campaign: any = structuredClone(record("CAMPAIGN"));
  const commands: any[] = [];
  await page.route("**/operations/records?kind=CAMPAIGN", (r) =>
    r.fulfill({ json: { items: [campaign], next_cursor: null } }),
  );
  await page.route("**/operations/commands", async (r) => {
    const c = r.request().postDataJSON();
    commands.push(c);
    expect(c.expected_version).toBe(campaign.version);
    campaign = {
      ...campaign,
      version: campaign.version + 1,
      payload: {
        ...campaign.payload,
        status: c.action === "ARCHIVE_CAMPAIGN" ? "ARCHIVED" : "ACTIVE",
        tasks: campaign.payload.tasks.map((t: any) =>
          t.id === c.task_id ? { ...t, completed: c.completed } : t,
        ),
      },
    };
    await r.fulfill({ json: campaign });
  });
  await page.goto("/campaigns");
  await page.getByRole("button", { name: "Review checklist" }).click();
  const drawer = page.getByRole("dialog");
  await drawer.getByRole("checkbox").first().check();
  await expect.poll(() => commands.length).toBe(1);
  await drawer
    .getByRole("button", { name: "Archive campaign", exact: true })
    .click();
  await drawer
    .getByLabel("Reason for this decision")
    .fill("The preparations are complete and reviewed.");
  await drawer.getByRole("button", { name: "Confirm archive" }).click();
  await expect(drawer).toHaveCount(0);
  await page.getByLabel("Campaign filter").selectOption("ARCHIVED");
  await page.getByRole("button", { name: "Review checklist" }).click();
  await expect(
    page.getByRole("dialog").getByRole("checkbox").first(),
  ).toBeDisabled();
  expect(commands.map((c) => c.action)).toEqual([
    "UPDATE_CAMPAIGN_TASK",
    "ARCHIVE_CAMPAIGN",
  ]);
});

test("account actions live in the top bar and cancelling logout keeps the workspace open", async ({
  page,
}) => {
  await fixtures(page);
  await page.goto("/overview");
  await page.getByRole("button", { name: "Open account menu" }).click();
  const menu = page.getByRole("navigation", { name: "Account", exact: true });
  for (const name of ["Settings", "Profile", "Help & support"])
    await expect(menu.getByRole("link", { name, exact: true })).toBeVisible();
  await expect(
    page.locator(".sidebar").getByRole("link", { name: "Help & support" }),
  ).toHaveCount(0);
  await menu.getByRole("button", { name: "Log out" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByRole("button", { name: "Stay signed in" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByLabel("Region", { exact: true })).toHaveValue(
    "22222222-2222-4222-8222-222222222222",
  );
});
