import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { choosePractice, ownerHeaders, readPractice, type PracticeState } from "../support/practice.js";
import { openGenerationControls } from "../support/generation-controls.js";

type MatchItem = { id: string; text: string };
type Exercise = {
  id: string;
  formatId: string;
  prompt: {
    cue?: string;
    choices: string[];
    blocks: { id: string; text: string }[];
    matchingColumns?: { left: MatchItem[]; right: MatchItem[] };
    fixedPunctuation?: { beforeBlockId: string; text: string }[];
  };
};
type P2State = Omit<PracticeState, "currentExercise"> & {
  focusId: string;
  planRevision: string;
  contentRevision: string;
  generatorRevision: string;
  introductionOpened: boolean;
  introductionRequired: boolean;
  currentExercise: Exercise | null;
};
type Answer =
  | { kind: "text"; value: string }
  | { kind: "order"; words: string[]; punctuation?: boolean }
  | { kind: "matching"; pairs: Record<string, string> };

const PROFILES: readonly {
  focus: string; title: string; count: number; teaching: string[]; answers: Answer[];
}[] = [
  {
    focus: "vocabulary-p2-s1", title: "A small breakfast", count: 3,
    teaching: ["at spise → spiser", "et æble", "brød", "billig"],
    answers: [
      { kind: "matching", pairs: { "an apple": "et æble", bread: "brød", "eats/is eating": "spiser" } },
      { kind: "text", value: "inexpensive" }, { kind: "text", value: "bread" },
    ],
  },
  {
    focus: "vocabulary-p2-s2", title: "Drinks on the table", count: 4,
    teaching: ["at drikke → drikker", "kaffe", "vand", "mælk"],
    answers: [
      { kind: "text", value: "milk" },
      { kind: "matching", pairs: { coffee: "kaffe", water: "vand", milk: "mælk", "drinks/is drinking": "drikker" } },
      { kind: "text", value: "the speaker (jeg)" }, { kind: "order", words: ["Nora", "drikker", "mælk"] },
    ],
  },
  {
    focus: "vocabulary-p2-s3", title: "Getting attention and receiving a roll", count: 4,
    teaching: ["Undskyld.", "Værsgo.", "en bolle", "en butik"],
    answers: [
      { kind: "matching", pairs: {
        "excuse me (getting attention)": "undskyld", "here you are (handing something over)": "værsgo",
        "a bread roll": "en bolle", "a shop": "en butik",
      } },
      { kind: "text", value: "Undskyld." }, { kind: "text", value: "a bread roll" },
      { kind: "text", value: "Værsgo." },
    ],
  },
  {
    focus: "vocabulary-p2-s4", title: "Buying and paying", count: 3,
    teaching: ["en café", "at købe → køber", "at betale → betaler", "i en café"],
    answers: [
      { kind: "text", value: "buys" }, { kind: "text", value: "the speaker pays" },
      { kind: "order", words: ["Jeg", "køber", "en bolle"] },
    ],
  },
  {
    focus: "vocabulary-p2-s5", title: "Marking a polite wish", count: 3,
    teaching: ["at ville → vil", "gerne", "Tak."],
    answers: [
      { kind: "text", value: "a willing wish or polite preference" },
      { kind: "order", words: ["Jeg", "vil", "gerne", "købe", "en bolle"] },
      { kind: "text", value: "Tak." },
    ],
  },
  {
    focus: "vocabulary-p2-s6", title: "Requesting a supplied quantity", count: 4,
    teaching: ["en kop", "en flaske", "en kop kaffe", "en flaske vand", "have"],
    answers: [
      { kind: "matching", pairs: { "a cup": "en kop", "a bottle": "en flaske" } },
      { kind: "text", value: "Nora" },
      { kind: "order", words: ["Jeg", "vil", "gerne", "have", "en kop kaffe", "tak"], punctuation: true },
      { kind: "order", words: ["Jeg", "vil", "gerne", "have", "en flaske vand", "tak"], punctuation: true },
    ],
  },
];

