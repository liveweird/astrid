import { MantineProvider } from "@mantine/core";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import type { Exercise, MatchingColumns, MatchingPair } from "../api/models";
import ExercisePrompt from "./ExercisePrompt";
import MatchingPrompt from "./MatchingPrompt";

const columns: MatchingColumns = {
  left: [{ id: "l0", text: "book" }, { id: "l1", text: "newspaper" }],
  right: [{ id: "r0", text: "en avis" }, { id: "r1", text: "en bog" }],
};

function Harness({ disabled = false }: { disabled?: boolean }) {
  const [pairs, setPairs] = useState<MatchingPair[]>([]);
  return <MatchingPrompt columns={columns} pairs={pairs} disabled={disabled} onPairs={setPairs} />;
}

describe("MatchingPrompt", () => {
  it("does not reuse a half-selected left item after authoritative pairs have claimed it", async () => {
    const onPairs = vi.fn();
    const { rerender } = render(<MantineProvider env="test">
      <MatchingPrompt columns={columns} pairs={[]} disabled={false} onPairs={onPairs} />
    </MantineProvider>);
    await userEvent.click(screen.getByRole("button", { name: "book" }));

    rerender(<MantineProvider env="test">
      <MatchingPrompt columns={columns} pairs={[{ leftId: "l0", rightId: "r1" }]} disabled={false} onPairs={onPairs} />
    </MantineProvider>);

    expect(screen.getByRole("button", { name: "book" })).toHaveAttribute("aria-pressed", "false");
    expect(screen.getByRole("button", { name: "en avis" })).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "en avis" }));
    expect(onPairs).not.toHaveBeenCalled();
  });

  it("forms and removes pairs with native keyboard buttons while preserving language metadata", async () => {
    render(<MantineProvider env="test"><Harness /></MantineProvider>);

    const book = screen.getByRole("button", { name: "book" });
    book.focus();
    await userEvent.keyboard("{Enter}");
    expect(book).toHaveAttribute("aria-pressed", "true");
    const danishBook = screen.getByRole("button", { name: "en bog" });
    expect(danishBook).toBeEnabled();
    danishBook.focus();
    await userEvent.keyboard("{Enter}");

    const chosen = screen.getByRole("group", { name: "Chosen matches" });
    expect(within(chosen).getByRole("group", { name: "Chosen match for book" })).toHaveTextContent("en bog");
    expect(screen.getByText("book", { selector: "p" })).toHaveAttribute("lang", "en");
    expect(screen.getByText("en bog", { selector: "p" })).toHaveAttribute("lang", "da");
    expect(book).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "Remove match for book" }));
    expect(screen.queryByRole("group", { name: "Chosen matches" })).not.toBeInTheDocument();
    expect(book).toBeEnabled();
  });

  it("freezes every matching control while an attempt is pending or graded", () => {
    render(<MantineProvider env="test"><Harness disabled /></MantineProvider>);
    expect(screen.getByRole("group", { name: "Match English and Danish words" })).toHaveAttribute("tabindex", "-1");
    for (const button of screen.getAllByRole("button")) expect(button).toBeDisabled();
  });

  it("clears an unfinished English selection when a new exercise reuses opaque item ids", async () => {
    const matchingExercise = (id: string, left: string): Extract<Exercise, { formatId: "E014" }> => ({
      id, formatId: "E014", targetSkillId: null, supportSkillIds: [],
      contentRevision: "pilot-6", generatorRevision: "generator-6",
      instruction: "Match English and Danish words.", context: "Match each meaning.",
      lexicalTargets: [
        { entryId: "L014", senseId: "L014-S01", facet: "meaning", direction: "en-da" },
        { entryId: "L015", senseId: "L015-S01", facet: "meaning", direction: "en-da" },
        { entryId: "L018", senseId: "L018-S01", facet: "meaning", direction: "en-da" },
        { entryId: "L024", senseId: "L024-S01", facet: "meaning", direction: "en-da" },
      ],
      prompt: {
        before: "", after: "", choices: [], blocks: [], punctuation: "",
        cueLanguage: "en", answerLanguage: "da",
        matchingColumns: {
          left: [{ id: "l0", text: left }, { id: "l1", text: "newspaper" }],
          right: [{ id: "r0", text: "en avis" }, { id: "r1", text: "en bog" }],
        },
      },
      introduction: [], hint: "Match each meaning.",
    });
    const props = {
      text: "", order: [], matchingPairs: [], disabled: false,
      onText: vi.fn(), onOrder: vi.fn(), onMatchingPairs: vi.fn(),
    };
    const { rerender } = render(
      <MantineProvider env="test"><ExercisePrompt exercise={matchingExercise("exercise-a", "book")} {...props} /></MantineProvider>,
    );
    await userEvent.click(screen.getByRole("button", { name: "book" }));
    expect(screen.getByRole("button", { name: "book" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "en bog" })).toBeEnabled();

    rerender(
      <MantineProvider env="test"><ExercisePrompt exercise={matchingExercise("exercise-b", "letter")} {...props} /></MantineProvider>,
    );
    expect(screen.getByRole("button", { name: "letter" })).toHaveAttribute("aria-pressed", "false");
    expect(screen.getByRole("button", { name: "en bog" })).toBeDisabled();
  });
});
