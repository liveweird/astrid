import { act, renderHook } from "@testing-library/react";
import { APP_VERSION } from "../changelog/version";
import { markChangelogSeen, useChangelogUnseen } from "./useChangelogSeen";

const KEY = "astrid.changelog";

describe("changelog seen state", () => {
  beforeEach(() => localStorage.clear());

  it("updates mounted indicators immediately when the current version is read", () => {
    const { result } = renderHook(useChangelogUnseen);
    expect(result.current).toBe(true);
    act(() => markChangelogSeen());
    expect(result.current).toBe(false);
    expect(JSON.parse(localStorage.getItem(KEY)!)).toEqual({ seenVersion: APP_VERSION });
  });

  it("treats corrupt or older state as unseen and responds to another tab", () => {
    localStorage.setItem(KEY, "{broken");
    const { result } = renderHook(useChangelogUnseen);
    expect(result.current).toBe(true);
    localStorage.setItem(KEY, JSON.stringify({ seenVersion: "0.1.0" }));
    act(() => window.dispatchEvent(new StorageEvent("storage", { key: KEY })));
    expect(result.current).toBe(true);
    localStorage.setItem(KEY, JSON.stringify({ seenVersion: APP_VERSION }));
    act(() => window.dispatchEvent(new StorageEvent("storage", { key: KEY })));
    expect(result.current).toBe(false);
  });

  it("ignores unrelated storage changes and handles a cross-tab clear", () => {
    localStorage.setItem(KEY, JSON.stringify({ seenVersion: APP_VERSION }));
    const { result } = renderHook(useChangelogUnseen);
    expect(result.current).toBe(false);
    act(() => window.dispatchEvent(new StorageEvent("storage", { key: "another.key" })));
    expect(result.current).toBe(false);
    localStorage.removeItem(KEY);
    act(() => window.dispatchEvent(new StorageEvent("storage", { key: null })));
    expect(result.current).toBe(true);
  });

  it("treats unavailable reads and invalid payload shapes as unseen", () => {
    localStorage.setItem(KEY, JSON.stringify({ seenVersion: 2 }));
    const invalid = renderHook(useChangelogUnseen);
    expect(invalid.result.current).toBe(true);
    invalid.unmount();
    const getItem = vi.spyOn(localStorage, "getItem").mockImplementation(() => {
      throw new DOMException("denied", "SecurityError");
    });
    try {
      const unavailable = renderHook(useChangelogUnseen);
      expect(unavailable.result.current).toBe(true);
      unavailable.unmount();
    } finally {
      getItem.mockRestore();
    }
  });

  it("does not fail when persistence is unavailable", () => {
    const { result } = renderHook(useChangelogUnseen);
    const setItem = vi.spyOn(localStorage, "setItem").mockImplementation(() => {
      throw new DOMException("full", "QuotaExceededError");
    });
    try {
      expect(() => act(() => markChangelogSeen())).not.toThrow();
      expect(result.current).toBe(true);
    } finally {
      setItem.mockRestore();
    }
  });
});
