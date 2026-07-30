"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import type { ChronicleCard, PartyDetail, PartyInvitationLink, PartyMember, PartyRole } from "@/lib/types";
import { AppShell } from "@/components/AppShell";
import { GameProgressBar } from "@/components/chronicle/GameProgressBar";
import { WhatsAppIcon } from "@/components/WhatsAppIcon";

const roleLabels: Record<PartyRole, string> = {
  OWNER: "Proprietário",
  NARRATOR: "Narrador",
  PLAYER: "Jogador",
};

const statusLabels: Record<PartyMember["status"], string> = {
  ACTIVE: "Ativo",
  DISABLED: "Desabilitado temporariamente",
  REMOVED: "Removido",
};

function compareChronicles(a: ChronicleCard, b: ChronicleCard): number {
  if (a.published !== b.published) return a.published ? 1 : -1;
  if (!a.published) {
    const awaitingDiff = Number(b.awaitingCurrentUser ?? false) - Number(a.awaitingCurrentUser ?? false);
    if (awaitingDiff !== 0) return awaitingDiff;
    return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
  }
  return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
}

export default function PartyArchivePage() {
  const { partyId } = useParams<{ partyId: string }>();
  const router = useRouter();
  const [party, setParty] = useState<PartyDetail>();
  const [chronicles, setChronicles] = useState<ChronicleCard[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [kind, setKind] = useState<"GAME" | "WRITTEN">("GAME");
  const [title, setTitle] = useState("");
  const [initialContent, setInitialContent] = useState("");
  const [cycles, setCycles] = useState(1);
  const [editors, setEditors] = useState<string[]>([]);
  const [invitation, setInvitation] = useState<PartyInvitationLink>();
  const [membersCopyFeedback, setMembersCopyFeedback] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [inviteError, setInviteError] = useState("");
  const [message, setMessage] = useState("");
  const narrator = party?.currentUserRole === "OWNER" || party?.currentUserRole === "NARRATOR";
  const owner = party?.currentUserRole === "OWNER";
  const sortedChronicles = useMemo(() => [...chronicles].sort(compareChronicles), [chronicles]);

  const load = useCallback(async () => {
    const [partyDetail, chronicleCards] = await Promise.all([
      api<PartyDetail>(`/parties/${partyId}`),
      api<ChronicleCard[]>(`/parties/${partyId}/chronicles`),
    ]);
    setParty(partyDetail);
    setChronicles(chronicleCards);
    if (partyDetail.currentUserRole === "OWNER" || partyDetail.currentUserRole === "NARRATOR") {
      setInvitation(await api<PartyInvitationLink>(`/parties/${partyId}/invitation`));
    }
  }, [partyId]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    try {
      const body = kind === "GAME"
        ? { title, cycleCount: cycles, initialContent }
        : { title, editorIds: editors };
      const path = kind === "GAME" ? "game" : "written";
      const result = await api<{ id: string }>(`/parties/${partyId}/chronicles/${path}`, {
        method: "POST",
        body: JSON.stringify(body),
      });
      setTitle("");
      setInitialContent("");
      setShowCreate(false);
      router.push(`/app/party/${partyId}/chronicle/${result.id}?type=${kind}`);
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : "Não foi possível criar a crônica.");
    }
  }

  function buildInviteMessage() {
    if (!party || !invitation) return "";
    const appName = process.env.NEXT_PUBLIC_APP_NAME ?? "Narrative Platform";
    return `Por ordem de um Narrador, o bardo do reino convida você para a party "${party.name}" no reino de "${appName}". Para participar, basta apresentar o seu convite: ${invitation.inviteUrl}!`;
  }

  async function copyInviteMessage() {
    const inviteMessage = buildInviteMessage();
    if (!inviteMessage) return;
    await navigator.clipboard.writeText(inviteMessage);
    setMembersCopyFeedback(true);
    window.setTimeout(() => setMembersCopyFeedback(false), 2000);
  }

  function sendInviteWhatsApp() {
    const inviteMessage = buildInviteMessage();
    if (!inviteMessage) return;
    window.open(`https://wa.me/?text=${encodeURIComponent(inviteMessage)}`, "_blank", "noopener,noreferrer");
  }

  async function regenerateLink() {
    if (!window.confirm("Gerar um novo link invalida o link atual imediatamente. Continuar?")) return;
    setInviteError("");
    setRegenerating(true);
    try {
      setInvitation(await api<PartyInvitationLink>(`/parties/${partyId}/invitation/regenerate`, { method: "POST" }));
    } catch (cause) {
      setInviteError(cause instanceof Error ? cause.message : "Não foi possível gerar um novo link.");
    } finally {
      setRegenerating(false);
    }
  }

  async function updateRole(member: PartyMember, role: Exclude<PartyRole, "OWNER">) {
    await api(`/parties/${partyId}/members/${member.userId}/role`, {
      method: "PUT",
      body: JSON.stringify({ role }),
    });
    await load();
  }

  async function removeMember(member: PartyMember) {
    const confirmed = window.confirm(
      `Remover ${member.displayName} permanentemente desta party? Para voltar, a pessoa precisará aceitar um novo convite.`,
    );
    if (!confirmed) return;
    await api(`/parties/${partyId}/members/${member.userId}`, { method: "DELETE" });
    await load();
  }

  return (
    <AppShell partyId={partyId}>
      <Link className="back-link" href="/app">← Grupos</Link>
      <header className="page-header archive-header">
        <div>
          <p className="eyebrow">Histórias</p>
          <h1>{party?.name ?? "Carregando..."}</h1>
          <p>Memórias, relatos e verdades registradas pela party.</p>
        </div>
        <div className="header-top">
          <div className="header-actions">
            <button className="button primary" onClick={() => setShowCreate(!showCreate)}>✎ Registrar crônica</button>
          </div>
        </div>
      </header>

      {narrator && invitation && party && (
        <div className="invite-members-panel card">
          <div className="invite-members-row">
            <span className="invite-members-label">Convite para Membros:</span>
            <code className="invite-members-url" title={invitation.inviteUrl}>{invitation.inviteUrl}</code>
            <div className="invite-members-actions">
              <button
                type="button"
                className="invite-icon-button"
                title={regenerating ? "Gerando novo link..." : "Gerar novo link"}
                aria-label="Gerar novo link"
                onClick={regenerateLink}
                disabled={regenerating}
              >
                ↻
              </button>
              <button
                type="button"
                className="invite-icon-button"
                title={membersCopyFeedback ? "Copiado!" : "Copiar convite"}
                aria-label="Copiar convite"
                onClick={copyInviteMessage}
              >
                {membersCopyFeedback ? "✓" : "⧉"}
              </button>
              <button
                type="button"
                className="invite-icon-button"
                title="Enviar convite pelo WhatsApp"
                aria-label="Enviar convite pelo WhatsApp"
                onClick={sendInviteWhatsApp}
              >
                <WhatsAppIcon />
              </button>
            </div>
          </div>
          {inviteError && <small className="invite-members-error">{inviteError}</small>}
        </div>
      )}

      {narrator && party && (
        <details className="members-panel card">
          <summary>Gerenciar membros</summary>
          <div className="membership-explanation">
            <p><strong>Desabilitar</strong> é temporário: bloqueia o acesso e permite reativação.</p>
            <p><strong>Remover</strong> é permanente para o vínculo atual: o histórico é preservado, mas a pessoa só volta por um novo convite.</p>
          </div>
          <div className="member-list">
            {party.members.map((member) => (
              <div className={`member-row member-${member.status.toLowerCase()}`} key={member.userId}>
                <div>
                  <strong>{member.displayName}</strong>
                  <small>@{member.username} · {roleLabels[member.role]} · {statusLabels[member.status]}</small>
                </div>
                {member.role !== "OWNER" && (
                  <div className="member-actions">
                    {member.status === "ACTIVE" && (
                      <button className="button ghost" onClick={() => api(`/parties/${partyId}/members/${member.userId}/disable`, { method: "POST" }).then(load)}>Desabilitar</button>
                    )}
                    {member.status === "DISABLED" && (
                      <button className="button secondary" onClick={() => api(`/parties/${partyId}/members/${member.userId}/reactivate`, { method: "POST" }).then(load)}>Reativar</button>
                    )}
                    {owner && member.status === "ACTIVE" && member.role === "PLAYER" && (
                      <button className="button secondary" onClick={() => updateRole(member, "NARRATOR")}>Tornar narrador</button>
                    )}
                    {owner && member.status === "ACTIVE" && member.role === "NARRATOR" && (
                      <button className="button secondary" onClick={() => updateRole(member, "PLAYER")}>Tornar jogador</button>
                    )}
                    {owner && member.status === "ACTIVE" && (
                      <button className="button secondary" onClick={() => {
                        if (window.confirm(`Transferir a propriedade da party para ${member.displayName}? Você passará a ser jogador.`)) {
                          void api(`/parties/${partyId}/transfer`, {
                            method: "POST",
                            body: JSON.stringify({ newOwnerId: member.userId }),
                          }).then(load);
                        }
                      }}>Transferir propriedade</button>
                    )}
                    <button className="button danger-outline" onClick={() => removeMember(member)}>Remover</button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </details>
      )}

      {showCreate && (
        <form className="create-chronicle card" onSubmit={create}>
          <div className="segmented">
            <button type="button" className={kind === "GAME" ? "active" : ""} onClick={() => setKind("GAME")}>História-jogo</button>
            {narrator && <button type="button" className={kind === "WRITTEN" ? "active" : ""} onClick={() => setKind("WRITTEN")}>História escrita</button>}
          </div>
          <label>Título<input value={title} onChange={(event) => setTitle(event.target.value)} required /></label>
          {kind === "GAME" ? (
            <>
              <label>Ciclos
                <select value={cycles} onChange={(event) => setCycles(Number(event.target.value))}>
                  <option value={1}>1 ciclo</option>
                  <option value={2}>2 ciclos</option>
                  <option value={3}>3 ciclos</option>
                </select>
              </label>
              <label>Início da história
                <textarea
                  rows={9}
                  maxLength={10000}
                  placeholder="Escreva o primeiro fragmento. Ao criar, a história já será iniciada e passará para a próxima pessoa."
                  value={initialContent}
                  onChange={(event) => setInitialContent(event.target.value)}
                  required
                />
              </label>
            </>
          ) : (
            <fieldset>
              <legend>Quem pode editar?</legend>
              {party?.members.filter((member) => member.status === "ACTIVE").map((member) => (
                <label className="checkbox" key={member.userId}>
                  <input
                    type="checkbox"
                    checked={editors.includes(member.userId)}
                    onChange={(event) => setEditors(event.target.checked ? [...editors, member.userId] : editors.filter((id) => id !== member.userId))}
                  />
                  {member.displayName}
                </label>
              ))}
            </fieldset>
          )}
          {message && <p className="error-message">{message}</p>}
          <button className="button primary">Criar e abrir</button>
        </form>
      )}

      <section className="chronicle-grid">
        {sortedChronicles.map((item, index) => {
          const hasProgress = item.type === "GAME" && item.status === "IN_PROGRESS" && typeof item.totalTurns === "number";
          return (
          <Link
            href={`/app/party/${partyId}/chronicle/${item.id}?type=${item.type}`}
            key={item.id}
            className={`chronicle-card card ${index === 0 ? "featured" : ""} ${item.status !== "PUBLISHED" ? "building" : ""}`}
          >
            <div className={`chronicle-art art-${index % 4}`}><span>{item.status === "PUBLISHED" ? "✦" : "◌"}</span></div>
            <div className="chronicle-copy">
              {hasProgress && (
                <div className="card-progress-row">
                  <GameProgressBar completed={item.completedTurns ?? 0} total={item.totalTurns as number} />
                  <span className={`status-pill status-${item.status.toLowerCase()}`}>{item.status.replaceAll("_", " ")}</span>
                </div>
              )}
              <div className="card-heading">
                <h2>{item.title}</h2>
                {!hasProgress && <span className={`status-pill status-${item.status.toLowerCase()}`}>{item.status.replaceAll("_", " ")}</span>}
              </div>
              <p>{item.preview || (item.status === "IN_PROGRESS" ? "Uma história ainda está sendo construída. Os trechos permanecem velados." : "Este registro ainda não possui uma versão publicada.")}</p>
              <small>Criado por {item.creatorName}</small>
            </div>
          </Link>
          );
        })}
        {chronicles.length === 0 && (
          <div className="empty-state card">
            <span>✦</span>
            <h2>Ainda não há registros neste arquivo.</h2>
            <p>Comece escrevendo a primeira crônica.</p>
          </div>
        )}
      </section>
    </AppShell>
  );
}
