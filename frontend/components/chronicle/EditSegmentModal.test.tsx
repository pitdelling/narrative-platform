import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EditSegmentModal } from "@/components/chronicle/EditSegmentModal";
import type { Segment } from "@/lib/types";

function buildSegment(overrides: Partial<Segment> = {}): Segment {
  return {
    id: "segment-1",
    sequenceNumber: 1,
    cycleNumber: 1,
    authorId: "user-1",
    author: "Aelwyn",
    status: "ACTIVE",
    visible: true,
    content: "<p>Original content</p>",
    size: "SHORT",
    submittedAt: "2026-02-01T09:00:00.000Z",
    ...overrides,
  };
}

describe("EditSegmentModal", () => {
  it("renders nothing when closed", () => {
    const { container } = render(
      <EditSegmentModal open={false} segment={buildSegment()} onClose={vi.fn()} onSave={vi.fn()} />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("prefills the editor from the segment's content", async () => {
    render(<EditSegmentModal open segment={buildSegment()} onClose={vi.fn()} onSave={vi.fn()} />);
    expect(await screen.findByText("Original content")).toBeInTheDocument();
  });

  it("cancels without calling onSave", async () => {
    const onSave = vi.fn();
    const onClose = vi.fn();
    render(<EditSegmentModal open segment={buildSegment()} onClose={onClose} onSave={onSave} />);
    await screen.findByText("Original content");
    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(onClose).toHaveBeenCalledOnce();
    expect(onSave).not.toHaveBeenCalled();
  });

  it("saves with the (possibly edited) reason field", async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    render(<EditSegmentModal open segment={buildSegment()} onClose={vi.fn()} onSave={onSave} />);
    await screen.findByText("Original content");

    const reasonInput = screen.getByLabelText("Motivo");
    await userEvent.clear(reasonInput);
    await userEvent.type(reasonInput, "Fixed a typo.");
    await userEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => expect(onSave).toHaveBeenCalledOnce());
    expect(onSave.mock.calls[0][1]).toBe("Fixed a typo.");
  });
});
