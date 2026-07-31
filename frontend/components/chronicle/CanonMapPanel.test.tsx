import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { CanonMapPanel } from "@/components/chronicle/CanonMapPanel";
import type { AiArtifacts } from "@/lib/types";

function artifacts(overrides: Partial<AiArtifacts>): AiArtifacts {
  return {
    aiConfigured: true,
    adaptation: { status: "PUBLISHED" },
    canonMap: { status: "COMPLETED", categories: [] },
    synopsis: { status: "COMPLETED", content: "A synopsis." },
    ...overrides,
  };
}

describe("CanonMapPanel", () => {
  it("renders nothing when the chronicle is not finished", () => {
    const { container } = render(<CanonMapPanel artifacts={artifacts({})} finished={false} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("is collapsed by default", async () => {
    render(<CanonMapPanel artifacts={artifacts({})} finished={true} />);

    const details = (await screen.findByText("Mapa do cânone")).closest("details");
    expect(details).not.toBeNull();
    expect(details).not.toHaveAttribute("open");
  });

  it("shows a pending message while the map has not started", async () => {
    render(<CanonMapPanel artifacts={artifacts({ canonMap: { status: "PENDING", categories: [] } })} finished={true} />);

    expect(await screen.findByText("O mapa do cânone será preparado a partir da thread.")).toBeInTheDocument();
  });

  it("shows the AI-not-configured message when the environment has no provider key", async () => {
    render(<CanonMapPanel artifacts={artifacts({ aiConfigured: false, canonMap: { status: "PENDING", categories: [] } })} finished={true} />);

    expect(await screen.findByText("O mapa do cânone não está disponível neste ambiente.")).toBeInTheDocument();
  });

  it("shows a neutral message when no categories are configured", async () => {
    render(<CanonMapPanel
      artifacts={artifacts({ canonMap: { status: "COMPLETED", categories: [] } })}
      finished={true}
    />);

    expect(await screen.findByText("Nenhuma categoria de mapa do cânone foi configurada para esta party.")).toBeInTheDocument();
  });

  it("shows a neutral message when categories exist but no elements were identified", async () => {
    render(<CanonMapPanel
      artifacts={artifacts({
        canonMap: {
          status: "COMPLETED",
          categories: [
            { id: "cat-1", name: "Pessoas", color: "#7665a7", displayOrder: 0, tags: [] },
            { id: "cat-2", name: "Lugares", color: "#c29042", displayOrder: 1, tags: [] },
          ],
        },
      })}
      finished={true}
    />);

    expect(await screen.findByText("Nenhum elemento foi identificado nas categorias configuradas.")).toBeInTheDocument();
  });

  it("shows the failed-state message without fabricating content", async () => {
    render(<CanonMapPanel artifacts={artifacts({ canonMap: { status: "FAILED", categories: [], errorMessage: "boom" } })} finished={true} />);

    expect(await screen.findByText("Não foi possível gerar o mapa do cânone.")).toBeInTheDocument();
    expect(screen.getByText("A thread original permanece intacta e completa.")).toBeInTheDocument();
  });

  it("renders tag chips with origin badges for a completed map", async () => {
    render(<CanonMapPanel
      artifacts={artifacts({
        canonMap: {
          status: "COMPLETED",
          categories: [
            {
              id: "cat-1",
              name: "Pessoas",
              color: "#7665a7",
              displayOrder: 0,
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
      })}
      finished={true}
    />);

    expect(await screen.findByText("Lucinda")).toBeInTheDocument();
    expect(screen.getByText("Inferido pela IA")).toBeInTheDocument();
    expect(screen.getByText("Complemento criativo")).toBeInTheDocument();
    expect(screen.getByText("Trechos 2, 4")).toBeInTheDocument();
  });
});
