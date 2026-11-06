import { MantineProvider } from "@mantine/core";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import InspectorPanel from "./InspectorPanel";

describe("InspectorPanel", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("hides diagnostics from the previous exercise while retaining the in-memory key", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json({
      exercise: {}, seed: 7, index: 2, canonicalSentenceId: "C001:L001:L013",
      constructionId: "C001", lexicalIds: ["L001", "L013"],
      clauseRoles: ["SUBJECT", "FINITE_VERB"], canonicalSentence: "Jeg sover.",
    })));
    const { rerender } = render(
      <MantineProvider env="test"><InspectorPanel exerciseId="exercise-a" /></MantineProvider>,
    );
    const key = "a-secure-inspector-key-with-32-chars";
    await userEvent.type(screen.getByLabelText("Inspector key"), key);
    await userEvent.click(screen.getByRole("button", { name: "Inspect this exercise" }));
    expect(await screen.findByText("Jeg sover.")).toBeVisible();

    rerender(<MantineProvider env="test"><InspectorPanel exerciseId="exercise-b" /></MantineProvider>);
    expect(screen.queryByText("Jeg sover.")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Inspector key")).toHaveValue(key);
  });
});
