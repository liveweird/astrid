import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  choosePractice, context, json, owner, practiceId, profiles, resetPracticeBrowser, state,
} from "../../test/practiceFixtures";
import { renderWithProviders } from "../../test/render";
import Practice from "./Practice";
import { listOutbox, saveOutboxEntry, saveRunRecovery } from "./practiceRecovery";

beforeEach(resetPracticeBrowser);

it("starts a chosen practice without deleting an uncertain saved run", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let renewed = false;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) { renewed = true; return json({ mode: "development" }); }
    if (path.endsWith("practice-profiles")) return json(profiles(renewed ? "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa" : owner));
    if (path.endsWith("practice-context")) return json(context(renewed ? "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa" : owner));
    if (path.endsWith("/attempts")) throw new TypeError("connection closed");
    if (path.endsWith("practice-sessions")) return json({ ...state(), id: "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb" }, 201);
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Check answer" }));
  const startNew = await screen.findByRole("button", { name: "Start new practice" });
  expect(startNew).toBeEnabled();
  const recoveryMessage = screen.getByRole("alert").textContent;
  const savedAttempt = listOutbox(practiceId)[0]!;
  await user.click(startNew);
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  expect(screen.getByRole("alert")).toHaveTextContent(recoveryMessage!);
  expect(screen.getByRole("button", { name: "Retry saved operation" })).toBeVisible();
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  expect(listOutbox(practiceId)[0]?.body).toBe(savedAttempt.body);
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  await choosePractice(user);
  await waitFor(() => expect(new URLSearchParams(window.location.search).get("session")).not.toBe(practiceId));
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
});

it("keeps the current recovery while choosing and after starting another practice", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let renewed = false;
  const randomSeeds = [123, 456];
  vi.spyOn(crypto, "getRandomValues").mockImplementation((array) => {
    (array as Uint32Array)[0] = randomSeeds.shift()!;
    return array;
  });
  const creationSeeds: number[] = [];
  const nextOwner = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) { renewed = true; return json({ mode: "development" }); }
    if (path.endsWith("practice-profiles")) return json(profiles(renewed ? nextOwner : owner));
    if (path.endsWith("practice-context")) return json(context(renewed ? nextOwner : owner));
    if (path.endsWith("practice-sessions")) {
      creationSeeds.push(JSON.parse(String(init?.body)).seed);
      return json({ ...state(), ownerSessionId: nextOwner }, 201);
    }
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "Start new practice" }));
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  await choosePractice(user);
  await waitFor(() => expect(new URLSearchParams(window.location.search).get("session")).not.toBe(practiceId));
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  await choosePractice(user);
  await waitFor(() => expect(creationSeeds).toEqual([123, 456]));
});

it("deduplicates fresh catalog bootstrap across Strict Mode remounts without creating a run", async () => {
  const paths: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    paths.push(path);
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    throw new Error(`Unexpected ${path}`);
  }));

  renderWithProviders(<Practice />, { strict: true });
  expect(await screen.findByRole("heading", { name: "Choose your practice" })).toBeVisible();
  expect(screen.getByRole("button", { name: "Start practice" })).toBeDisabled();
  expect(paths.filter((path) => path.endsWith("development-session"))).toHaveLength(1);
  expect(paths.filter((path) => path.endsWith("practice-profiles"))).toHaveLength(1);
  expect(paths.filter((path) => path.endsWith("practice-sessions"))).toHaveLength(0);
});

