import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { openGenerationControls } from "../support/generation-controls.js";
import { choosePractice, ownerHeaders, readPractice, type PracticeState } from "../support/practice.js";
import { textLanguage } from "../support/vocabulary.js";

type MatchItem = { id: string; text: string };
type ChoiceMapping = { choice: string; mappings: { danish: string; english: string }[] };
type Exercise = {
  id: string;
  formatId: string;
  context: string;
  targetScopeId?: string;
  prompt: {
    cue?: string;
    choices: string[];
    blocks: { id: string; text: string }[];
    passage?: string[];
    passageLabels?: { sentenceIndex: number; text: string }[];
    choiceMappings?: ChoiceMapping[];
    matchingColumns?: { left: MatchItem[]; right: MatchItem[] };
  };
};
type P3State = Omit<PracticeState, "currentExercise" | "summary"> & {
  focusId: string;
  planRevision: string;
  contentRevision: string;
  generatorRevision: string;
  introductionOpened: boolean;
  introductionRequired: boolean;
  currentExercise: Exercise | null;
  summary: Omit<PracticeState["summary"], "rows"> & { rows: Record<string, unknown>[] };
};
type Answer =
  | { kind: "text"; value: string }
  | { kind: "input"; value: string }
  | { kind: "order"; words: string[] }
  | { kind: "matching"; pairs: Record<string, string> };

const HOME_MAPPING = "hjemme = static at home; hjem = homeward destination";
const STATION_MAPPING = "til stationen = destination; på stationen = static location";
const CAR_CONTEXT = "Car context: This journey is by car.";
const RAIL_CONTEXT = "Rail context: This journey is by train.";
const PROFILES: readonly {
  focus: string;
  title: string;
  count: number;
  teaching: string[];
  answers: Answer[];
  readingRoles: string[];
}[] = [
  {
    focus: "vocabulary-p3-s1", title: "Two ways to travel", count: 4,
    teaching: ["en bil", "en cykel", "at cykle → cykler", "nu"], readingRoles: ["subject"],
    answers: [
      { kind: "text", value: "a bicycle" },
      { kind: "matching", pairs: { "a car": "en bil", "a bicycle": "en cykel", "travels by bicycle": "cykler" } },
      { kind: "text", value: "Nora" }, { kind: "order", words: ["Nora", "cykler", "nu"] },
    ],
  },
  {
    focus: "vocabulary-p3-s2", title: "Walking to the station and home", count: 4,
    teaching: ["en station", "at gå → går", "til stationen", "hjem"], readingRoles: ["destination"],
    answers: [
      { kind: "matching", pairs: { "a station": "en station", "walks/goes on foot": "går", "to the station": "til stationen", "home/homeward": "hjem" } },
      { kind: "text", value: "to the station" }, { kind: "text", value: "destination/direction" },
      { kind: "order", words: ["Anna", "går", "til stationen"] },
    ],
  },
  {
    focus: "vocabulary-p3-s3", title: "City, home and library", count: 4,
    teaching: ["at bo → bor", "i Odense", "hjemme", "et bibliotek"], readingRoles: ["static-location"],
    answers: [
      { kind: "matching", pairs: { "lives/resides": "bor", "in Odense": "i Odense", "at home": "hjemme", "a library": "et bibliotek" } },
      { kind: "text", value: "in Odense" }, { kind: "text", value: "at home" },
      { kind: "text", value: HOME_MAPPING },
    ],
  },
  {
    focus: "vocabulary-p3-s4", title: "Ticket, bus and train", count: 3,
    teaching: ["en bus", "et tog", "en billet"], readingRoles: ["object"],
    answers: [
      { kind: "matching", pairs: { "a bus": "en bus", "a train": "et tog", "a ticket": "en billet" } },
      { kind: "text", value: "a ticket" }, { kind: "text", value: "et tog" },
    ],
  },
  {
    focus: "vocabulary-p3-s5", title: "Waiting at the station", count: 4,
    teaching: ["at køre → kører", "at vente → venter", "på stationen"], readingRoles: ["static-location"],
    answers: [
      { kind: "matching", pairs: { "travels/drives by vehicle": "kører", waits: "venter", "at the station": "på stationen" } },
      { kind: "text", value: "at the station" }, { kind: "text", value: STATION_MAPPING },
      { kind: "input", value: "kører" },
    ],
  },
  {
    focus: "vocabulary-p3-s6", title: "A change and the pharmacy", count: 4,
    teaching: ["at skifte → skifter", "et apotek", "til apoteket", "Jeg går til apoteket."],
    readingRoles: ["action", "destination"],
    answers: [
      { kind: "matching", pairs: { "changes trains/connections": "skifter", "a pharmacy": "et apotek", "to the pharmacy": "til apoteket" } },
      { kind: "text", value: "changes trains/connections" }, { kind: "text", value: "to the pharmacy" },
      { kind: "order", words: ["Jeg", "går", "til apoteket"] },
    ],
  },
];

