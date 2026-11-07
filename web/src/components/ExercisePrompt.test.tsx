import { MantineProvider } from "@mantine/core";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import type { Exercise } from "../api/models";
import ExercisePrompt from "./ExercisePrompt";

function exercise(formatId: string, promptMetadata: Partial<Exercise["prompt"]> = {}): Exercise {
  return {
    id: "011835c0-bb42-47ef-b21e-ae21411b24a1",
    formatId,
    targetSkillId: "G005",
    supportSkillIds: [],
    contentRevision: "pilot-1",
    generatorRevision: "generator-1",
    instruction: "Build a sentence",
    context: "A statement",
    prompt: {
      before: "Jeg har",
      after: formatId === "E002" ? " bog." : " en bog.",
      choices: formatId === "E001" ? ["læse", "læser"] : [],
      blocks: formatId === "E003" ? [
        { id: "b1", text: "jeg" }, { id: "b2", text: "læser" }, { id: "b3", text: "I" },
      ] : [],
      punctuation: ".",
      ...promptMetadata,
    },
    introduction: [],
    hint: "Start with the subject.",
  };
}

function renderPrompt(formatId: string, onText = vi.fn(), onOrder = vi.fn()) {
  render(
    <MantineProvider env="test">
      <ExercisePrompt
        exercise={exercise(formatId)} text="" order={[]} disabled={false}
        onText={onText} onOrder={onOrder}
      />
    </MantineProvider>,
  );
}

