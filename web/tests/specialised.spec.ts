import { test, expect } from "@playwright/test";
import { fixtures } from "./fixtures";

test("specialised flood analysis runs and evidence can be recorded", async ({ page }) => {
  await fixtures(page);
  await page.goto("/specialised");
  await expect(page.getByLabel("Capability")).toBeVisible();
  await page.getByRole("button", { name: "Run Flood synthesis" }).click();
  await expect(page.getByText("Gauge and rainfall evidence reviewed.")).toBeVisible();
  await expect(page.getByText("No local flood-impact estimate is produced from this evidence.")).toBeVisible();
  await page.getByRole("button", { name: "Evidence & tasks" }).click();
  await expect(page.getByRole("heading", { name: "Record local evidence" })).toBeVisible();
  await page.getByLabel("Gauge ID").fill("g-1");
  await page.getByLabel("Name", { exact: true }).fill("River gauge");
  await page.getByLabel("Level (m)", { exact: true }).fill("2.4");
  await page.getByLabel("Watch level (m)").fill("2.5");
  await page.getByRole("button", { name: "Record evidence" }).click();
  await expect(page.getByRole("heading", { name: "Evidence-gap worklist" })).toBeVisible();
});
