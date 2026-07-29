"use client";

import { useTheme } from "@/components/ThemeProvider";
import type { ThemePreference } from "@/lib/theme";

const OPTIONS: { value: ThemePreference; label: string }[] = [
  { value: "light", label: "Claro" },
  { value: "dark", label: "Escuro" },
  { value: "system", label: "Sistema" },
];

export function ThemeToggle() {
  const { preference, setPreference } = useTheme();

  return (
    <div className="theme-toggle">
      <p className="eyebrow" id="theme-toggle-label">Tema</p>
      <div className="segmented" role="radiogroup" aria-labelledby="theme-toggle-label">
        {OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={preference === option.value}
            className={preference === option.value ? "active" : ""}
            onClick={() => setPreference(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}