async function stateFor(page: Page) {
  await expect(page).toHaveURL(/\/practice\?session=[0-9a-f-]+$/);
  const id = new URL(page.url()).searchParams.get("session");
  expect(id).not.toBeNull();
  return await readPractice(page.request, id!) as unknown as P2State;
}

async function chooseProfile(page: Page, profile: typeof PROFILES[number]) {
  await expect(page.getByRole("heading", { name: "Food, cafés and shopping", exact: true })).toBeVisible();
  await choosePractice(page, profile.title);
  await expect(page).toHaveURL(/\/practice\?session=[0-9a-f-]+$/);
  await expect(page.getByRole("button", { name: "Session introduction", exact: true })).toBeVisible();
  const state = await stateFor(page);
  expect(state).toMatchObject({
    focusId: profile.focus, planRevision: `${profile.focus}-1`, contentRevision: "pilot-19",
    generatorRevision: "generator-19", itemCount: profile.count,
    introductionRequired: true, introductionOpened: false,
  });
  return state;
}

async function openProfile(page: Page, profile: typeof PROFILES[number]) {
  await page.goto("/practice");
  return await chooseProfile(page, profile);
}

async function begin(page: Page) {
  const saved = page.waitForResponse((response) => response.request().method() === "POST"
    && new URL(response.url()).pathname.endsWith("/support") && response.status() === 200);
  await page.getByRole("button", { name: "Session introduction", exact: true }).click();
  await saved;
  await expect(page.getByRole("region", { name: "Session introduction", exact: true })).toBeVisible();
  return await stateFor(page);
}

async function advance(page: Page, previous: P2State) {
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

async function introduce(page: Page, owner: string, focusId: string, planRevision: string, seed: number) {
  const practiceSessionId = crypto.randomUUID();
  const created = await page.request.post("/api/v1/practice-sessions", {
    headers: ownerHeaders(owner), data: { practiceSessionId, focusId, planRevision, seed },
  });
  expect(created.status()).toBe(201);
  const taught = await page.request.post(`/api/v1/practice-sessions/${practiceSessionId}/support`, {
    headers: ownerHeaders(owner), data: { kind: "introduction" },
  });
  expect(taught.status()).toBe(200);
}

async function assertTeachingGate(page: Page, exercise: Exercise) {
  const answer = exercise.formatId === "E042"
    ? { kind: "matching", pairs: exercise.prompt.matchingColumns!.left.map((left, index) => ({
      leftId: left.id, rightId: exercise.prompt.matchingColumns!.right[index]!.id,
    })) }
    : exercise.formatId === "E043"
      ? { kind: "order", blockIds: exercise.prompt.blocks.map(({ id }) => id) }
      : { kind: "text", text: exercise.prompt.choices[0] ?? "unfamiliar" };
  const response = await page.request.post("/api/v1/attempts", {
    headers: { "X-Astrid-Client": "playground" },
    data: {
      attemptId: crypto.randomUUID(), exerciseId: exercise.id,
      answer, hintUsed: false,
    },
  });
  expect(response.status()).toBe(409);
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
    expect(block, `The order bank contains ${word}`).toBeDefined();
    remaining.splice(remaining.indexOf(block!), 1);
    await page.getByLabel("Available Danish word tiles", { exact: true })
      .getByRole("button", { name: block!.text, exact: true }).press("Enter");
  }
}

