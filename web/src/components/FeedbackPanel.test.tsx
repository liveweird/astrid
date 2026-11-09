import { MantineProvider } from "@mantine/core";
import { render, screen } from "@testing-library/react";
import FeedbackPanel from "./FeedbackPanel";
import type { Attempt } from "../api/models";

const base: Attempt = {
  attemptId: "attempt-1",
  exerciseId: "exercise-1",
  targetSatisfaction: "not-met",
  linguisticStatus: "unknown",
  issues: [],
  feedback: "This did not exercise the requested target.",
  completedSentence: "En bog har jeg.",
  evaluationRevision: "evaluation-1",
  hintUsed: false,
  retryNumber: 1,
  createdAt: 0,
};

describe("FeedbackPanel", () => {
  it("separates target performance from an unknown linguistic status", () => {
    render(<MantineProvider env="test"><FeedbackPanel attempt={base} /></MantineProvider>);
    expect(screen.getByText("Target to revisit")).toBeInTheDocument();
    expect(screen.getByText("Language status unknown")).toBeInTheDocument();
    expect(screen.getByText(/not a general judgment about Danish/i)).toBeInTheDocument();
    expect(screen.getByText("En bog har jeg.")).toHaveAttribute("lang", "da");
  });

  it("labels target satisfaction and acceptable Danish independently", () => {
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{ ...base, targetSatisfaction: "met", linguisticStatus: "acceptable" }} />
      </MantineProvider>,
    );
    expect(screen.getByText("Target met")).toBeInTheDocument();
    expect(screen.getByText("Acceptable Danish")).toBeInTheDocument();
  });

  it("recognizes an English choice without making a Danish disclaimer", () => {
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{
          ...base,
          linguisticScope: "english-choice",
          responseConstraintStatus: "met",
          linguisticStatus: "acceptable",
        }} />
      </MantineProvider>,
    );
    expect(screen.getByText("Recognized English choice")).toBeVisible();
    expect(screen.queryByText(/general judgment about Danish/i)).not.toBeInTheDocument();
  });

  it("separates an unrecognized English answer from the supplied-choice constraint", () => {
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{
          ...base,
          linguisticScope: "english-choice",
          responseConstraintStatus: "not-met",
          issues: [{ code: "choice-required", message: "Choose one of the supplied meanings." }],
        }} />
      </MantineProvider>,
    );
    expect(screen.getByText("Supplied choice required")).toBeVisible();
    expect(screen.getByText("Unrecognized English choice")).toBeVisible();
    expect(screen.queryByText("Language status unknown")).not.toBeInTheDocument();
    expect(screen.getByText("Choose one of the supplied meanings.")).toBeVisible();
    expect(screen.queryByText(/general judgment about Danish/i)).not.toBeInTheDocument();
  });

  it("requires every reported facet before accepting a Danish meaning choice", () => {
    const choiceAttempt = {
      ...base,
      targetSatisfaction: "met" as const,
      meaningSatisfaction: "met" as const,
      linguisticStatus: "acceptable" as const,
      linguisticScope: "danish-choice" as const,
      responseConstraintStatus: "met" as const,
    };
    const { rerender } = render(
      <MantineProvider env="test"><FeedbackPanel attempt={choiceAttempt} /></MantineProvider>,
    );
    expect(screen.getByText("Good work")).toBeVisible();
    expect(screen.getByText("Meaning matched")).toBeVisible();
    expect(screen.getByText("Recognized Danish choice")).toBeVisible();
    expect(screen.getByText("Supplied choice used")).toBeVisible();

    rerender(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{ ...choiceAttempt, meaningSatisfaction: "not-met" }} />
      </MantineProvider>,
    );
    expect(screen.queryByText("Good work")).not.toBeInTheDocument();
    expect(screen.getByText("Meaning to revisit")).toBeVisible();
  });

  it("keeps the legacy unevaluable target label", () => {
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{ ...base, targetSatisfaction: "unevaluable" }} />
      </MantineProvider>,
    );
    expect(screen.getByText("Target not evaluated")).toBeVisible();
  });

  it("reserves green Good work for a fully acceptable constrained response", () => {
    const { rerender } = render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{
          ...base,
          targetSatisfaction: "met",
          linguisticStatus: "acceptable",
          linguisticScope: "danish-headword",
          responseConstraintStatus: "met",
        }} />
      </MantineProvider>,
    );
    expect(screen.getByText("Good work")).toBeVisible();
    expect(screen.getByText("Recognized Danish headword")).toBeVisible();

    rerender(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{
          ...base,
          targetSatisfaction: "met",
          linguisticStatus: "acceptable",
          linguisticScope: "danish-headword",
          responseConstraintStatus: "not-met",
        }} />
      </MantineProvider>,
    );
    expect(screen.queryByText("Good work")).not.toBeInTheDocument();
    expect(screen.getByText("Headword identified; adjust your answer.")).toBeVisible();
    expect(screen.getByText("One-word response required")).toBeVisible();
  });

  it("acknowledges a known headword while keeping a known language issue visible", () => {
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{
          ...base,
          targetSatisfaction: "met",
          linguisticStatus: "known-issue",
          linguisticScope: "danish-headword",
          responseConstraintStatus: "not-met",
        }} />
      </MantineProvider>,
    );
    expect(screen.getByText("Headword identified; adjust your answer.")).toBeVisible();
    expect(screen.getByText("Known language issue")).toBeVisible();
    expect(screen.getByText("One-word response required")).toBeVisible();
  });

  it("requires all four writing facets before showing green feedback", () => {
    const writing = {
      ...base,
      targetSatisfaction: "met" as const,
      meaningSatisfaction: "not-met" as const,
      linguisticStatus: "acceptable" as const,
      linguisticScope: "danish-sentence" as const,
      responseConstraintStatus: "met" as const,
      completedSentence: "Han læser en avis.",
    };
    const { rerender } = render(
      <MantineProvider env="test"><FeedbackPanel attempt={writing} /></MantineProvider>,
    );
    expect(screen.getByText("Target met")).toBeVisible();
    expect(screen.getByText("Meaning to revisit")).toBeVisible();
    expect(screen.getByText("Acceptable Danish")).toBeVisible();
    expect(screen.getByText("One statement supplied")).toBeVisible();
    expect(screen.getByText("Target met; adjust the rest of your answer.")).toBeVisible();
    expect(screen.queryByText("Good work")).not.toBeInTheDocument();

    rerender(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{ ...writing, meaningSatisfaction: "met" }} />
      </MantineProvider>,
    );
    expect(screen.getByText("Meaning matched")).toBeVisible();
    expect(screen.getByText("Good work")).toBeVisible();
  });

  it("describes a fronted tile answer with a neutral response-constraint label", () => {
    const frontedTileAttempt = {
      ...base,
      targetSatisfaction: "met" as const,
      meaningSatisfaction: "met" as const,
      linguisticStatus: "acceptable" as const,
      linguisticScope: "danish-sentence" as const,
      responseConstraintStatus: "not-met" as const,
      completedSentence: "Et brev læser hun.",
    };
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={frontedTileAttempt} tileResponse />
      </MantineProvider>,
    );

    expect(screen.getByText("Response constraint to revisit")).toBeVisible();
    expect(screen.queryByText("One statement required")).not.toBeInTheDocument();
    expect(screen.getByText("Target met; adjust the rest of your answer.")).toBeVisible();
  });

  it("explains bounded unknown writing and labels its response shape without choice wording", () => {
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{
          ...base,
          targetSatisfaction: "unevaluable",
          meaningSatisfaction: "unknown",
          linguisticScope: "danish-sentence",
          responseConstraintStatus: "not-met",
          completedSentence: "Hun læser en avis. Han sover.",
        }} />
      </MantineProvider>,
    );
    expect(screen.getByText("Target not evaluated")).toBeVisible();
    expect(screen.getByText("Meaning not evaluated")).toBeVisible();
    expect(screen.getByText("One statement required")).toBeVisible();
    expect(screen.queryByText("Supplied choice required")).not.toBeInTheDocument();
    expect(screen.getByText(/outside the pilot’s licensed patterns/i)).toBeVisible();
    expect(screen.getByText("Hun læser en avis. Han sover.")).toBeVisible();
  });

  it("does not infer omitted writing meaning or shape evidence as met", () => {
    const writing = {
      ...base,
      targetSatisfaction: "met" as const,
      linguisticStatus: "acceptable" as const,
      linguisticScope: "danish-sentence" as const,
      responseConstraintStatus: "met" as const,
      meaningSatisfaction: "met" as const,
    };
    const { rerender } = render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{ ...writing, meaningSatisfaction: undefined }} />
      </MantineProvider>,
    );
    expect(screen.queryByText("Good work")).not.toBeInTheDocument();
    expect(screen.getByText("Target met; adjust the rest of your answer.")).toBeVisible();

    rerender(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{ ...writing, responseConstraintStatus: undefined }} />
      </MantineProvider>,
    );
    expect(screen.queryByText("Good work")).not.toBeInTheDocument();
    expect(screen.getByText("Target met; adjust the rest of your answer.")).toBeVisible();
  });

  it("uses bounded word-form labels for a typed capitalization failure", () => {
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{
          ...base,
          targetSatisfaction: "unevaluable",
          linguisticStatus: "known-issue",
          linguisticScope: "danish-word-form",
          responseConstraintStatus: "not-met",
          completedSentence: "I",
          issues: [{ code: "STANDALONE_PRONOUN_CASE_REQUIRED", message: "Use capital I for several people." }],
        }} />
      </MantineProvider>,
    );

    expect(screen.getByText("Known language issue")).toBeVisible();
    expect(screen.getByText("Review the written form.")).toBeVisible();
    expect(screen.getByText("One-word form required")).toBeVisible();
    expect(screen.getByText("I")).toHaveAttribute("lang", "da");
    expect(screen.queryByText("Supplied choice required")).not.toBeInTheDocument();
    expect(screen.queryByText("One statement required")).not.toBeInTheDocument();
  });

  it("does not describe a known case-only word form as the wrong target", () => {
    render(
      <MantineProvider env="test">
        <FeedbackPanel attempt={{
          ...base,
          targetSatisfaction: "met",
          linguisticStatus: "known-issue",
          linguisticScope: "danish-word-form",
          responseConstraintStatus: "not-met",
          completedSentence: "jeg",
          issues: [{ code: "STANDALONE_PRONOUN_CASE_REQUIRED", message: "Use lowercase jeg here." }],
        }} />
      </MantineProvider>,
    );

    expect(screen.getByText("Word identified; use the taught spelling.")).toBeVisible();
    expect(screen.queryByText("Look at the target once more")).not.toBeInTheDocument();
  });

});
