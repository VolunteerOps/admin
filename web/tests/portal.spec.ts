import { test, expect } from "@playwright/test";
import { fixtures } from "./fixtures";
import { areas, titles } from "../src/lib/catalog";
import { commandSchema } from "../../../backend/src/modules/operations/operations.schema";
test("desktop sign-in, recovery and registration are native web forms", async ({
  page,
}) => {
  await fixtures(page, { signedIn: false });
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Let’s get to work." }),
  ).toBeVisible();
  await page.screenshot({
    path: "../designs/next-portal/sign-in-desktop.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Forgot password?" }).click();
  await expect(
    page.getByRole("heading", { name: "Reset your password" }),
  ).toBeVisible();
  await page.screenshot({
    path: "../designs/next-portal/recovery-desktop.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Back to sign in" }).click();
  await page.getByRole("button", { name: "Create an account" }).click();
  await expect(page.getByLabel("Full name")).toBeVisible();
  await page.screenshot({
    path: "../designs/next-portal/register-desktop.png",
    fullPage: true,
  });
});
for (const route of [
  ...areas.flatMap((a) => a.pages.map((p) => p[0])),
  "profile",
  "privacy",
  "securityAccount",
  "invitations",
  "pro",
  "support",
])
  test("visual and navigation: " + route, async ({ page }) => {
    const errors: string[] = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await fixtures(page);
    await page.goto("/" + route);
    await expect(
      page.getByRole("heading", { level: 1, name: titles[route], exact: true }),
    ).toBeVisible();
    await expect(page.getByLabel("Region", { exact: true })).toHaveValue(
      "22222222-2222-4222-8222-222222222222",
    );
    await page.waitForTimeout(400);
    expect(errors).toEqual([]);
    await expect(page.getByRole("heading",{name:"This section could not open"})).toHaveCount(0);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBeTruthy();
    await page.screenshot({
      path: `../designs/next-portal/${route}-desktop.png`,
      fullPage: true,
    });
  });
test("create mission sends the backend contract and an idempotency key", async ({
  page,
}) => {
  const requests = await fixtures(page);
  await page.goto("/missions");
  await page
    .getByRole("button", { name: "Plan a mission", exact: true })
    .first()
    .click();
  await page.getByRole("button", {name:"Use form", exact:true}).click();
  await page.getByLabel("Mission name").fill("Neighbourhood readiness");
  await page
    .getByLabel("Mission brief")
    .fill("Prepare a qualified team for the neighbourhood response.");
  await page.getByLabel("Meeting point").fill("Nairobi community centre");
  await page.getByLabel("Starts", { exact: true }).fill("2026-11-02T09:00");
  await page.getByLabel("Ends", { exact: true }).fill("2026-11-02T15:00");
  await page.getByLabel("Latitude", { exact: true }).fill("-1.28");
  await page.getByLabel("Longitude", { exact: true }).fill("36.82");
  await page.screenshot({
    path: "../designs/next-portal/mission-create-desktop.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Save mission draft" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  const sent = requests.find((r) => r.body.action === "CREATE_MISSION");
  expect(commandSchema.safeParse(sent.body).success).toBe(true);
  expect(sent.key).toMatch(/^[a-f0-9-]{36}$/);
});
test("empty, denied, dark and responsive views remain usable", async ({
  browser,
}) => {
  for (const width of [390, 768, 1440]) {
    const page = await browser.newPage({ viewport: { width, height: 1000 } });
    await fixtures(page, { empty: true });
    await page.goto("/overview");
    await expect(
      page.getByRole("heading", { name: "Overview", exact: true }),
    ).toBeVisible();
    await expect(page.getByText("You’re up to date")).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBeTruthy();
    await page.screenshot({
      path: `../designs/next-portal/overview-empty-${width}.png`,
      fullPage: true,
    });
    await page.getByRole("button", { name: "Switch theme" }).click();
    await page.screenshot({
      path: `../designs/next-portal/overview-dark-${width}.png`,
      fullPage: true,
    });
    if (width < 850) {
      await page.getByRole("button", { name: "Open navigation" }).click();
      await expect(
        page.getByRole("navigation", { name: "Main navigation" }),
      ).toBeVisible();
      await page.screenshot({
        path: `../designs/next-portal/navigation-${width}.png`,
      });
    }
    await page.close();
  }
  const page = await browser.newPage();
  await fixtures(page, { denied: true });
  await page.goto("/missions");
  await expect(page.locator(".notice[role=alert]")).toContainText(
    "workspace role does not allow",
  );
  await page.close();
});