async function answer(page: Page, exercise: Exercise, expected: Answer) {
  if (expected.kind === "matching") {
    await chooseMatching(page, exercise, expected.pairs);
    await page.getByRole("button", { name: "Check matches", exact: true }).click();
  } else if (expected.kind === "order") {
    await chooseOrder(page, exercise, expected.words);
    if (expected.punctuation) {
      const punctuation = page.getByLabel("Fixed punctuation ,", { exact: true });
      await expect(punctuation).toBeVisible();
      await expect(punctuation.locator("xpath=ancestor::button")).toHaveCount(0);
      const unit = punctuation.locator("xpath=ancestor::*[@data-fixed-punctuation-unit]");
      const preceding = unit.getByRole("button");
      const [unitBox, precedingBox, punctuationBox] = await Promise.all([
        unit.boundingBox(), preceding.boundingBox(), punctuation.boundingBox(),
      ]);
      expect(unitBox).not.toBeNull();
      expect(precedingBox).not.toBeNull();
      expect(punctuationBox).not.toBeNull();
      expect(Math.abs(punctuationBox!.x - (precedingBox!.x + precedingBox!.width))).toBeLessThanOrEqual(1);
      expect(punctuationBox!.y).toBeLessThan(precedingBox!.y + precedingBox!.height);
      expect(punctuationBox!.y + punctuationBox!.height).toBeGreaterThan(precedingBox!.y);
      expect(unitBox!.x).toBeGreaterThanOrEqual(0);
      expect(unitBox!.x + unitBox!.width).toBeLessThanOrEqual(await page.evaluate(() => innerWidth));
    }
    await page.getByRole("button", { name: "Check answer", exact: true }).click();
  } else {
    await page.getByRole("group", { name: "Answer choices", exact: true })
      .getByRole("button", { name: expected.value, exact: true }).click();
    await page.getByRole("button", { name: "Check answer", exact: true }).click();
  }
  await expect(page.getByRole("status")).toBeFocused();
}

async function scan(page: Page) {
  const result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();
  expect(result.violations.map(({ id, nodes }) => ({
    id, targets: nodes.map((node) => node.target),
  }))).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
}

for (const profile of PROFILES) {
  test(`P2 teaches and completes every authored activity in ${profile.focus}`, async ({ page }) => {
    const unopened = await openProfile(page, profile);
    await assertTeachingGate(page, unopened.currentExercise!);
    let state = await begin(page);
    const introduction = page.getByRole("region", { name: "Session introduction", exact: true });
    for (const form of profile.teaching) {
      await expect(introduction.getByText(form, { exact: true }).first()).toHaveAttribute("lang", "da");
    }
    if (profile.focus === "vocabulary-p2-s4") {
      await expect(introduction.getByText("Supplied location — in a café", { exact: true })).toBeVisible();
    }
    if (profile.focus === "vocabulary-p2-s6") {
      await expect(introduction.getByText("request have ≠ possession har = has", { exact: true }))
        .toHaveAttribute("lang", "en");
    }
    const seen: string[] = [];
    while (state.state !== "completed") {
      const exercise = state.currentExercise!;
      const expected = profile.answers[state.cursor]!;
      seen.push(exercise.formatId);
      await answer(page, exercise, expected);
      state = await advance(page, state);
    }
    expect(seen).toHaveLength(profile.count);
    expect(seen.every((format) => ["E040", "E041", "E042", "E043", "E044"].includes(format))).toBe(true);
    expect(state.summary).toMatchObject({ attemptedItems: profile.count, completedItems: profile.count });
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeFocused();
    const review = await page.request.get("/api/v1/practice-review-context");
    expect(review.status()).toBe(200);
    const reviewState = await review.json() as {
      itemCount: number; focusId: string; planRevision: string; sourceLessonCount: number;
    };
    expect(reviewState).toMatchObject({
      focusId: "cumulative-review", planRevision: "cumulative-review-3", sourceLessonCount: 1,
    });
    expect(reviewState.itemCount).toBeGreaterThan(0);
    await page.reload();
    expect((await stateFor(page)).summary).toEqual(state.summary);
  });
}

