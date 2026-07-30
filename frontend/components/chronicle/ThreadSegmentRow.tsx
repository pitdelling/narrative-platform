import type { Segment, Turn } from "@/lib/types";
import { formatDateTime, turnStatusLabels } from "@/lib/format";

interface ThreadSegmentRowProps {
  turn: Turn;
  segment?: Segment;
  narrator: boolean;
  onEdit: (segment: Segment) => void;
  onDisable: (segment: Segment) => void;
  onRestore: (segment: Segment) => void;
}

export function ThreadSegmentRow({ turn, segment, narrator, onEdit, onDisable, onRestore }: ThreadSegmentRowProps) {
  const classNames = [
    "thread-item",
    segment?.status === "DISABLED" ? "disabled" : "",
    turn.status === "SKIPPED" ? "skipped" : "",
    turn.status === "EXPIRED" ? "expired" : "",
  ].filter(Boolean).join(" ");

  return (
    <article className={classNames}>
      <div className="thread-marker" aria-hidden="true">{turn.sequenceNumber}</div>
      <div className="thread-body card">
        <div className="thread-meta">
          <strong>{turn.author}</strong>
          <span>
            Ciclo {turn.cycleNumber} · Posição {turn.positionInCycle} · {turnStatusLabels[turn.status]}
            {segment && (
              <>
                {" · "}
                <time dateTime={segment.submittedAt}>{formatDateTime(segment.submittedAt)}</time>
              </>
            )}
          </span>
        </div>
        {segment
          ? segment.visible
            ? (
              <>
                <p className="segment-content">{segment.status === "DISABLED" ? "Removido pelo Narrador" : segment.content}</p>
                {segment.status === "DISABLED" && segment.disabledReason && <p className="removal-reason">Motivo: {segment.disabledReason}</p>}
                {narrator && (
                  <div className="thread-actions">
                    <button onClick={() => onEdit(segment)}>Editar</button>
                    {segment.status === "DISABLED"
                      ? <button onClick={() => onRestore(segment)}>Restaurar</button>
                      : <button onClick={() => onDisable(segment)}>Desabilitar</button>}
                  </div>
                )}
              </>
            )
            : <HiddenBlock size={segment.size} />
          : <PendingBlock status={turn.status} />}
      </div>
    </article>
  );
}

function HiddenBlock({ size }: { size: Segment["size"] }) {
  return (
    <div className={`hidden-fragment hidden-${size.toLowerCase()}`}>
      <div className="hidden-fragment-easter-egg" aria-hidden="true">
        Aaaaaaaahhhhh seu elfo esperto, achou mesmo que você veria o texto dos outros? O Narrador é tudo, sabe tudo E VÊ TUDO!
      </div>
      <div className="hidden-fragment-cover">
        <span>◌ Fragmento velado</span>
        <small>O conteúdo ainda não foi revelado para você.</small>
      </div>
    </div>
  );
}

function PendingBlock({ status }: { status: string }) {
  const label = status === "SKIPPED"
    ? "Turno pulado"
    : status === "EXPIRED"
      ? "Turno expirado"
      : "Aguardando este fragmento";
  return <div className={`pending-fragment pending-${status.toLowerCase()}`}><span>{label}</span></div>;
}
