import type { components } from "./schema";

export type DevelopmentSession = components["schemas"]["DevelopmentSession"];
export type Skill = components["schemas"]["Skill"];
export type ExerciseFormat = components["schemas"]["Format"];
export type Catalog = components["schemas"]["Catalog"];

export type PromptBlock = components["schemas"]["Block"];
export type Exercise = components["schemas"]["Exercise"];

// The server's Kotlin discriminator uses these wire values. Keeping the union explicit also
// avoids generators substituting component names for discriminator values.
export type AttemptAnswer = { kind: "text"; text: string } | { kind: "order"; blockIds: string[] };
export type AttemptRequest = Omit<components["schemas"]["AttemptRequest"], "answer"> & { answer: AttemptAnswer };
export type Attempt = components["schemas"]["Attempt"];

export type InspectorDetails = components["schemas"]["Inspector"];

export type PracticeContext = components["schemas"]["PracticeContext"];
export type PracticeProfiles = components["schemas"]["PracticeProfiles"];
export type PracticeProfile = components["schemas"]["PracticeProfile"];
export type PracticeAttemptContext = components["schemas"]["PracticeAttemptContext"];
export type PracticeItem = components["schemas"]["PracticeItem"];
export type PracticeOutcomeCounts = components["schemas"]["PracticeOutcomeCounts"];
export type PracticeSummaryRow = components["schemas"]["PracticeSummaryRow"];
export type PracticeSummary = components["schemas"]["PracticeSummary"];
export type PracticeSession = components["schemas"]["PracticeSession"];
export type PracticeCreationRequest = components["schemas"]["PracticeSessionRequest"];
export type PracticeFollowUpRequest = components["schemas"]["PracticeFollowUpRequest"];
