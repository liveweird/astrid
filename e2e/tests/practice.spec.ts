import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { expectFeedbackBadgesUnclipped } from "../support/feedback-layout.js";
import { ownerHeaders, PRACTICE_HEADERS, readPractice, saveWrongAttempt, wrongAnswer, type PracticeExercise, type PracticeState } from "../support/practice.js";

async function open(page: Page) {
  await page.goto("/practice");
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  const id = new URL(page.url()).searchParams.get("session");
  expect(id).toBeTruthy();
  return readPractice(page.request, id!);
}

async function chooseWrong(page: Page, exercise: PracticeExercise) {
  const answer = wrongAnswer(exercise);
  if (answer.kind === "order") {
    for (const id of answer.blockIds!) {
      const block = exercise.prompt.blocks.find((candidate) => candidate.id === id)!;
      await page.getByLabel("Available sentence parts").getByRole("button", { name: block.text, exact: true }).click();
    }
  } else if (exercise.formatId === "E007") {
    await page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button", { name: answer.text, exact: true }).click();
  } else await page.getByRole("textbox").fill(answer.text!);
}

async function checkWrong(page: Page, state: PracticeState) {
  await chooseWrong(page, state.currentExercise!);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("status")).toBeFocused();
  await expect(page.getByRole("status").getByText("Target to revisit", { exact: true })).toBeVisible();
}

async function finishByApi(page: Page, state: PracticeState) {
  while (state.state !== "completed") {
    if (state.items[state.cursor]!.attemptCount === 0) await saveWrongAttempt(page.request, state.currentExercise!);
    const response = await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, { headers: ownerHeaders(state.ownerSessionId), data: { position: state.cursor } });
    expect(response.status()).toBe(200);
    state = await response.json() as PracticeState;
  }
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map((violation) => ({ id: violation.id, nodes: violation.nodes.map((node) => node.target.join(" ")) }))).toEqual([]);
}

test("practice keeps saved feedback on refresh and completes without a pass gate", async ({ page }) => {
  let state = await open(page);
  let writes = 0;
  page.on("request", (request) => { if (new URL(request.url()).pathname === "/api/v1/attempts" && request.method() === "POST") writes += 1; });
  const checked = wrongAnswer(state.currentExercise!);
  await checkWrong(page, state);
  await page.reload();
  await expect(page.getByRole("status")).toBeVisible();
  await expect(page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button", { name: checked.text, exact: true })).toHaveAttribute("aria-pressed", "true");
  expect(writes).toBe(1);
  state = await readPractice(page.request, state.id);
  expect(state.items[0]!.attemptCount).toBe(1);
  for (let position = 0; position < 8; position += 1) {
    if (position > 0) await checkWrong(page, state);
    await page.getByRole("button", { name: "Continue", exact: true }).click();
    if (position < 7) {
      await expect(page.getByText(`Part ${position + 2} of 8`, { exact: true })).toBeVisible();
      state = await readPractice(page.request, state.id);
    }
  }
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeFocused();
  await expect(page.getByText(/not a mastery judgment/)).toBeVisible();
  expect(writes).toBe(8);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect(writes).toBe(8);
});

test("practice replays a committed lost attempt after refresh with the exact saved identity", async ({ page }) => {
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) { await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  const state = await open(page);
  await chooseWrong(page, state.currentExercise!);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toHaveCount(0);
  await expect(page.getByRole("group", { name: "Answer choices", exact: true }).getByRole("button").first()).toBeDisabled();
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Learn this first", exact: true })).toBeDisabled();
  await page.reload();
  await expect(page.getByRole("status")).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  expect((await readPractice(page.request, state.id)).items[0]!.attemptCount).toBe(1);
});

test("practice unlocks after an uncertain answer is definitively stale", async ({ page }) => {
  const state = await open(page);
  await page.route("**/api/v1/attempts", (route) => route.abort("connectionfailed"), { times: 1 });
  await chooseWrong(page, state.currentExercise!);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("Your answer is not confirmed");
  await saveWrongAttempt(page.request, state.currentExercise!);
  expect((await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, {
    headers: ownerHeaders(state.ownerSessionId), data: { position: 0 },
  })).ok()).toBeTruthy();
  await page.getByRole("button", { name: "Retry saved operation", exact: true }).click();
  await expect(page.getByText("Part 2 of 8", { exact: true })).toBeVisible();
  await expect(page.getByRole("alert")).toContainText(/not recorded/);
  await expect(page.getByRole("alert")).toBeFocused();
  await expect(page.getByRole("alert")).not.toContainText("Your answer is not confirmed");
  await expect(page.getByRole("button", { name: "Start new practice", exact: true })).toBeEnabled();
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeEnabled();
  expect((await readPractice(page.request, state.id)).items[0]!.attemptCount).toBe(1);
});

