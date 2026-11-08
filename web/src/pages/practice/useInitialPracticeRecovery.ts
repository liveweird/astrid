import { useQuery } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { ApiError, loadPracticeContext } from "../../api/client";
import type { AttemptRequest, PracticeProfiles, PracticeSession } from "../../api/models";
import { answerValues, attemptFromEntry, practiceError, reconcileOutbox } from "./practiceOperations";
import { practiceProfilesQuery } from "./practiceProfiles";
import {
  latestRunRecovery, listOutbox, loadRunRecovery, type PracticeOutboxEntry, type PracticeRunRecovery,
} from "./practiceRecovery";

export type InitialRecoveryHandlers = {
  applyState: (session: PracticeSession) => void;
  bindUrl: (practiceSessionId: string) => void;
  setBusy: (value: boolean) => void;
  setError: (value: string) => void;
  setOrder: (value: string[]) => void;
  setPendingEntry: (value: PracticeOutboxEntry | null) => void;
  setRecoveryUnavailable: (value: boolean) => void;
  setRun: (value: PracticeRunRecovery) => void;
  showFreshChooser: (catalog: PracticeProfiles | null, message?: string) => void;
  setText: (value: string) => void;
  setUncertain: (value: AttemptRequest | null) => void;
};

/** Shows the fresh chooser once the session-establishing profile read settles. */
function useFreshChooser(freshStart: boolean, handlersRef: { current: InitialRecoveryHandlers }) {
  const profiles = useQuery({ ...practiceProfilesQuery, enabled: freshStart });
  const shown = useRef(false);
  const { data, error, isError, isPending } = profiles;
  useEffect(() => {
    if (!freshStart || isPending || shown.current) return;
    shown.current = true;
    const handlers = handlersRef.current;
    if (isError) handlers.showFreshChooser(null, practiceError(error));
    else handlers.showFreshChooser(data);
    handlers.setBusy(false);
  }, [data, error, freshStart, handlersRef, isError, isPending]);
}

/**
 * Recovers the run bound to this tab (`boundId`, read once on mount) or the latest saved run;
 * with neither, the fresh chooser opens.
 */
export function useInitialPracticeRecovery(boundId: string | null, handlers: InitialRecoveryHandlers) {
  const handlersRef = useRef(handlers);
  const [freshStart, setFreshStart] = useState(false);
  useFreshChooser(freshStart, handlersRef);
  useEffect(() => {
    const handlers = handlersRef.current;
    let active = true;
    const recoverSaved = async (recovery: PracticeRunRecovery) => {
      handlers.setRun(recovery);
      handlers.bindUrl(recovery.practiceSessionId);
      try {
        const context = await loadPracticeContext();
        if (!active) return;
        if (context.ownerSessionId !== recovery.ownerSessionId) {
          if (active) {
            handlers.setRecoveryUnavailable(true);
            handlers.setError("This saved practice belongs to an expired browser session. It was not reassigned or replayed.");
          }
          return;
        }
        const next = await reconcileOutbox(recovery, {
          unavailable: (message) => {
            if (active) { handlers.setRecoveryUnavailable(true); handlers.setError(message); }
          },
          error: (message) => { if (active) handlers.setError(message); },
          uncertain: (request) => {
            if (!active) return;
            handlers.setUncertain(request);
            const restored = answerValues(request?.answer);
            handlers.setText(restored.text);
            handlers.setOrder(restored.order);
          },
          pending: (entry) => { if (active) handlers.setPendingEntry(entry); },
          definitive: (_entry, caught) => {
            if (active && caught instanceof ApiError && [401, 403, 404].includes(caught.status)) {
              handlers.setRecoveryUnavailable(true);
            }
          },
        });
        if (active && next.session) {
          handlers.applyState(next.session);
          const pendingAttempt = listOutbox(recovery.practiceSessionId).find((entry) => entry.kind === "attempt");
          const request = pendingAttempt ? attemptFromEntry(pendingAttempt) : null;
          if (request) {
            handlers.setUncertain(request);
            const restored = answerValues(request.answer);
            handlers.setText(restored.text);
            handlers.setOrder(restored.order);
          }
        }
      } catch (caught) {
        if (active) {
          handlers.setRecoveryUnavailable(caught instanceof ApiError && [401, 403, 404].includes(caught.status));
          handlers.setError(practiceError(caught));
        }
      } finally {
        if (active) handlers.setBusy(false);
      }
    };
    const recover = async () => {
      const recovery = boundId ? loadRunRecovery(boundId) : latestRunRecovery();
      if (boundId && !recovery) {
        if (active) {
          handlers.setRecoveryUnavailable(true);
          handlers.setError("This tab’s saved practice record is unavailable. Start a new practice when you are ready.");
          handlers.setBusy(false);
        }
        return;
      }
      if (recovery) await recoverSaved(recovery); else if (active) setFreshStart(true);
    };
    void recover();
    return () => { active = false; };
  }, [boundId]);
}
