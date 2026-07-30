import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { CanonMapPanel } from "@/components/chronicle/CanonMapPanel";
import type { AiArtifacts } from "@/lib/types";

const apiMock = vi.fn();
vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => apiMock(...args),
}));

function artifacts(overrides: Partial<AiArtifacts>): AiArtifacts {
  return {
    aiConfigured: true,
    canonMap: { status: "COMPLETED", categories: [] },
    synopsis: { status: "COMPLETED", content: "A synopsis." },
    ...overrides,
  };
}

describe("CanonMapPanel", () => {
  beforeEach(() => {
    apiMock.mockReset();
  });

  it("renders nothing when the chronicle is not finished", () => {
    const { container } = render(<CanonMapPanel partyId="p1" chronicleId="c1" finished={false} />);
    expect(container).toBeEmptyDOMElement();
    expect(apiMock).not.toHaveBeenCalled();
  });

  it("is collapsed by default", async () => {
    apiMock.mockResolvedValue(artifacts({}));
    render(<CanonMapPanel partyId="p1" chronicleId="c1" finished={true} />);

    const details = (await screen.findByText("Mapa do cânone")).closest("details");
    expect(details).not.toBeNull();
    expect(details).not.toHaveAttribute("open");
  });

  it("shows a pending message while the map has not started", async () => {
    apiMock.mockResolvedValue(artifacts({ canonMap: { status: "PENDING", categories: [] } }));
    render(<CanonMapPanel partyId="p1" chronicleId="c1" finished={true} />);

    expect(await screen.findByText("O mapa do cânone será preparado a partir da thread.")).toBeInTheDocument();
  });

  it("shows the AI-not-configured message when the environment has no provider key", async () => {
    apiMock.mockResolvedValue(artifacts({ aiConfigured: false, canonMap: { status: "PENDING", categories: [] } }));
    render(<CanonMapPanel partyId="p1" chronicleId="c1" finished={true} />);

    expect(await screen.findByText("O mapa do cânone não está disponível neste ambiente.")).toBeInTheDocument();
  });

  it("shows a neutral message when every category is disabled", async () => {
    apiMock.mockResolvedValue(artifacts({
      canonMap: {
        status: "COMPLETED",
        categories: [
          { category: "PERSON", enabled: false, color: "VIOLET", displayOrder: 1, tags: [] },
          { category: "PLACE", enabled: false, color: "GOLD", displayOrder: 2, tags: [] },
        ],
      },
    }));
    render(<CanonMapPanel partyId="p1" chronicleId="c1" finished={true} />);

    expect(await screen.findByText("A extração de tags está desativada para esta party.")).toBeInTheDocument();
  });

  it("shows the failed-state message without fabricating content", async () => {
    apiMock.mockResolvedValue(artifacts({ canonMap: { status: "FAILED", categories: [], errorMessage: "boom" } }));
    render(<CanonMapPanel partyId="p1" chronicleId="c1" finished={true} />);

    expect(await screen.findByText("Não foi possível gerar o mapa do cânone.")).toBeInTheDocument();
    expect(screen.getByText("A thread original permanece intacta e completa.")).toBeInTheDocument();
  });

  it("renders tag chips with origin badges for a completed map", async () => {
    apiMock.mockResolvedValue(artifacts({
      canonMap: {
        status: "COMPLETED",
        categories: [
          {
            category: "PERSON",
            enabled: true,
            color: "VIOLET",
            displayOrder: 1,
            tags: [
              {
                id: "tag-1",
                name: "Lucinda",
                summary: "Uma aventureira.",
                visualDescription: "Roupas marcadas pela maresia.",
                personalityDescription: "Irônica e impulsiva.",
                visualBasis: "INFERRED",
                personalityBasis: "CREATIVE_FILL",
                sourceSegmentPositions: [2, 4],
              },
            ],
          },
        ],
      },
    }));
    render(<CanonMapPanel partyId="p1" chronicleId="c1" finished={true} />);

    expect(await screen.findByText("Lucinda")).toBeInTheDocument();
    expect(screen.getByText("Inferido pela IA")).toBeInTheDocument();
    expect(screen.getByText("Complemento criativo")).toBeInTheDocument();
    expect(screen.getByText("Trechos 2, 4")).toBeInTheDocument();
  });
});
