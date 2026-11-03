import { test, expect } from "@playwright/test";
import { fixtures } from "./fixtures";

test("agent run shows its harness transcript and branches a follow-up", async ({ page }) => {
  await fixtures(page);
  await page.goto("/agent-runs");
  await page.getByRole("button", { name: /Eastlands staffing proposal/ }).click();
  await expect(page.getByText("Read mission and candidates")).toBeVisible();
  await expect(page.getByText("Depot stock checked")).toBeVisible();
  await expect(page.getByText("read_mission")).toBeVisible();
  await expect(page.getByText("Plan staffing for Saturday")).toBeVisible();
  await page.getByLabel("Follow-up question").fill("What changes if capacity drops to four?");
  await page.getByRole("button", { name: "Run follow-up" }).click();
  await expect(page.getByText("Follow-up run created from this run.")).toBeVisible();
});
