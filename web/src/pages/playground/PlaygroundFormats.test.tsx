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

async function verifyTileSelectionRecovery() {
  const fifthSession = {
    ...session, contentRevision: "pilot-5", generatorRevision: "generator-5", evaluationRevision: "evaluation-5",
  };
  const fifthCatalog = {
    ...catalog,
    contentRevision: "pilot-5", generatorRevision: "generator-5", evaluationRevision: "evaluation-5",
    formats: [{ id: "E013", title: "Choose the present form with word tiles", targetSkillId: "G004" }],
  };
  const tileExercise = {
    ...exercise,
    id: "7c24a8d9-07a5-4fa5-b464-a435145634a4",
    formatId: "E013", targetSkillId: "G004",
    contentRevision: "pilot-5", generatorRevision: "generator-5",
    instruction: "Build the Danish sentence and leave the infinitive unused.",
    context: "Choose the present verb form for an action happening now.",
    prompt: {
      before: "", after: "", choices: [], punctuation: ".", cue: "She reads a letter.",
      cueLanguage: "en", answerLanguage: "da", tileMode: "with-spares", requiredTileCount: 4,
      blocks: [
        { id: "b0", text: "hun" }, { id: "b1", text: "læser" }, { id: "b2", text: "et" },
        { id: "b3", text: "brev" }, { id: "b4", text: "læse" },
      ],
    },
    introduction: [],
  };
  const attemptBodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(fifthSession);
    if (path.endsWith("catalog")) return json(fifthCatalog);
    if (path.endsWith("exercises")) return json(tileExercise);
    attemptBodies.push(String(init?.body));
    if (attemptBodies.length === 1) throw new TypeError("response connection closed");
    const submitted = JSON.parse(attemptBodies[1]!);
    return json({
      ...attempt,
      attemptId: submitted.attemptId,
      exerciseId: tileExercise.id,
      targetSatisfaction: "unevaluable",
      meaningSatisfaction: "unknown",
      responseConstraintStatus: "not-met",
      linguisticStatus: "unknown",
      linguisticScope: "danish-sentence",
      evaluationRevision: "evaluation-5",
      feedback: "Build one complete proposition from the supplied tiles.",
      completedSentence: null,
    });
  }));

  await renderPlayground();
  await userEvent.click(await screen.findByRole("button", { name: "hun" }));
  const check = screen.getByRole("button", { name: "Check answer" });
  expect(check).toBeEnabled();
  await userEvent.click(check);

  expect(await screen.findByText("The result is not confirmed")).toBeVisible();
  expect(screen.getByRole("button", { name: "Remove hun from position 1" })).toBeDisabled();
  await userEvent.click(screen.getByRole("button", { name: "Check the same attempt" }));

  expect(await screen.findByText("Response constraint to revisit")).toBeVisible();
  expect(screen.queryByText("One statement required")).not.toBeInTheDocument();
  expect(attemptBodies[1]).toBe(attemptBodies[0]);
  expect(JSON.parse(attemptBodies[1]!)).toMatchObject({
    exerciseId: tileExercise.id,
    answer: { kind: "order", blockIds: ["b0"] },
  });
}

async function verifyEnglishToDanishChoice() {
  const fifthSession = {
    ...session, contentRevision: "pilot-5", generatorRevision: "generator-5", evaluationRevision: "evaluation-5",
  };
  const fifthCatalog = {
    ...catalog,
    contentRevision: "pilot-5", generatorRevision: "generator-5", evaluationRevision: "evaluation-5",
    formats: [{
      id: "E011", title: "Choose the Danish word", targetSkillId: null,
      lexicalTask: { facet: "meaning", direction: "en-da" },
    }],
  };
  const choiceExercise = {
    ...exercise,
    id: "fcda0d37-8863-4508-910b-22cf381061bc",
    formatId: "E011", targetSkillId: null,
    contentRevision: "pilot-5", generatorRevision: "generator-5",
    instruction: "Choose the Danish word.", context: "Choose the article and noun that match the English meaning.",
    prompt: {
      before: "", after: "", blocks: [], punctuation: "", cue: "magazine",
      cueLanguage: "en", answerLanguage: "da", choices: ["en bog", "en avis", "et brev", "et magasin"],
    },
    lexicalTarget: { entryId: "L024", senseId: "L024-S01", facet: "meaning", direction: "en-da" },
    introduction: [],
  };
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json(fifthSession);
    if (path.endsWith("catalog")) return json(fifthCatalog);
    return json(choiceExercise);
  }));

  await renderPlayground();
  expect(await screen.findByText("Target: Meaning · English → Danish")).toBeVisible();
  expect(screen.getByText("magazine")).toHaveAttribute("lang", "en");
  expect(screen.getByText("et magasin")).toHaveAttribute("lang", "da");
  expect(screen.queryByText(/L024/)).not.toBeInTheDocument();
}

