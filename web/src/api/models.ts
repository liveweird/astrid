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
