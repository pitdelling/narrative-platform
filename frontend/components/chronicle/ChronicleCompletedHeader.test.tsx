import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { ChronicleCompletedHeader } from "@/components/chronicle/ChronicleCompletedHeader";

describe("ChronicleCompletedHeader", () => {
  it("renders title, creator and both dates with accessible time elements", () => {
    render(
      <ChronicleCompletedHeader
        title="A Última Vigília"
        partyName="Os Errantes"
        creatorName="Aelwyn"
        createdAt="2026-01-01T10:00:00.000Z"
        completedAt="2026-01-15T18:00:00.000Z"
        cycleCount={2}
        totalTurns={8}
        completedTurns={8}
      />,
    );

    expect(screen.getByRole("heading", { level: 1, name: "A Última Vigília" })).toBeInTheDocument();
    expect(screen.getByText(/Os Errantes/)).toBeInTheDocument();
    expect(screen.getByText(/8 turnos · 2 ciclos/)).toBeInTheDocument();
    expect(screen.getByText(/Aelwyn/)).toBeInTheDocument();
    expect(screen.getByText("8 de 8 turnos concluídos")).toBeInTheDocument();

    const times = document.querySelectorAll("time");
    expect(times).toHaveLength(2);
    expect(times[0]).toHaveAttribute("datetime", "2026-01-01T10:00:00.000Z");
    expect(times[1]).toHaveAttribute("datetime", "2026-01-15T18:00:00.000Z");
  });

  it("omits the completion date instead of showing an invalid date when it is absent", () => {
    render(
      <ChronicleCompletedHeader
        title="A Última Vigília"
        partyName="Os Errantes"
        creatorName="Aelwyn"
        createdAt="2026-01-01T10:00:00.000Z"
        cycleCount={1}
        totalTurns={3}
        completedTurns={3}
      />,
    );

    expect(screen.queryByText(/Concluída em/)).not.toBeInTheDocument();
    expect(document.querySelectorAll("time")).toHaveLength(1);
  });
});