async function verifySuppliedConstituentOrder() {
  const tenthSession = {
    ...session, contentRevision: "pilot-10", generatorRevision: "generator-10", evaluationRevision: "evaluation-10",
  };
  const tenthCatalog = {
    ...catalog,
    contentRevision: "pilot-10", generatorRevision: "generator-10", evaluationRevision: "evaluation-10",
    formats: [{ id: "E024", title: "Arrange supplied sentence parts", targetSkillId: "G005" }],
  };
  const orderExercise = {
    ...exercise,
    id: "87777777-7777-4777-8777-777777777777", formatId: "E024", targetSkillId: "G005",
    targetScopeId: "subject-first-supplied", contentRevision: "pilot-10", generatorRevision: "generator-10",
    instruction: "Build the supplied statement.", context: "Begin with the subject and put the finite verb second.",
    prompt: {
      before: "", after: "", choices: [], punctuation: ".", cue: "He reads a book.",
      cueLanguage: "en", answerLanguage: "da", blocks: [
        { id: "s", text: "han" }, { id: "v", text: "læser" }, { id: "o", text: "en bog" },
      ],
    },
    introduction: [],
  };
  let attemptBody = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(tenthSession);
    if (path.endsWith("catalog")) return json(tenthCatalog);
    if (path.endsWith("exercises")) return json(orderExercise);
    attemptBody = String(init?.body);
    return json({
      ...attempt, exerciseId: orderExercise.id, targetSatisfaction: "met", meaningSatisfaction: "met",
      responseConstraintStatus: "met", linguisticStatus: "acceptable", linguisticScope: "danish-sentence",
      evaluationRevision: "evaluation-10", completedSentence: "Han læser en bog.",
    });
  }));

  await renderPlayground();
  await userEvent.click(await screen.findByRole("button", { name: "han" }));
  const check = screen.getByRole("button", { name: "Check answer" });
  await userEvent.click(screen.getByRole("button", { name: "læser" }));
  expect(check).toBeDisabled();
  await userEvent.click(screen.getByRole("button", { name: "en bog" }));
  expect(check).toBeEnabled();
  await userEvent.click(check);

  expect(JSON.parse(attemptBody)).toMatchObject({ answer: { kind: "order", blockIds: ["s", "v", "o"] } });
  expect(await screen.findByText("Good work")).toBeVisible();
}

async function verifyFinalCircumstanceOrder() {
  const fifteenthSession = {
    ...session, contentRevision: "pilot-15", generatorRevision: "generator-15", evaluationRevision: "evaluation-15",
  };
  const fifteenthCatalog = {
    ...catalog,
    contentRevision: "pilot-15", generatorRevision: "generator-15", evaluationRevision: "evaluation-15",
    formats: [{ id: "E029", title: "Add a supplied final circumstance", targetSkillId: "G016" }],
  };
  const orderExercise = {
    ...exercise,
    id: "99999999-9999-4999-8999-999999999999", formatId: "E029", targetSkillId: "G016",
    targetScopeId: "one-final-circumstance", contentRevision: "pilot-15", generatorRevision: "generator-15",
    instruction: "Keep the supplied clause core in order and add the supplied circumstance at the end.",
    context: "Supplied clause: I work. Supplied circumstance: today.",
    prompt: {
      before: "", after: "", choices: [], punctuation: ".", cue: "Jeg arbejder.",
      cueLanguage: "da", answerLanguage: "da", blocks: [
        { id: "s", text: "jeg" }, { id: "v", text: "arbejder" }, { id: "c", text: "i dag" },
      ],
    },
    introduction: [],
  };
  const attemptBodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(fifteenthSession);
    if (path.endsWith("catalog")) return json(fifteenthCatalog);
    if (path.endsWith("exercises")) return json(orderExercise);
    attemptBodies.push(String(init?.body));
    if (attemptBodies.length === 1) throw new TypeError("response connection closed");
    const request = JSON.parse(attemptBodies[1]!);
    return json({
      ...attempt, attemptId: request.attemptId, exerciseId: orderExercise.id,
      targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met",
      linguisticStatus: "acceptable", linguisticScope: "danish-sentence",
      evaluationRevision: "evaluation-15", feedback: "The supplied circumstance is final.",
      completedSentence: "Jeg arbejder i dag.",
    });
  }));

  await renderPlayground();
  const check = await screen.findByRole("button", { name: "Check answer" });
  await userEvent.click(screen.getByRole("button", { name: "jeg" }));
  await userEvent.click(screen.getByRole("button", { name: "arbejder" }));
  expect(check).toBeDisabled();
  await userEvent.click(screen.getByRole("button", { name: "i dag" }));
  expect(check).toBeEnabled();
  await userEvent.click(check);

  expect(await screen.findByText("The result is not confirmed")).toBeVisible();
  expect(screen.getByRole("button", { name: "Remove i dag from position 3" })).toBeDisabled();
  await userEvent.click(screen.getByRole("button", { name: "Check the same attempt" }));

  expect(await screen.findByText("Good work", { exact: true })).toBeVisible();
  expect(attemptBodies[1]).toBe(attemptBodies[0]);
  expect(JSON.parse(attemptBodies[1]!)).toMatchObject({
    exerciseId: orderExercise.id,
    answer: { kind: "order", blockIds: ["s", "v", "c"] },
  });
}

