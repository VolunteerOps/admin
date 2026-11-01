import { test, expect } from "@playwright/test";
import { fixtures } from "./fixtures";
test("unverified and pending accounts have complete web journeys without operational access", async ({
  browser,
}) => {
  for (const state of ["verification", "pending"]) {
    const page = await browser.newPage({
      viewport: { width: 1440, height: 1000 },
    });
    await fixtures(page, {
      verified: state !== "verification",
      approved: false,
    });
    await page.goto("/");
    await expect(
      page.getByRole("heading", {
        name:
          state === "verification"
            ? "Check your inbox."
            : "Your account is ready.",
        exact: true,
      }),
    ).toBeVisible();
    await expect(
      page.getByText("Eastlands community preparedness", { exact: true }),
    ).toHaveCount(0);
    await page.screenshot({
      path: `../designs/next-portal/${state}-desktop.png`,
      fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBeTruthy();
    await page.screenshot({
      path: `../designs/next-portal/${state}-mobile.png`,
      fullPage: true,
    });
    await page.close();
  }
});
