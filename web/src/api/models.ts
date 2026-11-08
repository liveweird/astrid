import type { components } from "./schema";

export type DevelopmentSession = components["schemas"]["DevelopmentSession"];
export type Catalog = components["schemas"]["Catalog"];

export type Exercise = components["schemas"]["Exercise"];

// The server's Kotlin discriminator uses these wire values. Keeping the union explicit also
// avoids generators substituting component names for discriminator values.
export type MatchingPair = components["schemas"]["MatchingPair"];
export type MatchingColumns = components["schemas"]["MatchingColumns"];
export type MatchingPairResult = components["schemas"]["MatchingPairResult"];
export type AttemptAnswer =
  | { kind: "text"; text: string }
  | { kind: "order"; blockIds: string[] }
  | { kind: "matching"; pairs: MatchingPair[] };
export type AttemptRequest = Omit<components["schemas"]["AttemptRequest"], "answer"> & { answer: AttemptAnswer };
export type Attempt = components["schemas"]["Attempt"];

export type InspectorDetails = components["schemas"]["Inspector"];

export type PracticeContext = components["schemas"]["PracticeContext"];
export type PracticeProfiles = components["schemas"]["PracticeProfiles"];
export type PracticeProfile = components["schemas"]["PracticeProfile"];
export type PracticeSummaryRow = components["schemas"]["PracticeSummaryRow"];
export type PracticeSession = components["schemas"]["PracticeSession"];
export type PracticeCreationRequest = components["schemas"]["PracticeSessionRequest"];
export type PracticeFollowUpRequest = components["schemas"]["PracticeFollowUpRequest"];
