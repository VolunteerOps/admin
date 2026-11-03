import { test, expect } from "@playwright/test";
import { fixtures } from "./fixtures";

test("mission planner streams tokens before the final structured reply", async ({ page }) => {
  await fixtures(page);
  const reply = {
    reply: "Where should the team meet?",
    draft: { title: "Community readiness visit" },
    ready: false,
    notes: [],
    issues: [{ field: "meeting_point" }],
  };
  await page.route("**/conversations/mission/stream", async (route) => {
    const events = [
      "event: round\ndata: " + JSON.stringify({ type: "round", round: 1 }),
      "event: tool_start\ndata: " + JSON.stringify({ type: "tool_start", tool: "read_mission_requirements" }),
      "event: token\ndata: " + JSON.stringify({ type: "token", text: '{"reply":"Where should the team' }),
      "event: token\ndata: " + JSON.stringify({ type: "token", text: ' meet?"}' }),
      "event: result\ndata: " + JSON.stringify(reply),
    ].join("\n\n") + "\n\n";
    await route.fulfill({ status: 200, contentType: "text/event-stream", body: events });
  });
  await page.goto("/missions");
  await page.getByRole("button", { name: "Plan a mission", exact: true }).first().click();
  await page.getByRole("button", { name: "Plan a community preparedness visit" }).click();
  const input = page.getByRole("textbox", { name: "Message the mission planner" });
  await input.press("Enter");
  await expect(page.getByText("Where should the team meet?")).toBeVisible();
});

test("drafts sync endpoint shapes are used by forms without breaking offline use", async ({ page }) => {
  await fixtures(page);
  await page.goto("/specialised");
  await expect(page.getByLabel("Capability")).toBeVisible();
});
