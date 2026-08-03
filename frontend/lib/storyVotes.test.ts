import { describe, expect, it } from "vitest";
import { dailyBudgetMessage, pluralizeVotes, rankBadgeLabel, sortPublishedChronicles, splitRankedChronicles } from "@/lib/storyVotes";
import type { ChronicleCard, DailyStoryVoteState, StoryVoteSummary } from "@/lib/types";

function card(id: string, publishedAt?: string): ChronicleCard {
  return {
    id,
    type: "GAME",
    status: "PUBLISHED",
    title: `Story ${id}`,
    creatorName: "Author",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    publishedAt,
    published: true,
  };
}

function summary(chronicleId: string, rank: number | undefined, totalVotes = 0): StoryVoteSummary {
  return {
    chronicleId,
    totalVotes,
    rank,
    currentUserVotesToday: 0,
    currentUserRemainingVotesToday: 2,
    canVote: true,
  };
}

describe("sortPublishedChronicles", () => {
  it("orders by the backend's authoritative rank, never by list position", () => {
    const chronicles = [card("b"), card("a"), card("c")];
    const summaries = new Map([
      ["b", summary("b", 3)],
      ["a", summary("a", 1)],
      ["c", summary("c", 2)],
    ]);

    const sorted = sortPublishedChronicles(chronicles, summaries, "RANK");

    expect(sorted.map((c) => c.id)).toEqual(["a", "c", "b"]);
  });

  it("breaks a rank tie (or missing summary) deterministically by id", () => {
    const chronicles = [card("z"), card("y")];
    const summaries = new Map<string, StoryVoteSummary>();

    const sorted = sortPublishedChronicles(chronicles, summaries, "RANK");

    expect(sorted.map((c) => c.id)).toEqual(["y", "z"]);
  });

  it("orders NEWEST by publishedAt descending", () => {
    const chronicles = [
      card("old", "2026-01-01T00:00:00Z"),
      card("new", "2026-03-01T00:00:00Z"),
      card("mid", "2026-02-01T00:00:00Z"),
    ];

    const sorted = sortPublishedChronicles(chronicles, new Map(), "NEWEST");

    expect(sorted.map((c) => c.id)).toEqual(["new", "mid", "old"]);
  });

  it("orders OLDEST by publishedAt ascending", () => {
    const chronicles = [
      card("old", "2026-01-01T00:00:00Z"),
      card("new", "2026-03-01T00:00:00Z"),
      card("mid", "2026-02-01T00:00:00Z"),
    ];

    const sorted = sortPublishedChronicles(chronicles, new Map(), "OLDEST");

    expect(sorted.map((c) => c.id)).toEqual(["old", "mid", "new"]);
  });

  it("pushes a missing publishedAt to the end regardless of mode", () => {
    const chronicles = [card("known", "2026-01-01T00:00:00Z"), card("unknown", undefined)];

    expect(sortPublishedChronicles(chronicles, new Map(), "NEWEST").map((c) => c.id)).toEqual(["known", "unknown"]);
    expect(sortPublishedChronicles(chronicles, new Map(), "OLDEST").map((c) => c.id)).toEqual(["known", "unknown"]);
  });
});

describe("splitRankedChronicles", () => {
  it("puts stories ranked 1-5 in the podium, ordered by rank", () => {
    const chronicles = [card("c", "2026-01-03T00:00:00Z"), card("a", "2026-01-01T00:00:00Z"), card("b", "2026-01-02T00:00:00Z")];
    const summaries = new Map([
      ["c", summary("c", 3, 5)],
      ["a", summary("a", 1, 20)],
      ["b", summary("b", 2, 10)],
    ]);

    const { podium, concluded } = splitRankedChronicles(chronicles, summaries);

    expect(podium.map((c) => c.id)).toEqual(["a", "b", "c"]);
    expect(concluded).toEqual([]);
  });

  it("keeps every tied story in the podium even when more than 5 share the same dense rank", () => {
    const chronicles = Array.from({ length: 7 }, (_, index) => card(`s${index}`, `2026-01-0${(index % 9) + 1}T00:00:00Z`));
    const summaries = new Map(chronicles.map((c) => [c.id, summary(c.id, 1, 10)]));

    const { podium, concluded } = splitRankedChronicles(chronicles, summaries);

    expect(podium).toHaveLength(7);
    expect(concluded).toHaveLength(0);
  });

  it("sends stories with no votes (no rank) and stories ranked below 5 to \"concluded\", newest first", () => {
    const chronicles = [
      card("no-votes", "2026-01-05T00:00:00Z"),
      card("rank-6", "2026-01-10T00:00:00Z"),
      card("rank-1", "2026-01-01T00:00:00Z"),
    ];
    const summaries = new Map([
      ["no-votes", summary("no-votes", undefined, 0)],
      ["rank-6", summary("rank-6", 6, 1)],
      ["rank-1", summary("rank-1", 1, 50)],
    ]);

    const { podium, concluded } = splitRankedChronicles(chronicles, summaries);

    expect(podium.map((c) => c.id)).toEqual(["rank-1"]);
    // Newest published first among the non-podium stories, regardless of vote count.
    expect(concluded.map((c) => c.id)).toEqual(["rank-6", "no-votes"]);
  });
});

describe("pluralizeVotes", () => {
  it("pluralizes zero, one and many votes correctly", () => {
    expect(pluralizeVotes(0)).toBe("0 votos");
    expect(pluralizeVotes(1)).toBe("1 voto");
    expect(pluralizeVotes(14)).toBe("14 votos");
  });
});

describe("rankBadgeLabel", () => {
  it("formats an ordinal placement label", () => {
    expect(rankBadgeLabel(1)).toBe("1º lugar");
    expect(rankBadgeLabel(5)).toBe("5º lugar");
  });
});

function dailyState(remainingUnits: number): DailyStoryVoteState {
  return { dateUtc: "2026-08-03", dailyLimit: 2, usedUnits: 2 - remainingUnits, remainingUnits, allocations: [] };
}

describe("dailyBudgetMessage", () => {
  it("announces two remaining votes", () => {
    expect(dailyBudgetMessage(dailyState(2))).toBe("Você ainda tem 2 votos hoje");
  });

  it("uses singular wording for one remaining vote", () => {
    expect(dailyBudgetMessage(dailyState(1))).toBe("Você ainda tem 1 voto hoje");
  });

  it("announces the budget is exhausted, without mentioning a local timezone", () => {
    const message = dailyBudgetMessage(dailyState(0));
    expect(message).toBe("Você já usou seus 2 votos de hoje");
    expect(message.toLowerCase()).not.toContain("fuso");
  });
});
