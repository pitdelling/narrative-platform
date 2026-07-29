import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { mockMatchMedia } from "@/lib/test-utils/matchMedia";
import { getStoredThemePreference, setStoredThemePreference, THEME_STORAGE_KEY } from "@/lib/theme";
import { ThemeProvider } from "@/components/ThemeProvider";
import { ThemeToggle } from "@/components/ThemeToggle";

function renderToggle() {
  return render(
    <ThemeProvider>
      <ThemeToggle />
    </ThemeProvider>,
  );
}

describe("theme-toggle accessibility", () => {
  it("exposes a labelled radiogroup with exactly three radio options", () => {
    mockMatchMedia(false);
    renderToggle();

    const group = screen.getByRole("radiogroup", { name: "Tema" });
    expect(group).toBeInTheDocument();

    const options = screen.getAllByRole("radio");
    expect(options).toHaveLength(3);
    expect(options.map((option) => option.textContent)).toEqual(["Claro", "Escuro", "Sistema"]);
  });

  it("reflects the stored preference via aria-checked on mount", () => {
    mockMatchMedia(false);
    setStoredThemePreference("dark");
    renderToggle();

    expect(screen.getByRole("radio", { name: "Escuro" })).toHaveAttribute("aria-checked", "true");
    expect(screen.getByRole("radio", { name: "Claro" })).toHaveAttribute("aria-checked", "false");
    expect(screen.getByRole("radio", { name: "Sistema" })).toHaveAttribute("aria-checked", "false");
  });

  it("is keyboard-operable and updates selection and persistence on activation", async () => {
    mockMatchMedia(false);
    const user = userEvent.setup();
    renderToggle();

    const lightOption = screen.getByRole("radio", { name: "Claro" });
    const darkOption = screen.getByRole("radio", { name: "Escuro" });

    expect(lightOption.tagName).toBe("BUTTON");
    lightOption.focus();
    expect(lightOption).toHaveFocus();

    await user.click(darkOption);

    expect(darkOption).toHaveAttribute("aria-checked", "true");
    expect(lightOption).toHaveAttribute("aria-checked", "false");
    expect(getStoredThemePreference()).toBe("dark");
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe("dark");
  });
});
