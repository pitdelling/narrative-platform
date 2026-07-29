"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import {
  applyResolvedTheme,
  getStoredThemePreference,
  resolveTheme,
  setStoredThemePreference,
  THEME_STORAGE_KEY,
  type ResolvedTheme,
  type ThemePreference,
} from "@/lib/theme";

interface ThemeContextValue {
  preference: ThemePreference;
  resolvedTheme: ResolvedTheme;
  setPreference: (preference: ThemePreference) => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
  // Lazily read localStorage/matchMedia during the client's first render pass instead of
  // syncing via an effect: ThemeProvider renders no theme-dependent DOM itself (the values
  // only drive `ThemeToggle`, which never exists during SSR/hydration), so there is no
  // hydration-mismatch risk, and this avoids an unnecessary extra render on mount.
  const [preference, setPreferenceState] = useState<ThemePreference>(() => getStoredThemePreference());
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => resolveTheme(getStoredThemePreference()));

  const reconcile = useCallback(() => {
    const storedPreference = getStoredThemePreference();
    const nextResolved = resolveTheme(storedPreference);
    setPreferenceState(storedPreference);
    setResolvedTheme(nextResolved);
    applyResolvedTheme(nextResolved);
  }, []);

  useEffect(() => {
    // Idempotent safety net: keep the DOM attribute in sync with the state computed above
    // (matches what the inline no-flash script already set on <html> before hydration).
    applyResolvedTheme(resolvedTheme);

    const mediaQuery = window.matchMedia?.("(prefers-color-scheme: dark)");
    const handleSystemChange = () => reconcile();
    mediaQuery?.addEventListener("change", handleSystemChange);

    const handleStorage = (event: StorageEvent) => {
      if (event.key === THEME_STORAGE_KEY || event.key === null) reconcile();
    };
    window.addEventListener("storage", handleStorage);

    return () => {
      mediaQuery?.removeEventListener("change", handleSystemChange);
      window.removeEventListener("storage", handleStorage);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- runs once on mount; live updates go through the listeners above, not this effect's deps.
  }, []);

  const setPreference = useCallback((next: ThemePreference) => {
    setStoredThemePreference(next);
    const nextResolved = resolveTheme(next);
    setPreferenceState(next);
    setResolvedTheme(nextResolved);
    applyResolvedTheme(nextResolved);
  }, []);

  const value = useMemo<ThemeContextValue>(
    () => ({ preference, resolvedTheme, setPreference }),
    [preference, resolvedTheme, setPreference],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme must be used within a ThemeProvider.");
  return context;
}
