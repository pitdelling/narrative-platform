import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AiTagSettingsPanel } from "@/components/chronicle/AiTagSettingsPanel";
import type { CanonCategory } from "@/lib/types";

const apiMock = vi.fn();
vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => apiMock(...args),
}));

const defaultCategories: CanonCategory[] = [
  { id: "cat-1", name: "Pessoas", description: "Gente da história.", color: "#7665a7", displayOrder: 0 },
  { id: "cat-2", name: "Lugares", description: "Locais relevantes.", color: "#c29042", displayOrder: 1 },
];

async function openPanel(user: ReturnType<typeof userEvent.setup>) {
  render(<AiTagSettingsPanel partyId="p1" narrator={true} />);
  await user.click(screen.getByText("Tags do mapa do cânone"));
}

describe("AiTagSettingsPanel", () => {
  beforeEach(() => {
    apiMock.mockReset();
  });

  it("renders nothing for a player", () => {
    const { container } = render(<AiTagSettingsPanel partyId="p1" narrator={false} />);
    expect(container).toBeEmptyDOMElement();
    expect(apiMock).not.toHaveBeenCalled();
  });

  it("loads and shows the party's categories when opened by a narrator", async () => {
    apiMock.mockResolvedValue(defaultCategories);
    const user = userEvent.setup();
    await openPanel(user);

    expect(await screen.findByText("Pessoas")).toBeInTheDocument();
    expect(screen.getByText("Lugares")).toBeInTheDocument();
    expect(screen.getByText("Locais relevantes.")).toBeInTheDocument();
  });

  it("shows an empty-state message when the party has no categories yet", async () => {
    apiMock.mockResolvedValue([]);
    const user = userEvent.setup();
    await openPanel(user);

    expect(await screen.findByText("Nenhuma categoria configurada ainda.")).toBeInTheDocument();
  });

  it("keeps the static rule about future generations visible", async () => {
    apiMock.mockResolvedValue(defaultCategories);
    const user = userEvent.setup();
    await openPanel(user);
    await screen.findByText("Pessoas");

    expect(screen.getByText(
      "As mudanças serão usadas em novas gerações. Mapas já gerados preservam a configuração anterior.",
    )).toBeInTheDocument();
  });

  it("adding a category posts immediately and cancelling makes no call", async () => {
    apiMock.mockResolvedValueOnce(defaultCategories);
    const user = userEvent.setup();
    await openPanel(user);
    await screen.findByText("Pessoas");

    await user.click(screen.getByText("Adicionar categoria"));
    await user.type(screen.getByPlaceholderText("Nome da categoria"), "Itens");
    expect(apiMock).toHaveBeenCalledTimes(1);

    apiMock.mockResolvedValueOnce({ id: "cat-3", name: "Itens", description: null, color: "#7665a7", displayOrder: 2 });
    apiMock.mockResolvedValueOnce([...defaultCategories, { id: "cat-3", name: "Itens", color: "#7665a7", displayOrder: 2 }]);
    await user.click(screen.getByLabelText("Salvar categoria"));

    expect(apiMock).toHaveBeenCalledTimes(3);
    const [, init] = apiMock.mock.calls[1];
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body).name).toBe("Itens");
  });

  it("cancelling a new category discards it without an API call", async () => {
    apiMock.mockResolvedValueOnce(defaultCategories);
    const user = userEvent.setup();
    await openPanel(user);
    await screen.findByText("Pessoas");

    await user.click(screen.getByText("Adicionar categoria"));
    await user.click(screen.getByLabelText("Cancelar edição da categoria"));

    expect(apiMock).toHaveBeenCalledTimes(1);
    expect(screen.queryByPlaceholderText("Nome da categoria")).not.toBeInTheDocument();
  });

  it("editing a category pre-fills the form and saves via PUT", async () => {
    apiMock.mockResolvedValueOnce(defaultCategories);
    const user = userEvent.setup();
    await openPanel(user);
    await screen.findByText("Pessoas");

    await user.click(screen.getAllByLabelText("Editar categoria")[0]);
    expect(screen.getByDisplayValue("Pessoas")).toBeInTheDocument();

    apiMock.mockResolvedValueOnce({ ...defaultCategories[0], name: "Gente" });
    apiMock.mockResolvedValueOnce(defaultCategories);
    await user.click(screen.getByLabelText("Salvar categoria"));

    const [, init] = apiMock.mock.calls[1];
    expect(init.method).toBe("PUT");
  });

  it("deletes a category after confirming", async () => {
    apiMock.mockResolvedValueOnce(defaultCategories);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const user = userEvent.setup();
    await openPanel(user);
    await screen.findByText("Pessoas");

    apiMock.mockResolvedValueOnce(undefined);
    apiMock.mockResolvedValueOnce([defaultCategories[1]]);
    await user.click(screen.getAllByLabelText("Excluir categoria")[0]);

    const [, init] = apiMock.mock.calls[1];
    expect(init.method).toBe("DELETE");
  });

  it("reorder arrows are disabled at the boundaries of the list", async () => {
    apiMock.mockResolvedValueOnce(defaultCategories);
    const user = userEvent.setup();
    await openPanel(user);
    await screen.findByText("Pessoas");

    const upButtons = screen.getAllByLabelText("Mover categoria para cima");
    const downButtons = screen.getAllByLabelText("Mover categoria para baixo");
    expect(upButtons[0]).toBeDisabled();
    expect(downButtons[downButtons.length - 1]).toBeDisabled();
    expect(downButtons[0]).not.toBeDisabled();
  });

  it("clicking move-down calls the endpoint and refreshes the list", async () => {
    apiMock.mockResolvedValueOnce(defaultCategories);
    const user = userEvent.setup();
    await openPanel(user);
    await screen.findByText("Pessoas");

    apiMock.mockResolvedValueOnce(undefined);
    apiMock.mockResolvedValueOnce([defaultCategories[1], defaultCategories[0]]);
    await user.click(screen.getAllByLabelText("Mover categoria para baixo")[0]);

    const [path, init] = apiMock.mock.calls[1];
    expect(path).toContain("/move-down");
    expect(init.method).toBe("POST");
  });

  it("a failed save shows an inline error and keeps the row in edit mode", async () => {
    apiMock.mockResolvedValueOnce(defaultCategories);
    const user = userEvent.setup();
    await openPanel(user);
    await screen.findByText("Pessoas");

    await user.click(screen.getByText("Adicionar categoria"));
    await user.type(screen.getByPlaceholderText("Nome da categoria"), "Magias");
    apiMock.mockRejectedValueOnce(new Error("falhou"));
    await user.click(screen.getByLabelText("Salvar categoria"));

    expect(await screen.findByText("falhou")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Nome da categoria")).toBeInTheDocument();
  });
});
