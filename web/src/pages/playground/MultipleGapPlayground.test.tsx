import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { attempt, catalog, exercise, json, renderPlayground, session } from "../../test/playgroundFixtures";

const gapPrompt = {
  segments: ["Jeg ", " ", " bog."],
  slots: [
    {
      id: "g0", label: "Present verb", targetSkillId: "G004", assessmentRole: "primary",
      choices: [{ id: "c0", text: "læse" }, { id: "c1", text: "læser" }],
    },
    {
      id: "g1", label: "Indefinite article", targetSkillId: "G006", assessmentRole: "support",
      choices: [{ id: "c0", text: "et" }, { id: "c1", text: "en" }],
    },
  ],
};

const gapExercise = {
  ...exercise,
  id: "2fb2c7ee-70f2-4766-a7bc-1eea7ac05933", formatId: "E015", targetSkillId: "G004",
  supportSkillIds: ["G001", "G002", "G003", "G005", "G006"],
  contentRevision: "pilot-7", generatorRevision: "generator-7",
  instruction: "Choose a present verb and article.",
  context: "Complete both gaps; the present verb is the primary target.",
  prompt: {
    before: "", after: "", choices: [], blocks: [], punctuation: "",
    cueLanguage: "da", answerLanguage: "da", gapPrompt,
  },
  introduction: [],
};

const gapCatalog = {
  ...catalog,
  contentRevision: "pilot-7", generatorRevision: "generator-7", evaluationRevision: "evaluation-7",
  formats: [{ id: "E015", title: "Choose a present verb and article", targetSkillId: "G004" }],
};

describe("multiple-gap playground", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("replays ordered selections exactly and retains editable mixed feedback for retry", async () => {
    const bodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
      if (path.endsWith("development-session")) return json({
        ...session, contentRevision: "pilot-7", generatorRevision: "generator-7", evaluationRevision: "evaluation-7",
      });
      if (path.endsWith("catalog")) return json(gapCatalog);
      if (path.endsWith("exercises")) return json(gapExercise);
      bodies.push(String(init?.body));
      if (bodies.length === 1) throw new TypeError("response connection closed");
      const submitted = JSON.parse(bodies.at(-1)!);
      return json({
        ...attempt, attemptId: submitted.attemptId, exerciseId: gapExercise.id,
        targetSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "known-issue",
        linguisticScope: "danish-gaps", evaluationRevision: "evaluation-7", retryNumber: 1,
        completedSentence: "Jeg læser et bog.", feedback: "The primary verb is correct; revisit the supporting article.",
        gapResults: [
          {
            slotId: "g0", choiceId: "c1", targetSkillId: "G004", assessmentRole: "primary",
            targetSatisfaction: "met", responseConstraintStatus: "met", linguisticStatus: "acceptable",
            feedback: "The present verb is correct.",
          },
          {
            slotId: "g1", choiceId: "c0", targetSkillId: "G006", assessmentRole: "support",
            targetSatisfaction: "not-met", responseConstraintStatus: "met", linguisticStatus: "known-issue",
            feedback: "The noun requires the other indefinite article.",
          },
        ],
      });
    }));

    await renderPlayground();
    const article = await screen.findByRole("group", { name: "Indefinite article — Supporting grammar" });
    await userEvent.click(within(article).getByRole("button", { name: "et" }));
    await userEvent.click(within(screen.getByRole("group", { name: "Present verb — Primary target" }))
      .getByRole("button", { name: "læser" }));
    await userEvent.click(screen.getByRole("button", { name: "Check gaps" }));
    expect(await screen.findByText("The result is not confirmed")).toBeVisible();
    for (const choice of ["et", "en", "læse", "læser"]) expect(screen.getByRole("button", { name: choice })).toBeDisabled();

    await userEvent.click(screen.getByRole("button", { name: "Check the same attempt" }));
    expect(await screen.findByRole("group", { name: "Present verb feedback" })).toHaveTextContent("læser");
    expect(screen.getByRole("group", { name: "Indefinite article feedback" })).toHaveTextContent("et");
    expect(screen.getByText("Verb target met; review the article.")).toBeVisible();
    expect(screen.queryByText("Good work")).not.toBeInTheDocument();
    expect(screen.queryByText(/Meaning/)).not.toBeInTheDocument();
    expect(bodies[1]).toBe(bodies[0]);
    expect(JSON.parse(bodies[0]!)).toMatchObject({ answer: { kind: "gaps", selections: [
      { slotId: "g1", choiceId: "c0" }, { slotId: "g0", choiceId: "c1" },
    ] } });

    await userEvent.click(screen.getByRole("button", { name: "Try again" }));
    await waitFor(() => expect(screen.getByRole("group", { name: "Present verb and article gaps" })).toHaveFocus());
    expect(screen.getByRole("button", { name: "Clear present verb selection" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Clear article selection" })).toBeEnabled();
  });

  it("clears gap selections when regeneration reuses the same slot and choice ids", async () => {
    let exerciseRequests = 0;
    vi.stubGlobal("fetch", vi.fn(async (path: string) => {
      if (path.endsWith("development-session")) return json({
        ...session, contentRevision: "pilot-7", generatorRevision: "generator-7", evaluationRevision: "evaluation-7",
      });
      if (path.endsWith("catalog")) return json(gapCatalog);
      exerciseRequests += 1;
      return json({ ...gapExercise, id: exerciseRequests === 1 ? gapExercise.id : "1196558a-f171-43d9-94db-bfd5303a3cd0" });
    }));

    await renderPlayground();
    await userEvent.click(within(await screen.findByRole("group", { name: "Present verb — Primary target" }))
      .getByRole("button", { name: "læser" }));
    expect(screen.getByRole("button", { name: "læser" })).toHaveAttribute("aria-pressed", "true");
    await userEvent.click(screen.getByText("Generation controls"));
    await userEvent.click(screen.getByRole("button", { name: "Regenerate" }));

    await waitFor(() => expect(exerciseRequests).toBe(2));
    expect(screen.getByRole("button", { name: "læser" })).toHaveAttribute("aria-pressed", "false");
    expect(screen.getByRole("button", { name: "Clear present verb selection" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Check gaps" })).toBeDisabled();
  });
});
