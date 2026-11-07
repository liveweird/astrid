import { MantineProvider } from "@mantine/core";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ComponentType } from "react";

const session = {
  mode: "development", inspectorEnabled: false, contentRevision: "pilot-2",
  generatorRevision: "generator-2", evaluationRevision: "evaluation-2",
};
const catalog = {
  contentRevision: "pilot-2", generatorRevision: "generator-2", evaluationRevision: "evaluation-2",
  sentenceCount: 99,
  skills: [
    { id: "G004", title: "Present form", explanation: "Choose a present verb.", prerequisiteIds: [] },
    { id: "G005", title: "Subject-first statements", explanation: "Arrange a statement.", prerequisiteIds: [] },
    { id: "G006", title: "Indefinite articles", explanation: "Choose an article.", prerequisiteIds: [] },
  ],
  formats: [
    { id: "E001", title: "Choose the present form", targetSkillId: "G004" },
    { id: "E002", title: "Supply the indefinite article", targetSkillId: "G006" },
    { id: "E003", title: "Arrange a subject-first statement", targetSkillId: "G005" },
    { id: "E004", title: "Supply the present form", targetSkillId: "G004" },
    { id: "E005", title: "Repair the verb form", targetSkillId: "G004" },
    { id: "E006", title: "Repair the indefinite article", targetSkillId: "G006" },
  ],
};
const exercise = {
  id: "011835c0-bb42-47ef-b21e-ae21411b24a1",
  formatId: "E001", targetSkillId: "G004", supportSkillIds: [],
  contentRevision: "pilot-2", generatorRevision: "generator-2",
  instruction: "Choose the present-tense verb.", context: "You are describing what happens now.",
  prompt: { before: "Jeg", after: "en bog", choices: ["læse", "læser"], blocks: [], punctuation: "." },
  introduction: [{ title: "læse · to read", body: "Present form: læser" }],
  hint: "The present form usually ends in -r.",
};
const attempt = {
  attemptId: "28d52d72-7a04-4a42-8e87-bf780f821510",
  exerciseId: exercise.id,
  targetSatisfaction: "met", linguisticStatus: "acceptable", issues: [],
  feedback: "You selected the present form.", completedSentence: "Jeg læser en bog.",
  evaluationRevision: "evaluation-2", hintUsed: false, retryNumber: 1, createdAt: 1,
};

function json(body: unknown) {
  return Response.json(body, { headers: { "Content-Type": "application/json" } });
}

async function renderPlayground() {
  vi.resetModules();
  const Playground = (await import("./Playground")).default as ComponentType;
  render(<MantineProvider env="test"><Playground /></MantineProvider>);
}

