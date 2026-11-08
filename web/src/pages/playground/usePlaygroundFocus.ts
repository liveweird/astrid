import { useEffect, useRef, useState } from "react";
import type { Attempt, Exercise } from "../../api/models";

export function useExerciseFocus(attempt: Attempt | null, exercise: Exercise | null) {
  const headingRef = useRef<HTMLHeadingElement>(null);
  const afterRender = useRef(false);
  useEffect(() => {
    if (!afterRender.current) return;
    headingRef.current?.focus();
    afterRender.current = false;
  }, [attempt, exercise]);
  const requestFocus = () => { afterRender.current = true; };
  return [headingRef, requestFocus] as const;
}

export function useRequestedFocus<T extends HTMLElement>() {
  const targetRef = useRef<T>(null);
  const [request, setRequest] = useState(0);
  useEffect(() => {
    if (request > 0) targetRef.current?.focus();
  }, [request]);
  return [targetRef, () => setRequest((current) => current + 1)] as const;
}
