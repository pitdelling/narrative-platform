import type { ReactNode } from "react";
import { formatDate } from "@/lib/format";
import { GameProgressBar } from "@/components/chronicle/GameProgressBar";

interface ChronicleCompletedHeaderProps {
  title: string;
  partyName: string;
  creatorName: string;
  createdAt: string;
  completedAt?: string;
  cycleCount: number;
  totalTurns: number;
  completedTurns: number;
  actions?: ReactNode;
}

export function ChronicleCompletedHeader({
  title,
  partyName,
  creatorName,
  createdAt,
  completedAt,
  cycleCount,
  totalTurns,
  completedTurns,
  actions,
}: ChronicleCompletedHeaderProps) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">História-jogo · {partyName} · História finalizada</p>
        <h1>{title}</h1>
        <GameProgressBar completed={completedTurns} total={totalTurns} />
        <p className="completion-metadata">
          {totalTurns} turno{totalTurns === 1 ? "" : "s"} · {cycleCount} ciclo{cycleCount === 1 ? "" : "s"}
        </p>
        <p className="completion-metadata">
          Criada por {creatorName} em <time dateTime={createdAt}>{formatDate(createdAt)}</time>
          {completedAt && (
            <>
              {" "}· Concluída em <time dateTime={completedAt}>{formatDate(completedAt)}</time>
            </>
          )}
        </p>
      </div>
      {actions && <div className="header-actions">{actions}</div>}
    </header>
  );
}
