import { test, expect } from "@playwright/test";
import { fixtures } from "./fixtures";
import { areas } from "../src/lib/catalog";
for (const route of [
  ...areas.flatMap((a) => a.pages.map((p) => p[0])),
  "profile",
  "privacy",
  "securityAccount",
  "invitations",
  "pro",
  "support",
])
  test("narrow viewport: " + route, async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    const errors: string[] = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await fixtures(page);
    await page.goto("/" + route);
    await expect(page.getByLabel("Region", { exact: true })).toHaveValue(
      "22222222-2222-4222-8222-222222222222",
    );
    await page.waitForTimeout(250);
    expect(errors).toEqual([]);
    await expect(page.getByRole("heading",{name:"This section could not open"})).toHaveCount(0);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBeTruthy();
    await page.screenshot({
      path: `../designs/next-portal/${route}-mobile.png`,
      fullPage: true,
    });
  });
test("phone entry screens use keyboard-accessible native controls", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await fixtures(page, { signedIn: false });
  await page.goto("/");
  await expect(
    page.getByRole("button", { name: "Sign in", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "../designs/next-portal/sign-in-mobile.png",
    fullPage: true,
  });
  await page
    .getByLabel("Email address", { exact: true })
    .fill("invalid@example.test");
  await page.getByLabel("Password", { exact: true }).fill("fixture-password");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.locator(".notice[role=alert]")).toContainText(
    "Check your email and password",
  );
});