test("P2 two-pair matching preserves a partial retry and then completes", async ({ page }) => {
  const profile = PROFILES[5]!;
  await openProfile(page, profile);
  const state = await begin(page);
  const exercise = state.currentExercise!;
  const oracle = (profile.answers[0] as Extract<Answer, { kind: "matching" }>).pairs;
  const first = exercise.prompt.matchingColumns!.left[0]!;
  await page.getByRole("group", { name: "English words", exact: true })
    .getByRole("button", { name: first.text, exact: true }).press("Enter");
  await page.getByRole("group", { name: "Danish words", exact: true })
    .getByRole("button", { name: oracle[first.text], exact: true }).press("Space");
  await page.getByRole("button", { name: "Check matches", exact: true }).click();
  await expect(page.getByText("Complete two matches", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  const unpaired = exercise.prompt.matchingColumns!.left[1]!;
  await page.getByRole("group", { name: "English words", exact: true })
    .getByRole("button", { name: unpaired.text, exact: true }).press("Enter");
  await page.getByRole("group", { name: "Danish words", exact: true })
    .getByRole("button", { name: oracle[unpaired.text], exact: true }).press("Space");
  await page.getByRole("button", { name: "Check matches", exact: true }).click();
  await expect(page.getByText("Two matches supplied", { exact: true })).toBeVisible();
});

test("P2 matching replays the exact saved response after a lost response and reload", async ({ page }) => {
  const profile = PROFILES[5]!;
  await openProfile(page, profile);
  const state = await begin(page);
  const exercise = state.currentExercise!;
  const oracle = (profile.answers[0] as Extract<Answer, { kind: "matching" }>).pairs;
  const bodies: string[] = [];
  await page.route("**/api/v1/attempts", async (route) => {
    bodies.push(route.request().postData()!);
    if (bodies.length === 1) {
      await route.fetch();
      await route.abort("connectionfailed");
    } else await route.continue();
  });
  await chooseMatching(page, exercise, oracle);
  await page.getByRole("button", { name: "Check matches", exact: true }).click();
  await expect(page.getByRole("alert", { name: "Your answer is not confirmed", exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("status")).toBeVisible();
  expect(bodies).toHaveLength(2);
  expect(bodies[1]).toBe(bodies[0]);
  const request = JSON.parse(bodies[0]!) as { answer: { kind: string; pairs: unknown[] } };
  expect(request.answer.kind).toBe("matching");
  expect(request.answer.pairs).toHaveLength(2);
  expect((await stateFor(page)).items[0]!.attemptCount).toBe(1);
});

test("P2 blocks an unknown complete order until the learner retries", async ({ page }) => {
  const profile = PROFILES[3]!;
  await openProfile(page, profile);
  let state = await begin(page);
  for (let position = 0; position < 2; position += 1) {
    await answer(page, state.currentExercise!, profile.answers[position]!);
    state = await advance(page, state);
  }
  const exercise = state.currentExercise!;
  const expected = profile.answers[2] as Extract<Answer, { kind: "order" }>;
  await chooseOrder(page, exercise, [...expected.words].reverse());
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByText("Language status unknown", { exact: true })).toBeVisible();
  await expect(page.getByText(/not a general judgment about Danish/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Try again", exact: true }).click();
  const chosen = page.getByLabel("Your Danish sentence", { exact: true }).getByRole("button");
  while (await chosen.count()) await chosen.first().click();
  await chooseOrder(page, exercise, expected.words);
  await page.getByRole("button", { name: "Check answer", exact: true }).click();
  await expect(page.getByRole("button", { name: "Continue", exact: true })).toBeEnabled();
});

test("cumulative review admits all six taught P2 families while a saved revision-one plan stays unchanged", async ({ page }) => {
  await page.goto("/#review");
  await page.getByRole("button", { name: "Prepare mixed review", exact: true }).click();
  await expect(page.getByText("No introduced material yet", { exact: true })).toBeVisible();
  const emptyResponse = await page.request.get("/api/v1/practice-review-context");
  expect(emptyResponse.status()).toBe(200);
  const empty = await emptyResponse.json() as { ownerSessionId: string };

  await introduce(page, empty.ownerSessionId, "vocabulary-p1-s1", "vocabulary-p1-s1-2", 700);
  let currentResponse = await page.request.get("/api/v1/practice-review-context");
  expect(currentResponse.status()).toBe(200);
  expect(await currentResponse.json()).toMatchObject({
    planRevision: "cumulative-review-3", sourceLessonCount: 1,
  });

  const legacyId = crypto.randomUUID();
  const legacyCreation = await page.request.post("/api/v1/practice-sessions", {
    headers: ownerHeaders(empty.ownerSessionId),
    data: { practiceSessionId: legacyId, focusId: "cumulative-review", planRevision: "cumulative-review-1", seed: 701 },
  });
  expect(legacyCreation.status()).toBe(201);
  const legacyBefore = await legacyCreation.json();

  for (const [index, profile] of PROFILES.entries()) {
    await introduce(page, empty.ownerSessionId, profile.focus, `${profile.focus}-1`, 710 + index);
  }
  currentResponse = await page.request.get("/api/v1/practice-review-context");
  expect(currentResponse.status()).toBe(200);
  const current = await currentResponse.json() as {
    itemCount: number; planRevision: string; sourceLessonCount: number;
  };
  expect(current).toMatchObject({ planRevision: "cumulative-review-3", sourceLessonCount: 7, itemCount: 8 });

  const legacyAfter = await readPractice(page.request, legacyId);
  expect(legacyAfter).toEqual(legacyBefore);
  expect(legacyAfter).toMatchObject({ focusId: "cumulative-review", planRevision: "cumulative-review-1" });
});

for (const theme of ["light", "dark"] as const) {
  test(`P2 matching requests fixed punctuation and summary reflow at 320px and 200% in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 844 });
    await openProfile(page, PROFILES[5]!);
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    let state = await begin(page);
    await scan(page);
    while (state.state !== "completed") {
      await answer(page, state.currentExercise!, PROFILES[5]!.answers[state.cursor]!);
      await scan(page);
      state = await advance(page, state);
    }
    await expect(page.getByRole("heading", { name: "Practice complete", exact: true })).toBeVisible();
    expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
    await scan(page);
  });

  test(`P2 long functional matching and meaning choices reflow at 320px and 200% in ${theme}`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme });
    await page.setViewportSize({ width: 320, height: 844 });
    await openProfile(page, PROFILES[2]!);
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    let state = await begin(page);
    expect(state.currentExercise?.formatId).toBe("E042");
    await expect(page.getByRole("button", { name: "excuse me (getting attention)", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "here you are (handing something over)", exact: true })).toBeVisible();
    await scan(page);
    await answer(page, state.currentExercise!, PROFILES[2]!.answers[0]!);
    await scan(page);

    await page.getByRole("button", { name: "Start new practice", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Choose your practice", exact: true })).toBeVisible();
    await chooseProfile(page, PROFILES[4]!);
    await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
    state = await begin(page);
    expect(state.currentExercise?.formatId).toBe("E040");
    await expect(page.getByRole("button", { name: "a willing wish or polite preference", exact: true })).toBeVisible();
    expect(await page.evaluate(() => getComputedStyle(document.documentElement).fontSize)).toBe("32px");
    await scan(page);
  });
}

const OPERATIONS = [
  { id: "E040", title: "Recognize an introduced Danish meaning", index: 5 },
  { id: "E041", title: "Choose a Danish response for a stated context", index: 5 },
  { id: "E042", title: "Match introduced meanings and Danish forms", index: 0 },
  { id: "E043", title: "Build a reviewed P2 sentence", index: 1 },
  { id: "E044", title: "Read an explicit P2 fact", index: 5 },
] as const;

function standaloneAnswer(exercise: Exercise): Answer {
  if (exercise.formatId === "E042") {
    const matching = PROFILES.flatMap(({ answers }) => answers)
      .filter((candidate): candidate is Extract<Answer, { kind: "matching" }> => candidate.kind === "matching")
      .find(({ pairs }) => exercise.prompt.matchingColumns!.left.every(({ text }) => text in pairs));
    expect(matching, "The displayed P2 board has an independent authored oracle").toBeDefined();
    return matching!;
  }
  if (exercise.formatId === "E043") {
    const orders: Record<string, string[]> = {
      "Write Nora drinks milk. Begin with Nora.": ["Nora", "drikker", "mælk"],
      "Write I buy a bread roll. Begin with the subject.": ["Jeg", "køber", "en bolle"],
      "Write I would like to buy a bread roll in the taught frame. Begin with the subject.":
        ["Jeg", "vil", "gerne", "købe", "en bolle"],
      "Write I would like a cup of coffee, thank you in the taught request frame.":
        ["Jeg", "vil", "gerne", "have", "en kop kaffe", "tak"],
      "Write I would like a bottle of water, thank you in the taught request frame.":
        ["Jeg", "vil", "gerne", "have", "en flaske vand", "tak"],
    };
    const words = orders[exercise.prompt.cue ?? ""];
    expect(words, "The displayed P2 order cue has an independent authored oracle").toBeDefined();
    return { kind: "order", words: words!, punctuation: Boolean(exercise.prompt.fixedPunctuation?.length) };
  }
  const cues: Record<string, string> = {
    billig: "inexpensive", mælk: "milk", køber: "buys",
    "Jeg vil gerne købe en bolle.": "a willing wish or polite preference",
    "A customer politely wants the shopkeeper's attention.": "Undskyld.",
    "The shopkeeper hands the stated roll to the customer.": "Værsgo.",
    "The customer intends to thank the shopkeeper.": "Tak.",
    "Who drinks water?": "the speaker (jeg)",
    "What happens last in this stated situation?": "the speaker pays",
    "Who requests water in the two parallel notes?": "Nora",
  };
  let value = cues[exercise.prompt.cue ?? ""];
  if (!value && exercise.prompt.cue === "What does Nora eat?") {
    value = exercise.prompt.choices.includes("a bread roll") ? "a bread roll" : "bread";
  }
  expect(value, "The displayed P2 choice cue has an independent authored oracle").toBeDefined();
  return { kind: "text", value: value! };
}

for (const operation of OPERATIONS) {
  test(`P2 standalone playground submits the advertised operation ${operation.id}`, async ({ page }) => {
    await page.goto("/playground");
    await expect(page.getByRole("button", { name: "Check answer", exact: true })).toBeVisible();
    await openGenerationControls(page);
    await page.getByRole("textbox", { name: "Next generation seed", exact: true }).fill("47");
    await page.getByRole("textbox", { name: "Next generation index", exact: true }).fill(String(operation.index));
    const generated = page.waitForResponse((response) => response.request().method() === "POST"
      && new URL(response.url()).pathname === "/api/v1/exercises" && response.status() === 201);
    await page.getByRole("combobox", { name: "Exercise type", exact: true }).click();
    await page.getByRole("option", { name: operation.title, exact: true }).click();
    const exercise = await (await generated).json() as Exercise;
    expect(exercise.formatId).toBe(operation.id);
    if (operation.id === "E042") expect(exercise.prompt.matchingColumns?.left).toHaveLength(2);
    if (operation.id === "E043") expect(exercise.prompt.fixedPunctuation).toHaveLength(1);

    const submitted = page.waitForResponse((response) => response.request().method() === "POST"
      && new URL(response.url()).pathname === "/api/v1/attempts" && response.status() === 200);
    await answer(page, exercise, standaloneAnswer(exercise));
    const response = await submitted;
    expect(await response.json()).toMatchObject({
      targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met",
    });
    const body = response.request().postDataJSON() as { answer: { kind: string } };
    const kind = operation.id === "E042" ? "matching" : operation.id === "E043" ? "order" : "text";
    expect(body.answer.kind).toBe(kind);
    await expect(page.getByRole("button", { name: "Try again", exact: true })).toBeVisible();
  });
}
