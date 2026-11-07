import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { readPractice, wrongAnswer } from "../support/practice.js";

const VERSION = /^v\d+\.\d+\.\d+$/;
const BUILD = /^v(\d+\.\d+\.\d+) · ([a-f0-9]{7,40}) · (\d{4}-\d{2}-\d{2} \d{2}:\d{2})$/;

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
}

async function openChangelog(page: Page) {
  await page.goto("/changelog");
  await expect(page.getByRole("heading", { name: "Changelog", exact: true })).toBeVisible();
}

test("the real build stamp opens release notes and clears the current-version indicator", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Danish engine playground", exact: true })).toBeVisible();
  const stamp = page.getByTitle("Build version", { exact: true });
  const text = (await stamp.textContent())!.trim();
  const parts = BUILD.exec(text);
  expect(parts, `Expected real committed build metadata, received ${text}`).not.toBeNull();
  if (process.env.E2E_EXPECTED_COMMIT) expect(parts![2]).toBe(process.env.E2E_EXPECTED_COMMIT);
  if (process.env.E2E_EXPECTED_COMMIT_TIME) expect(parts![3]).toBe(process.env.E2E_EXPECTED_COMMIT_TIME.slice(0, 16).replace("T", " "));
  await expect(stamp).toHaveAttribute("href", "/changelog");
  await expect(page.getByTitle("What's new", { exact: true })).toBeVisible();
  await stamp.click();
  await expect(page.getByRole("heading", { name: "Changelog", exact: true })).toBeVisible();
  await expect(page.locator("main").getByText(VERSION).first()).toHaveText(`v${parts![1]}`);
  await expect(page.locator("main").getByText(/^\d{4}-\d{2}-\d{2}$/).first()).toBeVisible();
  await expect(page.getByTitle("What's new", { exact: true })).toHaveCount(0);
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem("astrid.changelog")!).seenVersion)).toBe(parts![1]);
  await page.getByRole("link", { name: "Engine playground", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Danish engine playground", exact: true })).toBeVisible();
  await expect(page.getByTitle("What's new", { exact: true })).toHaveCount(0);
});

for (const storage of ["corrupt", "unavailable"] as const) {
  test(`direct changelog navigation works without learning requests when seen storage is ${storage}`, async ({ page }) => {
    await page.addInitScript((mode) => {
      if (mode === "corrupt") localStorage.setItem("astrid.changelog", "not-json");
      else {
        Storage.prototype.getItem = function () { throw new DOMException("Storage denied", "SecurityError"); };
        Storage.prototype.setItem = function () { throw new DOMException("Storage denied", "SecurityError"); };
      }
    }, storage);
    const apiRequests: string[] = [];
    page.on("request", (request) => {
      if (new URL(request.url()).pathname.startsWith("/api/")) apiRequests.push(request.url());
    });
    await openChangelog(page);
    await expect(page.locator("main").getByText(VERSION).first()).toBeVisible();
    await expect(page.locator("main").getByRole("list").first()).toBeVisible();
    await scan(page);
    await page.reload();
    await expect(page.getByRole("heading", { name: "Changelog", exact: true })).toBeVisible();
    expect(apiRequests).toEqual([]);
  });
}

test("reading the changelog in another tab immediately clears the original indicator", async ({ page, context }) => {
  await page.addInitScript(() => localStorage.setItem("astrid.changelog", JSON.stringify({ seenVersion: "0.0.0" })));
  await page.goto("/");
  await expect(page.getByTitle("What's new", { exact: true })).toBeVisible();
  const other = await context.newPage();
  try {
    await openChangelog(other);
    await expect(other.getByTitle("What's new", { exact: true })).toHaveCount(0);
    await expect(page.getByTitle("What's new", { exact: true })).toHaveCount(0);
  } finally { await other.close(); }
});

for (const theme of ["light", "dark"] as const) {
  test(`changelog and compact build stamp reflow at enlarged text in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 720 });
    await openChangelog(page);
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    await scan(page);
    const toggle = page.getByRole("button", { name: "Toggle navigation", exact: true });
    await toggle.click();
    await expect(page.getByTitle("Build version", { exact: true })).toBeVisible();
    await scan(page);
    await toggle.click();
    await page.setViewportSize({ width: 1200, height: 500 });
    await page.getByRole("button", { name: "Collapse navigation", exact: true }).click();
    const compact = page.getByRole("link", { name: VERSION });
    await expect(compact).toBeVisible();
    await expect(compact).toHaveAttribute("title", BUILD);
    await scan(page);
    await page.getByRole("button", { name: "Expand navigation", exact: true }).click();
    await expect(page.getByTitle("Build version", { exact: true })).toBeVisible();
    await scan(page);
  });
}

test("visiting release notes preserves an uncertain practice answer and its owner", async ({ page }) => {
  await page.goto("/practice");
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  const id = new URL(page.url()).searchParams.get("session")!;
  const state = await readPractice(page.request, id);
  const answer = wrongAnswer(state.currentExercise!);
  const bodies: string[] = [];
  let renewals = 0;
  page.on("request", (request) => {
    if (request.method() === "POST" && new URL(request.url()).pathname.endsWith("/development-session")) renewals += 1;
  });
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) { await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  await page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button", { name: answer.text!, exact: true }).click();
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.getByRole("link", { name: "Changelog", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Changelog", exact: true })).toBeVisible();
  await page.getByRole("link", { name: "Practice", exact: true }).click();
  await expect(page.getByRole("status")).toBeVisible();
  expect(new URL(page.url()).searchParams.get("session")).toBe(id);
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  expect(renewals).toBe(0);
  const restored = await readPractice(page.request, id);
  expect(restored.ownerSessionId).toBe(state.ownerSessionId);
  expect(restored.items[0]!.attemptCount).toBe(1);
});

test("a failed changelog chunk keeps navigation available and can reload safely", async ({ page }) => {
  const apiRequests: string[] = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/")) apiRequests.push(request.url());
  });
  await page.route("**/assets/Changelog-*.js", (route) => route.abort("connectionfailed"), { times: 1 });
  await page.goto("/changelog");
  await expect(page.getByRole("alert")).toContainText("Changelog");
  await expect(page.getByRole("navigation")).toBeVisible();
  await expect(page.getByRole("link", { name: "Practice", exact: true })).toBeVisible();
  await expect(page.getByTitle("What's new", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Reload changelog", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Changelog", exact: true })).toBeVisible();
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect(page.getByTitle("What's new", { exact: true })).toHaveCount(0);
  expect(apiRequests).toEqual([]);
});