async function stateFor(page: Page) {
  await expect(page).toHaveURL(/\/practice\?session=[0-9a-f-]+$/);
  return await readPractice(page.request, new URL(page.url()).searchParams.get("session")!) as unknown as P3State;
}

async function openProfile(page: Page, profile: typeof PROFILES[number]) {
  await page.goto("/practice");
  await expect(page.getByRole("heading", { name: "Places and journeys", exact: true })).toBeVisible();
  await choosePractice(page, profile.title);
  const state = await stateFor(page);
  expect(state).toMatchObject({
    focusId: profile.focus, planRevision: `${profile.focus}-1`, contentRevision: "pilot-20",
    generatorRevision: "generator-20", itemCount: profile.count,
    introductionRequired: true, introductionOpened: false,
  });
  return state;
}

async function begin(page: Page) {
  await page.getByRole("button", { name: "Session introduction", exact: true }).click();
  await expect(page.getByRole("region", { name: "Session introduction", exact: true })).toBeVisible();
  return await stateFor(page);
}

async function chooseMatching(page: Page, exercise: Exercise, oracle: Record<string, string>) {
  const columns = exercise.prompt.matchingColumns!;
  expect(new Set(columns.left.map(({ text }) => text))).toEqual(new Set(Object.keys(oracle)));
  expect(new Set(columns.right.map(({ text }) => text))).toEqual(new Set(Object.values(oracle)));
  for (const left of columns.left) {
    await page.getByRole("group", { name: "English words", exact: true })
      .getByRole("button", { name: left.text, exact: true }).press("Enter");
    await page.getByRole("group", { name: "Danish words", exact: true })
      .getByRole("button", { name: oracle[left.text], exact: true }).press("Space");
  }
}

async function chooseOrder(page: Page, exercise: Exercise, words: string[]) {
  const remaining = [...exercise.prompt.blocks];
  for (const word of words) {
    const block = remaining.find(({ text }) => text.toLocaleLowerCase("da-DK") === word.toLocaleLowerCase("da-DK"));
    expect(block, `The authored bank contains ${word}`).toBeDefined();
    remaining.splice(remaining.indexOf(block!), 1);
    await page.getByLabel("Available Danish word tiles", { exact: true })
      .getByRole("button", { name: block!.text, exact: true }).press("Enter");
  }
}