async function verifyKeyboardFormatSelection(
  arrowNavigation: boolean,
  reopenBehavior: "none" | "dismiss-current" | "select-different" = "none",
) {
  const orderingExercise = {
    ...exercise,
    id: "0bf48de0-bcaa-4d9a-85cf-f237d1acbd13",
    formatId: "E003",
    targetSkillId: "G005",
    instruction: "Arrange the blocks to make a statement beginning with the subject.",
    context: "Put the sentence parts in subject-first order.",
    prompt: {
      before: "", after: "", choices: [], punctuation: ".",
      blocks: [
        { id: "b0", text: "I" },
        { id: "b1", text: "har" },
        { id: "b2", text: "en hund" },
      ],
    },
    introduction: [],
  };
  const catalogWithOrdering = catalog;
  const exerciseBodies: Array<Record<string, unknown>> = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) return json(catalogWithOrdering);
    const body = JSON.parse(String(init?.body));
    exerciseBodies.push(body);
    if (exerciseBodies.length === 1) return json(exercise);
    return json(body.formatId === "E003" ? orderingExercise : {
      ...exercise, id: "d20938a8-65aa-4914-8c2c-675b06520629",
    });
  }));
  const user = userEvent.setup();
  await renderPlayground();
  await screen.findByRole("button", { name: "læser" });
  await user.click(screen.getByRole("button", { name: "Generation controls" }));
  let selector = screen.getByRole("combobox", { name: "Exercise type" });
  await user.click(selector);
  expect(screen.getByRole("option", { name: "Choose the present form" }))
    .toHaveAttribute("data-combobox-selected", "true");
  expect(exerciseBodies).toHaveLength(1);
  await user.clear(selector);
  await user.type(selector, "Arrange a subject-first statement");
  const option = screen.getByRole("option", { name: "Arrange a subject-first statement" });
  expect(option).toBeVisible();
  expect(option).toHaveAttribute("data-combobox-selected", "true");
  expect(exerciseBodies).toHaveLength(1);

  if (arrowNavigation) {
    await user.keyboard("{ArrowDown}");
    expect(exerciseBodies).toHaveLength(1);
  }
  await user.keyboard("{Enter}");

  expect(await screen.findByRole("heading", {
    name: "Arrange the blocks to make a statement beginning with the subject.",
  })).toBeVisible();
  expect(exerciseBodies).toHaveLength(2);
  expect(exerciseBodies[1]).toMatchObject({ formatId: "E003" });
  expect(screen.getByRole("combobox", { name: "Exercise type" }))
    .toHaveValue("Arrange a subject-first statement");

  if (reopenBehavior !== "none") {
    selector = screen.getByRole("combobox", { name: "Exercise type" });
    if (reopenBehavior === "dismiss-current") {
      await user.click(screen.getByRole("button", { name: "I" }));
      expect(screen.getByRole("button", { name: "Remove I from position 1" })).toBeVisible();
    }
    await user.click(selector);
    const currentOption = screen.getByRole("option", { name: "Arrange a subject-first statement" });
    expect(currentOption).toHaveAttribute("data-combobox-selected", "true");
    expect(exerciseBodies).toHaveLength(2);
    await user.tab();
    expect(exerciseBodies).toHaveLength(2);

    await user.click(selector);
    expect(screen.getByRole("option", { name: "Arrange a subject-first statement" }))
      .toHaveAttribute("data-combobox-selected", "true");
    expect(exerciseBodies).toHaveLength(2);
    if (reopenBehavior === "dismiss-current") {
      await user.keyboard("{Enter}");
      expect(selector).toHaveAttribute("aria-expanded", "false");
      expect(selector).toHaveValue("Arrange a subject-first statement");
      expect(exerciseBodies).toHaveLength(2);
      expect(screen.getByRole("button", { name: "Remove I from position 1" })).toBeVisible();
      expect(screen.getByRole("heading", {
        name: "Arrange the blocks to make a statement beginning with the subject.",
      })).toBeVisible();
    } else {
      await user.clear(selector);
      await user.type(selector, "Choose the present form");
      expect(screen.getByRole("option", { name: "Choose the present form" }))
        .toHaveAttribute("data-combobox-selected", "true");
      expect(exerciseBodies).toHaveLength(2);
      await user.keyboard("{Enter}");
      await waitFor(() => expect(exerciseBodies).toHaveLength(3));
      expect(exerciseBodies[2]).toMatchObject({ formatId: "E001" });
      const restoredHeading = await screen.findByRole("heading", { name: "Choose the present-tense verb." });
      await waitFor(() => expect(restoredHeading).toHaveFocus());
    }
  }
}

async function verifyKnownSubmissionErrorFocus() {
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) return json(catalog);
    if (path.endsWith("exercises")) return json(exercise);
    return Response.json(
      { detail: "The engine revision changed." },
      { status: 409, headers: { "Content-Type": "application/problem+json" } },
    );
  }));
  const user = userEvent.setup();
  await renderPlayground();
  await user.click(await screen.findByRole("button", { name: "læser" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));

  const alert = await screen.findByRole("alert");
  const reload = screen.getByRole("button", { name: "Reload engine" });
  await waitFor(() => expect(alert).toHaveFocus());
  await user.tab();
  expect(reload).toHaveFocus();
}

