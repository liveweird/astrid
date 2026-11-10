import { expect, test, type Page } from "@playwright/test";
import { choosePractice, readPractice } from "../support/practice.js";

const PROFILE = "Talking about one person";
type SavedOperation = { kind: string; body: string; ownerSessionId: string };

async function savedSupport(page: Page, practiceId: string) {
  return page.evaluate((id) => {
    const prefix = `astrid.practice.v1.outbox.${id}.support.`;
    const key = Object.keys(localStorage).find((candidate) => candidate.startsWith(prefix));
    return key ? JSON.parse(localStorage.getItem(key)!) as SavedOperation : null;
  }, practiceId);
}

async function savedRun(page: Page, practiceId: string) {
  return page.evaluate((id) => localStorage.getItem(`astrid.practice.v1.run.${id}`), practiceId);
}

test("first steps keep pending teaching with its unavailable owner until a new run is explicitly chosen", async ({ page, context }) => {
  await page.goto("/practice");
  await choosePractice(page, PROFILE);
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeVisible();
  const oldId = new URL(page.url()).searchParams.get("session")!;
  const old = await readPractice(page.request, oldId);
  expect(old).toMatchObject({ introductionRequired: true, introductionOpened: false });

  const supportBodies: string[] = [];
  let supportRequests = 0;
  await page.route("**/api/v1/practice-sessions/*/support", async (route) => {
    supportRequests += 1;
    supportBodies.push(route.request().postData()!);
    if (supportRequests === 1) await route.abort("connectionfailed");
    else await route.continue();
  });
  await page.getByRole("button", { name: "Session introduction", exact: true }).click();
  await expect(page.getByRole("button", { name: "Retry saved operation", exact: true })).toBeVisible();
  await expect(page.locator("[data-practice-focus=introduction]")).toBeHidden();
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeHidden();
  const pending = await savedSupport(page, oldId);
  const oldRunRecord = await savedRun(page, oldId);
  expect(pending).toMatchObject({ kind: "support", body: supportBodies[0], ownerSessionId: old.ownerSessionId });
  expect(oldRunRecord).not.toBeNull();

  await context.clearCookies();
  let bootstraps = 0;
  let creations = 0;
  let ownerReadStatus: number | undefined;
  await page.route(`**/api/v1/practice-sessions/${oldId}`, async (route) => {
    const response = await route.fetch();
    ownerReadStatus = response.status();
    await route.fulfill({ response });
  });
  page.on("request", (request) => {
    if (request.method() !== "POST") return;
    const path = new URL(request.url()).pathname;
    if (path === "/api/v1/development-session") bootstraps += 1;
    if (path === "/api/v1/practice-sessions") creations += 1;
  });
  await page.getByRole("button", { name: "Retry saved operation", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(/expired|unavailable/);
  await expect(page.getByRole("alert")).toBeFocused();
  expect(ownerReadStatus).toBe(401);
  expect(supportBodies).toHaveLength(1);
  expect(await savedSupport(page, oldId)).toEqual(pending);
  expect(bootstraps).toBe(0);
  expect(creations).toBe(0);
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeHidden();

  const restart = page.getByRole("button", { name: "Start new practice", exact: true }).first();
  await expect(restart).toBeEnabled();
  await restart.click();
  await choosePractice(page, PROFILE);
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeVisible();
  const nextId = new URL(page.url()).searchParams.get("session")!;
  const next = await readPractice(page.request, nextId);
  expect(nextId).not.toBe(oldId);
  expect(next.ownerSessionId).not.toBe(old.ownerSessionId);
  expect(next).toMatchObject({ introductionRequired: true, introductionOpened: false, summary: { attemptCount: 0 } });
  expect(bootstraps).toBe(1);
  expect(creations).toBe(1);
  expect(await savedRun(page, oldId)).toBe(oldRunRecord);
  expect(await savedSupport(page, oldId)).toEqual(pending);
  expect((await page.request.get(`/api/v1/practice-sessions/${oldId}`)).status()).toBe(404);
});
