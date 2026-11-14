import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, loadVocabularyContext } from "../../api/client";
import type { VocabularyContext } from "../../api/models";

const NO_CONTEXT = "There is no current saved lesson teaching in this browser session. Start and save lesson teaching first, then load it here.";
const LOAD_FAILED = "Saved lesson teaching could not be loaded. Try again when the learning server is available.";
const EXPIRED = "The saved lesson teaching context expired. Load it again after opening a current lesson session.";

type PersonalState = {
  libraryRevision?: string;
  context: VocabularyContext | null;
  message: string;
  loading: boolean;
};
const EMPTY: PersonalState = { context: null, message: "", loading: false };

export function useVocabularyContext(libraryRevision?: string) {
  const [state, setState] = useState<PersonalState>(EMPTY);
  const generation = useRef(0);
  const current = state.libraryRevision === libraryRevision ? state : EMPTY;
  const context = current.context;

  const load = useCallback(async () => {
    if (!libraryRevision) return;
    const request = generation.current + 1;
    generation.current = request;
    setState({ ...EMPTY, libraryRevision, loading: true });
    try {
      const next = await loadVocabularyContext();
      if (request !== generation.current) return;
      if (next.libraryRevision !== libraryRevision || next.sessionExpiresAt <= Date.now()) {
        setState({
          ...EMPTY, libraryRevision,
          message: next.sessionExpiresAt <= Date.now() ? EXPIRED : "Saved teaching belongs to a different vocabulary revision.",
        });
        return;
      }
      setState({ ...EMPTY, libraryRevision, context: next });
    } catch (error) {
      if (request !== generation.current) return;
      setState({
        ...EMPTY, libraryRevision,
        message: error instanceof ApiError && error.status === 401 ? NO_CONTEXT : LOAD_FAILED,
      });
    }
  }, [libraryRevision]);

  // Changing reference revisions invalidates outstanding work as well as hidden old claims.
  useEffect(() => () => { generation.current += 1; }, [libraryRevision]);
  useEffect(() => {
    if (!context) return;
    const remaining = context.sessionExpiresAt - Date.now();
    const timer = window.setTimeout(() => {
      setState({ ...EMPTY, libraryRevision, message: EXPIRED });
    }, Math.max(0, Math.min(remaining, 2_147_483_647)));
    return () => window.clearTimeout(timer);
  }, [context, libraryRevision]);
  useEffect(() => {
    const revalidate = () => {
      if (document.visibilityState === "visible" && context) void load();
    };
    document.addEventListener("visibilitychange", revalidate);
    return () => document.removeEventListener("visibilitychange", revalidate);
  }, [context, load]);

  return { context, load, loading: current.loading, message: current.message };
}