test("practice offers explicit recovery when authentication expires before a saved retry", async ({ page, context }) => {
  const state = await open(page);
  await page.route("**/api/v1/attempts", (route) => route.abort("connectionfailed"), { times: 1 });
  await chooseWrong(page, state.currentExercise!);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await context.clearCookies();
  await page.getByRole("button", { name: "Retry saved operation", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(/expired|unavailable/);
  await expect(page.getByRole("alert")).toBeFocused();
  const restart = page.getByRole("button", { name: "Start new practice", exact: true }).first();
  await expect(restart).toBeEnabled();
  await restart.click();
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  expect(new URL(page.url()).searchParams.get("session")).not.toBe(state.id);
});

test("practice restores acknowledged support and an uncertain Continue without skipping", async ({ page }) => {
  const state = await open(page);
  let supportLost = false;
  await page.route("**/api/v1/practice-sessions/*/support", async (route) => {
    if (!supportLost) { supportLost = true; await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  await page.getByRole("button", { name: "Learn this first", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.reload();
  await expect(page.getByRole("region", { name: "Learn this first", exact: true })).toBeVisible();
  await checkWrong(page, state);
  let advanceLost = false;
  await page.route("**/api/v1/practice-sessions/*/advance", async (route) => {
    if (!advanceLost) { advanceLost = true; await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByRole("status")).toBeVisible();
  await expect(page.getByRole("button", { name: "Try again", exact: true })).toBeDisabled();
  await page.reload();
  await expect(page.getByText("Part 2 of 8", { exact: true })).toBeVisible();
  const restored = await readPractice(page.request, state.id);
  expect(restored.cursor).toBe(1);
  expect(restored.items[0]!.firstAttemptContext?.lessonOpened).toBe(true);
});

test("practice cannot start a competing run while an answer response is in flight", async ({ page }) => {
  const state = await open(page);
  let release!: () => void;
  let reached!: () => void;
  const pending = new Promise<void>((resolve) => { release = resolve; });
  const committed = new Promise<void>((resolve) => { reached = resolve; });
  await page.route("**/api/v1/attempts", async (route) => {
    const response = await route.fetch();
    reached();
    await pending;
    await route.fulfill({ response });
  });
  await chooseWrong(page, state.currentExercise!);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await committed;
  try {
    await expect(page.getByRole("button", { name: "Start new practice", exact: true })).toBeDisabled();
    expect(new URL(page.url()).searchParams.get("session")).toBe(state.id);
  } finally { release(); }
  await expect(page.getByRole("status")).toBeVisible();
  expect((await readPractice(page.request, state.id)).items[0]!.attemptCount).toBe(1);
});

test("practice does not send a mutation when browser recovery storage fails", async ({ page }) => {
  let creates = 0;
  page.on("request", (request) => { if (request.method() === "POST" && new URL(request.url()).pathname === "/api/v1/practice-sessions") creates += 1; });
  await page.addInitScript(() => {
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key, value) {
      if (key.startsWith("astrid.practice.")) throw new DOMException("Storage full", "QuotaExceededError");
      original.call(this, key, value);
    };
  });
  await page.goto("/practice");
  await expect(page.getByText(/operation was not sent/)).toBeVisible();
  expect(creates).toBe(0);
});

for (const committed of [false, true]) {
  test(`practice recovers the exact creation after a ${committed ? "committed" : "uncommitted"} lost response`, async ({ page }) => {
    const bodies: string[] = [];
    await page.route("**/api/v1/practice-sessions", async (route) => {
      bodies.push(route.request().postData()!);
      if (bodies.length === 1) {
        if (committed) await route.fetch();
        await route.abort("connectionfailed");
      } else await route.continue();
    });
    await page.goto("/practice");
    await expect(page.getByRole("button", { name: "Start new practice", exact: true })).toBeVisible();
    const id = new URL(page.url()).searchParams.get("session");
    await page.reload();
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
    expect(new URL(page.url()).searchParams.get("session")).toBe(id);
    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    const state = await readPractice(page.request, id!);
    expect(state.cursor).toBe(0);
    expect(state.items.every((item) => item.attemptCount === 0)).toBeTruthy();
  });
}

test("practice does not replay a pending creation under a renewed cookie", async ({ page, context }) => {
  let creates = 0;
  let renewals = 0;
  await page.route("**/api/v1/practice-sessions", async (route) => {
    creates += 1;
    await route.abort("connectionfailed");
  });
  page.on("request", (request) => { if (request.method() === "POST" && new URL(request.url()).pathname === "/api/v1/development-session") renewals += 1; });
  await page.goto("/practice");
  await expect(page.getByRole("button", { name: "Start new practice", exact: true })).toBeVisible();
  expect(creates).toBe(1);
  const id = new URL(page.url()).searchParams.get("session")!;
  await context.clearCookies();
  expect((await page.request.post("/api/v1/development-session", { headers: PRACTICE_HEADERS, data: {} })).ok()).toBeTruthy();
  await page.reload();
  await expect(page.getByRole("alert")).toContainText(/expired|different|unavailable/);
  expect(creates).toBe(1);
  expect(renewals).toBe(1);
  expect((await page.request.get(`/api/v1/practice-sessions/${id}`)).status()).toBe(404);
});

test("practice explains a stale answer from another tab and restores the current item", async ({ page, context }) => {
  const state = await open(page);
  const other = await context.newPage();
  await other.goto(page.url());
  await expect(other.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  await checkWrong(page, state);
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await expect(page.getByText("Part 2 of 8", { exact: true })).toBeVisible();
  await chooseWrong(other, state.currentExercise!);
  await other.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(other.getByRole("alert")).toContainText(/another tab/);
  await expect(other.getByText("Part 2 of 8", { exact: true })).toBeVisible();
  expect((await readPractice(page.request, state.id)).items[0]!.attemptCount).toBe(1);
  await other.close();
});

test("practice advances with native keyboard focus in a short enlarged-text window", async ({ page }) => {
  await page.setViewportSize({ width: 768, height: 360 });
  const state = await open(page);
  await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  const answer = wrongAnswer(state.currentExercise!);
  const tab = process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit" ? "Alt+Tab" : "Tab";
  const activate = async (name: string) => {
    const button = page.getByRole("button", { name, exact: true });
    for (let step = 0; step < 60; step += 1) {
      if (await button.evaluate((element) => element === document.activeElement)) {
        await page.keyboard.press("Enter");
        return;
      }
      await page.keyboard.press(tab);
    }
    throw new Error(`Native keyboard traversal did not reach ${name}`);
  };
  await activate(answer.text!);
  await activate("Check answer");
  await expect(page.getByRole("status")).toBeFocused();
  await activate("Continue");
  await expect(page.getByText("Part 2 of 8", { exact: true })).toBeVisible();
  const heading = page.locator("h2[tabindex='-1']");
  await expect(heading).toBeFocused();
  const box = await heading.boundingBox();
  const header = await page.locator("header").boundingBox();
  expect(box!.y).toBeGreaterThanOrEqual(header!.height);
  expect(box!.y).toBeLessThan(360);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
});

for (const theme of ["light", "dark"] as const) {
  test(`practice support, feedback and summary reflow accessibly in ${theme} mode`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    const state = await open(page);
    await page.setViewportSize({ width: 390, height: 844 });
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    await scan(page);
    await page.getByRole("button", { name: "Session introduction", exact: true }).click();
    await expect(page.getByRole("region", { name: "Session introduction", exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Session introduction", exact: true }).click();
    await checkWrong(page, state);
    await expectFeedbackBadgesUnclipped(page);
    await scan(page);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
    await finishByApi(page, await readPractice(page.request, state.id));
    await page.reload();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    await scan(page);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  });
}
