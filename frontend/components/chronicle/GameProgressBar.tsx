interface GameProgressBarProps {
  completed: number;
  total: number;
}

export function GameProgressBar({ completed, total }: GameProgressBarProps) {
  if (total === 0) return null;

  const percent = Math.round((completed / total) * 100);

  return (
    <div
      className="progress-bar"
      role="progressbar"
      aria-valuenow={completed}
      aria-valuemin={0}
      aria-valuemax={total}
      aria-label="Progresso dos turnos"
    >
      <div className="progress-bar-track">
        <div className="progress-bar-fill" style={{ width: `${percent}%` }} />
      </div>
      <span className="progress-bar-label">
        {completed} de {total} turno{total === 1 ? "" : "s"} concluído{total === 1 ? "" : "s"}
      </span>
    </div>
  );
}
