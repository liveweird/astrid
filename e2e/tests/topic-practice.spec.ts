import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { choosePractice, ownerHeaders, readPractice, saveWrongAttempt, wrongAnswer, type PracticeState } from "../support/practice.js";

type TopicState = PracticeState & {
  focusId: string;
  planRevision: string;
  followUpPlanRevision?: string;
  followUp?: { sourcePracticeSessionId: string; selectedTargetKeys: string[] };
};
type VerbRow = { label: string; assessmentMode: string; plannedItems: number; attemptCount: number };
const labels = [
  "Present verbs — choosing the form",
  "Present verbs — written recall",
  "Present verbs — repairing the form",
];

async function openVerbs(page: Page) {
  await page.goto("/practice");
  await choosePractice(page, "Present verb forms");
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  return await readPractice(page.request, new URL(page.url()).searchParams.get("session")!) as TopicState;
}

async function finish(page: Page, state: PracticeState) {
  while (state.state !== "completed") {
    if (!state.items[state.cursor]!.attemptCount) await saveWrongAttempt(page.request, state.currentExercise!);
    const response = await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, {
      headers: ownerHeaders(state.ownerSessionId), data: { position: state.cursor },
    });
    expect(response.status()).toBe(200);
    state = await response.json() as PracticeState;
  }
  return state as TopicState;
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
}

