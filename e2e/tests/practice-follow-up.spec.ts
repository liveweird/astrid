import AxeBuilder from "@axe-core/playwright";
import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { createPractice, ownerHeaders, readPractice, saveWrongAttempt, type PracticeState } from "../support/practice.js";

type LexicalTarget = { entryId: string; senseId: string; facet: string; direction: string };
type TargetRow = { label: string; targetSkillId?: string | null; lexicalTarget?: LexicalTarget | null };
type ReviewState = PracticeState & {
  planRevision: string;
  followUp?: { sourcePracticeSessionId: string; selectedTargetKeys: string[] };
};

function targetKey(row: TargetRow) {
  if (row.targetSkillId) return `grammar:${row.targetSkillId}`;
  const target = row.lexicalTarget!;
  return `lexical:${target.entryId}:${target.senseId}:${target.facet}:${target.direction}`;
}

async function complete(request: APIRequestContext, state: PracticeState) {
  while (state.state !== "completed") {
    if (!state.items[state.cursor]!.attemptCount) await saveWrongAttempt(request, state.currentExercise!);
    const response = await request.post(`/api/v1/practice-sessions/${state.id}/advance`, {
      headers: ownerHeaders(state.ownerSessionId), data: { position: state.cursor },
    });
    expect(response.status()).toBe(200);
    state = await response.json() as PracticeState;
  }
  return state;
}

async function completedSource(page: Page) {
  const { state } = await createPractice(page.request);
  const source = await complete(page.request, state);
  await page.goto(`/practice?session=${source.id}`);
  // URL-only recovery is intentionally owner-bound; initialize browser metadata for this owned API fixture.
  await page.evaluate((saved) => {
    localStorage.setItem(`astrid.practice.v1.run.${saved.id}`, JSON.stringify({
      ownerSessionId: saved.ownerSessionId, practiceSessionId: saved.id,
      focusId: "reading-basics", planRevision: "practice-1", seed: 47,
    }));
  }, source);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  return source;
}

async function startReview(page: Page, row: TargetRow) {
  await page.getByRole("checkbox", { name: `Review ${row.label}`, exact: true }).check();
  await page.getByRole("button", { name: "Start selected review", exact: true }).click();
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  const id = new URL(page.url()).searchParams.get("session")!;
  return await readPractice(page.request, id) as ReviewState;
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
}

