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

  it("renders reading propositions only after explicit private inspection", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json({
      exercise: {}, seed: 12, index: 4, lexicalIds: ["L005", "L006", "L014", "L015", "L018"],
      clauseRoles: [],
      readingSource: {
        questionSubjectId: "L006", questionVerbId: "L010", answerNounId: "L015", answer: "a newspaper",
        propositions: [
          { sentenceId: "C002:L006:L010:L015", subjectId: "L006", verbId: "L010", objectId: "L015" },
          { sentenceId: "C002:L005:L010:L018", subjectId: "L005", verbId: "L010", objectId: "L018" },
        ],
      },
    })));
    render(<MantineProvider env="test"><InspectorPanel exerciseId="exercise-reading" /></MantineProvider>);
    expect(screen.queryByText("a newspaper")).not.toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("Inspector key"), "a-secure-inspector-key-with-32-chars");
    await userEvent.click(screen.getByRole("button", { name: "Inspect this exercise" }));

    expect(await screen.findByText("L015 · a newspaper")).toBeVisible();
    expect(screen.getByText(/C002:L006:L010:L015:/)).toBeVisible();
    expect(screen.queryByText("Canonical sentence")).not.toBeInTheDocument();
  });

  it("renders private tile identities, licensed orders and distractor rationale", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => Response.json({
      exercise: {}, seed: 47, index: 5,
      canonicalSentenceId: "C001:L006:L010:L018", canonicalSentence: "Hun læser et brev.",
      constructionId: "C001", lexicalIds: ["L006", "L010", "L018"], clauseRoles: [],
      tileSource: {
        tokens: [
          { blockId: "b0", lexicalId: "L006", form: "subject" },
          { blockId: "b1", lexicalId: "L010", form: "finite-present" },
          { blockId: "b2", lexicalId: "L010", form: "infinitive" },
        ],
        licensedOrders: [{ kind: "subject-first", blockIds: ["b0", "b1"] }],
        distractors: [{ blockId: "b2", rationale: "infinitive-spare" }],
      },
    })));
    render(<MantineProvider env="test"><InspectorPanel exerciseId="exercise-tiles" /></MantineProvider>);
    expect(screen.queryByText("Tile identities")).not.toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("Inspector key"), "a-secure-inspector-key-with-32-chars");
    await userEvent.click(screen.getByRole("button", { name: "Inspect this exercise" }));

    expect(await screen.findByText("Hun læser et brev.")).toHaveAttribute("lang", "da");
    expect(screen.getByText("b1: L010 · finite-present")).toBeVisible();
    expect(screen.getByText("subject-first: b0 → b1")).toBeVisible();
    expect(screen.getByText("b2: infinitive-spare")).toBeVisible();
  });
});
