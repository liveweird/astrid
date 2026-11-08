import { useSyncExternalStore } from "react";
import { APP_VERSION } from "../changelog/version";

const STORAGE_KEY = "astrid.changelog";
const listeners = new Set<() => void>();

function readSeenVersion(): string | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return null;
    const parsed = JSON.parse(stored) as { seenVersion?: unknown };
    return typeof parsed.seenVersion === "string" ? parsed.seenVersion : null;
  } catch {
    return null;
  }
}

function notify() {
  listeners.forEach((listener) => listener());
}

function onStorage(event: StorageEvent) {
  if (event.key === STORAGE_KEY || event.key === null) notify();
}

function subscribe(listener: () => void) {
  const first = listeners.size === 0;
  listeners.add(listener);
  if (first) window.addEventListener("storage", onStorage);
  return () => {
    listeners.delete(listener);
    if (listeners.size === 0) window.removeEventListener("storage", onStorage);
  };
}

function unseenSnapshot() {
  return readSeenVersion() !== APP_VERSION;
}

export function markChangelogSeen() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ seenVersion: APP_VERSION }));
  } catch {
    // Storage is optional; the indicator remains visible when this device cannot persist state.
  }
  notify();
}

export function useChangelogUnseen() {
  return useSyncExternalStore(subscribe, unseenSnapshot, unseenSnapshot);
}
