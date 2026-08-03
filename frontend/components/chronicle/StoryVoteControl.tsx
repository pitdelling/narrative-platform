import type { MouseEvent } from "react";
import { STORY_VOTE_DAILY_LIMIT } from "@/lib/storyVotes";

interface StoryVoteControlProps {
  storyTitle: string;
  unitsToday: number;
  totalVotes: number;
  remainingUnitsToday: number;
  canVote: boolean;
  disabled?: boolean;
  onChange: (nextUnits: number) => void;
}

/**
 * A floating up/down widget meant to sit in a card's corner (see `.story-vote-control` in
 * globals.css). Presentational only: never calls the API itself — the parent owns the `PUT`
 * request, applies the server's response, and revalidates the summary, so the backend stays
 * the only source of truth (no permanent optimistic update happens here). The number shown
 * between the arrows is the story's aggregate `totalVotes`, not the caller's own vote count —
 * `unitsToday` only drives the arrows' enabled/disabled state.
 */
export function StoryVoteControl({
  storyTitle, unitsToday, totalVotes, remainingUnitsToday, canVote, disabled, onChange,
}: StoryVoteControlProps) {
  const isBlocked = disabled || !canVote;
  const canDecrease = !isBlocked && unitsToday > 0;
  const canIncrease = !isBlocked && unitsToday < STORY_VOTE_DAILY_LIMIT && remainingUnitsToday > 0;

  function stopCardNavigation(event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();
  }

  return (
    <div className="story-vote-control">
      <button
        type="button"
        className="story-vote-button story-vote-up"
        disabled={!canIncrease}
        title={`Adicionar um voto a ${storyTitle}`}
        aria-label={`Adicionar um voto a ${storyTitle}`}
        onClick={(event) => {
          stopCardNavigation(event);
          onChange(unitsToday + 1);
        }}
      >
        <span aria-hidden="true">▲</span>
      </button>
      <span className="story-vote-count" aria-label={`Total de votos: ${totalVotes}`}>{totalVotes}</span>
      <button
        type="button"
        className="story-vote-button story-vote-down"
        disabled={!canDecrease}
        title={`Remover um voto de ${storyTitle}`}
        aria-label={`Remover um voto de ${storyTitle}`}
        onClick={(event) => {
          stopCardNavigation(event);
          onChange(unitsToday - 1);
        }}
      >
        <span aria-hidden="true">▼</span>
      </button>
    </div>
  );
}
