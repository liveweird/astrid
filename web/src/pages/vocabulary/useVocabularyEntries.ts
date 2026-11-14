import { queryOptions, useQuery } from "@tanstack/react-query";
import { loadVocabularyEntries } from "../../api/client";

/** Public development-mode reference data. It is finite and revisioned, so the app-level cache is appropriate. */
const vocabularyEntriesQuery = queryOptions({
  queryKey: ["vocabulary-entries"],
  queryFn: loadVocabularyEntries,
});

export function useVocabularyEntries() {
  return useQuery(vocabularyEntriesQuery);
}
