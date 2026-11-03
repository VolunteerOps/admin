import { test, expect } from "@playwright/test";
import { fixtures, mission, missionRow } from "./fixtures";
test("weather assessment carries its source into resource planning", async ({
  page,
}) => {
  await fixtures(page);
  await page.goto("/weather-analysis");
  await page.getByRole("button", { name: "Review assessment" }).click();
  await page
    .getByRole("link", { name: "Plan resources from this evidence" })
    .click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(
    page.getByLabel("Weather assessment (optional)", { exact: true }),
  ).toHaveValue(mission);
  await page.screenshot({
    path: "../designs/next-portal/weather-to-planning.png",
    fullPage: true,
  });
});
test("mission directory continues beyond the first backend page", async ({
  page,
}) => {
  await fixtures(page);
  await page.route("**/operations/records?kind=MISSION*", (route) => {
    const more = new URL(route.request().url()).searchParams.has("after");
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        items: [
          more
            ? {
                ...missionRow,
                id: "99999999-9999-4999-8999-999999999999",
                payload: {
                  ...missionRow.payload,
                  title: "Second response mission",
                },
              }
            : missionRow,
        ],
        next_cursor: more ? null : 50,
      }),
    });
  });
  await page.goto("/missions");
  await page.getByRole("button", { name: "Load more", exact: true }).click();
  await expect(
    page.getByText("Second response mission", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText(missionRow.payload.title, { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Load more", exact: true }),
  ).toHaveCount(0);
});
test("forecast layer controls use real evidence values and valid-time selection", async ({
  page,
}) => {
  await fixtures(page);
  const stats = { mean: 2, p10: 1, p25: 1.4, p50: 1.8, p75: 2.5, p90: 3 };
  await page.route("**/forecasts/latest", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        status: "AVAILABLE",
        cells: [
          {
            longitude: 36.86,
            latitude: -1.28,
            initTime: "2026-11-01T06:00:00Z",
            validTime: "2026-11-01T07:00:00Z",
            leadHours: 1,
            values: {
              total_precipitation_1hr: stats,
              temperature_2m: { ...stats, mean: 24 },
              wind_speed_10m: { ...stats, mean: 5 },
            },
          },
        ],
      }),
    }),
  );
  await page.goto("/weather-analysis");
  await expect(
    page.getByText("Hourly rainfall: 2.0 mm", { exact: true }),
  ).toBeVisible();
  await page.getByLabel("Forecast layer").selectOption("temperature_2m");
  await expect(
    page.getByText("Temperature: 24.0 °C", { exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: "../designs/next-portal/forecast-controls.png",
    fullPage: true,
  });
});
