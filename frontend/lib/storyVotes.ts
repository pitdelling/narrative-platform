import { api } from "@/lib/api";
import type { ChronicleCard, DailyStoryVoteState, SortMode, StoryVoteSummary } from "@/lib/types";

export const STORY_VOTE_DAILY_LIMIT = 2;

const MAX_PODIUM_RANK = 5;

/**
 * Missing `publishedAt` (should not happen for published stories) is always pushed to the
 * end, regardless of direction, rather than being treated as "oldest".
 */
function compareByPublishedAt(a: ChronicleCard, b: ChronicleCard, direction: 1 | -1): number {
  if (!a.publishedAt && !b.publishedAt) return a.id.localeCompare(b.id);
  if (!a.publishedAt) return 1;
  if (!b.publishedAt) return -1;
  const timeA = new Date(a.publishedAt).getTime();
  const timeB = new Date(b.publishedAt).getTime();
  if (timeA !== timeB) return direction * (timeA - timeB);
  return a.id.localeCompare(b.id);
}

function compareByRank(a: ChronicleCard, b: ChronicleCard, summaryByChronicleId: Map<string, StoryVoteSummary>): number {
  const rankA = summaryByChronicleId.get(a.id)?.rank ?? Number.MAX_SAFE_INTEGER;
  const rankB = summaryByChronicleId.get(b.id)?.rank ?? Number.MAX_SAFE_INTEGER;
  if (rankA !== rankB) return rankA - rankB;
  return compareByPublishedAt(a, b, -1);
}

/**
 * Only reorders the published subset of the list — the "in progress" group keeps its
 * existing ordering logic untouched (see `compareInProgressChronicles` in the party page).
 * `RANK` trusts the backend's authoritative, already-deterministic `rank`; it is never
 * recomputed from list position.
 */
export function sortPublishedChronicles(
  chronicles: ChronicleCard[],
  summaryByChronicleId: Map<string, StoryVoteSummary>,
  mode: SortMode
): ChronicleCard[] {
  const sorted = [...chronicles];

  if (mode === "RANK") {
    sorted.sort((a, b) => compareByRank(a, b, summaryByChronicleId));
    return sorted;
  }

  const direction = mode === "NEWEST" ? -1 : 1;
  sorted.sort((a, b) => compareByPublishedAt(a, b, direction));
  return sorted;
}

/**
 * Splits the published stories into the two blocks shown when the list is sorted by
 * Ranking: "podium" (rank 1-5, dense — ties can put more than 5 stories there) ordered by
 * rank, and "concluded" (no rank at all, i.e. zero votes, or rank below the podium) ordered
 * newest-published-first. A story only ever gets a rank once it has at least one vote —
 * `undefined` rank always means "no votes yet", never "not ranked yet for some other reason".
 */
export function splitRankedChronicles(
  chronicles: ChronicleCard[],
  summaryByChronicleId: Map<string, StoryVoteSummary>
): { podium: ChronicleCard[]; concluded: ChronicleCard[] } {
  const podium: ChronicleCard[] = [];
  const concluded: ChronicleCard[] = [];

  for (const chronicle of chronicles) {
    const rank = summaryByChronicleId.get(chronicle.id)?.rank;
    if (rank !== undefined && rank >= 1 && rank <= MAX_PODIUM_RANK) {
      podium.push(chronicle);
    } else {
      concluded.push(chronicle);
    }
  }

  podium.sort((a, b) => compareByRank(a, b, summaryByChronicleId));
  concluded.sort((a, b) => compareByPublishedAt(a, b, -1));
  return { podium, concluded };
}

export function pluralizeVotes(totalVotes: number): string {
  return `${totalVotes} ${totalVotes === 1 ? "voto" : "votos"}`;
}

export function rankBadgeLabel(rank: number): string {
  return `${rank}º lugar`;
}

/**
 * The day resets at UTC midnight, computed by the server — this text intentionally never
 * mentions the browser's timezone, so it doesn't imply a local-time reset.
 */
export function dailyBudgetMessage(state: DailyStoryVoteState): string {
  if (state.remainingUnits <= 0) return "Você já usou seus 2 votos de hoje";
  return `Você ainda tem ${state.remainingUnits} ${state.remainingUnits === 1 ? "voto" : "votos"} hoje`;
}

export function submitVote(partyId: string, chronicleId: string, units: number): Promise<DailyStoryVoteState> {
  return api<DailyStoryVoteState>(`/parties/${partyId}/chronicles/${chronicleId}/votes/today`, {
    method: "PUT",
    body: JSON.stringify({ units }),
  });
}
