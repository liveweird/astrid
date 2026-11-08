import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { PracticeSummaryRow } from "../../api/models";
import {
  completedState, context, json, outcomes, owner, practiceId, resetPracticeBrowser, state,
} from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import Practice from "./Practice";
import { saveRunRecovery } from "./practiceRecovery";

beforeEach(resetPracticeBrowser);

it("starts an unchecked learner-selected follow-up without renewing or abandoning its source", async () => {
  const sourceRun = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(sourceRun);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let creationPath = "";
  let creationBody = "";
  const paths: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    paths.push(path);
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith(`/practice-sessions/${practiceId}`)) return json(completedState());
    if (path.endsWith("/follow-ups")) {
      creationPath = path;
      creationBody = String(init?.body);
      const request = JSON.parse(creationBody) as { practiceSessionId: string; targetKeys: string[] };
      return json(state({
        id: request.practiceSessionId,
        focusId: "targeted-review",
        title: "Focused review",
        planRevision: "follow-up-1",
        followUp: { sourcePracticeSessionId: practiceId, selectedTargetKeys: request.targetKeys },
      }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  const start = await screen.findByRole("button", { name: "Start selected review" });
  expect(start).toBeDisabled();
  const lexical = screen.getByRole("checkbox", { name: "Review book · meaning" });
  const grammar = screen.getByRole("checkbox", { name: "Review Subject-first statements" });
  expect(lexical).not.toBeChecked();
  expect(grammar).not.toBeChecked();
  await user.click(lexical);
  await user.click(grammar);
  await user.click(start);

  expect(await screen.findByRole("heading", { name: "Focused review" })).toBeVisible();
  expect(screen.getByText("A focused review in 1 part. Continue after any saved answer.")).toBeVisible();
  expect(screen.getByRole("link", { name: "Source practice" })).toHaveAttribute("href", `/practice?session=${practiceId}`);
  expect(paths.some((path) => path.endsWith("development-session"))).toBe(false);
  expect(creationPath).toBe(`/api/v1/practice-sessions/${practiceId}/follow-ups`);
  const request = JSON.parse(creationBody) as { practiceSessionId: string; planRevision: string; targetKeys: string[] };
  expect(request).toMatchObject({
    planRevision: "follow-up-1",
    targetKeys: ["lexical:L014:L014-S01:meaning:da-en", "grammar:G005"],
  });
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
  expect(JSON.parse(localStorage.getItem(`astrid.practice.v1.run.${request.practiceSessionId}`)!)).toMatchObject({
    sourcePracticeSessionId: practiceId,
    selectedTargetKeys: request.targetKeys,
  });
});

it("keeps meaning recognition and written headword recall as separate review targets", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const recognition = completedState().summary.rows[0]! as PracticeSummaryRow;
  const recall = {
    ...recognition,
    label: "book — bog",
    lexicalTarget: { entryId: "L014", senseId: "L014-S01", facet: "singular-headword", direction: "en-da" },
  };
  let sentTargets: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith(`/practice-sessions/${practiceId}`)) {
      return json(state({ ...completedState(), summary: { ...completedState().summary, rows: [recognition, recall] } }));
    }
    if (path.endsWith("/follow-ups")) {
      const request = JSON.parse(String(init?.body)) as { practiceSessionId: string; targetKeys: string[] };
      sentTargets = request.targetKeys;
      return json(state({
        id: request.practiceSessionId,
        focusId: "targeted-review",
        title: "Focused review",
        planRevision: "follow-up-1",
        followUp: { sourcePracticeSessionId: practiceId, selectedTargetKeys: request.targetKeys },
      }));
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  expect(await screen.findByText("Meaning recognition")).toBeVisible();
  expect(screen.getByText("Written headword recall")).toBeVisible();
  expect(screen.getByRole("checkbox", { name: "Review book · meaning" })).not.toBeChecked();
  await user.click(screen.getByRole("checkbox", { name: "Review book — bog" }));
  await user.click(screen.getByRole("button", { name: "Start selected review" }));
  await screen.findByRole("heading", { name: "Focused review" });
  expect(sentTargets).toEqual(["lexical:L014:L014-S01:singular-headword:en-da"]);
});

it("uses the legacy follow-up revision for a known revision-one review source", async () => {
  const sourceId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "targeted-review", planRevision: "follow-up-1", seed: 47,
    sourcePracticeSessionId: sourceId, selectedTargetKeys: ["lexical:L014:L014-S01:meaning:da-en"] });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const completedReview = state({ ...completedState(), focusId: "targeted-review", planRevision: "follow-up-1",
    title: "Focused review", followUp: { sourcePracticeSessionId: sourceId, selectedTargetKeys: ["lexical:L014:L014-S01:meaning:da-en"] } });
  let revision = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith(`/practice-sessions/${practiceId}`)) return json(completedReview);
    if (path.endsWith("/follow-ups")) {
      const request = JSON.parse(String(init?.body)) as { practiceSessionId: string; planRevision: string; targetKeys: string[] };
      revision = request.planRevision;
      return json(state({ id: request.practiceSessionId, focusId: "targeted-review", planRevision: request.planRevision,
        title: "Focused review", followUp: { sourcePracticeSessionId: practiceId, selectedTargetKeys: request.targetKeys } }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("checkbox", { name: "Review book · meaning" }));
  await user.click(screen.getByRole("button", { name: "Start selected review" }));
  expect(await screen.findByRole("heading", { name: "Focused review" })).toBeVisible();
  expect(revision).toBe("follow-up-1");
});