describe("ExercisePrompt", () => {
  it("renders a Danish vocabulary cue with English meaning choices and no sentence shell", async () => {
    const onText = vi.fn();
    render(
      <MantineProvider env="test">
        <ExercisePrompt
          exercise={{
            ...exercise("E007", {
              before: "", after: "", punctuation: "", cue: "avis", cueLanguage: "da", answerLanguage: "en",
              choices: ["book", "newspaper", "letter (written message)"],
            }),
            targetSkillId: null,
            lexicalTarget: { entryId: "L015", senseId: "L015-S01", facet: "meaning", direction: "da-en" },
          }}
          text="" order={[]} disabled={false} onText={onText} onOrder={vi.fn()}
        />
      </MantineProvider>,
    );

    expect(screen.getByText("avis")).toHaveAttribute("lang", "da");
    const choices = screen.getByRole("group", { name: "Answer choices" });
    expect(choices).not.toHaveAttribute("lang", "da");
    for (const choice of ["book", "newspaper", "letter (written message)"]) {
      expect(screen.getByText(choice)).toHaveAttribute("lang", "en");
    }
    expect(screen.queryByLabelText("Danish sentence with answer choices")).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "newspaper" }));
    expect(onText).toHaveBeenCalledWith("newspaper");
  });

  it("renders an English vocabulary cue with the Danish headword input and character helper", async () => {
    const onText = vi.fn();
    render(
      <MantineProvider env="test">
        <ExercisePrompt
          exercise={{
            ...exercise("E008", {
              before: "", after: "", punctuation: "", cue: "book", cueLanguage: "en", answerLanguage: "da",
              choices: [], inputLabel: "Danish singular headword",
            }),
            targetSkillId: null,
            lexicalTarget: {
              entryId: "L014", senseId: "L014-S01", facet: "singular-headword", direction: "en-da",
            },
          }}
          text="" order={[]} disabled={false} onText={onText} onOrder={vi.fn()}
        />
      </MantineProvider>,
    );

    expect(screen.getByText("book")).toHaveAttribute("lang", "en");
    const input = screen.getByLabelText("Danish singular headword");
    expect(input).toHaveAttribute("lang", "da");
    await userEvent.type(input, "bog");
    expect(onText).toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Insert ø" })).toBeEnabled();
    expect(screen.queryByText("avis")).not.toBeInTheDocument();
  });

  it("offers closely related form choices as buttons", async () => {
    const onText = vi.fn();
    renderPrompt("E001", onText);
    await userEvent.click(screen.getByRole("button", { name: "læser" }));
    expect(onText).toHaveBeenCalledWith("læser");
    const sentence = screen.getByLabelText("Danish sentence with answer choices");
    expect(sentence).toHaveTextContent("Jeg");
    expect(sentence).toHaveTextContent("en bog.");
    expect(sentence).toHaveAttribute("lang", "da");
  });

  it("accepts a typed article between the sentence fragments", async () => {
    const onText = vi.fn();
    renderPrompt("E002", onText);
    fireEvent.change(screen.getByLabelText("Missing word"), { target: { value: "en" } });
    expect(onText).toHaveBeenCalledWith("en");
    const sentence = screen.getByLabelText("Danish sentence cloze");
    expect(sentence.textContent?.match(/\./g)).toHaveLength(1);
    expect(sentence).toHaveTextContent("bog.");
  });

  it("uses prompt metadata to show the corrupted word and repair field without revealing an answer", () => {
    render(
      <MantineProvider env="test">
        <ExercisePrompt
          exercise={exercise("E005", {
            before: "Jeg ",
            after: " en bog.",
            inputLabel: "Replacement verb",
            replacementWord: "læse",
          })}
          text="" order={[]} disabled={false} onText={vi.fn()} onOrder={vi.fn()}
        />
      </MantineProvider>,
    );

    const corruptedSentence = screen.getByRole("group", { name: "Danish sentence to repair" });
    expect(corruptedSentence).toHaveTextContent("Word to replace");
    const markedWord = screen.getByText("læse");
    expect(markedWord.previousSibling?.previousSibling?.textContent).toBe("Jeg ");
    expect(markedWord.nextSibling?.textContent).toBe(" en bog.");
    expect(screen.getByText("Word to replace:")).toHaveAttribute("lang", "en");
    expect(screen.getByLabelText("Replacement verb")).toHaveAccessibleDescription("Enter one word");
    expect(screen.queryByText("læser")).not.toBeInTheDocument();
  });

  it("preserves engine-owned spacing around a repaired C001 sentence", () => {
    render(
      <MantineProvider env="test">
        <ExercisePrompt
          exercise={exercise("E005", {
            before: "Hun ",
            after: ".",
            inputLabel: "Replacement verb",
            replacementWord: "sove",
          })}
          text="" order={[]} disabled={false} onText={vi.fn()} onOrder={vi.fn()}
        />
      </MantineProvider>,
    );

    const markedWord = screen.getByText("sove");
    expect(markedWord.previousSibling?.previousSibling?.textContent).toBe("Hun ");
    expect(markedWord.nextSibling?.textContent).toBe(".");
    expect(markedWord.parentElement?.textContent).not.toContain("sove .");
  });

  it("uses the supplied input label for recall and keeps the legacy fallback", () => {
    const { rerender } = render(
      <MantineProvider env="test">
        <ExercisePrompt
          exercise={exercise("E004", { inputLabel: "Present form" })}
          text="" order={[]} disabled={false} onText={vi.fn()} onOrder={vi.fn()}
        />
      </MantineProvider>,
    );
    expect(screen.getByLabelText("Present form")).toBeVisible();

    rerender(
      <MantineProvider env="test">
        <ExercisePrompt
          exercise={exercise("E002")}
          text="" order={[]} disabled={false} onText={vi.fn()} onOrder={vi.fn()}
        />
      </MantineProvider>,
    );
    expect(screen.getByLabelText("Missing word")).toBeVisible();
  });

  it("inserts Danish characters at the selection, restores the caret, and returns focus", async () => {
    function TextHarness() {
      const [text, setText] = useState("bla");
      return (
        <ExercisePrompt
          exercise={exercise("E005", {
            inputLabel: "Replacement verb",
            replacementWord: "ble",
          })}
          text={text} order={[]} disabled={false} onText={setText} onOrder={vi.fn()}
        />
      );
    }
    render(<MantineProvider env="test"><TextHarness /></MantineProvider>);
    const input = screen.getByLabelText("Replacement verb") as HTMLInputElement;
    input.focus();
    input.setSelectionRange(1, 2);
    await userEvent.tab();
    await userEvent.keyboard("{Enter}");

    expect(input).toHaveValue("bæa");
    expect(input).toHaveFocus();
    expect(input).toHaveProperty("selectionStart", 2);
    expect(input).toHaveProperty("selectionEnd", 2);
  });

  it("returns focus and restores the caret when insertion leaves the controlled value unchanged", async () => {
    function TextHarness() {
      const [text, setText] = useState("æ");
      return (
        <ExercisePrompt
          exercise={exercise("E004", { inputLabel: "Present form" })}
          text={text} order={[]} disabled={false} onText={setText} onOrder={vi.fn()}
        />
      );
    }
    render(<MantineProvider env="test"><TextHarness /></MantineProvider>);
    const input = screen.getByLabelText("Present form") as HTMLInputElement;
    input.focus();
    input.setSelectionRange(0, 1);
    await userEvent.tab();
    await userEvent.keyboard("{Enter}");

    expect(input).toHaveValue("æ");
    expect(input).toHaveFocus();
    expect(input).toHaveProperty("selectionStart", 1);
    expect(input).toHaveProperty("selectionEnd", 1);
  });

  it("disables the text field and every Danish character button together", () => {
    render(
      <MantineProvider env="test">
        <ExercisePrompt
          exercise={exercise("E006", {
            inputLabel: "Replacement article",
            replacementWord: "et",
          })}
          text="" order={[]} disabled onText={vi.fn()} onOrder={vi.fn()}
        />
      </MantineProvider>,
    );
    expect(screen.getByLabelText("Replacement article")).toBeDisabled();
    for (const character of ["æ", "ø", "å"]) {
      expect(screen.getByRole("button", { name: `Insert ${character}` })).toBeDisabled();
    }
  });

  it("lets a keyboard user append a sentence block without dragging", async () => {
    const onOrder = vi.fn();
    renderPrompt("E003", vi.fn(), onOrder);
    screen.getByRole("button", { name: "jeg" }).focus();
    await userEvent.keyboard("{Enter}");
    expect(onOrder).toHaveBeenCalledWith(["b1"]);
  });

  it("capitalizes the first chosen block while preserving lexical plural I later", () => {
    render(
      <MantineProvider env="test">
        <ExercisePrompt
          exercise={exercise("E003")} text="" order={["b1", "b3"]} disabled={false}
          onText={vi.fn()} onOrder={vi.fn()}
        />
      </MantineProvider>,
    );
    expect(screen.getByLabelText("Remove jeg from position 1")).toHaveTextContent("Jeg");
    expect(screen.getByLabelText("Remove I from position 2")).toHaveTextContent("I");
  });

  it("moves focus when a keyboard action removes the focused ordering button", async () => {
    function OrderingHarness() {
      const [order, setOrder] = useState<string[]>([]);
      return (
        <ExercisePrompt
          exercise={exercise("E003")} text="" order={order} disabled={false}
          onText={vi.fn()} onOrder={setOrder}
        />
      );
    }
    render(<MantineProvider env="test"><OrderingHarness /></MantineProvider>);
    screen.getByRole("button", { name: "jeg" }).focus();
    await userEvent.keyboard("{Enter}");
    expect(screen.getByRole("button", { name: "læser" })).toHaveFocus();
    await userEvent.keyboard("{Enter}");
    expect(screen.getByRole("button", { name: "I" })).toHaveFocus();
    await userEvent.keyboard("{Enter}");
    expect(screen.getByLabelText("Remove I from position 3")).toHaveFocus();
    await userEvent.keyboard("{Enter}");
    expect(screen.getByRole("button", { name: "I" })).toHaveFocus();
  });
});
