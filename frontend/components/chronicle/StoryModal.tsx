"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { ChronicleStatus, GeneratedStory } from "@/lib/types";
import { AiAdaptationPanel } from "@/components/chronicle/AiAdaptationPanel";

interface StoryModalProps {
  open: boolean;
  onClose: () => void;
  partyId: string;
  chronicleId: string;
  status: ChronicleStatus;
  currentStory?: GeneratedStory;
  canRegenerate: boolean;
  isRegenerating: boolean;
  onRegenerate: () => void;
}

export function StoryModal({
  open,
  onClose,
  partyId,
  chronicleId,
  status,
  currentStory,
  canRegenerate,
  isRegenerating,
  onRegenerate,
}: StoryModalProps) {
  const [versions, setVersions] = useState<GeneratedStory[] | null>(null);
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null);
  const [lastStoryId, setLastStoryId] = useState(currentStory?.id);

  if (currentStory?.id !== lastStoryId) {
    setLastStoryId(currentStory?.id);
    setSelectedVersionId(null);
  }

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    api<GeneratedStory[]>(`/parties/${partyId}/chronicles/${chronicleId}/generated-stories`)
      .then((result) => {
        if (!cancelled) setVersions(result);
      })
      .catch(() => {
        if (!cancelled) setVersions(null);
      });
    return () => {
      cancelled = true;
    };
  }, [open, partyId, chronicleId, currentStory?.id]);

  if (!open) return null;

  const displayedStory = versions?.find((version) => version.id === selectedVersionId) ?? currentStory;

  return (
    <div
      className="modal-layer"
      role="presentation"
      onMouseDown={(event) => {
        if (event.currentTarget === event.target) onClose();
      }}
    >
      <section className="story-modal card" role="dialog" aria-modal="true" aria-labelledby="ai-adaptation-heading">
        <button className="modal-close" type="button" aria-label="Fechar" onClick={onClose}>
          ×
        </button>
        <div className="story-modal-scroll">
          <div className="modal-heading">
            <p className="eyebrow">História adaptada</p>
            <div className="modal-actions">
              {versions && versions.length > 1 && (
                <select
                  aria-label="Selecionar versão da história"
                  value={selectedVersionId ?? versions[0]?.id ?? ""}
                  onChange={(event) => setSelectedVersionId(event.target.value)}
                >
                  {versions.map((version) => (
                    <option key={version.id} value={version.id}>
                      v{version.version} · {formatDate(version.createdAt)}
                      {version.id === currentStory?.id ? " (atual)" : ""}
                    </option>
                  ))}
                </select>
              )}
              {(canRegenerate || isRegenerating) && (
                <button
                  className="button primary regenerate-button"
                  type="button"
                  title="Regenerar história"
                  aria-label="Regenerar história"
                  onClick={onRegenerate}
                  disabled={isRegenerating}
                >
                  <span className="regenerate-icon" aria-hidden="true">⟲</span>
                  <span className="regenerate-label">{isRegenerating ? "Gerando..." : "Regenerar história"}</span>
                </button>
              )}
            </div>
          </div>
          <AiAdaptationPanel status={status} generatedStory={displayedStory} />
        </div>
      </section>
    </div>
  );
}
