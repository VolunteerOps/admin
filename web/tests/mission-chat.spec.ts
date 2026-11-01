import { test, expect } from "@playwright/test";
import { fixtures } from "./fixtures";
const reply = {
  reply: "Where should the team meet?",
  draft: { title: "Community readiness visit" },
  ready: false,
  notes: [],
  issues: [{ field: "meeting_point" }],
};

test("chat shows outgoing message immediately, retries without duplicating and keeps the composer fixed", async ({
  page,
}) => {
  await fixtures(page);
  const requests: any[] = [];
  let release!: () => void;
  const wait = new Promise<void>((resolve) => (release = resolve));
  await page.route("**/conversations/mission", async (route) => {
    requests.push({
      body: route.request().postDataJSON(),
      key: route.request().headers()["idempotency-key"],
    });
    if (requests.length === 1) {
      await wait;
      await route.fulfill({
        status: 503,
        json: { error: "BILLING_NOT_CONFIGURED" },
      });
    } else await route.fulfill({ json: reply });
  });
  await page.goto("/missions");
  await page
    .getByRole("button", { name: "Plan a mission", exact: true })
    .first()
    .click();
  await page
    .getByRole("button", { name: "Plan a community preparedness visit" })
    .click();
  const input = page.getByRole("textbox", {
    name: "Message the mission planner",
  });
  await expect(input).toHaveValue("Plan a community preparedness visit");
  await input.press("Enter");
  await expect(
    page.getByRole("log").getByText("Plan a community preparedness visit"),
  ).toBeVisible();
  await expect(page.getByText("Working on your plan…")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Send message" }),
  ).toBeDisabled();
  release();
  await expect(
    page
      .getByRole("region", { name: "Mission planning chat" })
      .getByRole("alert"),
  ).toContainText("Subscription checks are not set up");
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page.getByText(reply.reply)).toBeVisible();
  expect(requests[0].key).toBe(requests[1].key);
  expect(requests[1].body).toEqual(requests[0].body);
  await expect(
    page.getByRole("log").getByText("Plan a community preparedness visit"),
  ).toHaveCount(1);
  await input.fill("Community centre");
  await input.press("Shift+Enter");
  await expect(input).toHaveValue("Community centre\n");
  expect(requests).toHaveLength(2);
  expect(
    await page
      .locator(".mission-chat-drawer")
      .evaluate((e) => e.scrollHeight <= e.clientHeight + 1),
  ).toBe(true);
  expect(
    await page.evaluate(
      () => getComputedStyle(document.documentElement).overflow,
    ),
  ).toBe("hidden");
  await page.getByLabel("Close panel").click();
  expect(
    await page.evaluate(() => document.documentElement.style.overflow),
  ).not.toBe("hidden");
});

for (const code of ["AGENT_UNAVAILABLE", "MODEL_UNAVAILABLE", "META_ACCESS_DENIED"])
test(`failed ${code} replies can be edited and retries use a fresh attempt`, async ({
  page,
}) => {
  await fixtures(page);
  const sent: any[] = [];
  await page.route("**/conversations/mission", async (route) => {
    sent.push({
      key: route.request().headers()["idempotency-key"],
      ...route.request().postDataJSON(),
    });
    await route.fulfill(
      sent.length < 3
        ? { status: 503, json: { error: code } }
        : { json: reply },
    );
  });
  await page.goto("/missions");
  await page
    .getByRole("button", { name: "Plan a mission", exact: true })
    .first()
    .click();
  await page
    .getByLabel("Message the mission planner")
    .fill("Plan relief distribution");
  await page.getByLabel("Send message").click();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect.poll(() => sent.length).toBe(2);
  expect(sent[0].key).not.toBe(sent[1].key);
  await page.getByRole("button", { name: "Edit message", exact: true }).click();
  await expect(page.getByLabel("Message the mission planner")).toHaveValue(
    "Plan relief distribution",
  );
  await page
    .getByLabel("Message the mission planner")
    .fill("Plan a coastal readiness visit");
  await page.getByLabel("Send message").click();
  await expect(page.getByText(reply.reply)).toBeVisible();
  expect(sent[2].history).toEqual([]);
  await expect(
    page.getByRole("log").getByText("Plan relief distribution"),
  ).toHaveCount(0);
});

for (const mode of ["light", "dark", "mobile"] as const)
  test(`mission chat ${mode} layout`, async ({ page }) => {
    if (mode === "mobile")
      await page.setViewportSize({ width: 390, height: 844 });
    await fixtures(page);
    await page.route("**/conversations/mission", (route) =>
      route.fulfill({ json: reply }),
    );
    await page.goto("/missions");
    if (mode === "dark") await page.getByLabel("Switch theme").click();
    await page
      .getByRole("button", { name: "Plan a mission", exact: true })
      .first()
      .click();
    await page
      .getByLabel("Message the mission planner")
      .fill(
        "Organise a preparedness visit in Eastleigh, Nairobi. We will check supplies and brief local volunteers.",
      );
    await page.getByLabel("Send message").click();
    await expect(page.getByText(reply.reply)).toBeVisible();
    const composer = await page.locator(".mission-composer").boundingBox();
    expect(composer!.y + composer!.height).toBeLessThan(
      page.viewportSize()!.height,
    );
    expect(
      await page
        .locator(".mission-chat-drawer")
        .evaluate((e) => e.scrollWidth <= e.clientWidth),
    ).toBe(true);
    const colors = await page
      .locator(".mission-message.user")
      .evaluate((e) => ({
        background: getComputedStyle(e).backgroundColor,
        foreground: getComputedStyle(e).color,
      }));
    expect(colors).toEqual({
      background: "rgb(7, 51, 106)",
      foreground: "rgb(255, 255, 255)",
    });
    await page.screenshot({
      path: `../designs/next-portal/mission-chat-${mode}.png`,
      fullPage: true,
    });
  });