function escapePattern(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function mappingButton(page: Page, mapping: ChoiceMapping) {
  const name = mapping.mappings
    .map(({ danish, english }) => `${escapePattern(danish)}\\s*=\\s*${escapePattern(english)}`)
    .join("\\s*;\\s*");
  return page.getByRole("button", { name: new RegExp(`^\\s*${name}\\s*$`) });
}

function textChoiceButton(page: Page, exercise: Exercise, value: string) {
  const mapping = exercise.prompt.choiceMappings?.find((candidate) => candidate.choice === value);
  return mapping ? mappingButton(page, mapping) : page.getByRole("button", { name: value, exact: true });
}

async function answer(page: Page, exercise: Exercise, expected: Answer) {
  if (expected.kind === "matching") {
    await chooseMatching(page, exercise, expected.pairs);
    await page.getByRole("button", { name: "Check matches", exact: true }).click();
  } else if (expected.kind === "order") {
    await chooseOrder(page, exercise, expected.words);
    await page.getByRole("button", { name: "Check answer", exact: true }).click();
  } else if (expected.kind === "input") {
    await page.getByRole("textbox").fill(expected.value);
    await page.getByRole("button", { name: "Check answer", exact: true }).click();
  } else {
    await textChoiceButton(page, exercise, expected.value).click();
    await page.getByRole("button", { name: "Check answer", exact: true }).click();
  }
  await expect(page.getByRole("status")).toBeFocused();
}

async function advance(page: Page, previous: P3State) {
  const saved = page.waitForResponse((response) => response.request().method() === "POST"
    && new URL(response.url()).pathname.endsWith("/advance") && response.status() === 200);
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await saved;
  if (previous.cursor + 1 === previous.itemCount) {
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeFocused();
  } else {
    await expect(page.getByText(`Part ${previous.cursor + 2} of ${previous.itemCount}`, { exact: true })).toBeVisible();
  }
  return await stateFor(page);
}

async function assertTeachingGate(page: Page, exercise: Exercise) {
  const answer = exercise.formatId === "E047"
    ? { kind: "matching", pairs: [] }
    : exercise.formatId === "E049" ? { kind: "order", blockIds: exercise.prompt.blocks.map(({ id }) => id) }
      : { kind: "text", text: exercise.prompt.choices[0] ?? "unknown" };
  const response = await page.request.post("/api/v1/attempts", {
    headers: { "X-Astrid-Client": "playground" },
    data: { attemptId: crypto.randomUUID(), exerciseId: exercise.id, answer, hintUsed: false },
  });
  expect(response.status()).toBe(409);
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
}

async function expectMappingRowsContained(page: Page, exercise: Exercise) {
  for (const choice of exercise.prompt.choiceMappings ?? []) {
    const button = mappingButton(page, choice);
    const bounds = await button.boundingBox();
    expect(bounds).not.toBeNull();
    const labelFits = await button.locator(".mantine-Button-label").evaluate((label) => ({
      clientWidth: label.clientWidth, scrollWidth: label.scrollWidth,
      clientHeight: label.clientHeight, scrollHeight: label.scrollHeight,
    }));
    expect(labelFits.scrollWidth).toBeLessThanOrEqual(labelFits.clientWidth + 1);
    expect(labelFits.scrollHeight).toBeLessThanOrEqual(labelFits.clientHeight + 1);
    for (const mapping of choice.mappings) {
      for (const text of [mapping.danish, mapping.english]) {
        const row = await button.getByText(text, { exact: true }).boundingBox();
        expect(row).not.toBeNull();
        expect(row!.x).toBeGreaterThanOrEqual(bounds!.x - 1);
        expect(row!.x + row!.width).toBeLessThanOrEqual(bounds!.x + bounds!.width + 1);
        expect(row!.y).toBeGreaterThanOrEqual(bounds!.y - 1);
        expect(row!.y + row!.height).toBeLessThanOrEqual(bounds!.y + bounds!.height + 1);
      }
    }
  }
}

async function assertAuthoredTeaching(page: Page, profile: typeof PROFILES[number]) {
  const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
  for (const form of profile.teaching) await expect(introduction.getByText(form, { exact: true }).first()).toBeVisible();
  if (profile.focus !== "vocabulary-p3-s6") return;
  await expect(introduction.getByText(
    "Anna is travelling by train and changes trains/connections in Aarhus.", { exact: false },
  )).toBeVisible();
  for (const translation of [
    "Anna changes trains/connections in Aarhus.", "Nora walks to the pharmacy.", "Anna walks home.",
  ]) await expect(introduction.getByText(translation, { exact: true })).toBeVisible();
}

async function assertAssessmentPresentation(
  page: Page,
  profile: typeof PROFILES[number],
  exercise: Exercise,
) {
  if (exercise.context.startsWith("Car context:")) {
    expect(exercise.context).toBe(CAR_CONTEXT);
    await expect(page.getByText(CAR_CONTEXT, { exact: true })).toHaveAttribute("lang", "en");
  }
  if (exercise.context.startsWith("Rail context:")) {
    expect(exercise.context).toBe(RAIL_CONTEXT);
    await expect(page.getByText(RAIL_CONTEXT, { exact: true })).toHaveAttribute("lang", "en");
  }
  if (profile.focus === "vocabulary-p3-s6" && exercise.formatId === "E050"
    && exercise.prompt.cue === "What does Anna do in Aarhus?") {
    expect(exercise.context).toBe(RAIL_CONTEXT);
    expect(exercise.context.toLocaleLowerCase("en")).not.toMatch(/changes|connections/);
  }
  if (exercise.formatId === "E051" && exercise.targetScopeId === "p3-home-direction") {
    expect(exercise.prompt.cue).toBe("Nora går hjem.");
    await expect(page.getByRole("paragraph").filter({ hasText: /^Nora går hjem\.$/ })).toHaveAttribute("lang", "da");
  }
  for (const label of exercise.prompt.passageLabels ?? []) {
    await expect(page.getByRole("group", { name: "Reading passage", exact: true })
      .getByText(label.text, { exact: true })).toHaveAttribute("lang", "en");
    await expect(page.getByRole("group", { name: "Reading passage", exact: true })
      .getByText(exercise.prompt.passage![label.sentenceIndex]!, { exact: true })).toHaveAttribute("lang", "da");
  }
  for (const choice of exercise.prompt.choiceMappings ?? []) {
    const button = mappingButton(page, choice);
    await expect(button).toBeVisible();
    for (const mapping of choice.mappings) {
      await expect(button.getByText(mapping.danish, { exact: true })).toHaveAttribute("lang", "da");
      await expect(button.getByText(mapping.english, { exact: true })).toHaveAttribute("lang", "en");
    }
  }
}

for (const profile of PROFILES) {
  test(`P3 teaches and completes every authored activity in ${profile.focus}`, async ({ page }) => {
    const unopened = await openProfile(page, profile);
    await assertTeachingGate(page, unopened.currentExercise!);
    let state = await begin(page);
    await assertAuthoredTeaching(page, profile);
    await page.getByRole("button", { name: "Session introduction", exact: true }).click();
    await expect(page.getByRole("region", { name: "Session introduction", exact: true })).not.toBeVisible();
    const seen: string[] = [];
    while (state.state !== "completed") {
      const exercise = state.currentExercise!;
      seen.push(exercise.formatId);
      await assertAssessmentPresentation(page, profile, exercise);
      await answer(page, exercise, profile.answers[state.cursor]!);
      state = await advance(page, state);
    }
    expect(seen).toHaveLength(profile.count);
    expect(seen.every((format) => ["E045", "E046", "E047", "E048", "E049", "E050", "E051"].includes(format))).toBe(true);
    for (const role of profile.readingRoles) {
      expect(state.summary.rows.some((row) => row.readingRole === role)).toBe(true);
    }
    await page.reload();
    expect((await stateFor(page)).summary).toEqual(state.summary);
  });
}

test("P3 partial matching remains unknown until corrected and replays one saved board", async ({ page }) => {
  const profile = PROFILES[1]!;
  await openProfile(page, profile);
  const state = await begin(page);
  const exercise = state.currentExercise!;
  const oracle = (profile.answers[0] as Extract<Answer, { kind: "matching" }>).pairs;
  const first = exercise.prompt.matchingColumns!.left[0]!;
  await page.getByRole("group", { name: "English words", exact: true }).getByRole("button", { name: first.text, exact: true }).press("Enter");
  await page.getByRole("group", { name: "Danish words", exact: true }).getByRole("button", { name: oracle[first.text], exact: true }).press("Space");
  const response = page.waitForResponse((candidate) => candidate.request().method() === "POST" && candidate.url().endsWith("/attempts"));
  await page.getByRole("button", { name: "Check matches", exact: true }).click();
  const attempt = await (await response).json() as { matchingResults: { meaningSatisfaction: string }[] };
  expect(attempt.matchingResults.some((result) => result.meaningSatisfaction === "unknown")).toBe(true);
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  for (const left of exercise.prompt.matchingColumns!.left.slice(1)) {
    await page.getByRole("group", { name: "English words", exact: true })
      .getByRole("button", { name: left.text, exact: true }).press("Enter");
    await page.getByRole("group", { name: "Danish words", exact: true })
      .getByRole("button", { name: oracle[left.text], exact: true }).press("Space");
  }
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) { await route.fetch(); await route.abort("connectionfailed"); }
    else await route.continue();
  });
  await page.getByRole("button", { name: "Check matches", exact: true }).click();
  await expect(page.getByRole("alert", { name: "Your answer is not confirmed", exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("status")).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
});

