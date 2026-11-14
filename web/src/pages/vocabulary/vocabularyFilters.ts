import type { VocabularyEntry, VocabularyTeachingClaim } from "../../api/models";

export const SITUATIONS = [
  { value: "all", label: "All situations" },
  { value: "module-one", label: "Module 1: First steps" },
  { value: "vocabulary-p1", label: "A day of work and study" },
  { value: "vocabulary-p2", label: "Food, cafés and shopping" },
  { value: "vocabulary-p3", label: "Places and journeys" },
] as const;

export type SituationId = (typeof SITUATIONS)[number]["value"];

function searchable(entry: VocabularyEntry) {
  return [
    entry.headword,
    ...entry.senses.flatMap((sense) => [sense.meaning, ...sense.activeForms.map((form) => form.text)]),
  ].join("\n").toLocaleLowerCase("da-DK");
}

export function hasRecordedTeaching(entry: VocabularyEntry, claims: VocabularyTeachingClaim[]) {
  return claims.some((claim) => claim.entryId === entry.entryId && (claim.meaningRecorded || claim.formIds.length > 0));
}

export function filterVocabulary(
  entries: VocabularyEntry[],
  search: string,
  situation: SituationId,
  savedOnly: boolean,
  claims: VocabularyTeachingClaim[],
) {
  const needle = search.trim().toLocaleLowerCase("da-DK");
  return entries.filter((entry) =>
    (!needle || searchable(entry).includes(needle)) &&
    (situation === "all" || entry.situationIds.includes(situation)) &&
    (!savedOnly || hasRecordedTeaching(entry, claims)));
}
