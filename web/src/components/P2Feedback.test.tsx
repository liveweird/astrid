import { MantineProvider } from "@mantine/core";
import { render, screen } from "@testing-library/react";
import type { Attempt, Exercise } from "../api/models";
import FeedbackPanel from "./FeedbackPanel";

it("describes an honest two-pair P2 board without implying four matches", () => {
  const attempt = {
    attemptId: "attempt-p2", exerciseId: "p2-match", targetSatisfaction: "unevaluable",
    meaningSatisfaction: "unknown", linguisticStatus: "acceptable", linguisticScope: "danish-matching",
    responseConstraintStatus: "not-met", issues: [], feedback: "Complete every pair.", completedSentence: "",
    evaluationRevision: "evaluation-19", hintUsed: false, retryNumber: 1, createdAt: 0,
    matchingResults: [{
      leftId: "cup", rightId: "kop", targetSatisfaction: "met", meaningSatisfaction: "met",
      linguisticStatus: "acceptable", feedback: "This pair matches.",
    }],
  } as unknown as Attempt;
  const exercise = {
    id: "p2-match", formatId: "E042", targetSkillId: "G005", supportSkillIds: [],
    contentRevision: "pilot-19", generatorRevision: "generator-19", instruction: "Match.", context: "Food.",
    prompt: { before: "", after: "", choices: [], blocks: [], punctuation: "", matchingColumns: {
      left: [{ id: "cup", text: "a cup" }, { id: "bottle", text: "a bottle" }],
      right: [{ id: "kop", text: "en kop" }, { id: "flaske", text: "en flaske" }],
    } }, introduction: [], hint: "Match the heads.",
  } as unknown as Exercise;
  render(<MantineProvider env="test"><FeedbackPanel attempt={attempt} exercise={exercise} /></MantineProvider>);
  expect(screen.getByText("Two lexical meanings")).toBeVisible();
  expect(screen.getByText("Supplied Danish words and expressions")).toBeVisible();
  expect(screen.getByText("Complete two matches")).toBeVisible();
  expect(screen.queryByText(/Four/)).not.toBeInTheDocument();
});
