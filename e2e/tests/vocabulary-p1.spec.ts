import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { choosePractice, readPractice, type PracticeState } from "../support/practice.js";
import { openGenerationControls } from "../support/generation-controls.js";

type MatchItem = { id: string; text: string };
type MatchPair = { leftId: string; rightId: string };
type P1Exercise = {
  id: string;
  formatId: string;
  targetSkillId?: string;
  targetScopeId?: string;
  readingTarget?: { facet: string; role?: string };
  prompt: {
    cue?: string;
    choices: string[];
    passage?: string[];
    blocks: { id: string; text: string }[];
    matchingColumns?: { left: MatchItem[]; right: MatchItem[] };
    tileMode?: string;
    requiredTileCount?: number;
  };
};
type SavedGrade = {
  attemptId: string;
  targetSatisfaction: string;
  meaningSatisfaction: string;
  responseConstraintStatus: string;
  linguisticStatus: string;
  matchingResults?: { leftId: string; rightId?: string; targetSatisfaction: string }[];
  issues: { code: string }[];
};
type P1Item = PracticeState["items"][number] & {
  canAdvance?: boolean;
  lastAttempt?: SavedGrade;
};
type P1State = Omit<PracticeState, "currentExercise" | "items"> & {
  focusId: string;
  planRevision: string;
  contentRevision: string;
  generatorRevision: string;
  introductionOpened: boolean;
  introductionRequired: boolean;
  currentExercise: P1Exercise | null;
  items: P1Item[];
};

const PROFILES = [
  { focus: "vocabulary-p1-s1", title: "Work, reading and today", count: 4, summaryRows: 4 },
  { focus: "vocabulary-p1-s2", title: "Reading and writing", count: 4, summaryRows: 6 },
  { focus: "vocabulary-p1-s3", title: "Study, courses and Danish", count: 5, summaryRows: 8 },
  { focus: "vocabulary-p1-s4", title: "Coffee and water", count: 5, summaryRows: 7 },
  { focus: "vocabulary-p1-s5", title: "Where and when", count: 5, summaryRows: 6 },
] as const;

// Literal per-session signatures from the authored packet, independent of generated plans.
const EXPECTED_SLOTS: Record<string, readonly string[]> = {
  "vocabulary-p1-s1": ["E031:arbejder", "E031:en bog", "E031:i dag", "E039:What does Anna read?"],
  "vocabulary-p1-s2": [
    "E033:matching", "E032:Choose the introduced bundle for a newspaper.",
    "E039:What does Nora write?", "E039:What does the text explicitly say Nora has?",
  ],
  "vocabulary-p1-s3": [
    "E033:matching", "E039:What does Nora read?",
    "E037:Write She reads a book now. Begin with the subject.",
    "E034:Type the introduced present form of at studere.",
    "E035:Type the introduced Danish expression for now.",
  ],
  "vocabulary-p1-s4": [
    "E033:matching", "E039:What does Nora drink?",
    "E038:Write Nora drinks water. Begin with Nora. One word is extra.",
    "E034:Type the introduced present form of at drikke.",
    "E036:Type the introduced Danish word for water as liquid.",
  ],
  "vocabulary-p1-s5": [
    "E033:matching", "E039:Where does Nora study?", "E039:When does Nora work?",
    "E037:Write I work at home. Begin with the subject.",
    "E037:Write I work in Odense. Begin with the subject.",
  ],
};

const TEXT_ANSWERS: Record<string, string> = {
  arbejder: "works",
  "en bog": "a book",
  "i dag": "today",
  "Choose the introduced bundle for a newspaper.": "en avis",
  "What does Anna read?": "a book",
  "What does Nora write?": "a written letter",
  "What does the text explicitly say Nora has?": "a computer",
  "What does Nora read?": "a book",
  "Type the introduced present form of at studere.": "studerer",
  "Type the introduced Danish expression for now.": "nu",
  "What does Nora drink?": "water",
  "Type the introduced present form of at drikke.": "drikker",
  "Type the introduced Danish word for water as liquid.": "vand",
  "Where does Nora study?": "at home",
  "When does Nora work?": "in the morning",
};

