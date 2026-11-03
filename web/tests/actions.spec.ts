import { test, expect } from "@playwright/test";
import { fixtures, mission, stockRow, org, region } from "./fixtures";
import { commandSchema } from "../../../backend/src/modules/operations/operations.schema";
import { invitationSchema } from "../../../backend/src/modules/organizations/organizations.schema";
import {
  resourcePlanRequest,
  planApproval,
} from "../../../backend/src/modules/sentinel/planning-contract";
const reason = "Reviewed by the regional coordinator.";

test("inventory creation and stock adjustment use versioned backend commands", async ({
  page,
}) => {
  const sent = await fixtures(page);
  await page.goto("/resources");
  await page
    .getByRole("button", { name: "Add resource", exact: true })
    .first()
    .click();
  await page.getByLabel("Title", { exact: true }).fill("Response blankets");
  await page.getByLabel("Initial stock").fill("30");
  await page.getByLabel("Storage location").fill("Nairobi central store");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add resource", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(
    commandSchema.safeParse(
      sent.find((r) => r.body?.action === "CREATE_RESOURCE").body,
    ).success,
  ).toBe(true);
  await page
    .getByRole("button", { name: "Review Emergency first aid kits" })
    .click();
  await page.getByRole("button", { name: "Adjust stock", exact: true }).click();
  await page.getByLabel("Stock change").fill("-2");
  await page.getByLabel("Reason for this decision").fill(reason);
  await page
    .getByRole("button", { name: "Confirm change", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  const payload = sent.find((r) => r.body?.action === "ADJUST_STOCK").body;
  expect(commandSchema.safeParse(payload).success).toBe(true);
  expect(payload.expected_version).toBe(stockRow.version);
});
for (const [route, action, fields] of [
  ["applications", "Assign", {}],
  ["assignments", "Check in", {}],
  [
    "reports",
    "Triage report",
    { "Review decision": "RESOLVED", "Reason for this decision": reason },
  ],
  [
    "hours",
    "Approve hours",
    { "Approved hours": "4", "Reason for this decision": reason },
  ],
  [
    "learning",
    "Review training",
    { "Valid until": "2027-11-01T09:00", "Reason for this decision": reason },
  ],
  [
    "qualifications",
    "Revoke credential",
    { "Reason for this decision": reason },
  ],
  ["bulletins", "Withdraw bulletin", { "Reason for this decision": reason }],
  [
    "support",
    "Reply support",
    { Message: "The support case has been reviewed." },
  ],
] as const)
  test("versioned action: " + action, async ({ page }) => {
    const sent = await fixtures(page);
    await page.goto("/" + route);
    await page.locator("tbody .table-link").first().click();
    await page
      .getByRole("dialog")
      .getByRole("button", { name: action, exact: true })
      .click();
    for (const [name, value] of Object.entries(fields)) {
      const field = page.getByRole("dialog").getByLabel(name, { exact: true });
      if (await field.evaluate((e) => e.tagName === "SELECT"))
        await field.selectOption(value);
      else await field.fill(value);
    }
    await page.screenshot({
      path: "../designs/next-portal/action-" + route + ".png",
      fullPage: true,
    });
    await page
      .getByRole("button", { name: "Confirm change", exact: true })
      .click();
    await expect(page.getByRole("dialog")).toHaveCount(0);
    const command = sent.find((r) => r.path.endsWith("/commands"));
    expect(
      commandSchema.safeParse(command.body).success,
      JSON.stringify(command.body),
    ).toBe(true);
    expect(command.key).toBeTruthy();
  });
test("conversation can start from an empty inbox", async ({ page }) => {
  const sent = await fixtures(page);
  await page.goto("/inbox");
  await page
    .getByRole("button", { name: "New conversation", exact: true })
    .click();
  await page.getByLabel("Mission", { exact: true }).selectOption(mission);
  await page
    .getByLabel("Message", { exact: true })
    .fill("Please confirm the meeting point before deployment.");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "New conversation", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(
    commandSchema.safeParse(sent.find((r) => r.body?.action === "MESSAGE").body)
      .success,
  ).toBe(true);
});
test("invitations preserve role boundaries and backend schema", async ({
  page,
}) => {
  const sent = await fixtures(page);
  await page.goto("/people");
  await page.getByRole("button", { name: "Invite a person" }).click();
  await page
    .getByLabel("Email address", { exact: true })
    .fill("person@example.test");
  await page.getByLabel("Workspace role").selectOption("COORDINATOR");
  await page.getByLabel("Reason for this decision").fill(reason);
  await page
    .getByRole("button", { name: "Send invitation", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(
    invitationSchema.safeParse(
      sent.find((r) => r.path.endsWith("/invitations")).body,
    ).success,
  ).toBe(true);
});
test("sensitive action asks for password and preserves failed form on cancellation", async ({
  page,
}) => {
  await fixtures(page);
  await page.route("**/operations/commands", (route) =>
    route.fulfill({
      status: 401,
      contentType: "application/json",
      body: JSON.stringify({ error: "RECENT_AUTH_REQUIRED" }),
    }),
  );
  await page.goto("/security");
  await page.getByLabel("Reason for this decision").fill(reason);
  await page.getByRole("button", { name: "Update dispatch control" }).click();
  await expect(
    page.getByRole("heading", { name: "Confirm it's you" }),
  ).toBeVisible();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Close panel" })
    .click();
  await expect(page.locator(".notice[role=alert]")).toContainText(
    "Confirmation cancelled",
  );
  await expect(page.getByLabel("Reason for this decision")).toHaveValue(reason);
});
test("resource approval requires both field reviews and a version", async ({
  page,
}) => {
  const sent = await fixtures(page);
  await page.goto("/resource-planning");
  await page.getByRole("button", { name: "Review assessment" }).click();
  await page
    .getByRole("button", { name: "Approve proposal", exact: true })
    .click();
  await page.getByLabel("Reason for this decision").fill(reason);
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Approve proposal", exact: true })
    .last()
    .click();
  await expect(page.locator(".notice[role=alert]")).toContainText(
    "Review routes",
  );
  expect(sent.some((r) => r.path.endsWith("/approve"))).toBe(false);
  await page.getByLabel("I reviewed access routes").check();
  await page.getByLabel("I reviewed current field conditions").check();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Approve proposal", exact: true })
    .last()
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  expect(
    planApproval.safeParse(sent.find((r) => r.path.endsWith("/approve")).body)
      .success,
  ).toBe(true);
});
