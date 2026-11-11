import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { choosePractice, ownerHeaders, readPractice, type PracticeState } from "../support/practice.js";

type CourseExercise = {
  id: string; formatId: string; targetScopeId?: string; context: string;
  lexicalTarget?: { entryId: string; senseId: string; facet: string; direction: string };
  prompt: {
    before?: string; after?: string; cue?: string; cueLanguage?: string; answerLanguage?: string;
    choices: string[]; blocks?: { id: string; text: string }[]; tileMode?: string;
  };
};
type CourseItem = PracticeState["items"][number] & { canAdvance?: boolean; targetScopeId?: string; assessmentMode?: string };
type CourseState = Omit<PracticeState, "items" | "currentExercise"> & {
  focusId: string; planRevision: string; contentRevision: string; generatorRevision: string;
  introductionOpened: boolean; introductionRequired: boolean;
  currentExercise: CourseExercise | null; items: CourseItem[];
};
const PROFILES = [
  { focus: "module-one-singular", title: "Talking about one person", scope: "human-singular", badge: "One-person subject words", selection: 4, recall: 2, itemCount: 6, pair: 8 },
  { focus: "module-one-groups", title: "Talking to and about groups", scope: "human-group", badge: "Group subject words", selection: 3, recall: 3, itemCount: 6, pair: 8 },
  { focus: "module-one-er", title: "Using the verb to be", scope: "vaere-present", badge: "Present verb form", selection: 2, recall: 4, itemCount: 6, pair: 8 },
  { focus: "module-one-things", title: "Referring to one thing", scope: "inanimate-singular-subject", badge: "One-thing reference words", selection: 2, recall: 2, itemCount: 4, pair: 9 },
] as const;
const SUBJECTS: Record<string, string> = {
  I: "jeg", "you (one person)": "du", he: "han", she: "hun", we: "vi",
  "you (several people)": "I", "they (several people)": "de",
};
const PRESENT_FORMS: Record<string, string> = {
  "Jeg |.": "arbejder", "Hun | en avis.": "læser", "Vi |.": "sover",
  "De |.": "arbejder", "I | en bog.": "læser", "Han |.": "sover",
};
const POSSESSION_FORMS: Record<string, string> = {
  "Han | et brev.": "har", "Vi | en avis.": "har", "Du | en bog.": "har", "De | et brev.": "har",
};
const READING_WORDS: Record<string, { answer: string; entryId: string; senseId: string; label: string }> = {
  "magazine (printed publication)": { answer: "et magasin", entryId: "L024", senseId: "L024-S01", label: "Magazine (printed publication)" },
  book: { answer: "en bog", entryId: "L014", senseId: "L014-S01", label: "Book" },
  newspaper: { answer: "en avis", entryId: "L015", senseId: "L015-S01", label: "Newspaper" },
  "letter (written message)": { answer: "et brev", entryId: "L018", senseId: "L018-S01", label: "Letter (written message)" },
};
const PERSONAL_FORMS: Record<string, { answer: string; proposition: string }> = {
  "Jeg | Anna.": { answer: "hedder", proposition: "I am called Anna." },
  "Du | Nora.": { answer: "hedder", proposition: "You are called Nora." },
  "Hun | i Aarhus.": { answer: "bor", proposition: "She lives in Aarhus." },
  "De | i Odense.": { answer: "bor", proposition: "They live in Odense." },
  "Vi | dansk.": { answer: "taler", proposition: "We speak Danish." },
  "I | dansk.": { answer: "taler", proposition: "You speak Danish." },
};
const ABILITY_FORMS: Record<string, string> = {
  "Hun kan | et brev.": "læse", "Vi kan | dansk.": "læse",
  "Du kan | et brev.": "læse", "De kan | dansk.": "læse",
};
const MODAL_CHOICES = [
  "A personal wish or intention.", "An external requirement.", "Permission to do it.",
] as const;
const MODAL_ROUNDS = [
  {
    focus: "module-one-volition", title: "Expressing an intention", scope: "modal-volition-meaning",
    statement: "Hun vil læse et brev.", form: "at ville → vil", example: "Jeg vil læse en bog.",
    correct: MODAL_CHOICES[0], wrong: MODAL_CHOICES[1], order: ["vi", "vil", "læse", "en", "avis"],
    next: "Understanding a requirement", orderVariant: "reversed-noun-phrase",
  },
  {
    focus: "module-one-obligation", title: "Understanding a requirement", scope: "modal-obligation-meaning",
    statement: "De skal læse en bog.", form: "at skulle → skal", example: "Du skal læse et brev.",
    correct: MODAL_CHOICES[1], wrong: MODAL_CHOICES[2], order: ["jeg", "skal", "læse", "en", "avis"],
    next: "Understanding permission", orderVariant: "missing-object",
  },
  {
    focus: "module-one-permission", title: "Understanding permission", scope: "modal-permission-meaning",
    statement: "Du må læse et brev.", form: "at måtte → må", example: "Han må læse en avis.",
    correct: MODAL_CHOICES[2], wrong: MODAL_CHOICES[0], order: ["hun", "må", "læse", "en", "bog"],
    next: "Adding when and where", orderVariant: "canonical",
  },
] as const;
const CIRCUMSTANCE_ORDERS = {
  "Jeg arbejder.": { context: "Supplied clause: I work. Supplied circumstance: today.", canonical: ["jeg", "arbejder", "i dag"] },
  "Hun læser et brev.": { context: "Supplied clause: She reads a letter. Supplied circumstance: now.", canonical: ["hun", "læser", "et brev", "nu"] },
  "Vi arbejder.": { context: "Supplied clause: We work. Supplied circumstance: in Aarhus.", canonical: ["vi", "arbejder", "i Aarhus"] },
  "De læser en avis.": { context: "Supplied clause: They read a newspaper. Supplied circumstance: today.", canonical: ["de", "læser", "en avis", "i dag"] },
} as const;
const SUBJECT_FIRST_ORDERS: Record<string, string[]> = {
  "We work.": ["vi", "arbejder"],
  "You sleep.": ["du", "sover"],
  "He reads a book.": ["han", "læser", "en bog"],
  "I read a newspaper.": ["jeg", "læser", "en", "avis"],
  "You read a letter.": ["I", "læser", "et", "brev"],
  "They read a book.": ["de", "læser", "en", "bog"],
};
async function stateFor(page: Page) {
  return await readPractice(page.request, new URL(page.url()).searchParams.get("session")!) as unknown as CourseState;
}
function answerFor(exercise: CourseExercise) {
  if (exercise.formatId === "E011") {
    const target = READING_WORDS[exercise.prompt.cue ?? ""];
    expect(target, "The literal authored English meaning identifies one Danish bundle").toBeDefined();
    return target!.answer;
  }
  if (exercise.targetScopeId === "personal-introduction-present") {
    const target = PERSONAL_FORMS[`${exercise.prompt.before ?? ""}|${exercise.prompt.after ?? ""}`];
    expect(target, "The literal supplied personal statement identifies one present form").toBeDefined();
    return target!.answer;
  }
  if (exercise.targetScopeId === "kan-bare-infinitive") {
    const answer = ABILITY_FORMS[`${exercise.prompt.before ?? ""}|${exercise.prompt.after ?? ""}`];
    expect(answer, "The literal supplied ability frame identifies the bare infinitive").toBeDefined();
    return answer!;
  }
  if (exercise.targetScopeId === "have-possession-present") {
    const answer = POSSESSION_FORMS[`${exercise.prompt.before ?? ""}|${exercise.prompt.after ?? ""}`];
    expect(answer, "The literal supplied possession frame identifies har").toBeDefined();
    return answer!;
  }
  if (exercise.targetScopeId === "named-three-verbs-present") {
    const answer = PRESENT_FORMS[`${exercise.prompt.before ?? ""}|${exercise.prompt.after ?? ""}`];
    expect(answer, "The literal supplied sentence frame identifies the named present form").toBeDefined();
    return answer!;
  }
  if (exercise.targetScopeId === "vaere-present") return "er";
  if (exercise.targetScopeId === "inanimate-singular-subject") {
    if (exercise.prompt.cue === "en bog") return "den";
    if (exercise.prompt.cue === "et brev") return "det";
    expect(exercise.prompt.cue, "The literal Danish noun bundle identifies the supplied reference oracle").toMatch(/^(en bog|et brev)$/);
    throw new Error(`Unsupported supplied noun bundle: ${exercise.prompt.cue ?? "missing"}`);
  }
  const answer = SUBJECTS[exercise.prompt.cue!];
  expect(answer, "The literal authored English cue has an independent Danish-form oracle").toBeDefined();
  return answer!;
}

