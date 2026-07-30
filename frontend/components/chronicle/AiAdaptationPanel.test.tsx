import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { AiAdaptationPanel } from "@/components/chronicle/AiAdaptationPanel";
import type { GeneratedStory } from "@/lib/types";

const story: GeneratedStory = {
  id: "story-1",
  version: 2,
  title: "A Queda de Aravel",
  content: "Parágrafo um.\n\nParágrafo dois.",
  model: "gpt-test",
  createdAt: "2026-01-10T10:00:00.000Z",
};

describe("AiAdaptationPanel", () => {
  it("always shows the title and heading", () => {
    render(<AiAdaptationPanel status="AI_PENDING" />);
    expect(screen.getByRole("heading", { name: "Adaptação da história" })).toBeInTheDocument();
    expect(screen.getByText("Uma interpretação gerada a partir da thread original.")).toBeInTheDocument();
  });

  it("renders the generated story as plain text, preserving line breaks, even when archived", () => {
    render(<AiAdaptationPanel status="ARCHIVED" generatedStory={story} />);
    expect(screen.getByText("A Queda de Aravel")).toBeInTheDocument();
    expect(screen.getByText(/Parágrafo um\./)).toBeInTheDocument();
    expect(screen.getByText(/gerada por IA/i)).toBeInTheDocument();
  });

  it("shows a pending state when there is no story yet", () => {
    render(<AiAdaptationPanel status="AI_PENDING" />);
    expect(screen.getByText(/aguarda processamento/i)).toBeInTheDocument();
  });

  it("shows a processing state when there is no story yet", () => {
    render(<AiAdaptationPanel status="AI_PROCESSING" />);
    expect(screen.getByText(/tecendo a adaptação/i)).toBeInTheDocument();
  });

  it("shows a generic failure message without leaking raw errors", () => {
    render(<AiAdaptationPanel status="FAILED" />);
    expect(screen.getByText(/falhou/i)).toBeInTheDocument();
    expect(screen.queryByText(/exception|stack|openai/i)).not.toBeInTheDocument();
  });

  it("shows an honest message for an archived chronicle with no story", () => {
    render(<AiAdaptationPanel status="ARCHIVED" />);
    expect(screen.getByText(/arquivada sem uma adaptação/i)).toBeInTheDocument();
  });
});
