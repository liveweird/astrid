import { useEffect, useRef, useState } from "react";
import type { PracticeSession } from "../../api/models";
import type { PracticeOutboxEntry } from "./practiceRecovery";

export type PracticeFocusTarget = "item" | "feedback" | "summary" | "error" | "introductionControl" | "introduction" | "lesson" | "hint" | "gaps" | "matching";

export function pendingFocus(entry: PracticeOutboxEntry, session: PracticeSession): PracticeFocusTarget {
  if (entry.kind === "attempt") return "feedback";
  if (entry.kind === "advance") return session.cursor + 1 === session.itemCount ? "summary" : "item";
  if (entry.kind !== "support") return "item";
  try {
    const { kind } = JSON.parse(entry.body) as { kind?: unknown };
    return kind === "introduction" || kind === "lesson" || kind === "hint" ? kind : "item";
  } catch { return "item"; }
}

export function reconciledFocus(previousCursor: number | undefined, next: PracticeSession | null, requested: PracticeFocusTarget) {
  if (!next) return "error";
  if (next.state === "completed") return "summary";
  if (next.introductionRequired && !next.introductionOpened) return "introductionControl";
  return next.cursor !== previousCursor ? "item" : requested;
}

export function recoveredFocus(session: PracticeSession, pending?: PracticeOutboxEntry) {
  if (session.state === "completed") return "summary";
  if (session.introductionRequired && !session.introductionOpened) return "introductionControl";
  if (pending?.kind === "support") return pendingFocus(pending, session);
  if (pending?.kind === "attempt") return "feedback";
  if (pending) return "item";
  const item = session.items.find((candidate) => candidate.position === session.cursor);
  return item?.lastAttempt ? "feedback" : "item";
}

export function usePracticeFocusQueue(error: string) {
  const itemHeadingRef = useRef<HTMLHeadingElement>(null);
  const feedbackRef = useRef<HTMLDivElement>(null);
  const errorRef = useRef<HTMLDivElement>(null);
  const summaryHeadingRef = useRef<HTMLHeadingElement>(null);
  const introductionContentRef = useRef<HTMLParagraphElement>(null);
  const introductionControlRef = useRef<HTMLButtonElement>(null);
  const lessonContentRef = useRef<HTMLParagraphElement>(null);
  const hintRef = useRef<HTMLDivElement>(null);
  const gapFocusRef = useRef<HTMLDivElement>(null);
  const matchingFocusRef = useRef<HTMLDivElement>(null);
  const [request, setRequest] = useState<{ target: PracticeFocusTarget; sequence: number }>({
    target: "item", sequence: 0,
  });
  const requestFocus = (target: PracticeFocusTarget) => {
    setRequest((current) => ({ target, sequence: current.sequence + 1 }));
  };
  useEffect(() => {
    if (!error && request.sequence === 0) return;
    const refs = {
      item: itemHeadingRef, feedback: feedbackRef, summary: summaryHeadingRef, error: errorRef,
      introductionControl: introductionControlRef, introduction: introductionContentRef,
      lesson: lessonContentRef, hint: hintRef, gaps: gapFocusRef, matching: matchingFocusRef,
    };
    const target = refs[error ? "error" : request.target].current;
    target?.focus({ preventScroll: true });
    target?.scrollIntoView({ block: "start", inline: "nearest" });
  }, [error, request]);
  return {
    errorRef, feedbackRef, gapFocusRef, matchingFocusRef, hintRef, introductionContentRef, introductionControlRef, itemHeadingRef,
    lessonContentRef, requestFocus, summaryHeadingRef,
  };
}
