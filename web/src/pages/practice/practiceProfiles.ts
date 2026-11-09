import { queryOptions } from "@tanstack/react-query";
import { loadPracticeProfiles, startDevelopmentSession } from "../../api/client";
import type {
  PracticeCreationRequest, PracticeFollowUpRequest, PracticeProfile, PracticeProfiles, PracticeSession,
} from "../../api/models";
import { isOwnerUnavailable, outboxEntry, PracticeCatalogError } from "./practiceOperations";
import type { PracticeRunRecovery } from "./practiceRecovery";

let profileBootstrap: Promise<PracticeProfiles> | null = null;

export const PRESENTATION_PROFILES = [
  {
    focusId: "reading-basics", planRevision: "practice-1", title: "Mixed practice",
    description: "Practise familiar words, sentence order and articles in eight varied parts.",
    itemCount: 8, prerequisiteSkillIds: [],
  },
  {
    focusId: "present-verbs", planRevision: "present-verbs-1", title: "Present verb forms",
    description: "Choose, recall and repair present-tense verb forms in six focused parts.",
    itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003"],
  },
  {
    focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", title: "Verbs and articles together",
    description: "Practise a present verb and an indefinite article in the same sentence.",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G005"],
  },
] satisfies PracticeProfile[];

export function profileKey(profile: Pick<PracticeProfile, "focusId" | "planRevision">) {
  return `${profile.focusId}/${profile.planRevision}`;
}

export function validateProfiles(catalog: PracticeProfiles) {
  const expected = new Map(PRESENTATION_PROFILES.map((profile) => [profileKey(profile), profile]));
  if (!catalog.ownerSessionId || catalog.sessionExpiresAt <= 0 || catalog.profiles.length !== expected.size) {
    throw new PracticeCatalogError("The practice catalog is unavailable or incomplete.");
  }
  for (const profile of catalog.profiles) {
    const known = expected.get(profileKey(profile));
    if (!known || profile.title !== known.title || profile.itemCount !== known.itemCount) {
      throw new PracticeCatalogError("The practice catalog contains an unsupported profile.");
    }
  }
  return catalog;
}

async function bootstrapPracticeProfiles() {
  profileBootstrap ??= (async () => {
    await startDevelopmentSession();
    return validateProfiles(await loadPracticeProfiles());
  })();
  try {
    return await profileBootstrap;
  } finally {
    profileBootstrap = null;
  }
}

/**
 * GET /practice-profiles for a fresh chooser, after establishing the development session. An
 * explicit start never reads this cache: it re-reads the profiles to learn the current owner.
 */
export const practiceProfilesQuery = queryOptions({
  queryKey: ["practice-profiles"],
  queryFn: bootstrapPracticeProfiles,
});

async function profilesForExplicitStart() {
  try {
    return validateProfiles(await loadPracticeProfiles());
  } catch (caught) {
    if (isOwnerUnavailable(caught)) return bootstrapPracticeProfiles();
    throw caught;
  }
}

export async function preparePracticeCreation(chosenKey: string) {
  const catalog = await profilesForExplicitStart();
  const profile = catalog.profiles.find((candidate) => profileKey(candidate) === chosenKey);
  if (!profile) throw new PracticeCatalogError("The selected practice is not available in the current catalog.");
  const practiceSessionId = crypto.randomUUID(), seed = freshPracticeSeed();
  let request: PracticeCreationRequest;
  if (profile.focusId === "reading-basics" && profile.planRevision === "practice-1") {
    request = { practiceSessionId, focusId: "reading-basics", planRevision: "practice-1", seed };
  } else if (profile.focusId === "present-verbs" && profile.planRevision === "present-verbs-1") {
    request = { practiceSessionId, focusId: "present-verbs", planRevision: "present-verbs-1", seed };
  } else if (profile.focusId === "verb-article-gaps" && profile.planRevision === "verb-article-gaps-1") {
    request = { practiceSessionId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed };
  } else {
    throw new PracticeCatalogError("The selected practice profile has an unsupported revision.");
  }
  const recovery: PracticeRunRecovery = { ownerSessionId: catalog.ownerSessionId, ...request };
  const creation = outboxEntry(recovery, "creation", request.practiceSessionId, request);
  return { recovery, creation };
}

export function followUpRevisionFor(session: PracticeSession) {
  if (session.followUpPlanRevision) return session.followUpPlanRevision;
  if (session.focusId === "reading-basics" && session.planRevision === "practice-1") return "follow-up-1" as const;
  if (session.focusId === "targeted-review" && session.planRevision === "follow-up-1") return "follow-up-1" as const;
  return null;
}

export function prepareFollowUpCreation(
  run: PracticeRunRecovery,
  session: PracticeSession,
  planRevision: PracticeFollowUpRequest["planRevision"],
  selectedTargets: Set<string>,
) {
  const request: PracticeFollowUpRequest = {
    practiceSessionId: crypto.randomUUID(),
    planRevision,
    seed: freshPracticeSeed(),
    targetKeys: [...selectedTargets],
  };
  const recovery: PracticeRunRecovery = {
    ownerSessionId: run.ownerSessionId,
    practiceSessionId: request.practiceSessionId,
    focusId: "targeted-review",
    planRevision: request.planRevision,
    seed: request.seed,
    sourcePracticeSessionId: session.id,
    selectedTargetKeys: request.targetKeys,
  };
  const creation = outboxEntry(recovery, "creation", request.practiceSessionId, request);
  return { recovery, creation };
}

function freshPracticeSeed() {
  return crypto.getRandomValues(new Uint32Array(1))[0]! & 0x7fffffff;
}