it("shows all six P2 catalog profiles and starts the explicitly selected lesson identity", async () => {
  let creation = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) {
      creation = String(init?.body);
      const request = JSON.parse(creation) as { practiceSessionId: string };
      return json(state({
        id: request.practiceSessionId, focusId: "vocabulary-p2-s6", planRevision: "vocabulary-p2-s6-1",
        title: "Requesting a supplied quantity", itemCount: 4, introductionRequired: true,
      }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);

  expect(await screen.findByRole("heading", { name: "Food, cafés and shopping" })).toBeVisible();
  for (const title of [
    "A small breakfast", "Drinks on the table", "Getting attention and receiving a roll",
    "Buying and paying", "Marking a polite wish", "Requesting a supplied quantity",
  ]) {
    expect(screen.getByRole("radio", { name: title })).not.toBeChecked();
  }
  await choosePractice(user, "Requesting a supplied quantity");

  expect(await screen.findByRole("heading", { name: "Requesting a supplied quantity" })).toBeVisible();
  expect(JSON.parse(creation)).toMatchObject({
    focusId: "vocabulary-p2-s6", planRevision: "vocabulary-p2-s6-1",
  });
});

it("ignores a stale Strict Mode recovery after a newer chosen run starts", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  saveOutboxEntry({ ...run, kind: "creation", identity: practiceId, body: JSON.stringify({
    practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47,
  }), queuedAt: 1 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let contextReads = 0;
  let resolveStale: ((response: Response) => void) | undefined;
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("practice-context")) {
      contextReads += 1;
      if (contextReads === 1) return new Promise<Response>((resolve) => { resolveStale = resolve; });
      return json(context());
    }
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) {
      const request = JSON.parse(String(init?.body)) as { practiceSessionId: string; focusId: string; planRevision: string };
      return json(state({ id: request.practiceSessionId, focusId: request.focusId, planRevision: request.planRevision,
        title: request.practiceSessionId === practiceId ? "Words and simple statements" : "Present verb forms" }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />, { strict: true });
  await screen.findByRole("heading", { name: "Words and simple statements" });
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  await choosePractice(user, "Present verb forms");
  expect(await screen.findByRole("heading", { name: "Present verb forms" })).toBeVisible();
  resolveStale?.(json({ detail: "stale recovery expired" }, 401));
  await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
  expect(screen.queryByRole("button", { name: "Retry saved operation" })).not.toBeInTheDocument();
  expect(screen.getByRole("heading", { name: "Present verb forms" })).toBeVisible();
});

it("starts only the explicitly selected present-verb profile with its exact identity", async () => {
  const creations: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) {
      creations.push(String(init?.body));
      const request = JSON.parse(String(init?.body)) as { practiceSessionId: string };
      return json(state({ id: request.practiceSessionId, focusId: "present-verbs", planRevision: "present-verbs-1",
        title: "Present verb forms", itemCount: 6, followUpPlanRevision: "follow-up-2" }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await screen.findByRole("heading", { name: "Choose your practice" });
  expect(screen.getByRole("radio", { name: "Mixed practice" })).not.toBeChecked();
  expect(screen.getByRole("radio", { name: "Present verb forms" })).not.toBeChecked();
  expect(screen.getByRole("button", { name: "Start practice" })).toBeDisabled();
  expect(creations).toEqual([]);
  await choosePractice(user, "Present verb forms");
  expect(await screen.findByRole("heading", { name: "Present verb forms" })).toBeVisible();
  expect(creations).toHaveLength(1);
  expect(JSON.parse(creations[0]!)).toMatchObject({ focusId: "present-verbs", planRevision: "present-verbs-1" });
});

it("starts the four-part verbs-and-articles profile with its pinned identity", async () => {
  let creation = "";
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) {
      creation = String(init?.body);
      const request = JSON.parse(creation) as { practiceSessionId: string };
      return json(state({
        id: request.practiceSessionId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1",
        title: "Verbs and articles together", itemCount: 4, followUpPlanRevision: "follow-up-3",
      }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await choosePractice(user, "Verbs and articles together");

  expect(await screen.findByRole("heading", { name: "Verbs and articles together" })).toBeVisible();
  expect(JSON.parse(creation)).toMatchObject({
    focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1",
  });
});

it("opens and cancels the chooser for an expired owner without renewing or changing the draft", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let renewals = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) { renewals += 1; return json({ mode: "development" }); }
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-profiles")) return json({ detail: "expired" }, 401);
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "dog" }));
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  expect(await screen.findByRole("heading", { name: "Choose your practice" })).toBeVisible();
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  expect(screen.getByRole("button", { name: "dog" })).toHaveAttribute("aria-pressed", "true");
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  expect(renewals).toBe(0);
});

it("keeps the previous run and URL when saving a chosen creation fails partway", async () => {
  const run = { ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 };
  saveRunRecovery(run);
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("practice-context")) return json(context());
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) { creations += 1; return json(state()); }
    if (path.includes("/practice-sessions/")) return json(state());
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "Start new practice" }));
  const originalSetItem = localStorage.setItem.bind(localStorage);
  const storage = vi.spyOn(localStorage, "setItem").mockImplementation((key, value) => {
    if (key.startsWith("astrid.practice.v1.outbox.") && key.includes(".creation.")) {
      throw new DOMException("Storage full", "QuotaExceededError");
    }
    originalSetItem(key, value);
  });
  await choosePractice(user, "Present verb forms");
  expect(await screen.findByText("This browser could not save recovery data. The operation was not sent.")).toBeVisible();
  expect(creations).toBe(0);
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
  expect(localStorage.getItem("astrid.practice.v1.latest")).toBe(practiceId);
  storage.mockRestore();
});

it("keeps the chooser safe when the server catalog is unsupported", async () => {
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json({ ...profiles(), profiles: [profiles().profiles[0]] });
    if (path.endsWith("practice-sessions")) { creations += 1; return json(state()); }
    throw new Error(`Unexpected ${path}`);
  }));
  renderWithProviders(<Practice />);
  expect(await screen.findByText("The practice catalog is unavailable or incomplete.")).toBeVisible();
  expect(screen.getByRole("button", { name: "Start practice" })).toBeDisabled();
  expect(creations).toBe(0);
});