async function submitOrder(page: Page, exercise: CourseExercise, properSubset = false) {
  const expected = SUBJECT_FIRST_ORDERS[exercise.prompt.cue ?? ""];
  expect(expected, "The English proposition identifies one literal Danish order").toBeDefined();
  const selected = properSubset ? expected!.slice(0, 1) : expected!;
  for (const text of selected) {
    await page.getByRole("group", { name: /Available/ }).getByRole("button", { name: text, exact: true }).press("Enter");
  }
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("status")).toBeFocused();
}
async function submitModalOrder(page: Page, exercise: CourseExercise, words: readonly string[]) {
  const chosen = page.getByLabel("Your Danish sentence", { exact: true }).getByRole("button");
  while (await chosen.count()) await chosen.first().click();
  const available = page.getByRole("group", { name: /Available/ });
  for (const word of words) {
    await available.getByRole("button", { name: word, exact: true }).press("Enter");
  }
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("status")).toBeFocused();
  expect(exercise.formatId).toBe("E025");
}
async function chooseCircumstanceOrder(page: Page, exercise: CourseExercise, parts: readonly string[]) {
  expect(exercise.formatId).toBe("E029");
  const available = page.getByRole("group", { name: "Available sentence parts", exact: true });
  for (const part of parts) await available.getByRole("button", { name: part, exact: true }).press("Enter");
}
async function submitCircumstanceOrder(page: Page, exercise: CourseExercise, parts: readonly string[]) {
  await chooseCircumstanceOrder(page, exercise, parts);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("status")).toBeFocused();
}
async function clearChosenOrder(page: Page) {
  const chosen = page.getByLabel("Your sentence", { exact: true }).getByRole("button");
  while (await chosen.count()) await chosen.first().click();
}
async function expectChosenOrder(page: Page, parts: readonly string[]) {
  for (const [position, part] of parts.entries()) {
    await expect(page.getByRole("button", {
      name: `Remove ${part} from position ${position + 1}`, exact: true,
    })).toBeVisible();
  }
}
async function openCourse(page: Page, title: string, enlarged = false) {
  await page.goto("/practice");
  await expect(page.getByRole("heading", { name: "Module 1: First steps", exact: true })).toBeVisible();
  if (enlarged) await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  await choosePractice(page, title);
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeFocused();
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeHidden();
  return await stateFor(page);
}
async function beginCourse(page: Page) {
  await page.getByRole("button", { name: "Session introduction", exact: true }).click();
  await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
  const state = await stateFor(page);
  expect(state.introductionOpened).toBe(true);
  return state;
}
async function submit(page: Page, exercise: CourseExercise, text = answerFor(exercise)) {
  if (exercise.prompt.choices.length) await page.getByLabel("Answer choices").getByRole("button", { name: text, exact: true }).click();
  else await page.getByRole("textbox").fill(text);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("status")).toBeFocused();
}
async function next(page: Page, previous: CourseState) {
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  if (previous.cursor + 1 === previous.itemCount) await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeFocused();
  else await expect(page.getByText(`Part ${previous.cursor + 2} of ${previous.itemCount}`, { exact: true })).toBeVisible();
  return await stateFor(page);
}
async function finishByApi(page: Page, state: CourseState) {
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    const saved = await page.request.post("/api/v1/attempts", {
      headers: { "X-Astrid-Client": "playground" },
      data: { attemptId: crypto.randomUUID(), exerciseId: exercise.id, answer: { kind: "text", text: answerFor(exercise) }, hintUsed: false },
    });
    expect(saved.status()).toBe(200);
    const result = await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, {
      headers: ownerHeaders(state.ownerSessionId), data: { position: state.cursor },
    });
    expect(result.status()).toBe(200);
    state = await result.json() as CourseState;
  }
  return state;
}
async function reachRecall(page: Page, wanted?: string) {
  let state = await stateFor(page);
  while (state.currentExercise!.prompt.choices.length || (wanted !== undefined && answerFor(state.currentExercise!) !== wanted)) {
    await submit(page, state.currentExercise!);
    state = await next(page, await stateFor(page));
    expect(state.state).toBe("active");
  }
  return state;
}
type SavedGrade = {
  attemptId: string; targetSatisfaction: string; linguisticStatus: string;
  meaningSatisfaction?: string; responseConstraintStatus: string;
  completedSentence: string | null; issues: { code: string }[];
};
function latest(state: CourseState) {
  return state.items[state.cursor]!.lastAttempt as SavedGrade;
}
async function scan(page: Page) {
  const result = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  expect(result.violations.map(({ id, nodes }) => ({ id, targets: nodes.map((node) => node.target) }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  const overflow = await page.locator("#main-content p, #main-content h2, #main-content h3, #main-content h4").evaluateAll((elements) => elements.flatMap((element) => {
    const bounds = element.getBoundingClientRect();
    if (!bounds.width || !bounds.height) return [];
    const range = document.createRange(); range.selectNodeContents(element);
    return [...range.getClientRects()].some((rect) => rect.width > 0 &&
      (rect.left < bounds.left - 1 || rect.right > bounds.right + 1)) ? [element.textContent] : [];
  }));
  expect(overflow, "Teaching and summary text stays inside its own column").toEqual([]);
}

test("named present forms teach three verbs and retain selection and recall", async ({ page }) => {
  await openCourse(page, "Using present verb forms");
  let state = await beginCourse(page);
  const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
  for (const form of ["arbejde", "arbejder", "læse", "læser", "sove", "sover"]) {
    await expect(introduction.getByText(new RegExp(`\\b${form}\\b`)).first()).toHaveAttribute("lang", "da");
  }
  await scan(page);
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    expect(exercise.targetScopeId).toBe("named-three-verbs-present");
    await submit(page, exercise);
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
    state = await next(page, await stateFor(page));
  }
  expect(state.summary.rows).toEqual(expect.arrayContaining([
    expect.objectContaining({ targetScopeId: "named-three-verbs-present", assessmentMode: "selection", plannedItems: 3 }),
    expect.objectContaining({ targetScopeId: "named-three-verbs-present", assessmentMode: "recall", plannedItems: 3 }),
  ]));
  await page.getByRole("button", { name: "Next: Building subject-first statements", exact: true }).click();
  await expect(page.getByRole("radio", { name: "Building subject-first statements", exact: true })).toBeChecked();
  expect(new URL(page.url()).searchParams.get("session")).toBe(state.id);
});

test("subject-first assembly checks constituents and partial word tiles with saved order recovery", async ({ page }) => {
  await openCourse(page, "Building subject-first statements");
  let state = await beginCourse(page);
  await scan(page);
  let partialChecked = false;
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    expect(exercise.targetScopeId).toBe("subject-first-supplied");
    const partial = exercise.formatId === "E025" && !partialChecked;
    if (partial) {
      partialChecked = true;
      await submitOrder(page, exercise, true);
      const saved = await stateFor(page);
      const firstExpected = SUBJECT_FIRST_ORDERS[exercise.prompt.cue!]![0]!;
      const firstBlock = exercise.prompt.blocks!.find((block) => block.text === firstExpected);
      expect(firstBlock).toBeDefined();
      expect(saved.items[saved.cursor]!.lastAnswer).toMatchObject({ kind: "order", blockIds: [firstBlock!.id] });
      expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
      await expect(page.getByRole("status").getByText("Response constraint to revisit", { exact: true })).toBeVisible();
      await page.reload();
      await expect(page.getByRole("status")).toBeVisible();
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await page.getByRole("button", { name: new RegExp(`^Remove ${firstExpected} from position 1$`) }).click();
    }
    await submitOrder(page, exercise);
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
    state = await next(page, await stateFor(page));
  }
  expect(partialChecked).toBe(true);
  await page.getByRole("button", { name: "Next: Saying what someone has", exact: true }).click();
  await expect(page.getByRole("radio", { name: "Saying what someone has", exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect(state.summary.rows).toEqual(expect.arrayContaining([
    expect.objectContaining({ targetScopeId: "subject-first-supplied", assessmentMode: "constituent-order", plannedItems: 3 }),
    expect.objectContaining({ targetScopeId: "subject-first-supplied", assessmentMode: "word-order", plannedItems: 3 }),
  ]));
  await page.setViewportSize({ width: 320, height: 720 });
  await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  await scan(page);
});

test("possession with har requires teaching and retains literal first and latest outcomes", async ({ page }) => {
  let state = await openCourse(page, "Saying what someone has");
  expect(state).toMatchObject({
    focusId: "module-one-possession", planRevision: "module-one-possession-1",
    contentRevision: "pilot-11", generatorRevision: "generator-11",
    introductionRequired: true, introductionOpened: false, itemCount: 4,
    prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  });
  expect(state.items.every((item) => item.targetScopeId === "have-possession-present")).toBe(true);
  const early = await page.request.post("/api/v1/attempts", {
    headers: { "X-Astrid-Client": "playground" },
    data: { attemptId: crypto.randomUUID(), exerciseId: state.currentExercise!.id, answer: { kind: "text", text: "har" }, hintUsed: false },
  });
  expect(early.status()).toBe(409);
  expect((await stateFor(page)).summary.attemptCount).toBe(0);

  state = await beginCourse(page);
  const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
  await expect(introduction.getByText("at have → har", { exact: true })).toHaveAttribute("lang", "da");
  await expect(introduction.getByText("Jeg har en bog.", { exact: true })).toHaveAttribute("lang", "da");
  await expect(introduction.getByText(/same present form with each subject/i)).toBeVisible();
  await expect(page.getByText("Present possession with har", { exact: true }).and(page.locator("span"))).toBeVisible();
  await scan(page);

  let checkedInfinitive = false;
  let checkedUnknown = false;
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    expect(answerFor(exercise)).toBe("har");
    expect(exercise.prompt.answerLanguage).toBe("da");
    const sentence = page.getByRole("group", { name: exercise.prompt.choices.length ? "Danish present verb choices" : "Danish sentence cloze" });
    await expect(sentence).toBeVisible();
    await expect(page.getByText(exercise.prompt.cue!, { exact: true })).toHaveAttribute("lang", "da");
    if (exercise.prompt.choices.length) {
      await expect(sentence).toHaveAttribute("lang", "da");
      for (const choice of exercise.prompt.choices) {
        await expect(sentence.getByText(choice, { exact: true })).toHaveAttribute("lang", "da");
      }
    } else {
      await expect(page.getByRole("textbox")).toHaveAttribute("lang", "da");
    }

    if (exercise.prompt.choices.length && !checkedInfinitive) {
      checkedInfinitive = true;
      await submit(page, exercise, "have");
      const saved = await stateFor(page);
      expect(latest(saved)).toMatchObject({ targetSatisfaction: "not-met", linguisticStatus: "known-issue" });
      expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await submit(page, exercise, "har");
    } else if (!exercise.prompt.choices.length && !checkedUnknown) {
      checkedUnknown = true;
      await submit(page, exercise, "havde");
      const unknown = await stateFor(page);
      expect(latest(unknown)).toMatchObject({ targetSatisfaction: "unevaluable", linguisticStatus: "unknown" });
      expect(unknown.items[unknown.cursor]!.canAdvance).toBe(false);
      await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
      const savedAttempt = latest(unknown).attemptId;
      await page.reload();
      await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
      expect(latest(await stateFor(page)).attemptId).toBe(savedAttempt);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await expect(page.getByRole("textbox")).toHaveValue("");
      await submit(page, exercise, "har");
    } else {
      await submit(page, exercise);
    }
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
    state = await next(page, await stateFor(page));
  }

  expect(checkedInfinitive).toBe(true);
  expect(checkedUnknown).toBe(true);
  expect(state.summary).toMatchObject({ completedItems: 4, attemptedItems: 4, attemptCount: 6, retryCount: 2 });
  expect(state.summary.rows).toEqual(expect.arrayContaining([
    expect.objectContaining({
      targetScopeId: "have-possession-present", assessmentMode: "selection", plannedItems: 2,
      firstOutcomes: expect.objectContaining({ fullyMet: 1, targetNotMet: 1, languageKnownIssue: 1 }),
      latestOutcomes: expect.objectContaining({ fullyMet: 2 }),
    }),
    expect.objectContaining({
      targetScopeId: "have-possession-present", assessmentMode: "recall", plannedItems: 2,
      firstOutcomes: expect.objectContaining({ fullyMet: 1, unevaluable: 1, languageUnknown: 1 }),
      latestOutcomes: expect.objectContaining({ fullyMet: 2 }),
    }),
  ]));
  await page.getByRole("button", { name: "Next: Words for things you read", exact: true }).click();
  await expect(page.getByRole("radio", { name: "Words for things you read", exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect((await stateFor(page)).summary).toEqual(state.summary);
  await page.setViewportSize({ width: 320, height: 720 });
  await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  await scan(page);
});

test("reading words retain related meaning choices and per-sense results", async ({ page }) => {
  let state = await openCourse(page, "Words for things you read");
  expect(state).toMatchObject({
    focusId: "module-one-reading-words", planRevision: "module-one-reading-words-1",
    contentRevision: "pilot-5", generatorRevision: "generator-5",
    introductionRequired: true, introductionOpened: false, itemCount: 4, prerequisiteSkillIds: [],
  });
  const expectedTargets = Object.values(READING_WORDS).map(({ entryId, senseId }) => ({
    entryId, senseId, facet: "meaning", direction: "en-da",
  }));
  expect(state.items.map((item) => item.lexicalTarget)).toEqual(expectedTargets);
  for (const item of state.items) {
    expect(item).not.toHaveProperty("targetSkillId");
    expect(item).not.toHaveProperty("targetScopeId");
    expect(item).not.toHaveProperty("assessmentMode");
    expect(item).not.toHaveProperty("assessments");
  }
  const early = await page.request.post("/api/v1/attempts", {
    headers: { "X-Astrid-Client": "playground" },
    data: { attemptId: crypto.randomUUID(), exerciseId: state.currentExercise!.id, answer: { kind: "text", text: "et magasin" }, hintUsed: false },
  });
  expect(early.status()).toBe(409);
  expect((await stateFor(page)).summary.attemptCount).toBe(0);

  state = await beginCourse(page);
  const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
  for (const phrase of ["et magasin", "en bog", "en avis", "et brev"]) {
    await expect(introduction.getByText(phrase, { exact: true })).toHaveAttribute("lang", "da");
  }
  await scan(page);

  const seen: string[] = [];
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    const target = READING_WORDS[exercise.prompt.cue ?? ""]!;
    seen.push(exercise.prompt.cue!);
    expect(exercise).toMatchObject({
      formatId: "E011", lexicalTarget: { entryId: target.entryId, senseId: target.senseId, facet: "meaning", direction: "en-da" },
    });
    expect(new Set(exercise.prompt.choices)).toEqual(new Set(Object.values(READING_WORDS).map(({ answer }) => answer)));
    await expect(page.getByText(exercise.prompt.cue!, { exact: true })).toHaveAttribute("lang", "en");
    const choices = page.getByRole("group", { name: "Answer choices", exact: true });
    for (const choice of exercise.prompt.choices) {
      await expect(choices.getByText(choice, { exact: true })).toHaveAttribute("lang", "da");
    }

    if (state.cursor === 0) {
      await submit(page, exercise, "en bog");
      const wrong = await stateFor(page);
      expect(latest(wrong)).toMatchObject({ targetSatisfaction: "not-met", linguisticStatus: "acceptable" });
      expect(wrong.items[0]!.canAdvance).toBe(true);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      const correct = choices.getByRole("button", { name: target.answer, exact: true });
      await correct.focus();
      await page.keyboard.press("Enter");
      await page.getByRole("button", { name: "Check answer", exact: true }).click();
      await expect(page.getByRole("status")).toBeFocused();
    } else {
      await submit(page, exercise, target.answer);
    }
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
    state = await next(page, await stateFor(page));
  }

  expect(seen).toEqual(Object.keys(READING_WORDS));
  expect(state.summary).toMatchObject({ completedItems: 4, attemptedItems: 4, attemptCount: 5, retryCount: 1 });
  expect(state.summary.rows).toHaveLength(4);
  for (const [cue, target] of Object.entries(READING_WORDS)) {
    expect(state.summary.rows).toContainEqual(expect.objectContaining({
      label: target.label,
      lexicalTarget: { entryId: target.entryId, senseId: target.senseId, facet: "meaning", direction: "en-da" },
      plannedItems: 1, attemptedItems: 1, advancedItems: 1,
      attemptCount: cue.startsWith("magazine") ? 2 : 1,
      retryCount: cue.startsWith("magazine") ? 1 : 0,
      firstOutcomes: expect.objectContaining(cue.startsWith("magazine") ? { targetNotMet: 1 } : { fullyMet: 1 }),
      latestOutcomes: expect.objectContaining({ fullyMet: 1 }),
      firstWithSupport: 1, firstWithPriorExposure: 1,
    }));
  }
  await expect(page.getByText("Meaning selection", { exact: true })).toHaveCount(4);
  await expect(page.getByText("Written headword recall", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Next: Names, homes and languages", exact: true }).click();
  await expect(page.getByRole("radio", { name: "Names, homes and languages", exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect((await stateFor(page)).summary).toEqual(state.summary);
  await scan(page);
});

test("personal present forms alternate choice and recall across six literal statements", async ({ page }) => {
  let state = await openCourse(page, "Names, homes and languages");
  expect(state).toMatchObject({
    focusId: "module-one-personal", planRevision: "module-one-personal-1",
    contentRevision: "pilot-12", generatorRevision: "generator-12",
    introductionRequired: true, introductionOpened: false, itemCount: 6,
    prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  });
  expect(state.items.every((item) => item.targetScopeId === "personal-introduction-present")).toBe(true);
  const early = await page.request.post("/api/v1/attempts", {
    headers: { "X-Astrid-Client": "playground" },
    data: { attemptId: crypto.randomUUID(), exerciseId: state.currentExercise!.id, answer: { kind: "text", text: "hedder" }, hintUsed: false },
  });
  expect(early.status()).toBe(409);
  expect((await stateFor(page)).summary.attemptCount).toBe(0);

  state = await beginCourse(page);
  const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
  for (const danish of [
    "at hedde → hedder", "Hun hedder Nora.",
    "at bo → bor", "Jeg bor i Odense.",
    "at tale → taler", "Du taler dansk.",
  ]) {
    await expect(introduction.getByText(danish, { exact: true })).toHaveAttribute("lang", "da");
  }
  for (const english of [
    "The verb gives someone's name.",
    "The verb states where someone lives.",
    "The verb states which language someone speaks.",
  ]) {
    await expect(introduction.getByText(english, { exact: true })).toBeVisible();
  }
  await expect(page.getByText("Present forms for personal details", { exact: true }).and(page.locator("span"))).toBeVisible();
  await scan(page);

  const seen: string[] = [];
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    const key = `${exercise.prompt.before ?? ""}|${exercise.prompt.after ?? ""}`;
    const target = PERSONAL_FORMS[key]!;
    seen.push(key);
    expect(exercise.targetScopeId).toBe("personal-introduction-present");
    expect(exercise.formatId).toBe(state.cursor % 2 === 0 ? "E022" : "E023");
    expect(exercise.context).toContain(target.proposition);
    await expect(page.getByText(exercise.prompt.cue!, { exact: true })).toHaveAttribute("lang", "da");
    if (exercise.prompt.choices.length) {
      const sentence = page.getByRole("group", { name: "Danish present verb choices", exact: true });
      await expect(sentence).toHaveAttribute("lang", "da");
      await submit(page, exercise, target.answer);
    } else {
      const input = page.getByRole("textbox", { name: "Danish present verb", exact: true });
      await expect(input).toHaveAttribute("lang", "da");
      await input.fill(target.answer);
      await page.getByRole("button", { name: "Check answer", exact: true }).click();
      await expect(page.getByRole("status")).toBeFocused();
    }
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
    state = await next(page, await stateFor(page));
  }

  expect(seen).toEqual(Object.keys(PERSONAL_FORMS));
  expect(state.summary).toMatchObject({ completedItems: 6, attemptedItems: 6, attemptCount: 6, retryCount: 0 });
  expect(state.summary.rows).toEqual(expect.arrayContaining([
    expect.objectContaining({
      targetScopeId: "personal-introduction-present", assessmentMode: "selection", plannedItems: 3,
      firstOutcomes: expect.objectContaining({ fullyMet: 3 }), latestOutcomes: expect.objectContaining({ fullyMet: 3 }),
    }),
    expect.objectContaining({
      targetScopeId: "personal-introduction-present", assessmentMode: "recall", plannedItems: 3,
      firstOutcomes: expect.objectContaining({ fullyMet: 3 }), latestOutcomes: expect.objectContaining({ fullyMet: 3 }),
    }),
  ]));
  await page.getByRole("button", { name: "Next: Using a verb after kan", exact: true }).click();
  await expect(page.getByRole("radio", { name: "Using a verb after kan", exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect((await stateFor(page)).summary).toEqual(state.summary);
  await scan(page);
});

test("ability after kan separates selection and one-word response outcomes", async ({ page }) => {
  let state = await openCourse(page, "Using a verb after kan");
  expect(state).toMatchObject({
    focusId: "module-one-ability", planRevision: "module-one-ability-1",
    contentRevision: "pilot-13", generatorRevision: "generator-13",
    introductionRequired: true, introductionOpened: false, itemCount: 4,
    prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  });
  expect(state.items.every((item) => item.targetScopeId === "kan-bare-infinitive")).toBe(true);
  const early = await page.request.post("/api/v1/attempts", {
    headers: { "X-Astrid-Client": "playground" },
    data: {
      attemptId: crypto.randomUUID(), exerciseId: state.currentExercise!.id,
      answer: { kind: "text", text: "læse" }, hintUsed: false,
    },
  });
  expect(early.status()).toBe(409);
  expect((await stateFor(page)).summary.attemptCount).toBe(0);

  state = await beginCourse(page);
  const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
  for (const danish of ["at kunne → kan", "at læse → læse efter kan", "Jeg kan læse et brev.", "Han kan læse dansk."]) {
    await expect(introduction.getByText(danish, { exact: true })).toHaveAttribute("lang", "da");
  }
  await expect(introduction.getByText(/no at/i)).toBeVisible();
  await expect(page.getByText("Verb form after kan", { exact: true }).and(page.locator("span"))).toBeVisible();
  await scan(page);

  const seen: string[] = [];
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    const key = `${exercise.prompt.before ?? ""}|${exercise.prompt.after ?? ""}`;
    seen.push(key);
    expect(ABILITY_FORMS[key]).toBe("læse");
    expect(exercise.targetScopeId).toBe("kan-bare-infinitive");
    expect(exercise.formatId).toBe(state.cursor < 2 ? "E026" : "E027");
    await expect(page.getByText("at læse", { exact: true }).first()).toHaveAttribute("lang", "da");
    if (state.cursor === 0) {
      expect(new Set(exercise.prompt.choices)).toEqual(new Set(["læse", "læser", "at læse"]));
      await submit(page, exercise, "at læse");
      const saved = await stateFor(page);
      expect(latest(saved)).toMatchObject({
        targetSatisfaction: "not-met", linguisticStatus: "known-issue", responseConstraintStatus: "met",
        issues: [{ code: "INFINITIVE_MARKER_AFTER_MODAL" }],
      });
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await submit(page, exercise, "læse");
    } else if (state.cursor === 2) {
      await submit(page, exercise, "at læse");
      const saved = await stateFor(page);
      expect(latest(saved)).toMatchObject({
        targetSatisfaction: "not-met", linguisticStatus: "known-issue", responseConstraintStatus: "not-met",
        issues: [{ code: "INFINITIVE_MARKER_AFTER_MODAL" }],
      });
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await submit(page, exercise, "læse");
    } else if (state.cursor === 3) {
      await submit(page, exercise, "læste");
      const unknown = await stateFor(page);
      expect(latest(unknown)).toMatchObject({
        targetSatisfaction: "unevaluable", linguisticStatus: "unknown", completedSentence: "",
        issues: [{ code: "UNRECOGNIZED_BARE_INFINITIVE" }],
      });
      await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
      const attemptId = latest(unknown).attemptId;
      await page.reload();
      expect(latest(await stateFor(page)).attemptId).toBe(attemptId);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await submit(page, exercise, "Læse");
      expect(latest(await stateFor(page))).toMatchObject({
        targetSatisfaction: "met", linguisticStatus: "known-issue", responseConstraintStatus: "not-met",
        issues: [{ code: "MID_SENTENCE_VERB_CASE_REQUIRED" }],
      });
    } else {
      await submit(page, exercise);
    }
    state = await next(page, await stateFor(page));
  }

  expect(seen).toEqual(Object.keys(ABILITY_FORMS));
  expect(state.summary).toMatchObject({ completedItems: 4, attemptedItems: 4, attemptCount: 7, retryCount: 3 });
  expect(state.summary.rows).toEqual(expect.arrayContaining([
    expect.objectContaining({
      targetScopeId: "kan-bare-infinitive", assessmentMode: "selection", plannedItems: 2,
      firstOutcomes: expect.objectContaining({ targetNotMet: 1 }),
      latestOutcomes: expect.objectContaining({ fullyMet: 2 }),
    }),
    expect.objectContaining({
      targetScopeId: "kan-bare-infinitive", assessmentMode: "recall", plannedItems: 2,
      firstOutcomes: expect.objectContaining({ targetNotMet: 1, unevaluable: 1 }),
      latestOutcomes: expect.objectContaining({ fullyMet: 1, responseConstraintNotMet: 1 }),
    }),
  ]));
  await page.getByRole("button", { name: "Next: Expressing an intention", exact: true }).click();
  await expect(page.getByRole("radio", { name: "Expressing an intention", exact: true })).toBeChecked();
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  expect((await stateFor(page)).summary).toEqual(state.summary);
  await scan(page);
});

for (const round of MODAL_ROUNDS) {
  test(`${round.focus} teaches contextual meaning then supplied modal order`, async ({ page }) => {
    let state = await openCourse(page, round.title);
    expect(state).toMatchObject({
      focusId: round.focus, planRevision: `${round.focus}-1`,
      contentRevision: "pilot-14", generatorRevision: "generator-14",
      introductionRequired: true, introductionOpened: false, itemCount: 2,
      prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
    });
    expect(state.items.map((item) => item.targetScopeId)).toEqual([round.scope, "supplied-modal-order"]);
    const early = await page.request.post("/api/v1/attempts", {
      headers: { "X-Astrid-Client": "playground" },
      data: {
        attemptId: crypto.randomUUID(), exerciseId: state.currentExercise!.id,
        answer: { kind: "text", text: round.correct }, hintUsed: false,
      },
    });
    expect(early.status()).toBe(409);
    expect((await stateFor(page)).summary.attemptCount).toBe(0);

    state = await beginCourse(page);
    const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
    await expect(introduction.getByText(round.form, { exact: true })).toHaveAttribute("lang", "da");
    await expect(introduction.getByText(round.example, { exact: true })).toHaveAttribute("lang", "da");
    await expect(page.getByText(
      round.scope === "modal-volition-meaning" ? "Intention in context"
        : round.scope === "modal-obligation-meaning" ? "Requirement in context"
          : "Permission in context",
      { exact: true },
    ).and(page.locator("span"))).toBeVisible();
    await scan(page);

    const interpretation = state.currentExercise!;
    expect(interpretation.formatId).toBe("E028");
    expect(interpretation.prompt.cue).toBe(round.statement);
    expect(interpretation.prompt.choices).toEqual(expect.arrayContaining([...MODAL_CHOICES]));
    expect(interpretation.prompt.choices).toHaveLength(3);
    await expect(page.getByText(round.statement, { exact: true })).toHaveAttribute("lang", "da");
    const choices = page.getByRole("group", { name: "English modal meaning choices" });
    await expect(choices).toHaveAttribute("lang", "en");
    await submit(page, interpretation, round.wrong);
    let saved = await stateFor(page);
    expect(latest(saved)).toMatchObject({
      targetSatisfaction: "not-met", meaningSatisfaction: "not-met",
      linguisticStatus: "acceptable", responseConstraintStatus: "met",
    });
    expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await submit(page, interpretation, round.correct);
    const correctedInterpretation = await stateFor(page);
    expect(latest(correctedInterpretation)).toMatchObject({
      targetSatisfaction: "met", meaningSatisfaction: "met",
      linguisticStatus: "acceptable", responseConstraintStatus: "met",
    });
    state = await next(page, correctedInterpretation);

    const construction = state.currentExercise!;
    expect(construction.formatId).toBe("E025");
    expect(construction.targetScopeId).toBe("supplied-modal-order");
    await expect(page.getByText("Supplied-modal statement order", { exact: true }).and(page.locator("span"))).toBeVisible();
    if (round.orderVariant === "reversed-noun-phrase") {
      await submitModalOrder(page, construction, [...round.order.slice(0, 3), round.order[4]!, round.order[3]!]);
      saved = await stateFor(page);
      expect(latest(saved)).toMatchObject({
        targetSatisfaction: "met", linguisticStatus: "unknown", responseConstraintStatus: "not-met",
        issues: [{ code: "REQUESTED_ORDER_CONSTRAINT" }],
      });
      expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await submitModalOrder(page, construction, round.order);
    } else if (round.orderVariant === "missing-object") {
      await submitModalOrder(page, construction, round.order.slice(0, 3));
      saved = await stateFor(page);
      expect(latest(saved)).toMatchObject({
        targetSatisfaction: "unevaluable", linguisticStatus: "unknown",
        issues: [{ code: "REQUIRED_WORDS_MISSING" }],
      });
      expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await submitModalOrder(page, construction, round.order);
    } else {
      await submitModalOrder(page, construction, round.order);
    }
    state = await next(page, await stateFor(page));

    expect(state.summary).toMatchObject({
      completedItems: 2, attemptedItems: 2,
      attemptCount: round.orderVariant === "canonical" ? 3 : 4,
      retryCount: round.orderVariant === "canonical" ? 1 : 2,
    });
    expect(state.summary.rows).toEqual(expect.arrayContaining([
      expect.objectContaining({
        targetSkillId: "G030", targetScopeId: round.scope, assessmentMode: "selection", plannedItems: 1,
      }),
      expect.objectContaining({
        targetSkillId: "G005", targetScopeId: "supplied-modal-order", assessmentMode: "word-order", plannedItems: 1,
      }),
    ]));
    if (round.next) {
      await page.getByRole("button", { name: `Next: ${round.next}`, exact: true }).click();
      await expect(page.getByRole("radio", { name: round.next, exact: true })).toBeChecked();
      await page.getByRole("button", { name: "Cancel", exact: true }).click();
    } else {
      await expect(page.getByRole("button", { name: /^Next:/ })).toHaveCount(0);
    }
    await page.reload();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    const reloaded = await stateFor(page);
    expect(reloaded.introductionOpened).toBe(true);
    expect(reloaded.summary).toEqual(state.summary);
    await scan(page);
  });
}

test("final circumstances retain clause-core and requested-end evidence", async ({ page }) => {
  let state = await openCourse(page, "Adding when and where");
  expect(state).toMatchObject({
    focusId: "module-one-circumstances", planRevision: "module-one-circumstances-1",
    contentRevision: "pilot-15", generatorRevision: "generator-15",
    introductionRequired: true, introductionOpened: false, itemCount: 4,
    prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  });
  expect(state.items.every((item) => item.targetScopeId === "one-final-circumstance")).toBe(true);
  expect(state.items.every((item) => item.assessmentMode === "constituent-order")).toBe(true);
  const early = await page.request.post("/api/v1/attempts", {
    headers: { "X-Astrid-Client": "playground" },
    data: {
      attemptId: crypto.randomUUID(), exerciseId: state.currentExercise!.id,
      answer: { kind: "order", blockIds: [] }, hintUsed: false,
    },
  });
  expect(early.status()).toBe(409);
  expect((await stateFor(page)).summary.attemptCount).toBe(0);

  state = await beginCourse(page);
  const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
  await expect(introduction.getByText(
    "Keep the supplied clause core in its original order and add the supplied time or place circumstance at the end.",
    { exact: true },
  )).toBeVisible();
  for (const form of ["i dag", "nu", "i Aarhus"]) {
    await expect(introduction.getByText(form, { exact: true })).toHaveAttribute("lang", "da");
  }
  await expect(introduction.getByText(
    "The expression meaning today contains two written words and fills one sentence constituent.",
    { exact: true },
  )).toBeVisible();
  for (const example of ["Du arbejder nu.", "Han læser en bog i dag.", "Jeg arbejder i Aarhus."]) {
    await expect(introduction.getByText(example, { exact: true })).toHaveAttribute("lang", "da");
  }
  await expect(page.getByText("Time or place at the end", { exact: true }).and(page.locator("span"))).toBeVisible();
  await scan(page);

  const seen = new Set<string>();
  while (state.state !== "completed") {
    const exercise = state.currentExercise!;
    const task = CIRCUMSTANCE_ORDERS[exercise.prompt.cue as keyof typeof CIRCUMSTANCE_ORDERS];
    expect(task, "The exact Danish core identifies one supplied English context and requested order").toBeDefined();
    expect(exercise).toMatchObject({ formatId: "E029", targetScopeId: "one-final-circumstance" });
    expect(exercise.prompt.answerLanguage).toBe("da");
    expect(exercise.prompt.cueLanguage).toBe("da");
    expect(exercise.prompt).not.toHaveProperty("tileMode");
    expect(exercise.prompt).not.toHaveProperty("requiredTileCount");
    expect(exercise.context).toBe(task.context);
    expect(exercise.prompt.blocks!.map(({ text }) => text).sort()).toEqual([...task.canonical].sort());
    expect(seen.has(exercise.prompt.cue!)).toBe(false);
    seen.add(exercise.prompt.cue!);
    await expect(page.getByText(exercise.prompt.cue!, { exact: true })).toHaveAttribute("lang", "da");
    await expect(page.getByText(task.context, { exact: true })).toBeVisible();
    await expect(page.getByRole("group", { name: "Available sentence parts", exact: true })).toBeVisible();
    await expect(page.getByText("Use every word tile.", { exact: true })).toHaveCount(0);

    if (state.cursor === 0) {
      await chooseCircumstanceOrder(page, exercise, task.canonical.slice(0, 1));
      await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeDisabled();
      await clearChosenOrder(page);
      const firstOrder = [task.canonical[1]!, task.canonical[0]!, ...task.canonical.slice(2)];
      await submitCircumstanceOrder(page, exercise, firstOrder);
      const saved = await stateFor(page);
      expect(latest(saved)).toMatchObject({
        targetSatisfaction: "met", meaningSatisfaction: "unknown", linguisticStatus: "unknown",
        responseConstraintStatus: "not-met", issues: [{ code: "REQUESTED_ORDER_CONSTRAINT" }],
      });
      expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await expectChosenOrder(page, firstOrder);
      await clearChosenOrder(page);
      await submitCircumstanceOrder(page, exercise, task.canonical);
    } else if (state.cursor === 1) {
      const firstOrder = [task.canonical.at(-1)!, ...task.canonical.slice(0, -1)];
      await submitCircumstanceOrder(page, exercise, firstOrder);
      const saved = await stateFor(page);
      expect(latest(saved)).toMatchObject({
        targetSatisfaction: "not-met", meaningSatisfaction: "unknown", linguisticStatus: "unknown",
        responseConstraintStatus: "not-met", issues: [{ code: "REQUESTED_ORDER_CONSTRAINT" }],
      });
      expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await expectChosenOrder(page, firstOrder);
      await clearChosenOrder(page);
      await submitCircumstanceOrder(page, exercise, task.canonical);
    } else if (state.cursor === 2) {
      await submitCircumstanceOrder(page, exercise, task.canonical);
      const saved = await stateFor(page);
      const expectedIds = task.canonical.map((text) => exercise.prompt.blocks!.find((block) => block.text === text)!.id);
      expect(saved.items[saved.cursor]!.lastAnswer).toEqual({ kind: "order", blockIds: expectedIds });
      await page.reload();
      for (const [position, text] of task.canonical.entries()) {
        await expect(page.getByRole("button", { name: `Remove ${text} from position ${position + 1}`, exact: true })).toBeVisible();
      }
      expect((await stateFor(page)).items[state.cursor]!.lastAnswer).toEqual({ kind: "order", blockIds: expectedIds });
      await page.getByRole("button", { name: "Try again", exact: true }).click();
      await expectChosenOrder(page, task.canonical);
      await clearChosenOrder(page);
      await submitCircumstanceOrder(page, exercise, task.canonical);
    } else {
      await submitCircumstanceOrder(page, exercise, task.canonical);
    }
    expect(latest(await stateFor(page))).toMatchObject({
      targetSatisfaction: "met", meaningSatisfaction: "met", linguisticStatus: "acceptable",
      responseConstraintStatus: "met", issues: [],
    });
    state = await next(page, await stateFor(page));
  }

  expect(seen).toEqual(new Set(Object.keys(CIRCUMSTANCE_ORDERS)));
  expect(state.summary).toMatchObject({ completedItems: 4, attemptedItems: 4, attemptCount: 7, retryCount: 3 });
  expect(state.summary.rows).toEqual([expect.objectContaining({
    targetSkillId: "G016", targetScopeId: "one-final-circumstance", assessmentMode: "constituent-order", plannedItems: 4,
    firstOutcomes: expect.objectContaining({
      fullyMet: 2, targetMet: 3, targetNotMet: 1, languageUnknown: 2, responseConstraintNotMet: 2,
    }),
    latestOutcomes: expect.objectContaining({ fullyMet: 4, targetMet: 4 }),
  })]);
  await expect(page.getByRole("button", { name: /^Next:/ })).toHaveCount(0);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
  const reloaded = await stateFor(page);
  expect(reloaded.introductionOpened).toBe(true);
  expect(reloaded.summary).toEqual(state.summary);
  await scan(page);
});

test("a final-circumstance order replays byte-identically after committed response loss", async ({ page }) => {
  await openCourse(page, "Adding when and where");
  const state = await beginCourse(page);
  const exercise = state.currentExercise!;
  const task = CIRCUMSTANCE_ORDERS[exercise.prompt.cue as keyof typeof CIRCUMSTANCE_ORDERS];
  expect(task).toBeDefined();
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) {
      await route.fetch();
      await route.abort("connectionfailed");
    } else await route.continue();
  });

  await chooseCircumstanceOrder(page, exercise, task.canonical);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("button", { name: "Retry saved operation", exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  const request = JSON.parse(bodies[0]!) as {
    attemptId: string; exerciseId: string; answer: { kind: string; blockIds: string[] };
  };
  expect(request).toMatchObject({ exerciseId: exercise.id, answer: { kind: "order" } });
  expect(request.answer.blockIds).toEqual(task.canonical.map(
    (text) => exercise.prompt.blocks!.find((block) => block.text === text)!.id,
  ));
  const recovered = await stateFor(page);
  expect(recovered.items[recovered.cursor]!).toMatchObject({
    attemptCount: 1, lastAnswer: request.answer, canAdvance: true,
  });
});

for (const theme of ["light", "dark"] as const) {
  test(`final-circumstance teaching feedback and summary reflow in ${theme} at 320`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 844 });
    await openCourse(page, "Adding when and where", true);
    expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
    let state = await beginCourse(page);
    await scan(page);
    let scannedFeedback = false;
    while (state.state !== "completed") {
      const exercise = state.currentExercise!;
      const task = CIRCUMSTANCE_ORDERS[exercise.prompt.cue as keyof typeof CIRCUMSTANCE_ORDERS];
      expect(task).toBeDefined();
      await submitCircumstanceOrder(page, exercise, task.canonical);
      if (!scannedFeedback) {
        await scan(page);
        scannedFeedback = true;
      }
      state = await next(page, await stateFor(page));
    }
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    await scan(page);
  });
}

for (const profile of PROFILES) {
  test(`first steps teach before assessment and retain scoped outcomes for ${profile.focus}`, async ({ page }) => {
    let state = await openCourse(page, profile.title);
    expect(state).toMatchObject({ focusId: profile.focus, planRevision: `${profile.focus}-1`, contentRevision: `pilot-${profile.pair}`, generatorRevision: `generator-${profile.pair}`, introductionRequired: true, introductionOpened: false, itemCount: profile.itemCount });
    expect(state.items.every((item) => item.targetScopeId === profile.scope)).toBe(true);
    expect(state.items[state.cursor]!.canAdvance).toBe(false);
    expect(state.items.filter((item) => item.position !== state.cursor).every((item) => item.canAdvance == null)).toBe(true);
    const early = await page.request.post("/api/v1/attempts", {
      headers: { "X-Astrid-Client": "playground" },
      data: { attemptId: crypto.randomUUID(), exerciseId: state.currentExercise!.id, answer: { kind: "text", text: answerFor(state.currentExercise!) }, hintUsed: false },
    });
    expect(early.status()).toBe(409);
    expect((await stateFor(page)).summary.attemptCount).toBe(0);
    state = await beginCourse(page);
    const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
    if (profile.focus === "module-one-things") {
      await expect(introduction.getByText("en bog · et brev", { exact: true })).toHaveAttribute("lang", "da");
      await expect(introduction.getByText("en bog → den · et brev → det", { exact: true })).toHaveAttribute("lang", "da");
      await expect(introduction.getByText(/refer back to the same mentioned thing/i)).toBeVisible();
    } else {
      await expect(introduction.getByRole("heading", { name: "Danish letters", exact: true })).toBeVisible();
      await expect(introduction.getByText("æ · ø · å", { exact: true })).toHaveAttribute("lang", "da");
      await expect(introduction.getByText("Jeg er Anna.", { exact: true })).toHaveAttribute("lang", "da");
      await expect(introduction.getByText(/first word is a pronoun functioning as the subject/)).toBeVisible();
    }
    await expect(page.getByText(profile.badge, { exact: true }).and(page.locator("span"))).toBeVisible();
    await expect(page.getByText(/^G(?:00[34]|038)$/)).toHaveCount(0);
    const typed: string[] = [];
    while (state.state !== "completed") {
      const exercise = state.currentExercise!;
      if (profile.focus === "module-one-things") {
        expect(exercise.prompt.cueLanguage).toBe("da");
        expect(exercise.prompt.answerLanguage).toBe("da");
        expect(exercise.context).toMatch(/common gender|neuter/i);
        await expect(page.getByText(exercise.prompt.cue!, { exact: true })).toHaveAttribute("lang", "da");
      }
      if (!exercise.prompt.choices.length) typed.push(answerFor(exercise));
      await submit(page, exercise);
      await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
      const saved = await stateFor(page);
      expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
      expect(saved.items[saved.cursor]!.lastAttemptContext!.introductionOpened).toBe(true);
      expect(saved.items[saved.cursor]!.lastAttemptContext!.priorExposure.some((event) => event.source === "introduction")).toBe(true);
      state = await next(page, saved);
    }
    expect(state.summary).toMatchObject({ completedItems: profile.itemCount, attemptedItems: profile.itemCount, attemptCount: profile.itemCount, retryCount: 0 });
    expect(state.summary.rows).toHaveLength(2);
    expect(state.summary.rows).toEqual(expect.arrayContaining([
      expect.objectContaining({ targetScopeId: profile.scope, assessmentMode: "selection", plannedItems: profile.selection, firstOutcomes: expect.objectContaining({ fullyMet: profile.selection }) }),
      expect.objectContaining({ targetScopeId: profile.scope, assessmentMode: "recall", plannedItems: profile.recall, firstOutcomes: expect.objectContaining({ fullyMet: profile.recall }) }),
    ]));
    if (profile.scope !== "vaere-present") expect(new Set(typed).size).toBe(profile.recall);
    await expect(page.getByRole("checkbox")).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Start selected review", exact: true })).toHaveCount(0);
    await page.reload();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    expect((await stateFor(page)).summary).toEqual(state.summary);
    let unexpectedCreations = 0;
    await page.route("**/api/v1/practice-sessions", async (route) => { unexpectedCreations += 1; await route.continue(); });
    await page.getByRole("button", { name: "Repeat this session", exact: true }).click();
    await expect(page.getByRole("radio", { name: profile.title, exact: true })).toBeFocused();
    await expect(page.getByRole("radio", { name: profile.title, exact: true })).toBeChecked();
    expect(unexpectedCreations).toBe(0);
    await page.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    const nextTitle = PROFILES[PROFILES.findIndex((candidate) => candidate.focus === profile.focus) + 1]?.title
      ?? "Using present verb forms";
    await page.getByRole("button", { name: `Next: ${nextTitle}`, exact: true }).click();
    await expect(page.getByRole("radio", { name: nextTitle, exact: true })).toBeFocused();
    await expect(page.getByRole("radio", { name: nextTitle, exact: true })).toBeChecked();
    expect(unexpectedCreations).toBe(0);
    await page.getByRole("button", { name: "Cancel", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    expect((await stateFor(page)).id).toBe(state.id);
  });
}

for (const profile of PROFILES) {
  for (const theme of ["light", "dark"] as const) {
    for (const viewport of [{ width: 320, height: 844 }, { width: 844, height: 390 }]) {
      test(`first steps reflow teaching and scoped summaries for ${profile.focus} in ${theme} at ${viewport.width}`, async ({ page }) => {
        await page.emulateMedia({ colorScheme: theme }); await page.setViewportSize(viewport);
        await openCourse(page, profile.title, true);
        await scan(page);
        const state = await beginCourse(page); await scan(page);
        const sections = page.locator("[data-practice-focus=introduction]");
        await expect(sections).toBeVisible();
        await submit(page, state.currentExercise!); await scan(page);
        const saved = await stateFor(page);
        await next(page, saved);
        await finishByApi(page, await stateFor(page)); await page.reload();
        await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
        await page.addStyleTag({ content: "html { font-size: 200% !important; }" }); await scan(page);
      });
    }
  }
}


for (const correction of [{ expected: "I", raw: "i" }, { expected: "de", raw: "De" }]) {
  test(`first steps preserve ambiguous ${correction.raw} casing without a wrong-person diagnosis`, async ({ page }) => {
    await openCourse(page, PROFILES[1].title); await beginCourse(page);
    const state = await reachRecall(page, correction.expected);
    await submit(page, state.currentExercise!, correction.raw);
    const saved = await stateFor(page);
    expect(latest(saved)).toMatchObject({ targetSatisfaction: "unevaluable", responseConstraintStatus: "not-met", issues: expect.arrayContaining([expect.objectContaining({ code: "STANDALONE_PRONOUN_CASE_REQUIRED" })]) });
    expect(saved.items[saved.cursor]!.lastAnswer).toMatchObject({ kind: "text", text: correction.raw });
    expect(saved.items[saved.cursor]!.canAdvance).toBe(true);
    await expect(page.getByRole("status").getByText("Good work", { exact: true })).toHaveCount(0);
    await expect(page.getByRole("status").getByText(correction.expected, { exact: true })).toHaveAttribute("lang", "da");
    await next(page, saved);
  });
}

test("first steps let the latest unknown block advance while preserving earlier and shape outcomes", async ({ page }) => {
  await openCourse(page, PROFILES[0].title); await beginCourse(page);
  const state = await reachRecall(page);
  await submit(page, state.currentExercise!);
  expect((await stateFor(page)).items[state.cursor]!.canAdvance).toBe(true);
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await submit(page, state.currentExercise!, "unfamiliar");
  const unknown = await stateFor(page);
  expect(latest(unknown)).toMatchObject({ targetSatisfaction: "unevaluable", linguisticStatus: "unknown", responseConstraintStatus: "met" });
  expect((latest(unknown).completedSentence ?? "").trim()).toBe("");
  expect(unknown.items[unknown.cursor]!.canAdvance).toBe(false);
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  const blocked = await page.request.post(`/api/v1/practice-sessions/${state.id}/advance`, { headers: ownerHeaders(state.ownerSessionId), data: { position: state.cursor } });
  expect(blocked.status()).toBe(409);
  const unknownRaw = await (await page.request.get(`/api/v1/attempts/${latest(unknown).attemptId}`)).text();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await submit(page, state.currentExercise!, "vi sover");
  const shaped = await stateFor(page);
  expect(latest(shaped)).toMatchObject({ targetSatisfaction: "unevaluable", responseConstraintStatus: "not-met", issues: expect.arrayContaining([expect.objectContaining({ code: "ONE_WORD_RESPONSE_REQUIRED" })]) });
  expect(shaped.items[shaped.cursor]!.canAdvance).toBe(true);
  await next(page, shaped);
  const completed = await finishByApi(page, await stateFor(page));
  expect(completed.summary).toMatchObject({ completedItems: 6, attemptCount: 8, retryCount: 2 });
  expect(completed.summary.rows).toEqual(expect.arrayContaining([expect.objectContaining({
    targetScopeId: "human-singular", assessmentMode: "recall", plannedItems: 2,
    firstOutcomes: expect.objectContaining({ fullyMet: 2 }),
    latestOutcomes: expect.objectContaining({ fullyMet: 1, unevaluable: 1, responseConstraintNotMet: 1 }),
  })]));
  expect(await (await page.request.get(`/api/v1/attempts/${latest(unknown).attemptId}`)).text()).toBe(unknownRaw);
});

test("one-thing recall keeps case evidence while the latest unknown supersedes an eligible answer", async ({ page }) => {
  await page.emulateMedia({ colorScheme: "dark" });
  await page.setViewportSize({ width: 320, height: 844 });
  await openCourse(page, PROFILES[3].title); await beginCourse(page);
  await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  const state = await reachRecall(page, "det");
  await submit(page, state.currentExercise!);
  expect((await stateFor(page)).items[state.cursor]!.canAdvance).toBe(true);
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await submit(page, state.currentExercise!, "Det");
  const cased = await stateFor(page);
  expect(latest(cased)).toMatchObject({
    targetSatisfaction: "met", linguisticStatus: "known-issue", responseConstraintStatus: "not-met",
    issues: expect.arrayContaining([expect.objectContaining({ code: "STANDALONE_PRONOUN_CASE_REQUIRED" })]),
  });
  expect(cased.items[cased.cursor]!.canAdvance).toBe(true);
  await expect(page.getByRole("status").getByText("det", { exact: true })).toHaveAttribute("lang", "da");
  await scan(page);
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await submit(page, state.currentExercise!, "dem");
  const unknown = await stateFor(page);
  expect(latest(unknown)).toMatchObject({ targetSatisfaction: "unevaluable", linguisticStatus: "unknown" });
  expect((latest(unknown).completedSentence ?? "").trim()).toBe("");
  expect(unknown.items[unknown.cursor]!.canAdvance).toBe(false);
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  await scan(page);
  const unknownRaw = await (await page.request.get(`/api/v1/attempts/${latest(unknown).attemptId}`)).text();
  expect(unknownRaw).not.toContain('"completedSentence":"det"');
  await page.reload();
  await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  expect(await (await page.request.get(`/api/v1/attempts/${latest(unknown).attemptId}`)).text()).toBe(unknownRaw);
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  await submit(page, state.currentExercise!, "det er");
  const shaped = await stateFor(page);
  expect(latest(shaped)).toMatchObject({
    targetSatisfaction: "unevaluable", responseConstraintStatus: "not-met",
    issues: expect.arrayContaining([expect.objectContaining({ code: "ONE_WORD_RESPONSE_REQUIRED" })]),
  });
  expect(shaped.items[shaped.cursor]!.canAdvance).toBe(true);
  await scan(page);
  await next(page, shaped);
  const completed = await finishByApi(page, await stateFor(page));
  expect(completed.summary).toMatchObject({ completedItems: 4, attemptCount: 7, retryCount: 3 });
  expect(completed.summary.rows).toEqual(expect.arrayContaining([expect.objectContaining({
    targetScopeId: "inanimate-singular-subject", assessmentMode: "recall", plannedItems: 2,
    firstOutcomes: expect.objectContaining({ fullyMet: 2 }),
    latestOutcomes: expect.objectContaining({ fullyMet: 1, unevaluable: 1, responseConstraintNotMet: 1 }),
  })]));
});

for (const committed of [false, true]) {
  test(`first steps confirm required teaching only after ${committed ? "committed" : "uncommitted"} support replay`, async ({ page }) => {
    const state = await openCourse(page, PROFILES[0].title);
    const bodies: string[] = [];
    await page.route("**/api/v1/practice-sessions/*/support", async (route) => {
      bodies.push(route.request().postData()!);
      if (bodies.length === 1) {
        if (committed) {
          await route.fetch();
        }
        await route.abort("connectionfailed");
      } else {
        await route.continue();
      }
    });
    await page.getByRole("button", { name: "Session introduction", exact: true }).click();
    await expect(page.getByRole("button", { name: "Retry saved operation", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeHidden();
    await expect(page.locator("[data-practice-focus=introduction]")).toBeHidden();
    await page.reload();
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
    expect(bodies).toHaveLength(2); expect(bodies[1]).toBe(bodies[0]);
    const recovered = await stateFor(page);
    expect(recovered).toMatchObject({ id: state.id, introductionOpened: true, summary: { attemptCount: 0 } });
    expect(recovered.currentExercise!.id).toBe(state.currentExercise!.id);
    await submit(page, recovered.currentExercise!);
    expect((await stateFor(page)).items[0]!.attemptCount).toBe(1);
  });

  for (const replayProfile of [PROFILES[0], PROFILES[3]]) {
    test(`first steps replay the exact typed form for ${replayProfile.focus} after ${committed ? "committed" : "uncommitted"} answer loss`, async ({ page }) => {
      await openCourse(page, replayProfile.title); await beginCourse(page);
      const state = await reachRecall(page), raw = `  ${answerFor(state.currentExercise!)}  `;
      const bodies: string[] = [], results: string[] = [];
      await page.route("**/api/v1/attempts", async (route) => {
        bodies.push(route.request().postData()!);
        if (bodies.length === 1) {
          if (committed) {
            const response = await route.fetch();
            results.push(await response.text());
          }
          await route.abort("connectionfailed");
        } else {
          const response = await route.fetch();
          results.push(await response.text());
          await route.fulfill({ response });
        }
      });
      await page.getByRole("textbox").fill(raw);
      await page.getByRole("button", { name: "Check answer", exact: true }).click();
      await expect(page.getByRole("button", { name: "Retry saved operation", exact: true })).toBeVisible();
      await page.reload();
      await expect(page.getByRole("status").getByText("Good work", { exact: true })).toBeVisible();
      expect(bodies).toHaveLength(2); expect(bodies[1]).toBe(bodies[0]);
      const request = JSON.parse(bodies[0]!) as { attemptId: string; exerciseId: string; answer: { kind: string; text: string } };
      expect(request).toMatchObject({ attemptId: expect.any(String), exerciseId: state.currentExercise!.id, answer: { kind: "text", text: raw } });
      const recovered = await stateFor(page);
      expect(recovered.items[recovered.cursor]!).toMatchObject({ attemptCount: 1, lastAnswer: { kind: "text", text: raw }, canAdvance: true });
      expect(results).toHaveLength(committed ? 2 : 1);
      expect(new Set(results)).toEqual(new Set([results.at(-1)!]));
      expect(await (await page.request.get(`/api/v1/attempts/${request.attemptId}`)).text()).toBe(results.at(-1));
    });
  }

  test(`first steps retain creation identity through ${committed ? "committed" : "uncommitted"} response loss`, async ({ page }) => {
    const bodies: string[] = [];
    await page.route("**/api/v1/practice-sessions", async (route) => {
      bodies.push(route.request().postData()!);
      if (bodies.length === 1) {
        if (committed) {
          await route.fetch();
        }
        await route.abort("connectionfailed");
      } else {
        await route.continue();
      }
    });
    await page.goto("/practice"); await choosePractice(page, PROFILES[2].title);
    await expect(page.getByRole("button", { name: "Retry saved operation", exact: true })).toBeVisible();
    const id = new URL(page.url()).searchParams.get("session");
    await page.reload();
    await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeHidden();
    expect(bodies).toHaveLength(2); expect(bodies[1]).toBe(bodies[0]);
    expect(await stateFor(page)).toMatchObject({ id, focusId: "module-one-er", introductionOpened: false, summary: { attemptCount: 0 } });
  });
}

test("first steps keep an unfinished typed form while lesson and hint support are saved", async ({ page }) => {
  await openCourse(page, PROFILES[0].title); await beginCourse(page);
  const state = await reachRecall(page), draft = answerFor(state.currentExercise!).slice(0, 1);
  await page.getByRole("textbox").fill(draft);
  await page.getByRole("button", { name: "Learn this first", exact: true }).click();
  await expect(page.getByRole("textbox")).toHaveValue(draft);
  await expect(page.locator("[data-practice-focus=lesson]")).toBeFocused();
  await page.getByRole("button", { name: "Show hint", exact: true }).click();
  await expect(page.getByRole("textbox")).toHaveValue(draft);
  await expect(page.locator("[data-practice-focus=hint]")).toBeFocused();
  await submit(page, state.currentExercise!);
  expect((await stateFor(page)).items[state.cursor]!.lastAttemptContext).toMatchObject({ lessonOpened: true, hintOpened: true });
});


for (const theme of ["light", "dark"] as const) {
  for (const viewport of [{ width: 320, height: 844 }, { width: 844, height: 390 }]) {
    test(`first-step chooser keeps all profiles keyboard-accessible in ${theme} at ${viewport.width}`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: theme }); await page.setViewportSize(viewport);
      await page.goto("/practice");
      await expect(page.getByRole("heading", { name: "Module 1: First steps", exact: true })).toBeVisible();
      await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
      await expect(page.getByRole("radio")).toHaveCount(17);
      await scan(page);
      const groupSession = page.getByRole("radio", { name: PROFILES[1].title, exact: true });
      await groupSession.focus(); await page.keyboard.press("Space");
      await expect(groupSession).toBeChecked();
      await expect(page.getByRole("button", { name: "Start practice", exact: true })).toBeEnabled();
    });
  }
}
