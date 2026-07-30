import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { CanonMapPanel } from "@/components/chronicle/CanonMapPanel";

describe("CanonMapPanel", () => {
  it("renders an honest empty state without fabricating tags", () => {
    render(<CanonMapPanel />);

    expect(screen.getByText("Mapa do cânone")).toBeInTheDocument();
    expect(screen.getByText("Nenhum mapa do cânone foi gerado para esta história.")).toBeInTheDocument();
    expect(screen.queryByText(/em breve/i)).not.toBeInTheDocument();
  });

  it("is collapsed by default", () => {
    render(<CanonMapPanel />);

    const details = screen.getByText("Mapa do cânone").closest("details");
    expect(details).not.toBeNull();
    expect(details).not.toHaveAttribute("open");
  });
});
