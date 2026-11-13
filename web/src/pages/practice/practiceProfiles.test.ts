import { owner, practiceId, profiles, resetPracticeBrowser } from "../../test/practiceFixtures";
import {
  nextTaughtProfile, preparePracticeCreation, profileKey, taughtProfileKey, VOCABULARY_P1_PROFILES,
  VOCABULARY_P2_PROFILES,
} from "./practiceProfiles";
import { listOutbox, loadRunRecovery, saveRunWithCreation } from "./practiceRecovery";

beforeEach(resetPracticeBrowser);

it.each(VOCABULARY_P1_PROFILES)("maps retained and current $focusId completion to current repeat and next choices", (profile) => {
  const retained = { focusId: profile.focusId, planRevision: `${profile.focusId}-1` };
  const current = { focusId: profile.focusId, planRevision: `${profile.focusId}-2` };
  expect(taughtProfileKey(retained)).toBe(profileKey(profile));
  expect(taughtProfileKey(current)).toBe(profileKey(profile));
  expect(nextTaughtProfile(retained)).toEqual(nextTaughtProfile(current));
  const next = VOCABULARY_P1_PROFILES[VOCABULARY_P1_PROFILES.indexOf(profile) + 1];
  expect(nextTaughtProfile(retained)).toEqual(next ?? null);
  expect(taughtProfileKey({ ...retained, planRevision: `${profile.focusId}-unsupported` })).toBeNull();
});

it.each(VOCABULARY_P2_PROFILES)("maps current $focusId completion to its repeat and next choices", (profile) => {
  expect(taughtProfileKey(profile)).toBe(profileKey(profile));
  const next = VOCABULARY_P2_PROFILES[VOCABULARY_P2_PROFILES.indexOf(profile) + 1];
  expect(nextTaughtProfile(profile)).toEqual(next ?? null);
});

it.each(VOCABULARY_P2_PROFILES)("creates the exact current request for $focusId", async (profile) => {
  vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify(profiles()), {
    status: 200, headers: { "Content-Type": "application/json" },
  })));
  try {
    const prepared = await preparePracticeCreation(profileKey(profile));
    expect(JSON.parse(prepared.creation.body)).toMatchObject({
      focusId: profile.focusId, planRevision: profile.planRevision,
    });
  } finally {
    vi.unstubAllGlobals();
  }
});

it("starts the current P1 revision without rewriting a retained creation outbox", async () => {
  const retained = {
    ownerSessionId: owner, practiceSessionId: practiceId,
    focusId: "vocabulary-p1-s2", planRevision: "vocabulary-p1-s2-1", seed: 47,
  };
  const creation = {
    ...retained, kind: "creation" as const, identity: practiceId, queuedAt: 100,
    body: JSON.stringify({
      practiceSessionId: practiceId, focusId: retained.focusId, planRevision: retained.planRevision, seed: 47,
    }, null, 2),
  };
  saveRunWithCreation(retained, creation);
  vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify(profiles()), {
    status: 200, headers: { "Content-Type": "application/json" },
  })));
  try {
    const prepared = await preparePracticeCreation(taughtProfileKey(retained)!);
    expect(JSON.parse(prepared.creation.body)).toMatchObject({
      focusId: "vocabulary-p1-s2", planRevision: "vocabulary-p1-s2-2",
    });
    expect(loadRunRecovery(practiceId)).toEqual(retained);
    expect(listOutbox(practiceId)).toEqual([creation]);
  } finally {
    vi.unstubAllGlobals();
  }
});