it("bootstraps only after an explicit start finds that the catalog owner expired", async () => {
  let profileReads = 0;
  let bootstraps = 0;
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) { bootstraps += 1; return json({ mode: "development" }); }
    if (path.endsWith("practice-profiles")) {
      profileReads += 1;
      if (profileReads === 2) return json({ detail: "owner expired" }, 401);
      return json(profiles());
    }
    if (path.endsWith("practice-sessions")) {
      creations += 1;
      const request = JSON.parse(String(init?.body)) as { practiceSessionId: string };
      return json(state({ id: request.practiceSessionId }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await choosePractice(user);
  expect(await screen.findByRole("heading", { name: "Words and simple statements" })).toBeVisible();
  expect(profileReads).toBe(3);
  expect(bootstraps).toBe(2);
  expect(creations).toBe(1);
});

it("keeps the prior run when explicit catalog validation fails without an owner-expiry response", async () => {
  saveRunRecovery({ ownerSessionId: owner, practiceSessionId: practiceId, focusId: "reading-basics", planRevision: "practice-1", seed: 47 });
  window.history.replaceState(null, "", `/practice?session=${practiceId}`);
  let profileReads = 0;
  let bootstraps = 0;
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path.endsWith("development-session")) { bootstraps += 1; return json({ mode: "development" }); }
    if (path.endsWith("practice-context")) return json(context());
    if (path.includes(`/practice-sessions/${practiceId}`)) return json(state());
    if (path.endsWith("practice-profiles")) {
      profileReads += 1;
      return profileReads === 1 ? json(profiles()) : json({ detail: "catalog temporarily unavailable" }, 503);
    }
    if (path.endsWith("practice-sessions")) { creations += 1; return json(state()); }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await user.click(await screen.findByRole("button", { name: "Start new practice" }));
  await waitFor(() => expect(profileReads).toBe(1));
  await choosePractice(user, "Present verb forms");
  expect(await screen.findByText("catalog temporarily unavailable")).toBeVisible();
  expect(new URLSearchParams(window.location.search).get("session")).toBe(practiceId);
  expect(localStorage.getItem(`astrid.practice.v1.run.${practiceId}`)).not.toBeNull();
  expect(bootstraps).toBe(0);
  expect(creations).toBe(0);
});

it("binds an uncertain chosen creation and keeps it when another practice starts", async () => {
  let creations = 0;
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) {
      const body = JSON.parse(String(init?.body)) as { practiceSessionId: string };
      expect(new URLSearchParams(window.location.search).get("session")).toBe(body.practiceSessionId);
      creations += 1;
      if (creations === 1) throw new TypeError("creation response lost");
      return json({ ...state(), id: body.practiceSessionId }, 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await choosePractice(user, "Present verb forms");
  await screen.findByRole("alert");
  const original = new URLSearchParams(window.location.search).get("session");
  expect(original).not.toBeNull();
  const savedCreation = listOutbox(original!)[0]!;
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  expect(screen.getByRole("button", { name: "Cancel" })).toBeVisible();
  await user.click(screen.getByRole("button", { name: "Cancel" }));
  expect(screen.getByRole("button", { name: "Retry saved operation" })).toBeVisible();
  expect(new URLSearchParams(window.location.search).get("session")).toBe(original);
  expect(listOutbox(original!)[0]?.body).toBe(savedCreation.body);
  await user.click(screen.getByRole("button", { name: "Start new practice" }));
  await choosePractice(user);
  await screen.findByRole("button", { name: "Check answer" });
  expect(new URLSearchParams(window.location.search).get("session")).not.toBe(original);
  expect(localStorage.getItem(`astrid.practice.v1.run.${original}`)).not.toBeNull();
  expect(listOutbox(original!)).toHaveLength(1);
});

it("retries a lost selected-profile creation with the exact saved body", async () => {
  const bodies: string[] = [];
  vi.stubGlobal("fetch", vi.fn(async (path: string, init?: RequestInit) => {
    if (path.endsWith("development-session")) return json({ mode: "development" });
    if (path.endsWith("practice-profiles")) return json(profiles());
    if (path.endsWith("practice-sessions")) {
      bodies.push(String(init?.body));
      const request = JSON.parse(String(init?.body)) as { practiceSessionId: string };
      if (bodies.length === 1) throw new TypeError("creation response lost");
      return json(state({ id: request.practiceSessionId, focusId: "present-verbs", planRevision: "present-verbs-1",
        title: "Present verb forms", itemCount: 6, followUpPlanRevision: "follow-up-2" }), 201);
    }
    throw new Error(`Unexpected ${path}`);
  }));
  const user = userEvent.setup();
  renderWithProviders(<Practice />);
  await choosePractice(user, "Present verb forms");
  const retry = await screen.findByRole("button", { name: "Retry saved operation" });
  const bound = new URLSearchParams(window.location.search).get("session");
  expect(bound).toBe(JSON.parse(bodies[0]!).practiceSessionId);
  await user.click(retry);
  expect(await screen.findByRole("heading", { name: "Present verb forms" })).toBeVisible();
  expect(bodies[1]).toBe(bodies[0]);
  expect(JSON.parse(bodies[1]!)).toMatchObject({ focusId: "present-verbs", planRevision: "present-verbs-1" });
});
