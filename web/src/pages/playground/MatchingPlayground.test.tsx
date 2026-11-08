import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { attempt, catalog, exercise, json, renderPlayground, session } from "../../test/playgroundFixtures";

const matchingExercise = {
  ...exercise,
  id: "9319cce0-328b-4758-bdaa-b34f2431a915",
  formatId: "E014",
  targetSkillId: null,
  contentRevision: "pilot-6",
  generatorRevision: "generator-6",
  instruction: "Match English and Danish words.",
  context: "Connect each English meaning to a supplied Danish noun phrase.",
  lexicalTargets: [
    { entryId: "L014", senseId: "L014-S01", facet: "meaning", direction: "en-da" },
    { entryId: "L015", senseId: "L015-S01", facet: "meaning", direction: "en-da" },
  ],
  prompt: {
    before: "", after: "", choices: [], blocks: [], punctuation: "",
    cueLanguage: "en", answerLanguage: "da",
    matchingColumns: {
      left: [{ id: "l0", text: "book" }, { id: "l1", text: "newspaper" }],
      right: [{ id: "r0", text: "en avis" }, { id: "r1", text: "en bog" }],
    },
  },
  introduction: [],
};

const matchingCatalog = {
  ...catalog,
  contentRevision: "pilot-6",
  generatorRevision: "generator-6",
  evaluationRevision: "evaluation-6",
  formats: [{
    id: "E014", title: "Match English and Danish words", targetSkillId: null,
    lexicalTask: { facet: "meaning", direction: "en-da" },
  }],
};

describe("matching playground", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("replays the exact pair order after response loss and retains editable matches for feedback retry", async () => {
    const bodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json({
        ...session, contentRevision: "pilot-6", generatorRevision: "generator-6", evaluationRevision: "evaluation-6",
      });
      if (path.endsWith("catalog")) return json(matchingCatalog);
      if (path.endsWith("exercises")) return json(matchingExercise);
      bodies.push(String(init?.body));
      if (bodies.length === 1) throw new TypeError("response connection closed");
      const submitted = JSON.parse(bodies.at(-1)!);
      return json({
        ...attempt,
        attemptId: submitted.attemptId,
        exerciseId: matchingExercise.id,
        targetSatisfaction: "not-met",
        meaningSatisfaction: "not-met",
        responseConstraintStatus: "not-met",
        linguisticStatus: "acceptable",
        linguisticScope: "danish-matching",
        evaluationRevision: "evaluation-6",
        retryNumber: bodies.length - 1,
        completedSentence: "",
        feedback: "Review the selected meanings and complete the board.",
        matchingResults: [
          {
            leftId: "l0", rightId: "r0", lexicalTarget: matchingExercise.lexicalTargets[0],
            targetSatisfaction: "not-met", meaningSatisfaction: "not-met",
            responseConstraintStatus: "met", linguisticStatus: "acceptable",
            feedback: "This Danish phrase has a different meaning.",
          },
          {
            leftId: "l1", lexicalTarget: matchingExercise.lexicalTargets[1],
            targetSatisfaction: "unevaluable", meaningSatisfaction: "unknown",
            responseConstraintStatus: "not-met", linguisticStatus: "unknown",
            feedback: "Choose a Danish match for this meaning.",
          },
        ],
      });
    }));

    await renderPlayground();
    await userEvent.click(await screen.findByRole("button", { name: "book" }));
    await userEvent.click(screen.getByRole("button", { name: "en avis" }));
    await userEvent.click(screen.getByRole("button", { name: "Check matches" }));
    expect(await screen.findByText("The result is not confirmed")).toBeVisible();
    expect(screen.getByRole("button", { name: "Remove match for book" })).toBeDisabled();

    await userEvent.click(screen.getByRole("button", { name: "Check the same attempt" }));
    const feedback = await screen.findByRole("group", { name: "Matching feedback" });
    expect(feedback).toHaveTextContent("book → en avis");
    expect(feedback).toHaveTextContent("newspaper — no match selected");
    expect(bodies[1]).toBe(bodies[0]);
    expect(JSON.parse(bodies[0]!)).toMatchObject({
      answer: { kind: "matching", pairs: [{ leftId: "l0", rightId: "r0" }] },
    });

    await userEvent.click(screen.getByRole("button", { name: "Try again" }));
    await waitFor(() => expect(screen.getByRole("group", { name: "Match English and Danish words" })).toHaveFocus());
    expect(screen.getByRole("group", { name: "Chosen matches" })).toHaveTextContent("book→en avis");
    expect(screen.getByRole("button", { name: "Remove match for book" })).toBeEnabled();
  });
});