test("follow-up selection keeps recognition and recall distinct and preserves its source", async ({ page }) => {
  const source = await completedSource(page);
  const before = JSON.stringify(await readPractice(page.request, source.id));
  const rows = source.summary.rows as TargetRow[];
  const recall = rows.find((row) => row.lexicalTarget?.direction === "en-da")!;
  const recognition = rows.find((row) => row.lexicalTarget?.senseId === recall.lexicalTarget!.senseId && row.lexicalTarget.direction === "da-en")!;
  const start = page.getByRole("button", { name: "Start selected review", exact: true });
  await expect(start).toBeDisabled();
  await expect(page.getByRole("checkbox", { checked: true })).toHaveCount(0);
  await page.getByRole("checkbox", { name: `Review ${recognition.label}`, exact: true }).check();
  await expect(start).toBeEnabled();
  await page.getByRole("checkbox", { name: `Review ${recognition.label}`, exact: true }).uncheck();
  await expect(start).toBeDisabled();
  const child = await startReview(page, recall);
  expect(child.id).not.toBe(source.id);
  expect(child.itemCount).toBe(1);
  expect(child.currentExercise!.formatId).toBe("E008");
  expect(child.followUp).toEqual({ sourcePracticeSessionId: source.id, selectedTargetKeys: [targetKey(recall)] });
  await expect(page.getByText("Part 1 of 1", { exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "Source practice", exact: true })).toHaveAttribute("href", `/practice?session=${source.id}`);
  await page.reload();
  await expect(page.getByText("Part 1 of 1", { exact: true })).toBeVisible();
  await saveWrongAttempt(page.request, child.currentExercise!);
  await complete(page.request, await readPractice(page.request, child.id));
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  const completed = await readPractice(page.request, child.id);
  const context = completed.items[0]!.firstAttemptContext as { sourceExposure?: { practiceSessionId: string; keys: string[] }[] };
  expect(context.sourceExposure?.some((entry) => entry.practiceSessionId === source.id && entry.keys.includes(`headword:${recall.lexicalTarget!.senseId}`))).toBe(true);
  expect(JSON.stringify(await readPractice(page.request, source.id))).toBe(before);
  await page.getByRole("link", { name: "Source practice", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect(new URL(page.url()).searchParams.get("session")).toBe(source.id);
  expect(JSON.stringify(await readPractice(page.request, source.id))).toBe(before);
});

for (const committed of [false, true]) {
  test(`follow-up creation recovers exact source and body after ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
    const source = await completedSource(page);
    const row = (source.summary.rows as TargetRow[]).find((candidate) => candidate.targetSkillId === "G006")!;
    const bodies: string[] = [];
    const endpoints: string[] = [];
    let renewals = 0;
    page.on("request", (request) => {
      if (request.method() === "POST" && new URL(request.url()).pathname.endsWith("/development-session")) renewals += 1;
    });
    await page.route("**/api/v1/practice-sessions/*/follow-ups", async (route) => {
      bodies.push(route.request().postData()!);
      endpoints.push(new URL(route.request().url()).pathname);
      if (bodies.length === 1) {
        if (committed) await route.fetch();
        await route.abort("connectionfailed");
      } else await route.continue();
    });
    await page.getByRole("checkbox", { name: `Review ${row.label}`, exact: true }).check();
    await page.getByRole("button", { name: "Start selected review", exact: true }).click();
    await expect(page.getByRole("alert")).toBeVisible();
    const id = new URL(page.url()).searchParams.get("session")!;
    expect(id).not.toBe(source.id);
    await page.reload();
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
    expect(bodies).toHaveLength(2);
    expect(bodies[1]).toBe(bodies[0]);
    expect(endpoints).toEqual([`/api/v1/practice-sessions/${source.id}/follow-ups`, `/api/v1/practice-sessions/${source.id}/follow-ups`]);
    expect(renewals).toBe(0);
    const child = await readPractice(page.request, id) as ReviewState;
    expect(child.itemCount).toBe(4);
    expect(child.items.every((item) => item.targetSkillId === "G006")).toBe(true);
    expect(new Set(child.items.map((item) => item.formatId))).toEqual(new Set(["E002", "E006"]));
    expect(child.followUp!.sourcePracticeSessionId).toBe(source.id);
    expect(await page.evaluate((sourceId) => Boolean(localStorage.getItem(`astrid.practice.v1.run.${sourceId}`)), source.id)).toBe(true);
  });
}

test("follow-up creation can retry the saved operation before a child state exists", async ({ page }) => {
  const source = await completedSource(page);
  const row = (source.summary.rows as TargetRow[]).find((candidate) => candidate.lexicalTarget?.direction === "da-en")!;
  const bodies: string[] = [];
  await page.route("**/api/v1/practice-sessions/*/follow-ups", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) { await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  await page.getByRole("checkbox", { name: `Review ${row.label}`, exact: true }).check();
  await page.getByRole("button", { name: "Start selected review", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.getByRole("button", { name: "Retry saved operation", exact: true }).click();
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
});

for (const theme of ["light", "dark"] as const) {
  test(`follow-up selection and source context remain accessible at enlarged text in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    const source = await completedSource(page);
    await page.setViewportSize({ width: 390, height: 844 });
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    const row = (source.summary.rows as TargetRow[]).find((candidate) => candidate.targetSkillId === "G005")!;
    await scan(page);
    const checkbox = page.getByRole("checkbox", { name: `Review ${row.label}`, exact: true });
    await checkbox.focus();
    await page.keyboard.press("Space");
    await expect(checkbox).toBeChecked();
    const start = page.getByRole("button", { name: "Start selected review", exact: true });
    const tab = process.platform === "darwin" && page.context().browser()?.browserType().name() === "webkit" ? "Alt+Tab" : "Tab";
    for (let count = 0; count < 50 && !(await start.evaluate((element) => element === document.activeElement)); count += 1) await page.keyboard.press(tab);
    await expect(start).toBeFocused();
    await page.keyboard.press("Enter");
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
    await expect(page.locator("h2[tabindex='-1']")).toBeFocused();
    await scan(page);
    const id = new URL(page.url()).searchParams.get("session")!;
    await complete(page.request, await readPractice(page.request, id));
    await page.reload();
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    await scan(page);
  });
}

test("all eight summary targets can be selected without merging lexical directions", async ({ page }) => {
  const source = await completedSource(page);
  const rows = source.summary.rows as TargetRow[];
  expect(rows).toHaveLength(8);
  for (const row of rows) await page.getByRole("checkbox", { name: `Review ${row.label}`, exact: true }).check();
  await expect(page.getByRole("checkbox", { checked: true })).toHaveCount(8);
  await page.getByRole("button", { name: "Start selected review", exact: true }).click();
  await expect(page.getByText("Part 1 of 8", { exact: true })).toBeVisible();
  const id = new URL(page.url()).searchParams.get("session")!;
  const child = await readPractice(page.request, id) as ReviewState;
  expect(child.itemCount).toBe(8);
  expect(new Set(child.followUp!.selectedTargetKeys)).toEqual(new Set(rows.map(targetKey)));
  await complete(page.request, child);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  const finished = await readPractice(page.request, id);
  expect(new Set((finished.summary.rows as TargetRow[]).map(targetKey))).toEqual(new Set(rows.map(targetKey)));
});

test("an uncertain follow-up creation is never reassigned after cookie rotation", async ({ page, context }) => {
  const source = await completedSource(page);
  const row = (source.summary.rows as TargetRow[]).find((candidate) => candidate.targetSkillId === "G005")!;
  let creates = 0;
  await page.route("**/api/v1/practice-sessions/*/follow-ups", async (route) => {
    creates += 1;
    await route.abort("connectionfailed");
  });
  await page.getByRole("checkbox", { name: `Review ${row.label}`, exact: true }).check();
  await page.getByRole("button", { name: "Start selected review", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  const id = new URL(page.url()).searchParams.get("session")!;
  await context.clearCookies();
  expect((await page.request.post("/api/v1/development-session", { headers: { "X-Astrid-Client": "playground" }, data: {} })).ok()).toBe(true);
  await page.reload();
  await expect(page.getByRole("alert")).toContainText(/expired|different|unavailable/);
  expect(creates).toBe(1);
  expect((await page.request.get(`/api/v1/practice-sessions/${id}`)).status()).toBe(404);
  expect((await page.request.get(`/api/v1/practice-sessions/${source.id}`)).status()).toBe(404);
});