it("keeps present-verb operations separate and requests their advertised follow-up revision", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "present-verbs", planRevision: "present-verbs-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const verbRows = [
    { label: "Present verbs — choosing the form", assessmentMode: "selection" },
    { label: "Present verbs — written recall", assessmentMode: "recall" },
    { label: "Present verbs — repairing the form", assessmentMode: "repair" },
  ].map((row) => ({ ...row, targetSkillId: "G004", plannedItems: 2, attemptedItems: 2, advancedItems: 2,
    attemptCount: 2, retryCount: 0, firstOutcomes: outcomes, latestOutcomes: outcomes,
    firstWithSupport: 0, firstWithPriorExposure: 0, lessonOpenedItems: 0, hintOpenedItems: 0 }));
  const completed = state({ focusId: "present-verbs", planRevision: "present-verbs-1", title: "Present verb forms",
    followUpPlanRevision: "follow-up-2", state: "completed", cursor: 6, itemCount: 6, currentExercise: null,
    summary: { ...completedState().summary, plannedItems: 6, completedItems: 6, rows: verbRows } });
  let followUpBody = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith(`/practice-sessions/${practiceId}`)) return json(completed);
    if (path.endsWith("/follow-ups")) {
      followUpBody = String(init?.body);
      const request = JSON.parse(followUpBody) as { practiceSessionId: string; targetKeys: string[] };
      return json(state({ id: request.practiceSessionId, focusId: "targeted-review", planRevision: "follow-up-2",
        title: "Focused review", followUpPlanRevision: "follow-up-2",
        followUp: { sourcePracticeSessionId: practiceId, selectedTargetKeys: request.targetKeys } }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  expect(await screen.findByText("Form selection")).toBeVisible();
  expect(screen.getByText("Written form recall")).toBeVisible();
  expect(screen.getByText("Form repair")).toBeVisible();
  await user.click(screen.getByRole("checkbox", { name: "Review Present verbs — choosing the form" }));
  await user.click(screen.getByRole("checkbox", { name: "Review Present verbs — written recall" }));
  await user.click(screen.getByRole("checkbox", { name: "Review Present verbs — repairing the form" }));
  await user.click(screen.getByRole("button", { name: "Start selected review" }));
  expect(await screen.findByRole("heading", { name: "Focused review" })).toBeVisible();
  expect(JSON.parse(followUpBody)).toMatchObject({ planRevision: "follow-up-2", targetKeys: [
    "grammar:G004:selection", "grammar:G004:recall", "grammar:G004:repair",
  ] });
});

it("refuses follow-up creation when unknown session metadata advertises no revision", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "unknown-focus", planRevision: "unknown-plan", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let followUps = 0;
  const unknown = state({ ...completedState(), focusId: "unknown-focus", planRevision: "unknown-plan" });
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith(`/practice-sessions/${practiceId}`)) return json(unknown);
    if (path.endsWith("/follow-ups")) { followUps += 1; return json(state()); }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("checkbox", { name: "Review book · meaning" }));
  await user.click(screen.getByRole("button", { name: "Start selected review" }));
  expect(await screen.findByText("This practice does not advertise a supported follow-up plan.")).toBeVisible();
  expect(followUps).toBe(0);
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
});

