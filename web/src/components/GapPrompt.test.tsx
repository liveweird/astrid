import { MantineProvider } from "@mantine/core";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { GapPromptData, GapSelection } from "../api/models";
import GapPrompt from "./GapPrompt";

const prompt: GapPromptData = {
  segments: ["Jeg ", " ", " bog."],
  slots: [
    {
      id: "g0", label: "Present verb — Primary target", targetSkillId: "G004", assessmentRole: "primary",
      choices: [{ id: "c0", text: "læse" }, { id: "c1", text: "læser" }],
    },
    {
      id: "g1", label: "Indefinite article — Supporting grammar", targetSkillId: "G006", assessmentRole: "support",
      choices: [{ id: "c0", text: "et" }, { id: "c1", text: "en" }],
    },
  ],
};

describe("GapPrompt", () => {
  it("preserves actual selection order when either scoped choice changes or clears", async () => {
    const onSelections = vi.fn();
    const renderPrompt = (selections: GapSelection[]) => (
      <MantineProvider env="test">
        <GapPrompt prompt={prompt} selections={selections} disabled={false} onSelections={onSelections} />
      </MantineProvider>
    );
    const { rerender } = render(renderPrompt([]));

    const articleGroup = screen.getByRole("group", { name: "Indefinite article — Supporting grammar" });
    await userEvent.click(within(articleGroup).getByRole("button", { name: "en" }));
    expect(onSelections).toHaveBeenLastCalledWith([{ slotId: "g1", choiceId: "c1" }]);

    rerender(renderPrompt([{ slotId: "g1", choiceId: "c1" }]));
    await userEvent.click(within(screen.getByRole("group", { name: "Present verb — Primary target" }))
      .getByRole("button", { name: "læser" }));
    expect(onSelections).toHaveBeenLastCalledWith([
      { slotId: "g1", choiceId: "c1" }, { slotId: "g0", choiceId: "c1" },
    ]);

    rerender(renderPrompt([{ slotId: "g1", choiceId: "c1" }, { slotId: "g0", choiceId: "c1" }]));
    await userEvent.click(within(articleGroup).getByRole("button", { name: "et" }));
    expect(onSelections).toHaveBeenLastCalledWith([
      { slotId: "g0", choiceId: "c1" }, { slotId: "g1", choiceId: "c0" },
    ]);
    await userEvent.click(screen.getByRole("button", { name: "Clear article selection" }));
    expect(onSelections).toHaveBeenLastCalledWith([{ slotId: "g0", choiceId: "c1" }]);
  });

  it("renders Danish sentence forms and freezes every choice and clear action", () => {
    render(
      <MantineProvider env="test">
        <GapPrompt prompt={prompt} selections={[{ slotId: "g0", choiceId: "c1" }]} disabled onSelections={vi.fn()} />
      </MantineProvider>,
    );
    const sentence = screen.getByText("Jeg").closest<HTMLElement>("[lang]");
    expect(sentence).toHaveAttribute("lang", "da");
    expect(sentence).toHaveTextContent("Jeg læser … bog.");
    for (const button of screen.getAllByRole("button")) expect(button).toBeDisabled();
  });
});
