import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AiTagSettingsPanel } from "@/components/chronicle/AiTagSettingsPanel";
import type { TagSetting } from "@/lib/types";

const apiMock = vi.fn();
vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => apiMock(...args),
}));

const defaultSettings: TagSetting[] = [
  { category: "PERSON", enabled: true, color: "VIOLET", displayOrder: 1 },
  { category: "PLACE", enabled: true, color: "GOLD", displayOrder: 2 },
  { category: "ITEM", enabled: true, color: "COPPER", displayOrder: 3 },
  { category: "SPELL", enabled: true, color: "AZURE", displayOrder: 4 },
  { category: "CREATURE", enabled: true, color: "GREEN", displayOrder: 5 },
];

describe("AiTagSettingsPanel", () => {
  beforeEach(() => {
    apiMock.mockReset();
  });

  it("renders nothing for a player", () => {
    const { container } = render(<AiTagSettingsPanel partyId="p1" narrator={false} />);
    expect(container).toBeEmptyDOMElement();
    expect(apiMock).not.toHaveBeenCalled();
  });

  it("loads and shows all five categories when opened by a narrator", async () => {
    apiMock.mockResolvedValue({ settings: defaultSettings });
    const user = userEvent.setup();
    render(<AiTagSettingsPanel partyId="p1" narrator={true} />);

    await user.click(screen.getByText("Tags do mapa do cânone"));

    expect(await screen.findByText("Pessoas")).toBeInTheDocument();
    expect(screen.getByText("Lugares")).toBeInTheDocument();
    expect(screen.getByText("Itens")).toBeInTheDocument();
    expect(screen.getByText("Magias")).toBeInTheDocument();
    expect(screen.getByText("Criaturas")).toBeInTheDocument();
  });

  it("saves the edited settings only when the button is clicked, not on every toggle", async () => {
    apiMock.mockResolvedValueOnce({ settings: defaultSettings });
    const user = userEvent.setup();
    render(<AiTagSettingsPanel partyId="p1" narrator={true} />);
    await user.click(screen.getByText("Tags do mapa do cânone"));
    await screen.findByText("Pessoas");

    const checkboxes = screen.getAllByRole("checkbox");
    await user.click(checkboxes[0]);
    expect(apiMock).toHaveBeenCalledTimes(1);

    apiMock.mockResolvedValueOnce(undefined);
    await user.click(screen.getByText("Salvar configurações"));

    expect(apiMock).toHaveBeenCalledTimes(2);
    const [, init] = apiMock.mock.calls[1];
    expect(init.method).toBe("PUT");
    const body = JSON.parse(init.body);
    expect(body.settings.find((item: TagSetting) => item.category === "PERSON").enabled).toBe(false);
  });

  it("restores the last confirmed state when saving fails", async () => {
    apiMock.mockResolvedValueOnce({ settings: defaultSettings });
    const user = userEvent.setup();
    render(<AiTagSettingsPanel partyId="p1" narrator={true} />);
    await user.click(screen.getByText("Tags do mapa do cânone"));
    await screen.findByText("Pessoas");

    const checkboxes = screen.getAllByRole("checkbox");
    await user.click(checkboxes[0]);

    apiMock.mockRejectedValueOnce(new Error("falhou"));
    await user.click(screen.getByText("Salvar configurações"));

    expect(await screen.findByText("falhou")).toBeInTheDocument();
    expect(screen.getAllByRole("checkbox")[0]).toBeChecked();
  });
});
