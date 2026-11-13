import { MantineProvider } from "@mantine/core";
import { createRef } from "react";
import { render, screen } from "@testing-library/react";
import type { PracticeSession } from "../api/models";
import { completedState, outcomes } from "../test/practiceFixtures";
import PracticeSummary from "./PracticeSummary";

function readingRow(label: string, readingRole: "subject" | "last-event", advancedItems: number) {
  return {
    label, readingTarget: { facet: "explicit-fact" as const }, readingRole,
    plannedItems: 1, attemptedItems: 1, advancedItems, attemptCount: 1, retryCount: 0,
    firstOutcomes: outcomes, latestOutcomes: outcomes, firstWithSupport: 0, firstWithPriorExposure: 1,
    lessonOpenedItems: 0, hintOpenedItems: 0,
  };
}

it("keeps P2 subject and chronology facts separate and reports their actual continued counts", () => {
  const baseline = completedState();
  const session = {
    ...baseline,
    focusId: "vocabulary-p2-s4", planRevision: "vocabulary-p2-s4-1", title: "Buying and paying",
    introductionRequired: true,
    summary: {
      ...baseline.summary,
      rows: [
        readingRow("Explicit subject fact", "subject", 1),
        readingRow("Explicit last event", "last-event", 0),
      ],
    },
  } as unknown as PracticeSession;
  render(
    <MantineProvider env="test">
      <PracticeSummary session={session} headingRef={createRef()} selectedTargets={new Set()} busy={false}
        onToggleTarget={vi.fn()} onStartReview={vi.fn()} onRepeat={vi.fn()} />
    </MantineProvider>,
  );

  expect(screen.getByText("Explicit subject facts")).toBeVisible();
  expect(screen.getByText("Explicit last-event facts")).toBeVisible();
  expect(screen.getByRole("article", { name: "Explicit subject fact" })).toHaveTextContent("1/1 continued");
  expect(screen.getByRole("article", { name: "Explicit last event" })).toHaveTextContent("0/1 continued");
  expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
});