test("P3 recall separates known wrong, unknown and one-word shape before correction", async ({ page }) => {
  const profile = PROFILES[4]!;
  await openProfile(page, profile);
  let state = await begin(page);
  for (let position = 0; position < 3; position += 1) {
    await answer(page, state.currentExercise!, profile.answers[position]!);
    state = await advance(page, state);
  }
  await expect(page.getByText(CAR_CONTEXT, { exact: true })).toHaveAttribute("lang", "en");
  const input = page.getByRole("textbox");
  await input.fill("køre");
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeEnabled();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await input.fill("bil");
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await input.fill("kører hjem");
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("One-word form required");
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await input.fill("kører");
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeEnabled();
});

test("P3 whole spatial contrast records one scoped result without lexical credit", async ({ page }) => {
  const profile = PROFILES[2]!;
  await openProfile(page, profile);
  let state = await begin(page);
  for (let position = 0; position < 3; position += 1) {
    await answer(page, state.currentExercise!, profile.answers[position]!);
    state = await advance(page, state);
  }
  const reversed = "hjemme = homeward destination; hjem = static at home";
  await textChoiceButton(page, state.currentExercise!, reversed).click();
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeEnabled();
  state = await advance(page, state);
  const rows = state.summary.rows.filter((row) => row.targetScopeId === "p3-home-spatial-contrast");
  expect(rows).toHaveLength(1);
  expect(rows[0]).not.toHaveProperty("lexicalTarget");
});

