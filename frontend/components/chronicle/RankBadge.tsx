import { rankBadgeLabel } from "@/lib/storyVotes";

interface RankBadgeProps {
  rank?: number;
}

const MAX_PODIUM_RANK = 5;

export function RankBadge({ rank }: RankBadgeProps) {
  if (!rank || rank < 1 || rank > MAX_PODIUM_RANK) return null;

  const label = rankBadgeLabel(rank);

  // eslint-disable-next-line @next/next/no-img-element -- static decorative artwork from /public, not worth next/image's overhead here.
  return <img className="rank-badge" src={`/badges/rank-${rank}.svg`} alt={label} title={label} />;
}
