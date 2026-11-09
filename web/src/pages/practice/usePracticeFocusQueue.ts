import { useEffect, useRef, useState } from "react";
import type { PracticeSession } from "../../api/models";
import type { PracticeOutboxEntry } from "./practiceRecovery";

export type PracticeFocusTarget = "item" | "feedback" | "summary" | "error" | "introduction" | "lesson" | "hint" | "gaps";

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
  return next.cursor !== previousCursor ? "item" : requested;
}

export function usePracticeFocusQueue(error: string) {
  const itemHeadingRef = useRef<HTMLHeadingElement>(null);
  const feedbackRef = useRef<HTMLDivElement>(null);
  const errorRef = useRef<HTMLDivElement>(null);
  const summaryHeadingRef = useRef<HTMLHeadingElement>(null);
  const introductionContentRef = useRef<HTMLParagraphElement>(null);
  const lessonContentRef = useRef<HTMLParagraphElement>(null);
  const hintRef = useRef<HTMLDivElement>(null);
  const gapFocusRef = useRef<HTMLDivElement>(null);
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
      introduction: introductionContentRef, lesson: lessonContentRef, hint: hintRef, gaps: gapFocusRef,
    };
    const target = refs[error ? "error" : request.target].current;
    target?.focus({ preventScroll: true });
    target?.scrollIntoView({ block: "start", inline: "nearest" });
  }, [error, request]);
  return {
    errorRef, feedbackRef, gapFocusRef, hintRef, introductionContentRef, itemHeadingRef,
    lessonContentRef, requestFocus, summaryHeadingRef,
  };
}