test("a fresh chooser creates no run before an explicit focus selection", async ({ page }) => {
  const bodies: string[] = [];
  let bootstraps = 0;
  page.on("request", (request) => {
    if (request.method() !== "POST") return;
    const path = new URL(request.url()).pathname;
    if (path === "/api/v1/practice-sessions") bodies.push(request.postData()!);
    if (path === "/api/v1/development-session") bootstraps += 1;
  });
  await page.goto("/practice");
  await expect(page.getByRole("heading", { name: "Choose your practice", exact: true })).toBeVisible();
  await expect(page.getByRole("radio")).toHaveCount(13);
  await expect(page.getByRole("radio", { name: "Referring to one thing", exact: true })).toBeVisible();
  await expect(page.getByRole("radio", { checked: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Start practice", exact: true })).toBeDisabled();
  expect(bodies).toEqual([]);
  expect(new URL(page.url()).searchParams.has("session")).toBe(false);
  await choosePractice(page, "Present verb forms");
  await expect(page.getByText("Part 1 of 6", { exact: true })).toBeVisible();
  expect(bodies).toHaveLength(1);
  expect(JSON.parse(bodies[0]!)).toMatchObject({ focusId: "present-verbs", planRevision: "present-verbs-1" });
  expect(bootstraps).toBe(1);
  const state = await readPractice(page.request, new URL(page.url()).searchParams.get("session")!) as TopicState;
  expect(state.followUpPlanRevision).toBe("follow-up-2");
  expect(state.items.map((item) => item.formatId)).toEqual(["E001", "E004", "E005", "E001", "E004", "E005"]);
  await page.reload();
  await expect(page.getByText("Part 1 of 6", { exact: true })).toBeVisible();
  expect(bodies).toHaveLength(1);
  expect(bootstraps).toBe(1);
});

test("selection, recall and repair stay separate through six answers and a frozen summary", async ({ page }) => {
  let state = await openVerbs(page);
  for (let position = 0; position < 6; position += 1) {
    const answer = wrongAnswer(state.currentExercise!);
    if (state.currentExercise!.formatId === "E001") {
      await page.getByRole("button", { name: answer.text, exact: true }).click();
    } else await page.getByRole("textbox").fill(answer.text!);
    await page.getByRole("button", { name: "Check answer", exact: true }).click();
    await expect(page.getByRole("status")).toBeVisible();
    await page.getByRole("button", { name: "Continue", exact: true }).click();
    if (position < 5) await expect(page.getByText(`Part ${position + 2} of 6`, { exact: true })).toBeVisible();
    else await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    state = await readPractice(page.request, state.id) as TopicState;
  }
  const rows = state.summary.rows as VerbRow[];
  expect(rows.map((row) => row.assessmentMode)).toEqual(["selection", "recall", "repair"]);
  expect(rows.map((row) => row.label)).toEqual(labels);
  for (const row of rows) {
    expect(row.plannedItems).toBe(2);
    expect(row.attemptCount).toBe(2);
    await expect(page.getByRole("article", { name: row.label, exact: true }).getByRole("checkbox")).toBeVisible();
  }
  // Wrong feedback repeats the supplied answer; it must not invent a correct-form exposure.
  for (const position of [1, 2, 4]) {
    expect(state.items[position]!.firstAttemptContext!.priorExposure.flatMap((entry) => entry.keys).some((key) => key.startsWith("verb-present:"))).toBe(false);
  }
  const frozen = JSON.stringify(state);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect(JSON.stringify(await readPractice(page.request, state.id))).toBe(frozen);
});

for (const count of [1, 2, 3]) {
  test(`${count} verb operation selections produce a bounded source-linked review`, async ({ page }) => {
    const source = await finish(page, await openVerbs(page));
    const frozen = JSON.stringify(source);
    await page.reload();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    const rows = source.summary.rows as VerbRow[];
    for (const row of rows.slice(0, count)) await page.getByRole("article", { name: row.label, exact: true }).getByRole("checkbox").check();
    await page.getByRole("button", { name: "Start selected review", exact: true }).click();
    const expectedCount = count === 3 ? 6 : 4;
    await expect(page.getByText(`Part 1 of ${expectedCount}`, { exact: true })).toBeVisible();
    const child = await readPractice(page.request, new URL(page.url()).searchParams.get("session")!) as TopicState;
    expect(child.planRevision).toBe("follow-up-2");
    expect(child.followUp).toEqual({ sourcePracticeSessionId: source.id, selectedTargetKeys: rows.slice(0, count).map((row) => `grammar:G004:${row.assessmentMode}`) });
    expect(new Set(child.items.map((item) => item.assessmentMode))).toEqual(new Set(rows.slice(0, count).map((row) => row.assessmentMode)));
    await page.reload();
    await expect(page.getByText(`Part 1 of ${expectedCount}`, { exact: true })).toBeVisible();
    await expect(page.getByRole("link", { name: "Source practice", exact: true })).toHaveAttribute("href", `/practice?session=${source.id}`);
    const completedChild = await finish(page, child);
    const inherited = completedChild.items.flatMap((item) =>
      (item.firstAttemptContext as { sourceExposure?: { source: string; keys: string[] }[] } | null)?.sourceExposure ?? []);
    expect(inherited.some((entry) => entry.source === "feedback" && entry.keys.some((key) => key.startsWith("verb-present:")))).toBe(false);
    await page.reload();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    expect(JSON.stringify(await readPractice(page.request, source.id))).toBe(frozen);
    await page.getByRole("link", { name: "Source practice", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    expect(new URL(page.url()).searchParams.get("session")).toBe(source.id);
  });
}

for (const committed of [false, true]) {
  test(`verb creation replays its exact profile after ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
    const bodies: string[] = [];
    await page.route("**/api/v1/practice-sessions", async (route) => {
      bodies.push(route.request().postData()!);
      if (bodies.length === 1) {
        if (committed) await route.fetch();
        await route.abort("connectionfailed");
      } else await route.continue();
    });
    await page.goto("/practice");
    await choosePractice(page, "Present verb forms");
    await expect(page.getByRole("alert")).toBeVisible();
    const id = new URL(page.url()).searchParams.get("session");
    await page.getByRole("button", { name: "Start new practice", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Choose your practice", exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(page.getByRole("alert")).toBeVisible();
    await expect(page.getByRole("button", { name: "Retry saved operation", exact: true })).toBeVisible();
    expect(new URL(page.url()).searchParams.get("session")).toBe(id);
    expect(bodies).toHaveLength(1);
    await page.reload();
    await expect(page.getByText("Part 1 of 6", { exact: true })).toBeVisible();
    expect(new URL(page.url()).searchParams.get("session")).toBe(id);
    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    expect(JSON.parse(bodies[0]!)).toMatchObject({ focusId: "present-verbs", planRevision: "present-verbs-1" });
  });
}

test("cancelling topic selection restores an uncertain answer and its exact retry", async ({ page }) => {
  const old = await openVerbs(page);
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) { await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  const answer = wrongAnswer(old.currentExercise!);
  await page.getByRole("button", { name: answer.text, exact: true }).click();
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.getByRole("button", { name: "Start new practice", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Choose your practice", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  expect(new URL(page.url()).searchParams.get("session")).toBe(old.id);
  await page.getByRole("button", { name: "Retry saved operation", exact: true }).click();
  await expect(page.getByRole("status")).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  expect((await readPractice(page.request, old.id)).items[0]!.attemptCount).toBe(1);
});

test("opening and cancelling the chooser preserves a draft and the old recovery record", async ({ page }) => {
  const state = await openVerbs(page);
  await saveWrongAttempt(page.request, state.currentExercise!);
  expect((await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, {
    headers: ownerHeaders(state.ownerSessionId), data: { position: 0 },
  })).status()).toBe(200);
  await page.reload();
  await page.getByRole("textbox").fill("læ");
  let bootstraps = 0;
  page.on("request", (request) => { if (request.method() === "POST" && new URL(request.url()).pathname === "/api/v1/development-session") bootstraps += 1; });
  const oldUrl = page.url();
  await page.getByRole("button", { name: "Start new practice", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Choose your practice", exact: true })).toBeVisible();
  expect(page.url()).toBe(oldUrl);
  await page.getByRole("radio", { name: "Mixed practice", exact: true }).check();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByRole("textbox")).toHaveValue("læ");
  expect(page.url()).toBe(oldUrl);
  expect(bootstraps).toBe(0);
  await page.getByRole("button", { name: "Start new practice", exact: true }).click();
  await choosePractice(page);
  await expect(page.getByText("Part 1 of 8", { exact: true })).toBeVisible();
  expect(new URL(page.url()).searchParams.get("session")).not.toBe(state.id);
  expect(await page.evaluate((id) => Boolean(localStorage.getItem(`astrid.practice.v1.run.${id}`)), state.id)).toBe(true);
  await page.goto(oldUrl);
  await expect(page.getByText("Part 2 of 6", { exact: true })).toBeVisible();
  expect((await readPractice(page.request, state.id)).cursor).toBe(1);
});

test("an expired owner is renewed only when the learner actually starts the chosen focus", async ({ page, context }) => {
  const old = await openVerbs(page);
  await context.clearCookies();
  let bootstraps = 0;
  let creates = 0;
  page.on("request", (request) => {
    if (request.method() !== "POST") return;
    const path = new URL(request.url()).pathname;
    if (path === "/api/v1/development-session") bootstraps += 1;
    if (path === "/api/v1/practice-sessions") creates += 1;
  });
  const oldUrl = page.url();
  await page.getByRole("button", { name: "Start new practice", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Choose your practice", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByText("Part 1 of 6", { exact: true })).toBeVisible();
  expect(page.url()).toBe(oldUrl);
  expect(bootstraps).toBe(0);
  expect(creates).toBe(0);
  await page.getByRole("button", { name: "Start new practice", exact: true }).click();
  await choosePractice(page, "Present verb forms");
  await expect(page.getByText("Part 1 of 6", { exact: true })).toBeVisible();
  const next = await readPractice(page.request, new URL(page.url()).searchParams.get("session")!);
  expect(next.id).not.toBe(old.id);
  expect(next.ownerSessionId).not.toBe(old.ownerSessionId);
  expect(bootstraps).toBe(1);
  expect(creates).toBe(1);
  expect(await page.evaluate((id) => Boolean(localStorage.getItem(`astrid.practice.v1.run.${id}`)), old.id)).toBe(true);
  expect((await page.request.get(`/api/v1/practice-sessions/${old.id}`)).status()).toBe(404);
});

test("a failed catalog check preserves the previous run and cancels without a mutation", async ({ page }) => {
  const old = await openVerbs(page);
  const oldUrl = page.url();
  let creates = 0;
  page.on("request", (request) => { if (request.method() === "POST" && new URL(request.url()).pathname === "/api/v1/practice-sessions") creates += 1; });
  await page.route("**/api/v1/practice-profiles", (route) => route.fulfill({ status: 503, contentType: "application/problem+json", body: JSON.stringify({ title: "Unavailable", detail: "Catalog unavailable" }) }));
  await page.getByRole("button", { name: "Start new practice", exact: true }).click();
  await choosePractice(page, "Mixed practice");
  await expect(page.getByRole("alert")).toBeVisible();
  expect(page.url()).toBe(oldUrl);
  expect(creates).toBe(0);
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByText("Part 1 of 6", { exact: true })).toBeVisible();
  expect((await readPractice(page.request, old.id)).cursor).toBe(0);
});

for (const theme of ["light", "dark"] as const) {
  test(`topic chooser and verb operations reflow with keyboard access in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/practice");
    await expect(page.getByRole("heading", { name: "Choose your practice", exact: true })).toBeVisible();
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    await scan(page);
    const radio = page.getByRole("radio", { name: "Present verb forms", exact: true });
    await radio.focus();
    await page.keyboard.press("Space");
    await expect(radio).toBeChecked();
    const start = page.getByRole("button", { name: "Start practice", exact: true });
    const tab = process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit" ? "Alt+Tab" : "Tab";
    for (let step = 0; step < 30 && !(await start.evaluate((element) => element === document.activeElement)); step += 1) await page.keyboard.press(tab);
    await expect(start).toBeFocused();
    await page.keyboard.press("Enter");
    await expect(page.getByText("Part 1 of 6", { exact: true })).toBeVisible();
    let state = await readPractice(page.request, new URL(page.url()).searchParams.get("session")!);
    for (let position = 0; position < 3; position += 1) {
      await scan(page);
      await saveWrongAttempt(page.request, state.currentExercise!);
      expect((await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, {
        headers: ownerHeaders(state.ownerSessionId), data: { position: state.cursor },
      })).status()).toBe(200);
      await page.reload();
      await expect(page.getByText(`Part ${position + 2} of 6`, { exact: true })).toBeVisible();
      await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
      state = await readPractice(page.request, state.id);
    }
    await finish(page, state);
    await page.reload();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    await scan(page);
  });
}
