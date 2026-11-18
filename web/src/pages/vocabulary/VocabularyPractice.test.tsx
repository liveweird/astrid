import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { VocabularyContext } from "../../api/models";
import { renderWithProviders } from "../../test/render";
import { latestRunRecovery, listOutbox, saveRunWithCreation } from "../practice/practiceRecovery";
import VocabularyPractice from "./VocabularyPractice";

const owner = "28d52d72-7a04-4a42-8e87-bf780f821510";
const context: VocabularyContext = {
  libraryRevision: "vocabulary-library-1",
  ownerSessionId: owner,
  sessionExpiresAt: Date.now() + 60_000,
  items: [],
  practice: { planRevision: "vocabulary-library-practice-1", recognitionCount: 3, recallCount: 1 },
};

function stubContext(body: unknown, status = 200) {
  const fetchMock = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

beforeEach(() => {
  localStorage.clear();
  window.history.replaceState(null, "", "/vocabulary");
});
afterEach(() => vi.unstubAllGlobals());

it("offers only the activities supported by saved teaching", () => {
  renderWithProviders(<VocabularyPractice
    context={{ ...context, practice: { ...context.practice!, recallCount: 0 } }}
    onInvalidate={vi.fn()}
  />);
  expect(screen.getByRole("button", { name: "Recognize meanings (3)" })).toBeVisible();
  expect(screen.queryByRole("button", { name: /Write Danish/ })).toBeNull();
});

it.each([
  ["Recognize meanings (3)", "vocabulary-library-recognition"],
  ["Write Danish (1)", "vocabulary-library-recall"],
])("revalidates the owner and stages the exact creation for %s", async (label, focusId) => {
  const fetchMock = stubContext(context);
  renderWithProviders(<VocabularyPractice context={context} onInvalidate={vi.fn()} />);
  await userEvent.click(screen.getByRole("button", { name: label }));
  await waitFor(() => expect(window.location.pathname).toBe("/practice"));
  const recovery = latestRunRecovery()!;
  expect(window.location.search).toBe(`?session=${recovery.practiceSessionId}`);
  expect(recovery).toMatchObject({ ownerSessionId: owner, focusId, planRevision: "vocabulary-library-practice-1" });
  const [creation] = listOutbox(recovery.practiceSessionId);
  expect(creation).toMatchObject({ kind: "creation", identity: recovery.practiceSessionId, ownerSessionId: owner });
  expect(JSON.parse(creation!.body)).toEqual({
    practiceSessionId: recovery.practiceSessionId, focusId, planRevision: recovery.planRevision, seed: recovery.seed,
  });
  expect(fetchMock).toHaveBeenCalledExactlyOnceWith("/api/v1/vocabulary-context", expect.objectContaining({
    credentials: "same-origin",
  }));
});

it.each([
  [{ ...context, ownerSessionId: "another-owner" }, 200, /teaching has changed/],
  [{ ...context, sessionExpiresAt: 1 }, 200, /teaching has changed/],
  [{ ...context, libraryRevision: "new-library" }, 200, /teaching has changed/],
  [{ ...context, practice: { ...context.practice!, recallCount: 0 } }, 200, /no longer available/],
  [{}, 401, /teaching expired/],
] as const)("keeps unavailable preparation out of the outbox", async (body, status, message) => {
  stubContext(body, status);
  const invalidate = vi.fn();
  renderWithProviders(<VocabularyPractice context={context} onInvalidate={invalidate} />);
  await userEvent.click(screen.getByRole("button", { name: "Write Danish (1)" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(message);
  expect(screen.getByRole("alert")).toHaveFocus();
  expect(invalidate).toHaveBeenCalledOnce();
  expect(window.location.pathname).toBe("/vocabulary");
  expect(latestRunRecovery()).toBeNull();
});

it("keeps a failed preparation retryable without staging an operation", async () => {
  stubContext({}, 503);
  const invalidate = vi.fn();
  renderWithProviders(<VocabularyPractice context={context} onInvalidate={invalidate} />);
  const recall = screen.getByRole("button", { name: "Write Danish (1)" });
  await userEvent.click(recall);
  expect(await screen.findByRole("alert")).toHaveTextContent("could not be prepared");
  expect(recall).toBeEnabled();
  expect(invalidate).not.toHaveBeenCalled();
  expect(latestRunRecovery()).toBeNull();
});

it("does not stage a run if the learner leaves while its owner is being checked", async () => {
  let finish: (response: Response) => void = () => {};
  vi.stubGlobal("fetch", vi.fn(() => new Promise<Response>((resolve) => { finish = resolve; })));
  const { unmount } = renderWithProviders(<VocabularyPractice context={context} onInvalidate={vi.fn()} />);
  await userEvent.click(screen.getByRole("button", { name: "Write Danish (1)" }));
  unmount();
  finish(new Response(JSON.stringify(context)));
  await Promise.resolve();
  expect(latestRunRecovery()).toBeNull();
  expect(window.location.pathname).toBe("/vocabulary");
});

it("offers recovery and keeps the saved run intact when a new start is cancelled", async () => {
  const run = {
    ownerSessionId: owner, practiceSessionId: "3614db25-afa3-43cb-8096-854f8180ad6c",
    focusId: "reading-basics", planRevision: "practice-1", seed: 3,
  };
  const entry = {
    ...run, kind: "creation" as const, identity: run.practiceSessionId, body: '{"seed":3}',
  };
  saveRunWithCreation(run, entry);
  const previous = listOutbox(run.practiceSessionId);
  const fetchMock = stubContext(context);
  renderWithProviders(<VocabularyPractice context={context} onInvalidate={vi.fn()} />);
  const recognize = screen.getByRole("button", { name: "Recognize meanings (3)" });
  await userEvent.click(recognize);
  expect(screen.getByRole("link", { name: "Resume saved run" }))
    .toHaveAttribute("href", `/practice?session=${run.practiceSessionId}`);
  await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
  expect(recognize).toHaveFocus();
  expect(latestRunRecovery()).toEqual(run);
  expect(listOutbox(run.practiceSessionId)).toEqual(previous);
  expect(fetchMock).not.toHaveBeenCalled();

  await userEvent.click(recognize);
  await userEvent.click(screen.getByRole("button", { name: "Start new run" }));
  await waitFor(() => expect(window.location.pathname).toBe("/practice"));
  expect(latestRunRecovery()?.focusId).toBe("vocabulary-library-recognition");
  expect(listOutbox(run.practiceSessionId)).toEqual(previous);
});
