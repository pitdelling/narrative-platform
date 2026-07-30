import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { StoryModal } from "@/components/chronicle/StoryModal";
import type { GeneratedStory } from "@/lib/types";

const apiMock = vi.fn();
vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => apiMock(...args),
}));

const currentStory: GeneratedStory = {
  id: "story-2",
  version: 2,
  title: "A Queda de Aravel",
  content: "Conteúdo da versão 2.",
  model: "gpt-test",
  createdAt: "2026-02-10T10:00:00.000Z",
};

const olderStory: GeneratedStory = {
  id: "story-1",
  version: 1,
  title: "A Queda de Aravel (rascunho)",
  content: "Conteúdo da versão 1.",
  model: "gpt-test",
  createdAt: "2026-01-10T10:00:00.000Z",
};

beforeEach(() => {
  apiMock.mockReset();
  apiMock.mockResolvedValue([currentStory, olderStory]);
});

describe("StoryModal", () => {
  it("renders nothing when closed", () => {
    const { container } = render(
      <StoryModal
        open={false}
        onClose={vi.fn()}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="PUBLISHED"
        currentStory={currentStory}
        canRegenerate={false}
        isRegenerating={false}
        onRegenerate={vi.fn()}
      />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("shows the current story when open", async () => {
    render(
      <StoryModal
        open
        onClose={vi.fn()}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="PUBLISHED"
        currentStory={currentStory}
        canRegenerate={false}
        isRegenerating={false}
        onRegenerate={vi.fn()}
      />,
    );
    expect(await screen.findByText("A Queda de Aravel")).toBeInTheDocument();
  });

  it("only shows the regenerate button when canRegenerate is true", async () => {
    const onRegenerate = vi.fn();
    render(
      <StoryModal
        open
        onClose={vi.fn()}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="PUBLISHED"
        currentStory={currentStory}
        canRegenerate
        isRegenerating={false}
        onRegenerate={onRegenerate}
      />,
    );
    const button = await screen.findByRole("button", { name: "Regenerar história" });
    expect(button).not.toBeDisabled();
    await userEvent.click(button);
    expect(onRegenerate).toHaveBeenCalledOnce();
  });

  it("renders icon and label spans inside the regenerate button", async () => {
    render(
      <StoryModal
        open
        onClose={vi.fn()}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="PUBLISHED"
        currentStory={currentStory}
        canRegenerate
        isRegenerating={false}
        onRegenerate={vi.fn()}
      />,
    );
    const button = await screen.findByRole("button", { name: "Regenerar história" });
    expect(button.querySelector(".regenerate-icon")).not.toBeNull();
    expect(button.querySelector(".regenerate-label")).toHaveTextContent("Regenerar história");
  });

  it("does not render a regenerate button when canRegenerate and isRegenerating are both false", async () => {
    render(
      <StoryModal
        open
        onClose={vi.fn()}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="PUBLISHED"
        currentStory={currentStory}
        canRegenerate={false}
        isRegenerating={false}
        onRegenerate={vi.fn()}
      />,
    );
    await screen.findByText("A Queda de Aravel");
    expect(screen.queryByRole("button", { name: "Regenerar história" })).not.toBeInTheDocument();
  });

  it("shows a disabled 'Gerando...' button while a regeneration is already in progress, even if canRegenerate is false", async () => {
    render(
      <StoryModal
        open
        onClose={vi.fn()}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="AI_PROCESSING"
        currentStory={currentStory}
        canRegenerate={false}
        isRegenerating
        onRegenerate={vi.fn()}
      />,
    );
    const button = await screen.findByRole("button", { name: "Regenerar história" });
    expect(button).toBeDisabled();
    expect(button.querySelector(".regenerate-label")).toHaveTextContent("Gerando...");
  });

  it("switches the displayed version through the version selector", async () => {
    render(
      <StoryModal
        open
        onClose={vi.fn()}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="PUBLISHED"
        currentStory={currentStory}
        canRegenerate={false}
        isRegenerating={false}
        onRegenerate={vi.fn()}
      />,
    );
    const select = await screen.findByLabelText("Selecionar versão da história");
    await userEvent.selectOptions(select, "story-1");
    expect(await screen.findByText("A Queda de Aravel (rascunho)")).toBeInTheDocument();
  });

  it("renders the close button outside the modal heading, so it never wraps with the toolbar", async () => {
    const { container } = render(
      <StoryModal
        open
        onClose={vi.fn()}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="PUBLISHED"
        currentStory={currentStory}
        canRegenerate={false}
        isRegenerating={false}
        onRegenerate={vi.fn()}
      />,
    );
    await screen.findByText("A Queda de Aravel");
    const heading = container.querySelector(".modal-heading");
    const closeButton = screen.getByRole("button", { name: "Fechar" });
    expect(heading?.contains(closeButton)).toBe(false);
    expect(container.querySelector(".story-modal")?.contains(closeButton)).toBe(true);
  });

  it("closes via the close button and the backdrop", async () => {
    const onClose = vi.fn();
    const { container } = render(
      <StoryModal
        open
        onClose={onClose}
        partyId="party-1"
        chronicleId="chronicle-1"
        status="PUBLISHED"
        currentStory={currentStory}
        canRegenerate={false}
        isRegenerating={false}
        onRegenerate={vi.fn()}
      />,
    );
    await userEvent.click(screen.getByRole("button", { name: "Fechar" }));
    expect(onClose).toHaveBeenCalledOnce();

    const backdrop = container.querySelector(".modal-layer");
    expect(backdrop).not.toBeNull();
    if (backdrop) await userEvent.click(backdrop);
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(2));
  });
});
