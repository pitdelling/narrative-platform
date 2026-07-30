"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { api } from "@/lib/api";
import type { GameDetail, PartyDetail, Segment, WrittenDetail } from "@/lib/types";
import { AppShell } from "@/components/AppShell";
import { DragonPanel } from "@/components/DragonPanel";

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
  const [draft, setDraft] = useState("");
  const [revealed, setRevealed] = useState(false);
  const [message, setMessage] = useState("");

  const load = useCallback(async (reveal = false) => {
    const detail = await api<GameDetail>(`/parties/${partyId}/chronicles/${chronicleId}/game?reveal=${reveal}`);
    setData(detail);
    setDraft(detail.currentDraft ?? "");
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

  if (!data) return <p>Consultando a crônica...</p>;

  return (
    <>
      <Link className="back-link" href={`/app/party/${partyId}`}>← Voltar às crônicas</Link>
      <header className="page-header">
        <div>
          <p className="eyebrow">História-jogo · {data.cycleCount} ciclo(s)</p>
          <h1>{data.title}</h1>
          <p>Turno {data.currentSequence} de {data.totalTurns} · {data.status.replaceAll("_", " ")}</p>
        </div>
        <div className="header-actions">
          {data.narrator && data.status === "IN_PROGRESS" && <button className="button secondary" onClick={() => action("/game/skip")}>Pular turno atual</button>}
          {data.narrator && data.status === "IN_PROGRESS" && <button className="button secondary" onClick={reveal} disabled={revealed}>{revealed ? `Revelado por ${data.revealSeconds}s` : "Revelar todos os trechos"}</button>}
          {data.narrator && ["PUBLISHED", "FAILED"].includes(data.status) && <button className="button primary" onClick={() => action("/regenerate")}>Regenerar história</button>}
          {data.narrator && <button className="button ghost" onClick={() => api(`/parties/${partyId}/chronicles/${chronicleId}`, { method: "DELETE" }).then(() => { window.location.href = `/app/party/${partyId}`; })}>Arquivar</button>}
        </div>
      </header>

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

      {data.generatedStory && (
        <article className="generated-story card">
          <div className="story-prose-frame">
            <div className="story-prose-column">
              <p className="eyebrow">Versão do Cronista · v{data.generatedStory.version}</p>
              <h2>{data.generatedStory.title}</h2>
              <div className="story-prose">{data.generatedStory.content}</div>
            </div>
            <DragonPanel />
          </div>
        </article>
      )}

      {["AI_PENDING", "AI_PROCESSING"].includes(data.status) && (
        <div className="notice card">
          <strong>O Cronista está organizando os fragmentos.</strong>
          <p>Sem uma chave de IA configurada, a thread permanece concluída e o trabalho aguarda processamento.</p>
        </div>
      )}

      <section className="thread">
        <div className="section-heading">
          <h2>Thread original</h2>
          <span>O autor de cada etapa permanece visível.</span>
        </div>
        {data.turns.map((turn) => {
          const segment = segments.get(turn.sequenceNumber);
          const classNames = [
            "thread-item",
            segment?.status === "DISABLED" ? "disabled" : "",
            turn.status === "SKIPPED" ? "skipped" : "",
            turn.status === "EXPIRED" ? "expired" : "",
          ].filter(Boolean).join(" ");
          return (
            <article key={turn.id} className={classNames}>
              <div className="thread-marker">{turn.sequenceNumber}</div>
              <div className="thread-body card">
                <div className="thread-meta">
                  <strong>{turn.author}</strong>
                  <span>Ciclo {turn.cycleNumber} · {turn.status}</span>
                </div>
                {segment
                  ? segment.visible
                    ? (
                      <>
                        <p className="segment-content">{segment.status === "DISABLED" ? "Removido pelo Narrador" : segment.content}</p>
                        {segment.status === "DISABLED" && segment.disabledReason && <p className="removal-reason">Motivo: {segment.disabledReason}</p>}
                        {data.narrator && (
                          <div className="thread-actions">
                            <button onClick={() => edit(segment)}>Editar</button>
                            {segment.status === "DISABLED"
                              ? <button onClick={() => action(`/segments/${segment.id}/restore`)}>Restaurar</button>
                              : <button onClick={() => disable(segment)}>Desabilitar</button>}
                          </div>
                        )}
                      </>
                    )
                    : <HiddenBlock size={segment.size} />
                  : <PendingBlock status={turn.status} />}
              </div>
            </article>
          );
        })}
      </section>

      {message && <p className="error-message action-message">{message}</p>}
    </>
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