async function verifyCurrentCatalogFormats() {
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) return json(catalog);
    return json(exercise);
  }));
  await renderPlayground();
  await screen.findByRole("button", { name: "læser" });
  await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));
  await userEvent.click(screen.getByRole("combobox", { name: "Exercise type" }));

  expect(screen.getAllByRole("option")).toHaveLength(6);
  for (const format of catalog.formats) {
    expect(screen.getByRole("option", { name: format.title })).toBeVisible();
  }
}

async function verifyRepairSupportTracking() {
  const repairExercise = {
    ...exercise,
    id: "f17d982e-7e7f-460f-b922-76e08ccce812",
    formatId: "E005",
    instruction: "Repair the verb form with one word.",
    context: "The highlighted verb is not in the present form.",
    prompt: {
      before: "Jeg", after: "en bog.", choices: [], blocks: [], punctuation: "",
      inputLabel: "Replacement verb", replacementWord: "læse",
    },
  };
  const attemptBodies: Array<Record<string, unknown>> = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) return json(catalog);
    if (path.endsWith("exercises")) return json(repairExercise);
    const submitted = JSON.parse(String(init?.body)) as Record<string, unknown>;
    attemptBodies.push(submitted);
    return json({
      ...attempt,
      attemptId: submitted.attemptId,
      exerciseId: repairExercise.id,
      hintUsed: submitted.hintUsed,
    });
  }));
  const user = userEvent.setup();
  await renderPlayground();
  const input = await screen.findByLabelText("Replacement verb");
  await user.type(input, "l");
  await user.click(screen.getByRole("button", { name: "Insert æ" }));
  await user.type(input, "ser");
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await screen.findByRole("status");

  expect(attemptBodies[0]).toMatchObject({
    exerciseId: repairExercise.id,
    answer: { kind: "text", text: "læser" },
    hintUsed: false,
  });

  await user.click(screen.getByRole("button", { name: "Try again" }));
  await user.click(screen.getByRole("button", { name: "Learn this first" }));
  await user.type(screen.getByLabelText("Replacement verb"), "læser");
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await waitFor(() => expect(attemptBodies).toHaveLength(2));
  expect(attemptBodies[1]).toMatchObject({ hintUsed: true });
}

async function verifyRepairUncertainRecovery() {
  const repairExercise = {
    ...exercise,
    id: "230f23b7-d1a4-45ac-a262-f5ff0130df78",
    formatId: "E006",
    targetSkillId: "G006",
    instruction: "Repair the article with one word.",
    prompt: {
      before: "Jeg har", after: "hund.", choices: [], blocks: [], punctuation: "",
      inputLabel: "Replacement article", replacementWord: "et",
    },
    introduction: [],
  };
  const attemptBodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(session);
    if (path.endsWith("catalog")) return json(catalog);
    if (path.endsWith("exercises")) return json(repairExercise);
    attemptBodies.push(String(init?.body));
    if (attemptBodies.length === 1) throw new TypeError("response connection closed");
    const submitted = JSON.parse(attemptBodies[1]!);
    return json({ ...attempt, attemptId: submitted.attemptId, exerciseId: repairExercise.id });
  }));
  const user = userEvent.setup();
  await renderPlayground();
  const input = await screen.findByLabelText("Replacement article");
  await user.type(input, "en");
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  await screen.findByText("The result is not confirmed");

  expect(input).toBeDisabled();
  expect(input).toHaveValue("en");
  for (const character of ["æ", "ø", "å"]) {
    expect(screen.getByRole("button", { name: `Insert ${character}` })).toBeDisabled();
  }
  await user.click(screen.getByRole("button", { name: "Check the same attempt" }));
  await screen.findByRole("status");
  expect(attemptBodies[1]).toBe(attemptBodies[0]);
  expect(JSON.parse(attemptBodies[1]!)).toMatchObject({
    answer: { kind: "text", text: "en" }, hintUsed: false,
  });
}

