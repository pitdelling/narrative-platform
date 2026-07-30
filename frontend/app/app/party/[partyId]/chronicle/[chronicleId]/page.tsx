"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { turnProgress } from "@/lib/progress";
import type { GameDetail, PartyDetail, Segment, WrittenDetail } from "@/lib/types";
import { AppShell } from "@/components/AppShell";
import { ChronicleCompletedHeader } from "@/components/chronicle/ChronicleCompletedHeader";
import { CanonMapPanel } from "@/components/chronicle/CanonMapPanel";
import { GameProgressBar } from "@/components/chronicle/GameProgressBar";
import { StoryModal } from "@/components/chronicle/StoryModal";
import { ThreadSegmentRow } from "@/components/chronicle/ThreadSegmentRow";

export default function ChronicleDetailPage() {
  const { partyId, chronicleId } = useParams<{ partyId: string; chronicleId: string }>();
  const type = useSearchParams().get("type") ?? "GAME";
  return (
    <AppShell partyId={partyId}>
      {type === "WRITTEN"
        ? <WrittenView partyId={partyId} chronicleId={chronicleId} />
        : <GameView partyId={partyId} chronicleId={chronicleId} />}
    </AppShell>
  );
}

function GameView({ partyId, chronicleId }: { partyId: string; chronicleId: string }) {
  const [data, setData] = useState<GameDetail>();
  const [party, setParty] = useState<PartyDetail>();
  const [loadError, setLoadError] = useState<ApiError>();
  const [draft, setDraft] = useState("");
  const [revealed, setRevealed] = useState(false);
  const [message, setMessage] = useState("");
  const [storyModalOpen, setStoryModalOpen] = useState(false);

  const load = useCallback(async (reveal = false) => {
    try {
      const [detail, partyDetail] = await Promise.all([
        api<GameDetail>(`/parties/${partyId}/chronicles/${chronicleId}/game?reveal=${reveal}`),
        api<PartyDetail>(`/parties/${partyId}`),
      ]);
      setData(detail);
      setParty(partyDetail);
      setDraft(detail.currentDraft ?? "");
      setLoadError(undefined);
    } catch (cause) {
      setLoadError(cause instanceof ApiError ? cause : new ApiError("Não foi possível carregar a crônica.", 0));
    }
  }, [partyId, chronicleId]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const segments = useMemo(
    () => new Map(data?.segments.map((segment) => [segment.sequenceNumber, segment])),
    [data],
  );

  const previousMessage = useMemo(() => {
    if (!data?.currentUserTurn) return undefined;
    return data.segments
      .filter((segment) => segment.visible && segment.status !== "DISABLED" && segment.sequenceNumber < data.currentSequence)
      .sort((left, right) => right.sequenceNumber - left.sequenceNumber)[0];
  }, [data]);

  const progress = useMemo(() => turnProgress(data?.turns ?? []), [data]);

  async function action(path: string, body?: unknown, method = "POST") {
    setMessage("");
    try {
      await api(`/parties/${partyId}/chronicles/${chronicleId}${path}`, {
        method,
        body: body ? JSON.stringify(body) : undefined,
      });
      await load(revealed);
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : "A ação falhou.");
    }
  }

  async function reveal() {
    const revealSeconds = data?.revealSeconds ?? 10;
    await load(true);
    setRevealed(true);
    window.setTimeout(() => {
      setRevealed(false);
      void load(false);
    }, revealSeconds * 1000);
  }

  async function disable(segment: Segment) {
    const reason = window.prompt("Motivo da remoção (opcional):") ?? "";
    await action(`/segments/${segment.id}/disable`, { reason });
  }

  async function edit(segment: Segment) {
    const content = window.prompt("Novo conteúdo:", segment.content ?? "");
    if (content) await action(`/segments/${segment.id}/edit`, { content, reason: "Edited by narrator." });
  }

  if (loadError) {
    const description = loadError.status === 403
      ? "Você não tem acesso a esta crônica."
      : loadError.status === 404
        ? "Crônica não encontrada."
        : loadError.message;
    return (
      <>
        <Link className="back-link" href={`/app/party/${partyId}`}>← Voltar às crônicas</Link>
        <p className="error-message">{description}</p>
      </>
    );
  }

  if (!data || !party) return <p>Consultando a crônica...</p>;

  const finished = data.status !== "IN_PROGRESS";
  // "Re-executar IA" only re-triggers the literary adaptation (data.status here reflects the
  // adaptation pipeline specifically). The canon map and synopsis run independently and have
  // no manual regeneration action in this console.
  const canRegenerate = data.narrator && ["PUBLISHED", "FAILED"].includes(data.status);
  const isRegenerating = data.narrator && ["AI_PENDING", "AI_PROCESSING"].includes(data.status);

  const headerActions = (
    <>
      {data.narrator && data.status === "IN_PROGRESS" && <button className="button secondary" onClick={() => action("/game/skip")}>Pular turno atual</button>}
      {data.narrator && data.status === "IN_PROGRESS" && <button className="button secondary" onClick={reveal} disabled={revealed}>{revealed ? `Revelado por ${data.revealSeconds}s` : "Revelar todos os trechos"}</button>}
      {finished && <button className="button primary" onClick={() => setStoryModalOpen(true)}>Ver história adaptada</button>}
      {(canRegenerate || isRegenerating) && (
        <button className="button secondary" onClick={() => action("/regenerate")} disabled={isRegenerating}>
          {isRegenerating ? "Gerando..." : "Re-executar IA"}
        </button>
      )}
      {data.narrator && <button className="button ghost" onClick={() => api(`/parties/${partyId}/chronicles/${chronicleId}`, { method: "DELETE" }).then(() => { window.location.href = `/app/party/${partyId}`; })}>Arquivar</button>}
    </>
  );

  return (
    <>
      <Link className="back-link" href={`/app/party/${partyId}`}>← Voltar às crônicas</Link>

      {finished ? (
        <ChronicleCompletedHeader
          title={data.title}
          partyName={party.name}
          creatorName={data.creatorName}
          createdAt={data.createdAt}
          completedAt={data.completedAt}
          cycleCount={data.cycleCount}
          totalTurns={data.totalTurns}
          completedTurns={progress.completed}
          actions={headerActions}
        />
      ) : (
        <header className="page-header">
          <div>
            <p className="eyebrow">História-jogo · {data.cycleCount} ciclo(s)</p>
            <h1>{data.title}</h1>
            <GameProgressBar completed={progress.completed} total={progress.total} />
            <p>Turno {data.currentSequence} de {data.totalTurns} · {data.status.replaceAll("_", " ")}</p>
          </div>
          <div className="header-actions">{headerActions}</div>
        </header>
      )}

      {data.currentUserTurn && (
        <section className="turn-editor turn-composer card">
          <p className="eyebrow">É a sua vez</p>
          <h2>Continue a história</h2>
          {previousMessage && (
            <aside className="last-message">
              <span>A última mensagem foi:</span>
              <strong>{previousMessage.author}</strong>
              <p>{previousMessage.content}</p>
            </aside>
          )}
          <textarea rows={11} value={draft} onChange={(event) => setDraft(event.target.value)} maxLength={10000} placeholder="Escreva apenas o que acontece a seguir..." />
          <div className="editor-actions">
            <button className="button secondary" onClick={() => action("/game/draft", { content: draft })}>Salvar</button>
            <button className="button primary" onClick={() => action("/game/publish", { content: draft })} disabled={!draft.trim()}>Publicar e passar</button>
            <button className="button ghost" onClick={() => action("/game/skip")}>Pular</button>
            <button className="button ghost" onClick={() => { setDraft(""); void action("/game/draft", undefined, "DELETE"); }}>Limpar</button>
          </div>
        </section>
      )}

      {finished && <CanonMapPanel partyId={partyId} chronicleId={chronicleId} finished={finished} />}

      <section className="thread" aria-labelledby="thread-heading">
        <div className="section-heading">
          <h2 id="thread-heading">Thread original</h2>
          <span>O autor de cada etapa permanece visível.</span>
        </div>
        <ol className="thread-list">
          {data.turns.map((turn) => (
            <li key={turn.id}>
              <ThreadSegmentRow
                turn={turn}
                segment={segments.get(turn.sequenceNumber)}
                narrator={data.narrator}
                onEdit={edit}
                onDisable={disable}
                onRestore={(segment) => action(`/segments/${segment.id}/restore`)}
              />
            </li>
          ))}
        </ol>
        {finished && <p className="thread-end-note">Fim da thread original.</p>}
      </section>

      {message && <p className="error-message action-message">{message}</p>}

      <StoryModal
        open={storyModalOpen}
        onClose={() => setStoryModalOpen(false)}
        partyId={partyId}
        chronicleId={chronicleId}
        status={data.status}
        currentStory={data.generatedStory}
        canRegenerate={canRegenerate}
        isRegenerating={isRegenerating}
        onRegenerate={() => action("/regenerate")}
      />
    </>
  );
}

