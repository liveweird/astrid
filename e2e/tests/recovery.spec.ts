import { expect, test } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";

test("reconnects after the initial development session fails", async ({ page }) => {
  let failSession = true;
  await page.route("**/api/v1/development-session", async (route) => {
    if (failSession) {
      failSession = false;
      await route.abort("connectionfailed");
    } else await route.continue();
  });

  await page.goto("/");
  const reconnect = page.getByRole("button", { name: "Try connection again" });
  await expect(reconnect).toBeVisible();
  await reconnect.click();
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
});

test("offers a full engine reload after a revision conflict", async ({ page }) => {
  let generationCount = 0;
  await page.route("**/api/v1/exercises", async (route) => {
    if (route.request().method() === "POST") generationCount += 1;
    if (generationCount === 2 && route.request().method() === "POST") {
      await route.fulfill({
        status: 409,
        contentType: "application/problem+json",
        body: JSON.stringify({ detail: "The engine revision changed.", code: "revision_conflict" }),
      });
    } else await route.continue();
  });

  await page.goto("/");
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  await openGenerationControls(page);
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  await selector.click();
  await page.getByRole("option", { name: "Supply the indefinite article" }).click();
  const reload = page.getByRole("button", { name: "Reload engine" });
  await expect(reload).toBeVisible();
  await reload.click();
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Danish engine playground" })).toBeVisible();
});

test("reconnects an expired existing session before generating again", async ({ page }) => {
  let sessionRequests = 0;
  let exerciseRequests = 0;
  await page.route("**/api/v1/development-session", async (route) => {
    sessionRequests += 1;
    await route.continue();
  });
  await page.route("**/api/v1/exercises", async (route) => {
    if (route.request().method() === "POST") exerciseRequests += 1;
    if (exerciseRequests === 2 && route.request().method() === "POST") {
      await route.fulfill({
        status: 401,
        contentType: "application/problem+json",
        body: JSON.stringify({ detail: "The session is no longer valid.", code: "unauthorized" }),
      });
    } else await route.continue();
  });

  await page.goto("/");
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  await openGenerationControls(page);
  let selector = page.getByRole("combobox", { name: "Exercise type" });
  await selector.click();
  await page.getByRole("option", { name: "Supply the indefinite article" }).click();
  await expect(page.getByText("Your learning session expired. Reconnect to continue.")).toBeVisible();
  await page.getByRole("button", { name: "Reconnect session" }).click();

  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  expect(sessionRequests).toBe(2);
  await openGenerationControls(page);
  selector = page.getByRole("combobox", { name: "Exercise type" });
  await selector.click();
  await page.getByRole("option", { name: "Supply the indefinite article" }).click();
  await expect(selector).toHaveValue("Supply the indefinite article");
});

test("replays the stored attempt after its committed response exceeds the browser deadline", async ({ page }) => {
  const bodies: string[] = [];
  const responseBodies: string[] = [];
  let releaseFirstResponse: (() => void) | undefined;
  let markFirstCommitted: (() => void) | undefined;
  const firstResponseGate = new Promise<void>((resolve) => { releaseFirstResponse = resolve; });
  const firstCommitted = new Promise<void>((resolve) => { markFirstCommitted = resolve; });

  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData() ?? "");
    const response = await route.fetch();
    const body = await response.text();
    responseBodies.push(body);
    if (bodies.length === 1) {
      markFirstCommitted?.();
      await firstResponseGate;
    }
    await route.fulfill({ response, body }).catch(() => {
      // The browser is expected to have aborted the first intercepted response.
    });
  });

  await page.goto("/");
  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  await page.getByLabel("Answer choices").getByRole("button").first().click();
  await page.clock.install();
  await page.getByRole("button", { name: "Check answer" }).click();
  await firstCommitted;
  await page.clock.fastForward(15_000);

  const replay = page.getByRole("button", { name: "Check the same attempt" });
  await expect(page.getByText("The result is not confirmed")).toBeVisible();
  await expect(replay).toBeEnabled();
  releaseFirstResponse?.();
  await replay.click();
  await expect(page.getByRole("status")).toBeVisible();

  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  expect(responseBodies).toHaveLength(2);
  expect(responseBodies[1]).toBe(responseBodies[0]);
  const storedAttempt = JSON.parse(responseBodies[1]!);
  expect(storedAttempt.attemptId).toBe(JSON.parse(bodies[1]!).attemptId);
  expect(storedAttempt.retryNumber).toBe(1);
  await expect(page.getByRole("status").getByText(/Try 1/)).toBeVisible();
});
