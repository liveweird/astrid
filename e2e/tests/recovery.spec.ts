import { expect, test } from "@playwright/test";

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
  await page.getByRole("button", { name: "Generation controls" }).click();
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
  await page.getByRole("button", { name: "Generation controls" }).click();
  let selector = page.getByRole("combobox", { name: "Exercise type" });
  await selector.click();
  await page.getByRole("option", { name: "Supply the indefinite article" }).click();
  await expect(page.getByText("Your learning session expired. Reconnect to continue.")).toBeVisible();
  await page.getByRole("button", { name: "Reconnect session" }).click();

  await expect(page.getByRole("button", { name: "Check answer" })).toBeVisible();
  expect(sessionRequests).toBe(2);
  await page.getByRole("button", { name: "Generation controls" }).click();
  selector = page.getByRole("combobox", { name: "Exercise type" });
  await selector.click();
  await page.getByRole("option", { name: "Supply the indefinite article" }).click();
  await expect(selector).toHaveValue("Supply the indefinite article");
});
