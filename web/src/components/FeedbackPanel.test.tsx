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
});
