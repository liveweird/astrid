import { MantineProvider } from "@mantine/core";
import { render, screen } from "@testing-library/react";
import type { Attempt, Exercise } from "../api/models";
import FeedbackPanel from "./FeedbackPanel";

it("maps server matching feedback to the displayed English cue and selected Danish phrase", () => {
  const matching: Extract<Attempt, { linguisticScope: "danish-matching" }> = {
    attemptId: "attempt-1", exerciseId: "exercise-1", issues: [], createdAt: 0,
    targetSatisfaction: "not-met", meaningSatisfaction: "not-met", linguisticStatus: "acceptable",
    linguisticScope: "danish-matching", responseConstraintStatus: "not-met", completedSentence: "",
    retryNumber: 2, hintUsed: false, evaluationRevision: "evaluation-6",
    feedback: "Review the selected meanings and complete the board.",
    matchingResults: [
      {
        leftId: "l0", rightId: "r1",
        lexicalTarget: { entryId: "L014", senseId: "L014-S01", facet: "meaning", direction: "en-da" },
        targetSatisfaction: "not-met", meaningSatisfaction: "not-met",
        responseConstraintStatus: "met", linguisticStatus: "acceptable",
        feedback: "This Danish phrase has a different meaning.",
      },
      {
        leftId: "l1",
        lexicalTarget: { entryId: "L015", senseId: "L015-S01", facet: "meaning", direction: "en-da" },
        targetSatisfaction: "unevaluable", meaningSatisfaction: "unknown",
        responseConstraintStatus: "not-met", linguisticStatus: "unknown",
        feedback: "Choose a Danish match for this meaning.",
      },
    ],
  };
  const exercise: Extract<Exercise, { formatId: "E014" }> = {
    id: "exercise-1", formatId: "E014", targetSkillId: null,
    lexicalTargets: [
      { entryId: "L014", senseId: "L014-S01", facet: "meaning", direction: "en-da" },
      { entryId: "L015", senseId: "L015-S01", facet: "meaning", direction: "en-da" },
      { entryId: "L018", senseId: "L018-S01", facet: "meaning", direction: "en-da" },
      { entryId: "L024", senseId: "L024-S01", facet: "meaning", direction: "en-da" },
    ],
    supportSkillIds: [], contentRevision: "pilot-6", generatorRevision: "generator-6",
    instruction: "Match English and Danish words.", context: "Match the meanings.",
    prompt: {
      matchingColumns: {
        left: [{ id: "l0", text: "book" }, { id: "l1", text: "newspaper" }],
        right: [{ id: "r0", text: "en bog" }, { id: "r1", text: "en avis" }],
      },
      before: "", after: "", choices: [], blocks: [], punctuation: "", cueLanguage: "en", answerLanguage: "da",
    },
    introduction: [], hint: "Match each meaning.",
  };

  render(<MantineProvider env="test"><FeedbackPanel attempt={matching} exercise={exercise} /></MantineProvider>);

  expect(screen.getByText("Two lexical meanings")).toBeVisible();
  expect(screen.getByText("Supplied Danish noun phrases")).toBeVisible();
  expect(screen.getByText("Complete two matches")).toBeVisible();
  expect(screen.getByRole("group", { name: "Match for book" })).toHaveTextContent("book → en avis");
  expect(screen.getByRole("group", { name: "Match for newspaper" })).toHaveTextContent("newspaper — no match selected");
  expect(screen.getByText("Retry after feedback", { exact: true })).toBeVisible();
  expect(screen.getByText("Try 2 · No support opened")).toBeVisible();
  expect(screen.queryByText("Good work")).not.toBeInTheDocument();
});
