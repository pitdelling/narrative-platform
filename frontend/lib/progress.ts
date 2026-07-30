import type { Turn } from "@/lib/types";

export function turnProgress(turns: Turn[]): { completed: number; total: number } {
  const completed = turns.filter((turn) => turn.status !== "WAITING" && turn.status !== "ACTIVE").length;
  return { completed, total: turns.length };
}