async function verifyVocabularyCatalog() {
  const vocabularyCatalog = {
    ...catalog,
    contentRevision: "pilot-3",
    generatorRevision: "generator-3",
    evaluationRevision: "evaluation-3",
    formats: [
      ...catalog.formats,
      { id: "E007", title: "Recognize the meaning", targetSkillId: null,
        lexicalTask: { facet: "meaning", direction: "da-en" } },
      { id: "E008", title: "Recall the Danish headword", targetSkillId: null,
        lexicalTask: { facet: "singular-headword", direction: "en-da" } },
    ],
  };
  const vocabularyExercise = {
    ...exercise,
    id: "9127eaf0-32dd-437c-8ce2-6e1a81f2b27f",
    formatId: "E007",
    targetSkillId: null,
    contentRevision: "pilot-3",
    generatorRevision: "generator-3",
    instruction: "Choose the English meaning.",
    context: "Choose the meaning of the Danish headword.",
    prompt: {
      before: "", after: "", choices: ["book", "newspaper", "letter (written message)"], blocks: [], punctuation: "",
      cue: "avis", cueLanguage: "da", answerLanguage: "en",
    },
    lexicalTarget: { entryId: "L015", senseId: "L015-S01", facet: "meaning", direction: "da-en" },
    introduction: [],
  };
  const exerciseBodies: Array<Record<string, unknown>> = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({
      ...session,
      contentRevision: "pilot-3", generatorRevision: "generator-3", evaluationRevision: "evaluation-3",
    });
    if (path.endsWith("catalog")) return json(vocabularyCatalog);
    const body = JSON.parse(String(init?.body));
    exerciseBodies.push(body);
    return json(exerciseBodies.length === 1 ? exercise : vocabularyExercise);
  }));

  await renderPlayground();
  await screen.findByRole("button", { name: "læser" });
  await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));
  await userEvent.click(screen.getByRole("combobox", { name: "Exercise type" }));
  expect(screen.getAllByRole("option")).toHaveLength(8);
  await userEvent.click(screen.getByRole("option", { name: "Recognize the meaning" }));

  expect(await screen.findByText("Target: Meaning · Danish → English")).toBeVisible();
  expect(screen.getByText("avis")).toHaveAttribute("lang", "da");
  expect(screen.queryByText("G004")).not.toBeInTheDocument();
  expect(screen.queryByText(/L015(?:-S01)?/)).not.toBeInTheDocument();
  expect(exerciseBodies[1]).toEqual({
    formatId: "E007", seed: 20261107, index: 0,
    contentRevision: "pilot-3", generatorRevision: "generator-3",
  });
}

