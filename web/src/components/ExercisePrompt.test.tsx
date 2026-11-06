import { MantineProvider } from "@mantine/core";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { Exercise } from "../api/models";
import ExercisePrompt from "./ExercisePrompt";

function exercise(formatId: string): Exercise {
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
});