const MATCHES: Record<string, Record<string, string>> = {
  "vocabulary-p1-s2": {
    "a newspaper": "en avis", "a written letter": "et brev", writes: "skriver", "a computer": "en computer",
  },
  "vocabulary-p1-s3": {
    "studies (as a student)": "studerer", "a course": "et kursus", "Danish (language)": "dansk", now: "nu",
  },
  "vocabulary-p1-s4": { drinks: "drikker", coffee: "kaffe", water: "vand" },
  "vocabulary-p1-s5": { "in Aarhus": "i Aarhus", "at home": "hjemme", "in the morning": "om morgenen" },
};

const TILE_ORDERS: Record<string, string[]> = {
  "Write She reads a book now. Begin with the subject.": ["hun", "læser", "en", "bog", "nu"],
  "Write Nora drinks water. Begin with Nora. One word is extra.": ["Nora", "drikker", "vand"],
  "Write I work at home. Begin with the subject.": ["jeg", "arbejder", "hjemme"],
  "Write I work in Odense. Begin with the subject.": ["jeg", "arbejder", "i", "Odense"],
};

const FRONTED_ORDERS: Record<string, string[]> = {
  "Write She reads a book now. Begin with the subject.": ["nu", "læser", "hun", "en", "bog"],
  "Write I work in Odense. Begin with the subject.": ["i", "Odense", "arbejder", "jeg"],
};

const TEACHING: Record<string, string[]> = {
  "vocabulary-p1-s1": ["at arbejde → arbejder", "at læse → læser", "en bog", "i dag"],
  "vocabulary-p1-s2": ["en avis", "et brev", "at skrive → skriver", "en computer"],
  "vocabulary-p1-s3": ["at studere → studerer", "et kursus", "dansk", "nu"],
  "vocabulary-p1-s4": ["at drikke → drikker", "kaffe", "vand"],
  "vocabulary-p1-s5": ["i Aarhus / i Odense", "hjemme", "om morgenen"],
};

async function stateFor(page: Page) {
  await expect(page).toHaveURL(/\/practice\?session=[0-9a-f-]+$/);
  const id = new URL(page.url()).searchParams.get("session");
  expect(id, "The practice URL carries the saved P1 run identity").not.toBeNull();
  return await readPractice(page.request, id!) as unknown as P1State;
}

async function openProfile(page: Page, profile: typeof PROFILES[number]) {
  await page.goto("/practice");
  await expect(page.getByRole("heading", { name: "A day of work and study", exact: true })).toBeVisible();
  await choosePractice(page, profile.title);
  const state = await stateFor(page);
  expect(state).toMatchObject({
    focusId: profile.focus,
    planRevision: `${profile.focus}-1`,
    contentRevision: "pilot-17",
    generatorRevision: "generator-17",
    itemCount: profile.count,
    introductionRequired: true,
    introductionOpened: false,
  });
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeFocused();
  return state;
}

async function beginProfile(page: Page) {
  await page.getByRole("button", { name: "Session introduction", exact: true }).click();
  await expect(page.getByRole("region", { name: "Session introduction", exact: true })).toBeVisible();
  const state = await stateFor(page);
  expect(state.introductionOpened).toBe(true);
  return state;
}

function answerFor(exercise: P1Exercise) {
  const answer = TEXT_ANSWERS[exercise.prompt.cue ?? ""];
  expect(answer, `A literal packet answer exists for ${exercise.formatId} ${exercise.prompt.cue ?? ""}`).toBeDefined();
  return answer!;
}

function pairsFor(focus: string, exercise: P1Exercise) {
  const columns = exercise.prompt.matchingColumns;
  const oracle = MATCHES[focus];
  expect(columns, "The matching slot supplies two independently ordered columns").toBeDefined();
  expect(oracle, `A literal packet matching oracle exists for ${focus}`).toBeDefined();
  expect(new Set(columns!.left.map(({ text }) => text))).toEqual(new Set(Object.keys(oracle!)));
  expect(new Set(columns!.right.map(({ text }) => text))).toEqual(new Set(Object.values(oracle!)));
  return columns!.left.map((left) => ({
    leftId: left.id,
    rightId: columns!.right.find((right) => right.text === oracle![left.text])!.id,
  }));
}