async function verifyWritingAndReadingCatalog() {
  const fourthCatalog = {
    ...catalog,
    contentRevision: "pilot-4", generatorRevision: "generator-4", evaluationRevision: "evaluation-4",
    formats: [
      { id: "E009", title: "Write a Danish sentence", targetSkillId: "G005" },
      { id: "E010", title: "Read a short text", targetSkillId: null,
        readingTask: { facet: "participant-action-complement" } },
    ],
  };
  const writingExercise = {
    ...exercise,
    id: "d6125de7-267d-4ca5-8e68-f7f3558d928b", formatId: "E009", targetSkillId: "G005",
    contentRevision: "pilot-4", generatorRevision: "generator-4",
    instruction: "Write one complete Danish statement.", context: "Use the introduced words to express the meaning.",
    prompt: {
      before: "", after: "", choices: [], blocks: [], punctuation: "", cue: "She reads a newspaper.",
      cueLanguage: "en", answerLanguage: "da", inputLabel: "Danish sentence",
    },
    introduction: [
      { title: "Noun phrase", body: "en avis", bodyLanguage: "da" },
      { title: "Finite verb", body: "læser", bodyLanguage: "da" },
      { title: "Pronoun", body: "hun", bodyLanguage: "da" },
    ],
    hint: "Begin with the subject, then use the finite verb.",
  };
  const readingExercise = {
    ...exercise,
    id: "9e9637f8-c290-45eb-ad22-8e8f281c885d", formatId: "E010", targetSkillId: null,
    contentRevision: "pilot-4", generatorRevision: "generator-4",
    instruction: "Read the short text and answer the question.", context: "Use the information stated in the text.",
    prompt: {
      before: "", after: "", blocks: [], punctuation: "", cue: "According to the text, what does she read?",
      cueLanguage: "en", answerLanguage: "en", choices: ["a book", "a newspaper", "a letter"],
      passage: ["Hun læser en avis.", "Han læser et brev.", "Hun har en bog."],
    },
    readingTarget: { facet: "participant-action-complement" }, introduction: [],
    hint: "Find the sentence with the requested participant and action.",
  };
  const exerciseBodies: Array<Record<string, unknown>> = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({
      ...session, contentRevision: "pilot-4", generatorRevision: "generator-4", evaluationRevision: "evaluation-4",
    });
    if (path.endsWith("catalog")) return json(fourthCatalog);
    if (path.endsWith("exercises")) {
      const body = JSON.parse(String(init?.body));
      exerciseBodies.push(body);
      return json(body.formatId === "E010" ? readingExercise : writingExercise);
    }
    return json(attempt);
  }));

  await renderPlayground();
  expect(await screen.findByText("She reads a newspaper.")).toHaveAttribute("lang", "en");
  expect(screen.getByLabelText("Danish sentence")).toHaveAttribute("lang", "da");
  expect(screen.queryByText("Hun læser en avis.")).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Learn this first" }));
  for (const reference of ["en avis", "læser", "hun"]) {
    expect(screen.getByText(reference, { exact: true })).toHaveAttribute("lang", "da");
  }
  await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));
  await userEvent.click(screen.getByRole("combobox", { name: "Exercise type" }));
  await userEvent.click(screen.getByRole("option", { name: "Read a short text" }));

  expect(await screen.findByText("Task: Reading · participant, action and object")).toBeVisible();
  expect(screen.getByRole("group", { name: "Reading passage" })).toHaveAttribute("lang", "da");
  expect(screen.getByText("According to the text, what does she read?")).toHaveAttribute("lang", "en");
  expect(screen.queryByText(/answerNounId|questionSubjectId/)).not.toBeInTheDocument();
  expect(exerciseBodies[1]).toEqual({
    formatId: "E010", seed: 20261107, index: 0,
    contentRevision: "pilot-4", generatorRevision: "generator-4",
  });
}

async function verifyWritingUncertainRecovery() {
  const writingCatalog = {
    ...catalog,
    contentRevision: "pilot-4", generatorRevision: "generator-4", evaluationRevision: "evaluation-4",
    formats: [{ id: "E009", title: "Write a Danish sentence", targetSkillId: "G005" }],
  };
  const writingExercise = {
    ...exercise,
    id: "35f6c122-3177-441e-bcf9-f0c82607b271", formatId: "E009", targetSkillId: "G005",
    contentRevision: "pilot-4", generatorRevision: "generator-4",
    instruction: "Write one complete Danish statement.", context: "Express the English meaning.",
    prompt: {
      before: "", after: "", choices: [], blocks: [], punctuation: "", cue: "She reads a newspaper.",
      cueLanguage: "en", answerLanguage: "da", inputLabel: "Danish sentence",
    },
    introduction: [], hint: "Begin with the subject.",
  };
  const attemptBodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({
      ...session, contentRevision: "pilot-4", generatorRevision: "generator-4", evaluationRevision: "evaluation-4",
    });
    if (path.endsWith("catalog")) return json(writingCatalog);
    if (path.endsWith("exercises")) return json(writingExercise);
    attemptBodies.push(String(init?.body));
    if (attemptBodies.length === 1) throw new TypeError("response connection closed");
    const submitted = JSON.parse(attemptBodies[1]!);
    return json({
      ...attempt, attemptId: submitted.attemptId, exerciseId: writingExercise.id,
      linguisticScope: "danish-sentence", responseConstraintStatus: "met", meaningSatisfaction: "met",
      completedSentence: "Hun læser en avis.",
    });
  }));
  await renderPlayground();
  const input = await screen.findByLabelText("Danish sentence");
  await userEvent.type(input, "Hun læser en avis.");
  await userEvent.click(screen.getByRole("button", { name: "Check answer" }));

  expect(await screen.findByText("The result is not confirmed")).toBeVisible();
  expect(input).toBeDisabled();
  expect(input).toHaveValue("Hun læser en avis.");
  for (const character of ["æ", "ø", "å"]) {
    expect(screen.getByRole("button", { name: `Insert ${character}` })).toBeDisabled();
  }
  await userEvent.click(screen.getByRole("button", { name: "Check the same attempt" }));
  expect(await screen.findByText("Meaning matched")).toBeVisible();
  expect(attemptBodies[1]).toBe(attemptBodies[0]);
  expect(JSON.parse(attemptBodies[1]!)).toMatchObject({
    exerciseId: writingExercise.id,
    answer: { kind: "text", text: "Hun læser en avis." },
    hintUsed: false,
  });
}

