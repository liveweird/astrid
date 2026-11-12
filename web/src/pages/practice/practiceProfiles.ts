import { queryOptions } from "@tanstack/react-query";
import { loadPracticeProfiles, startDevelopmentSession } from "../../api/client";
import type {
  PracticeCreationRequest, PracticeFollowUpRequest, PracticeProfile, PracticeProfiles, PracticeSession,
} from "../../api/models";
import { isOwnerUnavailable, outboxEntry, PracticeCatalogError } from "./practiceOperations";
import type { PracticeRunRecovery } from "./practiceRecovery";

let profileBootstrap: Promise<PracticeProfiles> | null = null;

export const VOCABULARY_P1_PROFILES = [
  {
    focusId: "vocabulary-p1-s1", planRevision: "vocabulary-p1-s1-2", title: "Work, reading and today",
    description: "Recognise actions, a book and today, then read a short routine.",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002"],
  },
  {
    focusId: "vocabulary-p1-s2", planRevision: "vocabulary-p1-s2-2", title: "Reading and writing",
    description: "Match words for reading, writing and a computer, then find stated facts.",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "vocabulary-p1-s3", planRevision: "vocabulary-p1-s3-2", title: "Study, courses and Danish",
    description: "Match new meanings, build a reading statement and recall introduced forms.",
    itemCount: 5, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "vocabulary-p1-s4", planRevision: "vocabulary-p1-s4-2", title: "Coffee and water",
    description: "Read who drinks what, choose a present form and recall a beverage word.",
    itemCount: 5, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "vocabulary-p1-s5", planRevision: "vocabulary-p1-s5-2", title: "Where and when",
    description: "Understand places and the morning, then build two supplied work statements.",
    itemCount: 5, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
] satisfies PracticeProfile[];

const PRESENTATION_PROFILES = [
  {
    focusId: "module-one-singular", planRevision: "module-one-singular-1", title: "Talking about one person",
    description: "Choose and type Danish subject words for one speaker, addressee or person.",
    itemCount: 6, prerequisiteSkillIds: ["G001", "G002"],
  },
  {
    focusId: "module-one-groups", planRevision: "module-one-groups-1", title: "Talking to and about groups",
    description: "Choose and type Danish subject words for speakers, addressees and groups.",
    itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003"],
  },
  {
    focusId: "module-one-er", planRevision: "module-one-er-1", title: "Using the verb to be",
    description: "Choose and type the Danish present form er in supplied statements.",
    itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003"],
  },
  {
    focusId: "module-one-things", planRevision: "module-one-things-1", title: "Referring to one thing",
    description: "Choose and type den or det for a previously mentioned thing with its gender supplied.",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G006"],
  },
  {
    focusId: "module-one-present", planRevision: "module-one-present-1", title: "Using present verb forms",
    description: "Choose and type present forms of three Danish verbs.",
    itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003"],
  },
  {
    focusId: "module-one-order", planRevision: "module-one-order-1", title: "Building subject-first statements",
    description: "Put Danish sentence parts and words in order.",
    itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003", "G004"],
  },
  {
    focusId: "module-one-possession", planRevision: "module-one-possession-1", title: "Saying what someone has",
    description: "Choose and type the Danish present form har for possession.",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "module-one-reading-words", planRevision: "module-one-reading-words-1", title: "Words for things you read",
    description: "Choose Danish words for a magazine, a book, a newspaper and a letter.",
    itemCount: 4, prerequisiteSkillIds: [],
  },
  {
    focusId: "module-one-personal", planRevision: "module-one-personal-1", title: "Names, homes and languages",
    description: "Choose and type Danish present forms for names, places and languages.",
    itemCount: 6, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "module-one-ability", planRevision: "module-one-ability-1", title: "Using a verb after kan",
    description: "Choose and type the bare infinitive læse after kan.",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "module-one-volition", planRevision: "module-one-volition-1", title: "Expressing an intention",
    description: "Interpret vil as intention and build one supplied vil statement.",
    itemCount: 2, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "module-one-obligation", planRevision: "module-one-obligation-1", title: "Understanding a requirement",
    description: "Interpret skal as a requirement and build one supplied skal statement.",
    itemCount: 2, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "module-one-permission", planRevision: "module-one-permission-1", title: "Understanding permission",
    description: "Interpret må as permission and build one supplied må statement.",
    itemCount: 2, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "module-one-circumstances", planRevision: "module-one-circumstances-1", title: "Adding when and where",
    description: "Keep a supplied clause core in order and add one supplied time or place circumstance at the end.",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
  {
    focusId: "module-one-questions", planRevision: "module-one-questions-1", title: "Asking yes or no questions",
    description: "Arrange supplied Danish constituents into neutral positive yes/no questions.",
    itemCount: 4, prerequisiteSkillIds: ["G001", "G002", "G003", "G004", "G005"],
  },
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
  ...VOCABULARY_P1_PROFILES,
] satisfies PracticeProfile[];

export const MODULE_ONE_PROFILES = [
  PRESENTATION_PROFILES[0]!,
  PRESENTATION_PROFILES[1]!,
  PRESENTATION_PROFILES[2]!,
  PRESENTATION_PROFILES[3]!,
  PRESENTATION_PROFILES[4]!,
  PRESENTATION_PROFILES[5]!,
  PRESENTATION_PROFILES[6]!,
  PRESENTATION_PROFILES[7]!,
  PRESENTATION_PROFILES[8]!,
  PRESENTATION_PROFILES[9]!,
  PRESENTATION_PROFILES[10]!,
  PRESENTATION_PROFILES[11]!,
  PRESENTATION_PROFILES[12]!,
  PRESENTATION_PROFILES[13]!,
  PRESENTATION_PROFILES[14]!,
];
export const EXISTING_PRACTICE_PROFILES = [
  PRESENTATION_PROFILES[15]!,
  PRESENTATION_PROFILES[16]!,
  PRESENTATION_PROFILES[17]!,
];

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
    expected.delete(profileKey(profile));
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

function creationRequestFor(
  profile: PracticeProfile,
  practiceSessionId: string,
  seed: number,
): PracticeCreationRequest {
  switch (profileKey(profile)) {
    case "reading-basics/practice-1":
      return { practiceSessionId, focusId: "reading-basics", planRevision: "practice-1", seed };
    case "present-verbs/present-verbs-1":
      return { practiceSessionId, focusId: "present-verbs", planRevision: "present-verbs-1", seed };
    case "verb-article-gaps/verb-article-gaps-1":
      return { practiceSessionId, focusId: "verb-article-gaps", planRevision: "verb-article-gaps-1", seed };
    case "module-one-singular/module-one-singular-1":
      return { practiceSessionId, focusId: "module-one-singular", planRevision: "module-one-singular-1", seed };
    case "module-one-groups/module-one-groups-1":
      return { practiceSessionId, focusId: "module-one-groups", planRevision: "module-one-groups-1", seed };
    case "module-one-er/module-one-er-1":
      return { practiceSessionId, focusId: "module-one-er", planRevision: "module-one-er-1", seed };
    case "module-one-things/module-one-things-1":
      return { practiceSessionId, focusId: "module-one-things", planRevision: "module-one-things-1", seed };
    case "module-one-present/module-one-present-1":
      return { practiceSessionId, focusId: "module-one-present", planRevision: "module-one-present-1", seed };
    case "module-one-order/module-one-order-1":
      return { practiceSessionId, focusId: "module-one-order", planRevision: "module-one-order-1", seed };
    case "module-one-possession/module-one-possession-1":
      return { practiceSessionId, focusId: "module-one-possession", planRevision: "module-one-possession-1", seed };
    case "module-one-reading-words/module-one-reading-words-1":
      return { practiceSessionId, focusId: "module-one-reading-words", planRevision: "module-one-reading-words-1", seed };
    case "module-one-personal/module-one-personal-1":
      return { practiceSessionId, focusId: "module-one-personal", planRevision: "module-one-personal-1", seed };
    case "module-one-ability/module-one-ability-1":
      return { practiceSessionId, focusId: "module-one-ability", planRevision: "module-one-ability-1", seed };
    case "module-one-volition/module-one-volition-1":
      return { practiceSessionId, focusId: "module-one-volition", planRevision: "module-one-volition-1", seed };
    case "module-one-obligation/module-one-obligation-1":
      return { practiceSessionId, focusId: "module-one-obligation", planRevision: "module-one-obligation-1", seed };
    case "module-one-permission/module-one-permission-1":
      return { practiceSessionId, focusId: "module-one-permission", planRevision: "module-one-permission-1", seed };
    case "module-one-circumstances/module-one-circumstances-1":
      return { practiceSessionId, focusId: "module-one-circumstances", planRevision: "module-one-circumstances-1", seed };
    case "module-one-questions/module-one-questions-1":
      return { practiceSessionId, focusId: "module-one-questions", planRevision: "module-one-questions-1", seed };
    case "vocabulary-p1-s1/vocabulary-p1-s1-2":
      return { practiceSessionId, focusId: "vocabulary-p1-s1", planRevision: "vocabulary-p1-s1-2", seed };
    case "vocabulary-p1-s2/vocabulary-p1-s2-2":
      return { practiceSessionId, focusId: "vocabulary-p1-s2", planRevision: "vocabulary-p1-s2-2", seed };
    case "vocabulary-p1-s3/vocabulary-p1-s3-2":
      return { practiceSessionId, focusId: "vocabulary-p1-s3", planRevision: "vocabulary-p1-s3-2", seed };
    case "vocabulary-p1-s4/vocabulary-p1-s4-2":
      return { practiceSessionId, focusId: "vocabulary-p1-s4", planRevision: "vocabulary-p1-s4-2", seed };
    case "vocabulary-p1-s5/vocabulary-p1-s5-2":
      return { practiceSessionId, focusId: "vocabulary-p1-s5", planRevision: "vocabulary-p1-s5-2", seed };
    default:
      throw new PracticeCatalogError("The selected practice profile has an unsupported revision.");
  }
}

export async function preparePracticeCreation(chosenKey: string) {
  const catalog = await profilesForExplicitStart();
  const profile = catalog.profiles.find((candidate) => profileKey(candidate) === chosenKey);
  if (!profile) throw new PracticeCatalogError("The selected practice is not available in the current catalog.");
  const practiceSessionId = crypto.randomUUID(), seed = freshPracticeSeed();
  const request = creationRequestFor(profile, practiceSessionId, seed);
  const recovery: PracticeRunRecovery = { ownerSessionId: catalog.ownerSessionId, ...request };
  const creation = outboxEntry(recovery, "creation", request.practiceSessionId, request);
  return { recovery, creation };
}

export function taughtProfileKey(session: Pick<PracticeRunRecovery, "focusId" | "planRevision">) {
  const profile = [...MODULE_ONE_PROFILES, ...VOCABULARY_P1_PROFILES].find((candidate) =>
    candidate.focusId === session.focusId && candidate.planRevision === session.planRevision);
  if (profile) return profileKey(profile);
  const retained = VOCABULARY_P1_PROFILES.find((candidate) =>
    candidate.focusId === session.focusId && session.planRevision === `${candidate.focusId}-1`);
  return retained ? profileKey(retained) : null;
}

export function nextTaughtProfile(session: Pick<PracticeRunRecovery, "focusId" | "planRevision">) {
  const currentKey = taughtProfileKey(session);
  const sequence = VOCABULARY_P1_PROFILES.some((profile) => profileKey(profile) === currentKey)
    ? VOCABULARY_P1_PROFILES : MODULE_ONE_PROFILES;
  const index = sequence.findIndex((profile) => profileKey(profile) === currentKey);
  return index >= 0 ? sequence[index + 1] ?? null : null;
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