async function choosePairs(page: Page, exercise: P1Exercise, pairs: MatchPair[]) {
  const columns = exercise.prompt.matchingColumns!;
  for (const pair of pairs) {
    const left = columns.left.find(({ id }) => id === pair.leftId)!;
    const right = columns.right.find(({ id }) => id === pair.rightId)!;
    const english = page.getByRole("group", { name: "English words", exact: true })
      .getByRole("button", { name: left.text, exact: true });
    await english.focus();
    await page.keyboard.press("Enter");
    const danish = page.getByRole("group", { name: "Danish words", exact: true })
      .getByRole("button", { name: right.text, exact: true });
    await danish.focus();
    await page.keyboard.press("Space");
  }
}

async function chooseTiles(page: Page, exercise: P1Exercise, words: readonly string[]) {
  const available = page.getByLabel("Available Danish word tiles", { exact: true });
  const remaining = [...exercise.prompt.blocks];
  for (const word of words) {
    const occurrence = remaining.find(({ text }) => text.toLocaleLowerCase("da-DK") === word.toLocaleLowerCase("da-DK"));
    expect(occurrence, `The supplied bank contains the literal ${word} occurrence`).toBeDefined();
    remaining.splice(remaining.indexOf(occurrence!), 1);
    await available.getByRole("button", { name: occurrence!.text, exact: true }).press("Enter");
  }
}

async function checkAnswer(page: Page, matching = false) {
  await page.getByRole("button", { name: matching ? "Check matches" : "Check answer", exact: true }).click();
  await expect(page.getByRole("status")).toBeFocused();
}

async function answerExercise(page: Page, exercise: P1Exercise, focus: string) {
  if (exercise.formatId === "E033") {
    await choosePairs(page, exercise, pairsFor(focus, exercise));
    await checkAnswer(page, true);
  } else if (["E037", "E038"].includes(exercise.formatId)) {
    await chooseTiles(page, exercise, TILE_ORDERS[exercise.prompt.cue ?? ""]!);
    await checkAnswer(page);
  } else {
    const literal = answerFor(exercise);
    const answer = ["E034", "E035", "E036"].includes(exercise.formatId)
      ? literal.toLocaleUpperCase("da-DK") : literal;
    if (exercise.prompt.choices.length) {
      await page.getByRole("group", { name: "Answer choices", exact: true })
        .getByRole("button", { name: answer, exact: true }).click();
    } else await page.getByRole("textbox", { name: "Danish answer", exact: true }).fill(answer);
    await checkAnswer(page);
  }
}

async function submitCurrent(page: Page, state: P1State) {
  await answerExercise(page, state.currentExercise!, state.focusId);
}

async function continueFrom(page: Page, previous: P1State) {
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  if (previous.cursor + 1 === previous.itemCount) {
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeFocused();
  } else {
    await expect(page.getByText(`Part ${previous.cursor + 2} of ${previous.itemCount}`, { exact: true })).toBeVisible();
  }
  return await stateFor(page);
}

function latest(state: P1State) {
  const grade = state.items[state.cursor]!.lastAttempt;
  expect(grade, "The current P1 item has a saved latest attempt").toBeDefined();
  return grade!;
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
}

async function assertTeachingGate(page: Page, profile: typeof PROFILES[number], state: P1State) {
  const exercise = state.currentExercise!;
  const response = await page.request.post("/api/v1/attempts", {
    headers: { "X-Astrid-Client": "playground" },
    data: {
      attemptId: crypto.randomUUID(), exerciseId: exercise.id,
      answer: exercise.formatId === "E033" ? { kind: "matching", pairs: pairsFor(profile.focus, exercise) }
        : { kind: "text", text: exercise.prompt.choices[0] ?? "unfamiliar" },
      hintUsed: false,
    },
  });
  expect(response.status()).toBe(409);
  expect((await stateFor(page)).summary.attemptCount).toBe(0);
}

