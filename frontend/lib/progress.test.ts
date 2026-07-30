import { describe, expect, it } from "vitest";
import { turnProgress } from "@/lib/progress";
import type { Turn } from "@/lib/types";

function turn(status: Turn["status"]): Turn {
  return {
    id: status,
    sequenceNumber: 1,
    cycleNumber: 1,
    positionInCycle: 1,
    userId: "user-1",
    author: "Author",
    status,
  };
}

describe("turnProgress", () => {
  it("counts submitted, skipped and expired turns as completed", () => {
    const turns = [turn("SUBMITTED"), turn("SKIPPED"), turn("EXPIRED"), turn("WAITING"), turn("ACTIVE")];
    expect(turnProgress(turns)).toEqual({ completed: 3, total: 5 });
  });

  it("returns zero completed and zero total for an empty list", () => {
    expect(turnProgress([])).toEqual({ completed: 0, total: 0 });
  });

  it("returns full completion when every turn is done", () => {
    const turns = [turn("SUBMITTED"), turn("SKIPPED")];
    expect(turnProgress(turns)).toEqual({ completed: 2, total: 2 });
  });
});
