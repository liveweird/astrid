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
});
