"use client";

import type { GameParticipant } from "@/lib/types";

interface ParticipantsPanelProps {
  participants: GameParticipant[];
  currentUserId: string;
  narrator: boolean;
  onLeave: (userId: string) => void;
  onRejoin: (userId: string) => void;
}

const statusLabels: Record<GameParticipant["status"], string> = {
  ACTIVE: "Participando",
  LEFT: "Fora da história",
};

export function ParticipantsPanel({ participants, currentUserId, narrator, onLeave, onRejoin }: ParticipantsPanelProps) {
  return (
    <details className="card members-panel">
      <summary>Participantes</summary>
      <div className="member-list">
        {participants.map((participant) => {
          const self = participant.userId === currentUserId;
          const canLeave = participant.status === "ACTIVE" && (self || narrator);
          const canRejoin = participant.status === "LEFT"
            && (participant.removedByType === "NARRATOR" ? narrator : (self || narrator));
          return (
            <div className={`member-row member-${participant.status.toLowerCase()}`} key={participant.userId}>
              <div>
                <strong>{participant.displayName}</strong>
                <small>{statusLabels[participant.status]}</small>
              </div>
              <div className="member-actions">
                {canLeave && (
                  <button
                    className="button danger-outline"
                    onClick={() => {
                      const message = self
                        ? "Sair desta história-jogo? Você deixará de participar dos próximos ciclos, mas seus trechos já escritos continuam no registro. Você poderá voltar clicando em \"Voltar para a história\"."
                        : `Remover ${participant.displayName} desta história-jogo? A pessoa deixará de participar dos próximos ciclos, mas os trechos já escritos permanecem. Só um narrador poderá trazê-la de volta.`;
                      if (window.confirm(message)) onLeave(participant.userId);
                    }}
                  >
                    {self ? "Sair da história" : "Remover da história"}
                  </button>
                )}
                {canRejoin && (
                  <button className="button secondary" onClick={() => onRejoin(participant.userId)}>
                    Voltar para a história
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </details>
  );
}