async function retryWrongReadingFact(page: Page, exercise: P1Exercise) {
  if (!exercise.prompt.passage) return;
  await expect(page.getByRole("group", { name: "Reading passage", exact: true })).toHaveAttribute("lang", "da");
  const correct = answerFor(exercise);
  const wrong = exercise.prompt.choices.find((choice) => choice !== correct)!;
  await page.getByRole("group", { name: "Answer choices", exact: true })
    .getByRole("button", { name: wrong, exact: true }).click();
  await checkAnswer(page);
  expect(latest(await stateFor(page))).toMatchObject({
    targetSatisfaction: "not-met", meaningSatisfaction: "not-met", linguisticStatus: "acceptable",
  });
  await page.getByRole("button", { name: "Try again", exact: true }).click();
}

async function retryMatchingStates(page: Page, focus: string, exercise: P1Exercise) {
  const pairs = pairsFor(focus, exercise);
  await choosePairs(page, exercise, pairs.slice(0, 1));
  await checkAnswer(page, true);
  let saved = await stateFor(page);
  expect(latest(saved)).toMatchObject({ targetSatisfaction: "unevaluable", responseConstraintStatus: "not-met" });
  expect(latest(saved).matchingResults?.filter(({ targetSatisfaction }) => targetSatisfaction === "met")).toHaveLength(1);
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await expect(page.getByRole("group", { name: "Match English and Danish words", exact: true })).toBeFocused();
  const remaining = pairs.slice(1);
  const swapped = [
    { leftId: remaining[0]!.leftId, rightId: remaining[1]!.rightId },
    { leftId: remaining[1]!.leftId, rightId: remaining[0]!.rightId },
    ...remaining.slice(2),
  ];
  await choosePairs(page, exercise, swapped);
  await checkAnswer(page, true);
  saved = await stateFor(page);
  expect(latest(saved)).toMatchObject({ targetSatisfaction: "not-met", responseConstraintStatus: "met" });
  expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  for (const left of exercise.prompt.matchingColumns!.left) {
    await page.getByRole("button", { name: `Remove match for ${left.text}`, exact: true }).click();
  }
}

async function clearChosenTiles(page: Page) {
  const chosen = page.getByLabel("Your Danish sentence", { exact: true }).getByRole("button");
  while (await chosen.count()) await chosen.first().click();
}

async function retryFrontedOrder(page: Page, exercise: P1Exercise, order: string[]) {
  await chooseTiles(page, exercise, order);
  await checkAnswer(page);
  expect(latest(await stateFor(page))).toMatchObject({
    targetSatisfaction: "not-met", meaningSatisfaction: "met", linguisticStatus: "acceptable",
  });
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await clearChosenTiles(page);
}

async function retrySpareInfinitive(page: Page, exercise: P1Exercise) {
  await chooseTiles(page, exercise, ["Nora", "drikke", "vand"]);
  await checkAnswer(page);
  expect(latest(await stateFor(page))).toMatchObject({
    targetSatisfaction: "not-met", meaningSatisfaction: "met", linguisticStatus: "known-issue",
    issues: [{ code: "INFINITIVE_USED" }],
  });
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await clearChosenTiles(page);
}

async function retryTypedInfinitive(page: Page, exercise: P1Exercise) {
  const infinitive = exercise.prompt.cue?.includes("studere") ? "studere" : "drikke";
  await page.getByRole("textbox", { name: "Danish answer", exact: true }).fill(infinitive);
  await checkAnswer(page);
  expect(latest(await stateFor(page))).toMatchObject({
    targetSatisfaction: "not-met", meaningSatisfaction: "met", linguisticStatus: "acceptable",
    issues: [{ code: "INFINITIVE_USED" }],
  });
  await page.getByRole("button", { name: "Try again", exact: true }).click();
}

