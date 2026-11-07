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

  it("renders private lexical evidence without grammar-only labels", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json({
      exercise: {}, seed: 9, index: 1,
      lexicalSource: {
        entryId: "L018", senseId: "L018-S01", headword: "brev",
        gloss: "letter (written message)", article: "et",
      },
    })));
    render(<MantineProvider env="test"><InspectorPanel exerciseId="exercise-lexical" /></MantineProvider>);
    await userEvent.type(screen.getByLabelText("Inspector key"), "a-secure-inspector-key-with-32-chars");
    await userEvent.click(screen.getByRole("button", { name: "Inspect this exercise" }));

    expect(await screen.findByText("L018 · L018-S01")).toBeVisible();
    expect(screen.getByText("brev")).toHaveAttribute("lang", "da");
    expect(screen.getByText("letter (written message)")).toHaveAttribute("lang", "en");
    expect(screen.queryByText("Canonical sentence")).not.toBeInTheDocument();
    expect(screen.queryByText("Construction")).not.toBeInTheDocument();
    expect(screen.getByText("seed 9, index 1")).toBeVisible();
  });
});
