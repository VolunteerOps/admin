import { test, expect } from "@playwright/test";
import { fixtures } from "./fixtures";

test("sentinel radar shows hazard layers, pin detail and planner routing", async ({ page }) => {
  await fixtures(page);
  await page.goto("/sentinel");
  await expect(page.getByRole("heading", { name: "Real-Time Multi-Hazard Radar" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Artificial disasters/ })).toBeVisible();
  await page.getByRole("button", { name: /Apartment building collapses/ }).click();
  await expect(page.getByText("-1.2800, 36.8200")).toBeVisible();
  await expect(page.getByRole("link", { name: "Resource planning" }).first()).toBeVisible();
  await page.getByRole("button", { name: "Ingest incident feeds" }).click();
  await expect(page.getByText("2 new reports added")).toBeVisible();
});

test("dashboard operations map shows live categories", async ({ page }) => {
  await fixtures(page);
  await page.goto("/operations-map");
  await expect(page.getByRole("heading", { name: "Live operations picture" })).toBeVisible();
  await expect(page.locator(".live-badge")).toBeVisible();
  await expect(page.getByRole("button", { name: /Ongoing disasters/ })).toBeVisible();
  await expect(page.getByRole("button", { name: /Volunteer reports/ })).toBeVisible();
});