async function exerciseReviewedWrongState(page: Page, focus: string, exercise: P1Exercise) {
  await retryWrongReadingFact(page, exercise);
  if (exercise.formatId === "E033") await retryMatchingStates(page, focus, exercise);
  else if (FRONTED_ORDERS[exercise.prompt.cue ?? ""]) {
    await retryFrontedOrder(page, exercise, FRONTED_ORDERS[exercise.prompt.cue ?? ""]!);
  } else if (exercise.formatId === "E038") await retrySpareInfinitive(page, exercise);
  else if (exercise.formatId === "E034") await retryTypedInfinitive(page, exercise);
}

for (const profile of PROFILES) {
  test(`P1 teaches direct entry and exercises every authored slot for ${profile.focus}`, async ({ page }) => {
    let state = await openProfile(page, profile);
    await assertTeachingGate(page, profile, state);
    state = await beginProfile(page);
    const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
    for (const form of TEACHING[profile.focus]) {
      await expect(introduction.getByText(form, { exact: true }).first()).toHaveAttribute("lang", "da");
    }
    const seen: string[] = [];
    while (state.state !== "completed") {
      const exercise = state.currentExercise!;
      seen.push(`${exercise.formatId}:${exercise.formatId === "E033" ? "matching" : exercise.prompt.cue}`);
      expect(exercise.formatId).toMatch(/^E03[1-9]$/);

      await exerciseReviewedWrongState(page, profile.focus, exercise);
      await submitCurrent(page, await stateFor(page));
      expect(latest(await stateFor(page))).toMatchObject({
        targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met",
      });
      state = await continueFrom(page, await stateFor(page));
    }

    expect(seen).toEqual(EXPECTED_SLOTS[profile.focus]);
    expect(new Set(seen).size).toBe(profile.count);
    expect(state.summary).toMatchObject({ attemptedItems: profile.count, completedItems: profile.count });
    expect(state.summary.rows).toHaveLength(profile.summaryRows);
    await expect(page.getByText(/^(Meaning (selection|recognition)|Bilingual matching)$/, { exact: true }).first()).toBeVisible();
    await page.reload();
    expect((await stateFor(page)).summary).toEqual(state.summary);

    const terminal = profile.focus === "vocabulary-p1-s5" ? "Repeat this session" : `Next: ${PROFILES[PROFILES.indexOf(profile) + 1]!.title}`;
    await page.getByRole("button", { name: terminal, exact: true }).click();
    const selected = profile.focus === "vocabulary-p1-s5" ? profile.title : PROFILES[PROFILES.indexOf(profile) + 1]!.title;
    await expect(page.getByRole("radio", { name: selected, exact: true })).toBeChecked();
    await page.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  });
}

test("P1 latest unknown blocks continuation after an eligible typed answer and preserves both saved results", async ({ page }) => {
  await openProfile(page, PROFILES[3]);
  let state = await beginProfile(page);
  while (state.currentExercise?.prompt.cue !== "Type the introduced present form of at drikke.") {
    await submitCurrent(page, state);
    state = await continueFrom(page, await stateFor(page));
  }
  await submitCurrent(page, state);
  const correct = latest(await stateFor(page));
  expect(correct.targetSatisfaction).toBe("met");
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await page.getByRole("textbox", { name: "Danish answer", exact: true }).fill("svømmer");
  await checkAnswer(page);
  const unknownState = await stateFor(page);
  const unknown = latest(unknownState);
  expect(unknown).toMatchObject({ targetSatisfaction: "not-met", linguisticStatus: "unknown" });
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  const savedUnknown = await (await page.request.get(`/api/v1/attempts/${unknown.attemptId}`)).text();
  await page.reload();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  expect(await (await page.request.get(`/api/v1/attempts/${unknown.attemptId}`)).text()).toBe(savedUnknown);
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await page.getByRole("textbox", { name: "Danish answer", exact: true }).fill("drikke");
  await checkAnswer(page);
  expect(latest(await stateFor(page))).toMatchObject({ targetSatisfaction: "not-met", linguisticStatus: "acceptable" });
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeEnabled();
});