test("P3 standalone recall separates the English instruction from the Danish cue", async ({ page }) => {
  await page.goto("/playground");
  await openGenerationControls(page);
  const catalog = await (await page.request.get("/api/v1/catalog")).json() as { formats: { id: string; title: string }[] };
  const title = catalog.formats.find(({ id }) => id === "E048")!.title;
  const selector = page.getByRole("combobox", { name: "Exercise type" });
  await selector.click();
  await page.getByRole("option", { name: title, exact: true }).click();
  await expect(page.getByText(CAR_CONTEXT, { exact: true })).toHaveAttribute("lang", "en");
  const instruction = page.getByRole("heading", {
    name: "The person is travelling home by car. Type the introduced present form of this verb.", exact: true,
  });
  expect(await textLanguage(instruction)).toBe("en");
  await expect(page.getByText("at køre", { exact: true })).toHaveAttribute("lang", "da");
  await expect(page.getByRole("textbox", { name: "Danish present form", exact: true })).toHaveAttribute("lang", "da");
});

const RETAINED_LESSONS = [
  "module-one-singular", "module-one-groups", "module-one-er", "module-one-things", "module-one-present",
  "module-one-order", "module-one-possession", "module-one-reading-words", "module-one-personal", "module-one-ability",
  "module-one-volition", "module-one-obligation", "module-one-permission", "module-one-circumstances", "module-one-questions",
  "vocabulary-p1-s1", "vocabulary-p1-s2", "vocabulary-p1-s3", "vocabulary-p1-s4", "vocabulary-p1-s5",
  "vocabulary-p2-s1", "vocabulary-p2-s2", "vocabulary-p2-s3", "vocabulary-p2-s4", "vocabulary-p2-s5", "vocabulary-p2-s6",
] as const;

async function introduce(page: Page, owner: string, focusId: string, seed: number) {
  const suffix = focusId.startsWith("vocabulary-p1") ? "-2" : "-1";
  const id = crypto.randomUUID();
  const created = await page.request.post("/api/v1/practice-sessions", {
    headers: ownerHeaders(owner), data: { practiceSessionId: id, focusId, planRevision: `${focusId}${suffix}`, seed },
  });
  expect(created.status()).toBe(201);
  expect((await page.request.post(`/api/v1/practice-sessions/${id}/support`, {
    headers: ownerHeaders(owner), data: { kind: "introduction" },
  })).status()).toBe(200);
}

