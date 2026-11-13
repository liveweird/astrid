import { MantineProvider } from "@mantine/core";
import { render, screen } from "@testing-library/react";
import type { Exercise } from "../api/models";
import ExercisePrompt from "./ExercisePrompt";

it("renders fixed request punctuation outside the tak tile only after preceding content", () => {
  const request = {
    id: "p2-order", formatId: "E043", targetSkillId: "G005", targetScopeId: "p2-polite-request",
    supportSkillIds: [], contentRevision: "pilot-19", generatorRevision: "generator-19",
    instruction: "Build the taught request.", context: "At a café.",
    prompt: {
      before: "", after: "", cue: "I would like a cup of coffee, thank you.",
      cueLanguage: "en", answerLanguage: "da", tileMode: "use-all", requiredTileCount: 6,
      choices: [], punctuation: ".", blocks: [
        { id: "s", text: "jeg" }, { id: "m", text: "vil" }, { id: "a", text: "gerne" },
        { id: "v", text: "have" }, { id: "o", text: "en kop kaffe" }, { id: "t", text: "tak" },
      ], fixedPunctuation: [{ beforeBlockId: "t", text: "," }],
    }, introduction: [], hint: "Use the taught order.",
  } as unknown as Exercise;
  const { rerender } = render(
    <MantineProvider env="test">
      <ExercisePrompt exercise={request} text="" order={["t"]} disabled={false}
        onText={vi.fn()} onOrder={vi.fn()} />
    </MantineProvider>,
  );
  expect(screen.queryByLabelText("Fixed punctuation ,")).not.toBeInTheDocument();

  rerender(
    <MantineProvider env="test">
      <ExercisePrompt exercise={request} text="" order={["s", "m", "a", "v", "o", "t"]} disabled={false}
        onText={vi.fn()} onOrder={vi.fn()} />
    </MantineProvider>,
  );
  const punctuation = screen.getByLabelText("Fixed punctuation ,");
  expect(punctuation).toHaveTextContent(",");
  expect(punctuation).toHaveAttribute("lang", "en");
  expect(punctuation.closest("button")).toBeNull();
  const punctuationUnit = punctuation.closest("[data-fixed-punctuation-unit]");
  expect(punctuationUnit).toContainElement(screen.getByLabelText("Remove en kop kaffe from position 5"));
  expect(punctuationUnit).not.toContainElement(screen.getByLabelText("Remove tak from position 6"));
  expect(punctuationUnit?.nextElementSibling).toBe(screen.getByLabelText("Remove tak from position 6"));
  expect(screen.getByLabelText("Your Danish sentence")).toHaveTextContent("Jegvilgernehaveen kop kaffe,tak.");
});