test("P1 matching replays the original serialized board after a committed response is lost and refreshes", async ({ page }) => {
  await openProfile(page, PROFILES[1]);
  const state = await beginProfile(page);
  const exercise = state.currentExercise!;
  expect(exercise.formatId).toBe("E033");
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) {
      await route.fetch();
      await route.abort("connectionfailed");
    } else await route.continue();
  });
  await choosePairs(page, exercise, pairsFor(state.focusId, exercise).reverse());
  await page.getByRole("button", { name: "Check matches", exact: true }).click();
  await expect(page.getByRole("alert", { name: "Your answer is not confirmed", exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("status")).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  const body = JSON.parse(bodies[0]!) as { attemptId: string; answer: { kind: string; pairs: MatchPair[] } };
  expect(body.answer).toEqual({ kind: "matching", pairs: pairsFor(state.focusId, exercise).reverse() });
  expect(latest(await stateFor(page)).attemptId).toBe(body.attemptId);
  expect((await stateFor(page)).items[0]!.attemptCount).toBe(1);
});

for (const theme of ["light", "dark"] as const) {
  test(`P1 teaching matching feedback tiles typed input and summary reflow at 320 and 200 percent in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 844 });
    await openProfile(page, PROFILES[2]);
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    let state = await beginProfile(page);
    await scan(page);
    while (state.state !== "completed") {
      const exercise = state.currentExercise!;
      if (exercise.formatId === "E033") {
        await choosePairs(page, exercise, pairsFor(state.focusId, exercise).slice(0, 1));
        await checkAnswer(page, true);
        await scan(page);
        await page.getByRole("button", { name: "Try again", exact: true }).click();
        for (const left of exercise.prompt.matchingColumns!.left.slice(0, 1)) {
          await page.getByRole("button", { name: `Remove match for ${left.text}`, exact: true }).click();
        }
      }
      await submitCurrent(page, await stateFor(page));
      await scan(page);
      state = await continueFrom(page, await stateFor(page));
    }
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    await scan(page);
  });
}

const OPERATIONS = [
  { id: "E031", title: "Recognize an introduced meaning" },
  { id: "E032", title: "Choose an introduced Danish form" },
  { id: "E033", title: "Match introduced meanings and forms" },
  { id: "E034", title: "Recall an introduced present form" },
  { id: "E035", title: "Recall an introduced fixed expression" },
  { id: "E036", title: "Recall an introduced mass noun" },
  { id: "E037", title: "Build a reviewed sentence" },
  { id: "E038", title: "Choose the present form in a sentence" },
  { id: "E039", title: "Read explicit facts" },
] as const;

for (const operation of OPERATIONS) {
  test(`P1 standalone playground submits the advertised operation ${operation.id}`, async ({ page }) => {
    await page.goto("/playground");
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
    await openGenerationControls(page);
    await page.getByRole("textbox", { name: "Next generation seed", exact: true }).fill("47");
    await page.getByRole("textbox", { name: "Next generation index", exact: true }).fill("5");
    const generated = page.waitForResponse((response) => response.request().method() === "POST" &&
      new URL(response.url()).pathname === "/api/v1/exercises" && response.status() === 201);
    await page.getByRole("combobox", { name: "Exercise type", exact: true }).click();
    await page.getByRole("option", { name: operation.title, exact: true }).click();
    const exercise = await (await generated).json() as P1Exercise;
    expect(exercise.formatId).toBe(operation.id);
    const focus = Object.keys(MATCHES).find((candidate) => exercise.prompt.matchingColumns?.left
      .every(({ text }) => text in MATCHES[candidate]!)) ?? "";
    const submitted = page.waitForResponse((response) => response.request().method() === "POST" &&
      new URL(response.url()).pathname === "/api/v1/attempts" && response.status() === 200);
    await answerExercise(page, exercise, focus);
    const response = await submitted;
    const result = await response.json() as SavedGrade;
    expect(result).toMatchObject({ targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met" });
    const body = response.request().postDataJSON() as { answer: { kind: string } };
    const kind = operation.id === "E033" ? "matching" : ["E037", "E038"].includes(operation.id) ? "order" : "text";
    expect(body.answer.kind).toBe(kind);
    await expect(page.getByRole("button", { name: "Try again", exact: true })).toBeVisible();
  });
}
