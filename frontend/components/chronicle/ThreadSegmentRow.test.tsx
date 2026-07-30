import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ThreadSegmentRow } from "@/components/chronicle/ThreadSegmentRow";
import type { Segment, Turn } from "@/lib/types";

function buildTurn(overrides: Partial<Turn> = {}): Turn {
  return {
    id: "turn-1",
    sequenceNumber: 1,
    cycleNumber: 1,
    positionInCycle: 1,
    userId: "user-1",
    author: "Aelwyn",
    status: "SUBMITTED",
    ...overrides,
  };
}

function buildSegment(overrides: Partial<Segment> = {}): Segment {
  return {
    id: "segment-1",
    sequenceNumber: 1,
    cycleNumber: 1,
    authorId: "user-1",
    author: "Aelwyn",
    status: "ACTIVE",
    visible: true,
    content: "A porta rangeu ao abrir.",
    size: "SHORT",
    submittedAt: "2026-02-01T09:00:00.000Z",
    ...overrides,
  };
}

const noop = vi.fn();

describe("ThreadSegmentRow", () => {
  it("renders an active visible segment's content without action buttons for non-narrators", () => {
    render(
      <ThreadSegmentRow turn={buildTurn()} segment={buildSegment()} narrator={false} onEdit={noop} onDisable={noop} onRestore={noop} />,
    );
    expect(screen.getByText("A porta rangeu ao abrir.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Editar" })).not.toBeInTheDocument();
  });

  it("hides disabled segment content and shows the removal notice for a non-narrator", () => {
    render(
      <ThreadSegmentRow
        turn={buildTurn()}
        segment={buildSegment({ status: "DISABLED", content: undefined, disabledReason: "Continuidade quebrada." })}
        narrator={false}
        onEdit={noop}
        onDisable={noop}
        onRestore={noop}
      />,
    );
    expect(screen.getByText("Removido pelo Narrador")).toBeInTheDocument();
    expect(screen.getByText(/Continuidade quebrada\./)).toBeInTheDocument();
    expect(screen.queryByText("A porta rangeu ao abrir.")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Restaurar" })).not.toBeInTheDocument();
  });

  it("offers restore/edit actions to a narrator on a disabled segment", () => {
    render(
      <ThreadSegmentRow
        turn={buildTurn()}
        segment={buildSegment({ status: "DISABLED" })}
        narrator={true}
        onEdit={noop}
        onDisable={noop}
        onRestore={noop}
      />,
    );
    expect(screen.getByRole("button", { name: "Restaurar" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Editar" })).toBeInTheDocument();
  });

  it("never renders content for a hidden (unrevealed) segment", () => {
    render(
      <ThreadSegmentRow
        turn={buildTurn()}
        segment={buildSegment({ visible: false, content: undefined })}
        narrator={false}
        onEdit={noop}
        onDisable={noop}
        onRestore={noop}
      />,
    );
    expect(screen.getByText("◌ Fragmento velado")).toBeInTheDocument();
    expect(screen.queryByText("A porta rangeu ao abrir.")).not.toBeInTheDocument();
  });

  it("shows a neutral 'skipped' card without inventing content", () => {
    render(
      <ThreadSegmentRow turn={buildTurn({ status: "SKIPPED" })} segment={undefined} narrator={false} onEdit={noop} onDisable={noop} onRestore={noop} />,
    );
    expect(screen.getByText("Turno pulado")).toBeInTheDocument();
  });

  it("shows a neutral 'expired' card without inventing content", () => {
    render(
      <ThreadSegmentRow turn={buildTurn({ status: "EXPIRED" })} segment={undefined} narrator={false} onEdit={noop} onDisable={noop} onRestore={noop} />,
    );
    expect(screen.getByText("Turno expirado")).toBeInTheDocument();
  });
});
