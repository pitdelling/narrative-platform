import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { GameProgressBar } from "@/components/chronicle/GameProgressBar";

describe("GameProgressBar", () => {
  it("renders nothing when there are no turns", () => {
    const { container } = render(<GameProgressBar completed={0} total={0} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders progress metadata and a proportional fill", () => {
    render(<GameProgressBar completed={3} total={6} />);

    const bar = screen.getByRole("progressbar");
    expect(bar).toHaveAttribute("aria-valuenow", "3");
    expect(bar).toHaveAttribute("aria-valuemax", "6");
    expect(screen.getByText("3 de 6 turnos concluídos")).toBeInTheDocument();
  });

  it("uses singular wording for a single turn", () => {
    render(<GameProgressBar completed={1} total={1} />);
    expect(screen.getByText("1 de 1 turno concluído")).toBeInTheDocument();
  });
});
