"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import type { ChronicleCard, PartyDetail, PartyMember, PartyRole } from "@/lib/types";
import { AppShell } from "@/components/AppShell";

interface InviteResponse {
  id: string;
  inviteUrl: string;
  whatsappUrl: string;
  expiresAt: string;
  emailSent: boolean;
}

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
  const [invite, setInvite] = useState<InviteResponse | undefined>(undefined);
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const narrator = party?.currentUserRole === "OWNER" || party?.currentUserRole === "NARRATOR";
  const owner = party?.currentUserRole === "OWNER";

  const load = useCallback(async () => {
    const [partyDetail, chronicleCards] = await Promise.all([
      api<PartyDetail>(`/parties/${partyId}`),
      api<ChronicleCard[]>(`/parties/${partyId}/chronicles`),
    ]);
    setParty(partyDetail);
    setChronicles(chronicleCards);
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

  async function createInvite(channel: "LINK" | "EMAIL" | "WHATSAPP") {
    const result = await api<InviteResponse>(`/parties/${partyId}/invites`, {
      method: "POST",
      body: JSON.stringify({ channel, recipientContact: email || null }),
    });
    setInvite(result);
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
      <Link className="back-link" href="/app">← Todas as parties</Link>
      <header className="page-header archive-header">
        <div>
          <p className="eyebrow">Arquivo do Cronista</p>
          <h1>{party?.name ?? "Carregando..."}</h1>
          <p>Memórias, relatos e verdades registradas pela party.</p>
        </div>
        <div className="header-actions">
          <button className="button primary" onClick={() => setShowCreate(!showCreate)}>✎ Registrar crônica</button>
          {narrator && <button className="button secondary" onClick={() => createInvite("LINK")}>Convidar jogador</button>}
        </div>
      </header>

      {invite && (
        <section className="notice card">
          <strong>Convite criado</strong>
          <input readOnly value={invite.inviteUrl} onFocus={(event) => event.currentTarget.select()} />
          <div>
            <a className="button secondary" href={invite.whatsappUrl} target="_blank" rel="noreferrer">Abrir no WhatsApp</a>
            <button className="button ghost" onClick={() => navigator.clipboard.writeText(invite.inviteUrl)}>Copiar link</button>
            <button className="button ghost" onClick={() => api(`/parties/${partyId}/invites/${invite.id}`, { method: "DELETE" }).then(() => setInvite(undefined))}>Revogar convite</button>
          </div>
          <small>Expira em {new Date(invite.expiresAt).toLocaleString("pt-BR")}</small>
        </section>
      )}

      {narrator && (
        <details className="email-invite card">
          <summary>Enviar convite por email</summary>
          <div className="inline-form">
            <input type="email" placeholder="jogador@exemplo.com" value={email} onChange={(event) => setEmail(event.target.value)} />
            <button className="button secondary" onClick={() => createInvite("EMAIL")}>Criar e enviar</button>
          </div>
          <small>Se o Resend não estiver configurado, o link ainda será criado para cópia manual.</small>
        </details>
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
        {chronicles.map((item, index) => (
          <Link
            href={`/app/party/${partyId}/chronicle/${item.id}?type=${item.type}`}
            key={item.id}
            className={`chronicle-card card ${index === 0 ? "featured" : ""} ${item.status !== "PUBLISHED" ? "building" : ""}`}
          >
            <div className={`chronicle-art art-${index % 4}`}><span>{item.status === "PUBLISHED" ? "✦" : "◌"}</span></div>
            <div className="chronicle-copy">
              <div className="card-heading">
                <h2>{item.title}</h2>
                <span className={`status-pill status-${item.status.toLowerCase()}`}>{item.status.replaceAll("_", " ")}</span>
              </div>
              <p>{item.preview || (item.status === "IN_PROGRESS" ? "Uma história ainda está sendo construída. Os trechos permanecem velados." : "Este registro ainda não possui uma versão publicada.")}</p>
              <small>Criado por {item.creatorName}</small>
            </div>
          </Link>
        ))}
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
