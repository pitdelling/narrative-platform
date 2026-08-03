import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { RankBadge } from "@/components/chronicle/RankBadge";

describe("RankBadge", () => {
  it("renders nothing when rank is undefined", () => {
    const { container } = render(<RankBadge />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing when rank is outside the top 5", () => {
    const { container } = render(<RankBadge rank={6} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders the matching badge artwork with an accessible label", () => {
    render(<RankBadge rank={1} />);
    const badge = screen.getByRole("img", { name: "1º lugar" });
    expect(badge).toHaveAttribute("src", "/badges/rank-1.svg");
  });

  it("picks the matching artwork file for each podium position", () => {
    render(<RankBadge rank={5} />);
    expect(screen.getByRole("img", { name: "5º lugar" })).toHaveAttribute("src", "/badges/rank-5.svg");
  });
});
