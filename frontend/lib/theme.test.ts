import { describe, expect, it } from "vitest";
import { mockMatchMedia } from "@/lib/test-utils/matchMedia";
import {
  getStoredThemePreference,
  getSystemTheme,
  resolveTheme,
  setStoredThemePreference,
  THEME_STORAGE_KEY,
} from "@/lib/theme";

describe("default system behavior", () => {
  it("resolves to dark when no preference is stored and the OS prefers dark", () => {
    mockMatchMedia(true);

    expect(getStoredThemePreference()).toBe("system");
    expect(getSystemTheme()).toBe("dark");
    expect(resolveTheme(getStoredThemePreference())).toBe("dark");
  });

  it("resolves to light when no preference is stored and the OS prefers light", () => {
    mockMatchMedia(false);

    expect(getStoredThemePreference()).toBe("system");
    expect(getSystemTheme()).toBe("light");
    expect(resolveTheme(getStoredThemePreference())).toBe("light");
  });
});

describe("explicit light selection", () => {
  it("stays light even when the OS prefers dark", () => {
    mockMatchMedia(true);

    setStoredThemePreference("light");

    expect(getStoredThemePreference()).toBe("light");
    expect(resolveTheme(getStoredThemePreference())).toBe("light");
  });
});

describe("explicit dark selection", () => {
  it("stays dark even when the OS prefers light", () => {
    mockMatchMedia(false);

    setStoredThemePreference("dark");

    expect(getStoredThemePreference()).toBe("dark");
    expect(resolveTheme(getStoredThemePreference())).toBe("dark");
  });
});

describe("persisted preference", () => {
  it("round-trips an explicit preference through localStorage", () => {
    setStoredThemePreference("dark");

    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe("dark");
    expect(getStoredThemePreference()).toBe("dark");
  });

  it("falls back to system when the stored value is garbage", () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, "blue");

    expect(getStoredThemePreference()).toBe("system");
  });

  it("falls back to system when nothing is stored", () => {
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBeNull();
    expect(getStoredThemePreference()).toBe("system");
  });
});