test("cumulative review three grows from 20 to 26 to 32 families without changing saved one and two", async ({ page }) => {
  await page.goto("/#review");
  await page.getByRole("button", { name: "Prepare mixed review", exact: true }).click();
  await expect(page.getByText("No introduced material yet", { exact: true })).toBeVisible();
  const empty = await (await page.request.get("/api/v1/practice-review-context")).json() as { ownerSessionId: string };
  for (const [index, focus] of RETAINED_LESSONS.slice(0, 20).entries()) await introduce(page, empty.ownerSessionId, focus, 800 + index);
  let context = await (await page.request.get("/api/v1/practice-review-context")).json() as { sourceLessonCount: number; planRevision: string };
  expect(context).toMatchObject({ sourceLessonCount: 20, planRevision: "cumulative-review-3" });
  const oneId = crypto.randomUUID();
  const oneResponse = await page.request.post("/api/v1/practice-sessions", {
    headers: ownerHeaders(empty.ownerSessionId),
    data: { practiceSessionId: oneId, focusId: "cumulative-review", planRevision: "cumulative-review-1", seed: 900 },
  });
  expect(oneResponse.status()).toBe(201);
  const one = await oneResponse.json();
  for (const [index, focus] of RETAINED_LESSONS.slice(20).entries()) await introduce(page, empty.ownerSessionId, focus, 901 + index);
  context = await (await page.request.get("/api/v1/practice-review-context")).json();
  expect(context).toMatchObject({ sourceLessonCount: 26, planRevision: "cumulative-review-3" });
  const twoId = crypto.randomUUID();
  const twoResponse = await page.request.post("/api/v1/practice-sessions", {
    headers: ownerHeaders(empty.ownerSessionId),
    data: { practiceSessionId: twoId, focusId: "cumulative-review", planRevision: "cumulative-review-2", seed: 910 },
  });
  expect(twoResponse.status()).toBe(201);
  const two = await twoResponse.json();
  for (const [index, profile] of PROFILES.entries()) await introduce(page, empty.ownerSessionId, profile.focus, 920 + index);
  context = await (await page.request.get("/api/v1/practice-review-context")).json();
  expect(context).toMatchObject({ sourceLessonCount: 32, planRevision: "cumulative-review-3" });
  expect(await readPractice(page.request, oneId)).toEqual(one);
  expect(await readPractice(page.request, twoId)).toEqual(two);
});

test("P3 cumulative rail presentation keeps the minimal scene separate from full teaching", async ({ page }) => {
  await page.goto("/#review");
  await page.getByRole("button", { name: "Prepare mixed review", exact: true }).click();
  await expect(page.getByText("No introduced material yet", { exact: true })).toBeVisible();
  const empty = await (await page.request.get("/api/v1/practice-review-context")).json() as { ownerSessionId: string };
  await introduce(page, empty.ownerSessionId, "vocabulary-p3-s6", 1001);
  await page.getByRole("button", { name: "Refresh preparation", exact: true }).click();

  let selectedSeed: number | null = null;
  for (let seed = 0; seed < 64 && selectedSeed === null; seed += 1) {
    const id = crypto.randomUUID();
    const response = await page.request.post("/api/v1/practice-sessions", {
      headers: ownerHeaders(empty.ownerSessionId),
      data: { practiceSessionId: id, focusId: "cumulative-review", planRevision: "cumulative-review-3", seed },
    });
    expect(response.status()).toBe(201);
    const state = await response.json() as P3State;
    if (state.currentExercise?.formatId === "E050"
      && state.currentExercise.prompt.cue === "What does Anna do in Aarhus?") selectedSeed = seed;
  }
  expect(selectedSeed, "A bounded cumulative seed selects authored P3 activity 6.2").not.toBeNull();
  await page.evaluate((seed) => {
    Object.defineProperty(globalThis.crypto, "getRandomValues", {
      configurable: true,
      value: <T extends ArrayBufferView | null>(array: T) => {
        if (array instanceof Uint32Array) array[0] = seed;
        return array;
      },
    });
  }, selectedSeed!);
  await page.getByRole("button", { name: "Start mixed review", exact: true }).click();
  await page.getByRole("button", { name: "Session introduction", exact: true }).click();
  const state = await stateFor(page);
  expect(state.currentExercise).toMatchObject({ formatId: "E050", context: RAIL_CONTEXT });
  expect(state.currentExercise!.context.toLocaleLowerCase("en")).not.toMatch(/changes|connections/);
  await expect(page.getByText(RAIL_CONTEXT, { exact: true })).toHaveAttribute("lang", "en");
});

for (const theme of ["light", "dark"] as const) {
  test(`P3 bilingual contrast and summary reflow at 320px and 200% in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 844 });
    await openProfile(page, PROFILES[2]!);
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    let state = await begin(page);
    await scan(page);
    while (state.state !== "completed") {
      if (state.currentExercise!.prompt.choiceMappings?.length) {
        await expectMappingRowsContained(page, state.currentExercise!);
      }
      await answer(page, state.currentExercise!, PROFILES[2]!.answers[state.cursor]!);
      await scan(page);
      state = await advance(page, state);
    }
    expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
    await scan(page);
  });
}
