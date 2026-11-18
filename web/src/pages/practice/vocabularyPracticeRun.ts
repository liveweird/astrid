export const VOCABULARY_PRACTICE_REVISION = "vocabulary-library-practice-1";

export const VOCABULARY_PRACTICE_OPTIONS = [
  { focusId: "vocabulary-library-recognition", count: "recognitionCount", label: "Recognize meanings" },
  { focusId: "vocabulary-library-recall", count: "recallCount", label: "Write Danish" },
] as const;

export function isVocabularyPractice({ focusId, planRevision }: { focusId: string; planRevision: string }) {
  return planRevision === VOCABULARY_PRACTICE_REVISION
    && VOCABULARY_PRACTICE_OPTIONS.some((option) => option.focusId === focusId);
}