describe("Playground", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it("offers a reconnect action when the initial engine session is unavailable", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => { throw new TypeError("offline"); }));
    await renderPlayground();

    expect(await screen.findByText("The learning engine could not be reached. Check that the server is running.")).toBeVisible();
    await userEvent.click(screen.getByRole("button", { name: "Try connection again" }));
  });

  it("establishes the session before protected reads and offers teaching before the prompt", async () => {
    const paths: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      paths.push(path);
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      return json(exercise);
    }));

    await renderPlayground();

    expect(await screen.findByText("Learn this first")).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "Learn this first" })).not.toBeInTheDocument();
    expect(screen.getByText(/Using it records supported practice/)).toBeVisible();
    await userEvent.click(screen.getByRole("button", { name: "Learn this first" }));
    expect(screen.getByText("læse · to read")).toBeVisible();
    expect(screen.getByRole("heading", { name: "Choose the present-tense verb." })).toBeVisible();
    expect(paths).toEqual([
      "/api/v1/development-session", "/api/v1/catalog", "/api/v1/exercises",
    ]);
    expect(screen.getByText("99 sentence combinations")).toBeVisible();
  });

  it("renders all six current exercise formats from the catalog", verifyCurrentCatalogFormats);

  it("renders the eight-format catalog and omits a grammar selector for vocabulary generation", verifyVocabularyCatalog);

  it("renders standalone writing and reading formats with distinct language semantics", verifyWritingAndReadingCatalog);

  it("submits repair text unsupported, then keeps lesson support sticky for a retry", verifyRepairSupportTracking);

  it("freezes repair input and its character controls while recovering an uncertain attempt", verifyRepairUncertainRecovery);

  it("freezes full-sentence writing and replays the exact pending attempt", verifyWritingUncertainRecovery);

  it("freezes the answer and replays the exact payload after an uncertain submission", async () => {
    const attemptBodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("exercises")) return json(exercise);
      attemptBodies.push(String(init?.body));
      if (attemptBodies.length === 1) throw new TypeError("response connection closed");
      const submitted = JSON.parse(attemptBodies[1]!);
      return json({ ...attempt, attemptId: submitted.attemptId, hintUsed: submitted.hintUsed });
    }));
    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "Learn this first" }));
    const choice = await screen.findByRole("button", { name: "læser" });
    await userEvent.click(choice);
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));

    expect(await screen.findByText("The result is not confirmed")).toBeVisible();
    const uncertainAlert = screen.getByRole("alert");
    await waitFor(() => expect(uncertainAlert).toHaveFocus());
    expect(choice).toBeDisabled();
    const replay = screen.getByRole("button", { name: "Check the same attempt" });
    await userEvent.tab();
    expect(replay).toHaveFocus();
    await userEvent.click(replay);

    const feedback = await screen.findByRole("status");
    await waitFor(() => expect(feedback).toHaveFocus());
    expect(attemptBodies).toHaveLength(2);
    expect(attemptBodies[1]).toBe(attemptBodies[0]);
    expect(JSON.parse(attemptBodies[1]!)).toMatchObject({ hintUsed: true });
    expect(screen.getByText(/Supported practice/)).toBeVisible();
  });

  it("keeps the timed-out attempt frozen and replays its exact supported payload", async () => {
    const attemptBodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("exercises")) return json(exercise);
      attemptBodies.push(String(init?.body));
      if (attemptBodies.length === 1) {
        return new Promise<Response>((_resolve, reject) => {
          init?.signal?.addEventListener("abort", () => {
            reject(new DOMException("The operation was aborted.", "AbortError"));
          }, { once: true });
        });
      }
      const submitted = JSON.parse(attemptBodies[1]!);
      return json({ ...attempt, attemptId: submitted.attemptId, hintUsed: submitted.hintUsed });
    }));
    await renderPlayground();
    const choice = await screen.findByRole("button", { name: "læser" });
    await userEvent.click(screen.getByRole("button", { name: "Show hint" }));
    expect(screen.getByText(exercise.hint)).toBeVisible();
    await userEvent.click(choice);
    await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));

    vi.useFakeTimers();
    fireEvent.click(screen.getByRole("button", { name: "Check answer" }));
    expect(choice).toBeDisabled();
    await act(async () => vi.advanceTimersByTimeAsync(15_000));
    vi.useRealTimers();

    expect(await screen.findByText("The result is not confirmed")).toBeVisible();
    const replay = screen.getByRole("button", { name: "Check the same attempt" });
    expect(replay).toBeEnabled();
    expect(choice).toBeDisabled();
    expect(screen.getByRole("combobox", { name: "Exercise type" })).toBeDisabled();
    expect(screen.getByText(exercise.hint)).toBeVisible();
    expect(JSON.parse(attemptBodies[0]!)).toMatchObject({
      exerciseId: exercise.id,
      answer: { kind: "text", text: "læser" },
      hintUsed: true,
    });

    await userEvent.click(replay);
    expect(await screen.findByRole("status")).toBeVisible();
    expect(attemptBodies).toHaveLength(2);
    expect(attemptBodies[1]).toBe(attemptBodies[0]);
    expect(screen.getByText("Try 1 · Supported practice", { exact: true })).toBeVisible();
  });

  it("keeps the displayed exercise and selector consistent when generation fails", async () => {
    let exerciseRequests = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      exerciseRequests += 1;
      if (exerciseRequests === 1) return json(exercise);
      return Response.json(
        { detail: "The requested exercise could not be generated." },
        { status: 409, headers: { "Content-Type": "application/problem+json" } },
      );
    }));
    await renderPlayground();
    await screen.findByRole("button", { name: "læser" });
    await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));
    const selector = screen.getByRole("combobox", { name: "Exercise type" });
    await userEvent.click(selector);
    await userEvent.click(screen.getByRole("option", { name: "Supply the indefinite article" }));

    expect(await screen.findByText("The engine revision changed. Generate a new exercise.")).toBeVisible();
    const alert = screen.getByRole("alert");
    const reload = screen.getByRole("button", { name: "Reload engine" });
    expect(reload).toBeVisible();
    expect(screen.getByRole("button", { name: "læser" })).toBeVisible();
    const liveSelector = screen.getByRole("combobox", { name: "Exercise type" });
    expect(liveSelector).toBe(selector);
    expect(liveSelector).toHaveValue("Choose the present form");
    await waitFor(() => expect(alert).toHaveFocus());
    await userEvent.tab();
    expect(reload).toHaveFocus();
  });

  it("commits the auto-highlighted exact search with Enter", () => verifyKeyboardFormatSelection(false));

  it("commits the exact searched format with ArrowDown and Enter", () => verifyKeyboardFormatSelection(true));

  it("dismisses the active format on Enter without regenerating", () =>
    verifyKeyboardFormatSelection(false, "dismiss-current"));

  it("selects a different format after a rapid blur and reopen", () =>
    verifyKeyboardFormatSelection(false, "select-different"));

  it("offers session reconnection when a protected call reports an expired session", async () => {
    let exerciseRequests = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      exerciseRequests += 1;
      if (exerciseRequests === 1) return json(exercise);
      return Response.json(
        { detail: "The session is no longer valid." },
        { status: 401, headers: { "Content-Type": "application/problem+json" } },
      );
    }));
    await renderPlayground();
    await screen.findByRole("button", { name: "læser" });
    await userEvent.click(screen.getByRole("button", { name: "Generation controls" }));
    await userEvent.click(screen.getByRole("combobox", { name: "Exercise type" }));
    await userEvent.click(screen.getByRole("option", { name: "Supply the indefinite article" }));

    expect(await screen.findByText("Your learning session expired. Reconnect to continue.")).toBeVisible();
    const reconnect = screen.getByRole("button", { name: "Reconnect session" });
    expect(reconnect).toBeVisible();
    expect(screen.getByRole("button", { name: "læser" })).toBeVisible();
    await userEvent.click(reconnect);
  });

  it("focuses a known submission error before its recovery action", verifyKnownSubmissionErrorFocus);

  it("retries the same next index until generation succeeds", async () => {
    const generatedIndexes: number[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("attempts")) return json(attempt);
      const body = JSON.parse(String(init?.body));
      generatedIndexes.push(body.index);
      if (generatedIndexes.length === 1) return json(exercise);
      if (generatedIndexes.length === 2) {
        return Response.json(
          { detail: "The requested exercise could not be generated." },
          { status: 409, headers: { "Content-Type": "application/problem+json" } },
        );
      }
      return json({ ...exercise, id: "f640dc3e-b2fb-4d6e-920f-6a3b8427beaf" });
    }));
    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));
    await screen.findByRole("status");

    await userEvent.click(screen.getByRole("button", { name: "Next exercise" }));
    await screen.findByText("The engine revision changed. Generate a new exercise.");
    await userEvent.click(screen.getByRole("button", { name: "Next exercise" }));

    await waitFor(() => expect(generatedIndexes).toEqual([0, 1, 1]));
    expect(screen.getByRole("button", { name: "Check answer" })).toBeVisible();
  });

  it("collapses lesson support and records a fresh unsupported attempt after Next", async () => {
    const attemptSupport: boolean[] = [];
    let exerciseRequest = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("attempts")) {
        const submitted = JSON.parse(String(init?.body));
        attemptSupport.push(submitted.hintUsed);
        return json({ ...attempt, attemptId: submitted.attemptId, exerciseId: submitted.exerciseId, hintUsed: submitted.hintUsed });
      }
      exerciseRequest += 1;
      return json(exerciseRequest === 1 ? exercise : { ...exercise, id: "a812da31-dfb7-4f87-90c3-878306f7f84a" });
    }));
    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "Learn this first" }));
    await userEvent.click(screen.getByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));
    await screen.findByRole("status");
    await userEvent.click(screen.getByRole("button", { name: "Next exercise" }));

    const lesson = await screen.findByRole("button", { name: "Learn this first" });
    expect(lesson).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("region", { name: "Learn this first" })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));
    await waitFor(() => expect(attemptSupport).toEqual([true, false]));
  });

  it("allows only one in-flight Next generation and disables retry actions", async () => {
    let finishNext: ((response: Response) => void) | undefined;
    const nextResponse = new Promise<Response>((resolve) => { finishNext = resolve; });
    let exerciseRequests = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      if (path.endsWith("development-session")) return json(session);
      if (path.endsWith("catalog")) return json(catalog);
      if (path.endsWith("attempts")) return json(attempt);
      exerciseRequests += 1;
      return exerciseRequests === 1 ? json(exercise) : nextResponse;
    }));
    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check answer" }));
    await screen.findByRole("status");
    const next = screen.getByRole("button", { name: "Next exercise" });
    await userEvent.dblClick(next);

    expect(exerciseRequests).toBe(2);
    expect(next).toBeDisabled();
    expect(screen.getByRole("button", { name: "Try again" })).toBeDisabled();
    finishNext?.(json({ ...exercise, id: "7a58bf73-af2f-453c-9277-ab983d34f472" }));
    await screen.findByRole("button", { name: "Check answer" });
  });
});