async function verifyNeutralQuestionOrder() {
  const sixteenthSession = {
    ...session, contentRevision: "pilot-16", generatorRevision: "generator-16", evaluationRevision: "evaluation-16",
  };
  const sixteenthCatalog = {
    ...catalog,
    contentRevision: "pilot-16", generatorRevision: "generator-16", evaluationRevision: "evaluation-16",
    formats: [{ id: "E030", title: "Arrange a neutral yes or no question", targetSkillId: "G018" }],
  };
  const orderExercise = {
    ...exercise,
    id: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", formatId: "E030", targetSkillId: "G018",
    targetScopeId: "neutral-positive-yes-no", contentRevision: "pilot-16", generatorRevision: "generator-16",
    instruction: "Turn the supplied positive statement into a neutral yes/no question.",
    context: "The subject is more than one addressee. English question: Do you all live in Aarhus?",
    prompt: {
      before: "", after: "", choices: [], punctuation: "?", cue: "I bor i Aarhus.",
      cueLanguage: "da", answerLanguage: "da", blocks: [
        { id: "v", text: "bor" }, { id: "s", text: "I" }, { id: "l", text: "i Aarhus" },
      ],
    },
    introduction: [],
  };
  const attemptBodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json(sixteenthSession);
    if (path.endsWith("catalog")) return json(sixteenthCatalog);
    if (path.endsWith("exercises")) return json(orderExercise);
    attemptBodies.push(String(init?.body));
    if (attemptBodies.length === 1) throw new TypeError("response connection closed");
    const request = JSON.parse(attemptBodies[1]!);
    return json({
      ...attempt, attemptId: request.attemptId, exerciseId: orderExercise.id,
      targetSatisfaction: "met", meaningSatisfaction: "met", responseConstraintStatus: "met",
      linguisticStatus: "acceptable", linguisticScope: "danish-sentence",
      evaluationRevision: "evaluation-16", feedback: "The finite verb is first and the subject is second.",
      completedSentence: "Bor I i Aarhus?",
    });
  }));

  await renderPlayground();
  const check = await screen.findByRole("button", { name: "Check answer" });
  await userEvent.click(screen.getByRole("button", { name: "bor" }));
  await userEvent.click(screen.getByRole("button", { name: "I" }));
  expect(check).toBeDisabled();
  await userEvent.click(screen.getByRole("button", { name: "i Aarhus" }));
  expect(check).toBeEnabled();
  expect(screen.getByLabelText("Your sentence", { exact: true })).toHaveTextContent("BorIi Aarhus?");
  await userEvent.click(check);

  expect(await screen.findByText("The result is not confirmed")).toBeVisible();
  expect(screen.getByRole("button", { name: "Remove I from position 2" })).toBeDisabled();
  await userEvent.click(screen.getByRole("button", { name: "Check the same attempt" }));

  expect(await screen.findByText("Good work", { exact: true })).toBeVisible();
  expect(attemptBodies[1]).toBe(attemptBodies[0]);
  expect(JSON.parse(attemptBodies[1]!)).toMatchObject({
    exerciseId: orderExercise.id,
    answer: { kind: "order", blockIds: ["v", "s", "l"] },
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

  it("submits a nonempty tile subset and replays the exact pending order", verifyTileSelectionRecovery);

  it("renders English-to-Danish meaning choice without exposing lexical identities", verifyEnglishToDanishChoice);

  it("requires every supplied constituent before submitting standalone subject-first order", verifySuppliedConstituentOrder);

  it("submits and exactly replays a complete standalone final-circumstance order", verifyFinalCircumstanceOrder);

  it("submits and exactly replays a complete standalone neutral-question order", verifyNeutralQuestionOrder);

  it("commits the auto-highlighted exact search with Enter", () => verifyKeyboardFormatSelection(false));

  it("commits the exact searched format with ArrowDown and Enter", () => verifyKeyboardFormatSelection(true));

  it("dismisses the active format on Enter without regenerating", () =>
    verifyKeyboardFormatSelection(false, "dismiss-current"));

  it("selects a different format after a rapid blur and reopen", () =>
    verifyKeyboardFormatSelection(false, "select-different"));
});
