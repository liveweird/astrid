import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { session, catalog, exercise, attempt, json, renderPlayground } from "../../test/playgroundFixtures";

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

describe("Playground formats", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it("renders all six current exercise formats from the catalog", verifyCurrentCatalogFormats);

  it("renders the eight-format catalog and omits a grammar selector for vocabulary generation", verifyVocabularyCatalog);

  it("renders standalone writing and reading formats with distinct language semantics", verifyWritingAndReadingCatalog);

  it("commits the auto-highlighted exact search with Enter", () => verifyKeyboardFormatSelection(false));

  it("commits the exact searched format with ArrowDown and Enter", () => verifyKeyboardFormatSelection(true));

  it("dismisses the active format on Enter without regenerating", () =>
    verifyKeyboardFormatSelection(false, "dismiss-current"));

  it("selects a different format after a rapid blur and reopen", () =>
    verifyKeyboardFormatSelection(false, "select-different"));
});