it("retries a lost follow-up creation with its exact source endpoint and body", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  const bodies: string[] = [];
  const paths: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith(`/practice-sessions/${practiceId}`)) return json(completedState());
    if (path.endsWith("/follow-ups")) {
      paths.push(path);
      bodies.push(String(init?.body));
      if (bodies.length === 1) throw new TypeError("creation response lost");
      const request = JSON.parse(bodies[0]!) as { practiceSessionId: string; targetKeys: string[] };
      return json(state({
        id: request.practiceSessionId,
        focusId: "targeted-review",
        title: "Focused review",
        planRevision: "follow-up-1",
        followUp: { sourcePracticeSessionId: practiceId, selectedTargetKeys: request.targetKeys },
      }));
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("checkbox", { name: "Review book · meaning" }));
  await user.click(screen.getByRole("button", { name: "Start selected review" }));
  const retry = await screen.findByRole("button", { name: "Retry saved operation" });
  expect(screen.queryByRole("button", { name: "Check answer" })).not.toBeInTheDocument();
  await user.click(retry);
  expect(await screen.findByRole("heading", { name: "Focused review" })).toBeVisible();
  expect(paths).toEqual([
    `/api/v1/practice-sessions/${practiceId}/follow-ups`,
    `/api/v1/practice-sessions/${practiceId}/follow-ups`,
  ]);
  expect(bodies[1]).toBe(bodies[0]);
});

it("does not create a follow-up under a changed practice owner", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let contextReads = 0;
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) {
      contextReads += 1;
      return json(context(contextReads === 1 ? owner : "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
    }
    if (path.endsWith(`/practice-sessions/${practiceId}`)) return json(completedState());
    if (path.endsWith("/follow-ups")) { creations += 1; return json({}); }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("checkbox", { name: "Review book · meaning" }));
  await user.click(screen.getByRole("button", { name: "Start selected review" }));
  expect(await screen.findByText(/expired browser session/)).toBeVisible();
  expect(creations).toBe(0);
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
});

it("does not send a follow-up when its browser recovery cannot be saved", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith(`/practice-sessions/${practiceId}`)) return json(completedState());
    if (path.endsWith("/follow-ups")) { creations += 1; return json({}); }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  const mounted = renderWithProviders(<Practice />);
  await screen.findByRole("button", { name: "Start selected review" });
  const originalSetItem = localStorage.setItem.bind(localStorage);
  const storage = vi.spyOn(localStorage, "setItem").mockImplementation((key, value) => {
    if (key.startsWith("astrid.practice.v1.outbox.") && key.includes(".creation.")) {
      throw new DOMException("Storage full", "QuotaExceededError");
    }
    originalSetItem(key, value);
  });
  await user.click(screen.getByRole("checkbox", { name: "Review book · meaning" }));
  await user.click(screen.getByRole("button", { name: "Start selected review" }));
  expect(await screen.findByText(/operation was not sent/)).toBeVisible();
  expect(creations).toBe(0);
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(practiceId);
  storage.mockRestore();
  mounted.unmount();
  window.history.replaceState(null, "", "/practice");
  renderWithProviders(<Practice />);
  expect(await screen.findByRole("heading", { name: "Practice complete" })).toBeVisible();
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
});
