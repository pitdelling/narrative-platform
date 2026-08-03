import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { StoryVoteControl } from "@/components/chronicle/StoryVoteControl";

const baseProps = {
  storyTitle: "The Dragon's Pact",
  unitsToday: 1,
  totalVotes: 7,
  remainingUnitsToday: 1,
  canVote: true,
};

describe("StoryVoteControl", () => {
  it("displays the story's aggregate total, not the caller's own vote count", () => {
    render(<StoryVoteControl {...baseProps} unitsToday={2} totalVotes={14} onChange={vi.fn()} />);
    expect(screen.getByLabelText("Total de votos: 14")).toHaveTextContent("14");
  });

  it("calls onChange with the incremented value", () => {
    const onChange = vi.fn();
    render(<StoryVoteControl {...baseProps} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText(`Adicionar um voto a ${baseProps.storyTitle}`));

    expect(onChange).toHaveBeenCalledWith(2);
  });

  it("calls onChange with the decremented value", () => {
    const onChange = vi.fn();
    render(<StoryVoteControl {...baseProps} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText(`Remover um voto de ${baseProps.storyTitle}`));

    expect(onChange).toHaveBeenCalledWith(0);
  });

  it("disables the minus button at the local minimum of 0", () => {
    render(<StoryVoteControl {...baseProps} unitsToday={0} onChange={vi.fn()} />);
    expect(screen.getByLabelText(`Remover um voto de ${baseProps.storyTitle}`)).toBeDisabled();
  });

  it("disables the plus button at the local maximum of 2", () => {
    render(<StoryVoteControl {...baseProps} unitsToday={2} remainingUnitsToday={0} onChange={vi.fn()} />);
    expect(screen.getByLabelText(`Adicionar um voto a ${baseProps.storyTitle}`)).toBeDisabled();
  });

  it("disables the plus button when the party-wide daily budget is exhausted", () => {
    render(<StoryVoteControl {...baseProps} unitsToday={0} remainingUnitsToday={0} onChange={vi.fn()} />);
    expect(screen.getByLabelText(`Adicionar um voto a ${baseProps.storyTitle}`)).toBeDisabled();
  });

  it("disables both buttons when canVote is false, even with remaining budget", () => {
    render(<StoryVoteControl {...baseProps} unitsToday={1} remainingUnitsToday={1} canVote={false} onChange={vi.fn()} />);
    expect(screen.getByLabelText(`Adicionar um voto a ${baseProps.storyTitle}`)).toBeDisabled();
    expect(screen.getByLabelText(`Remover um voto de ${baseProps.storyTitle}`)).toBeDisabled();
  });

  it("disables both buttons while an external action is pending", () => {
    render(<StoryVoteControl {...baseProps} disabled onChange={vi.fn()} />);
    expect(screen.getByLabelText(`Adicionar um voto a ${baseProps.storyTitle}`)).toBeDisabled();
    expect(screen.getByLabelText(`Remover um voto de ${baseProps.storyTitle}`)).toBeDisabled();
  });

  it("prevents the click from bubbling up to a wrapping card link", () => {
    const onChange = vi.fn();
    const linkClick = vi.fn();
    render(
      <a href="#" onClick={linkClick}>
        <StoryVoteControl {...baseProps} onChange={onChange} />
      </a>
    );

    fireEvent.click(screen.getByLabelText(`Adicionar um voto a ${baseProps.storyTitle}`));

    expect(onChange).toHaveBeenCalledWith(2);
    expect(linkClick).not.toHaveBeenCalled();
  });
});