function WrittenView({ partyId, chronicleId }: { partyId: string; chronicleId: string }) {
  const [data, setData] = useState<WrittenDetail>();
  const [party, setParty] = useState<PartyDetail>();
  const [content, setContent] = useState("");
  const [lockToken, setLockToken] = useState("");
  const [editorIds, setEditorIds] = useState<string[]>([]);
  const [message, setMessage] = useState("");
  const narrator = party?.currentUserRole === "OWNER" || party?.currentUserRole === "NARRATOR";

  const load = useCallback(async () => {
    const [detail, partyDetail] = await Promise.all([
      api<WrittenDetail>(`/parties/${partyId}/chronicles/${chronicleId}/written`),
      api<PartyDetail>(`/parties/${partyId}`),
    ]);
    setData(detail);
    setParty(partyDetail);
    setContent(detail.content);
    setEditorIds(detail.editorIds);
  }, [partyId, chronicleId]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  async function lock() {
    const result = await api<{ acquired: boolean; lockToken?: string; lockedBy?: string }>(`/parties/${partyId}/chronicles/${chronicleId}/written/lock`, { method: "POST" });
    if (result.acquired && result.lockToken) setLockToken(result.lockToken);
    else setMessage(`Bloqueado por ${result.lockedBy}.`);
    await load();
  }

  async function save() {
    if (!data) return;
    const result = await api<{ contentVersion: number }>(`/parties/${partyId}/chronicles/${chronicleId}/written/save`, {
      method: "POST",
      headers: { "X-Lock-Token": lockToken },
      body: JSON.stringify({ content, expectedVersion: data.contentVersion }),
    });
    setData({ ...data, content, contentVersion: result.contentVersion });
    setMessage("Salvo.");
  }

  async function updateEditors() {
    await api(`/parties/${partyId}/chronicles/${chronicleId}/written/editors`, {
      method: "PUT",
      body: JSON.stringify({ editorIds }),
    });
    setMessage("Permissões atualizadas.");
    await load();
  }

  if (!data) return <p>Consultando a crônica...</p>;

  return (
    <>
      <Link className="back-link" href={`/app/party/${partyId}`}>← Voltar às crônicas</Link>
      <header className="page-header">
        <div>
          <p className="eyebrow">História escrita</p>
          <h1>{data.title}</h1>
          <p>{data.status}</p>
        </div>
        {narrator && (
          <div className="header-actions">
            <button className="button primary" onClick={() => api(`/parties/${partyId}/chronicles/${chronicleId}/written/publish`, { method: "POST" }).then(load)}>Publicar</button>
            <button className="button ghost" onClick={() => api(`/parties/${partyId}/chronicles/${chronicleId}`, { method: "DELETE" }).then(() => { window.location.href = `/app/party/${partyId}`; })}>Arquivar</button>
          </div>
        )}
      </header>

      {narrator && party && (
        <details className="card editors-panel">
          <summary>Permissões de edição</summary>
          <p>O narrador pode conceder ou revogar o acesso sem compartilhar a edição em tempo real.</p>
          <div className="editor-permissions">
            {party.members.filter((member) => member.status === "ACTIVE").map((member) => (
              <label className="checkbox" key={member.userId}>
                <input type="checkbox" checked={editorIds.includes(member.userId)} onChange={(event) => setEditorIds(event.target.checked ? [...editorIds, member.userId] : editorIds.filter((id) => id !== member.userId))} />
                {member.displayName} <small>@{member.username}</small>
              </label>
            ))}
          </div>
          <button className="button secondary" onClick={updateEditors}>Atualizar permissões</button>
        </details>
      )}

      <section className="written-editor card">
        {data.lockedBy && !lockToken && <div className="lock-banner">Bloqueado para edição por <strong>{data.lockedBy}</strong>.</div>}
        <textarea rows={24} value={content} onChange={(event) => setContent(event.target.value)} readOnly={!lockToken} />
        {data.canEdit && (
          <div className="editor-actions">
            {!lockToken
              ? <button className="button primary" onClick={lock}>Começar a editar</button>
              : (
                <>
                  <button className="button primary" onClick={save}>Salvar</button>
                  <button className="button secondary" onClick={() => api(`/parties/${partyId}/chronicles/${chronicleId}/written/release-lock`, { method: "POST", headers: { "X-Lock-Token": lockToken } }).then(() => { setLockToken(""); return load(); })}>Liberar edição</button>
                </>
              )}
          </div>
        )}
        {message && <p>{message}</p>}
      </section>
    </>
  );
}
