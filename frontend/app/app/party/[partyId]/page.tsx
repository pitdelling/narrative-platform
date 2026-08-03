"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, usePathname, useRouter, useSearchParams } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import type {
  ChronicleCard, DailyStoryVoteState, PartyDetail, PartyInvitationLink, PartyMember, PartyRole,
  SortMode, StoryVoteSummary,
} from "@/lib/types";
import { AppShell } from "@/components/AppShell";
import { AiTagSettingsPanel } from "@/components/chronicle/AiTagSettingsPanel";
import { GameProgressBar } from "@/components/chronicle/GameProgressBar";
import { RankBadge } from "@/components/chronicle/RankBadge";
import { StoryVoteControl } from "@/components/chronicle/StoryVoteControl";
import { WhatsAppIcon } from "@/components/WhatsAppIcon";
import { isContentEmpty, RichTextEditor } from "@/components/RichTextEditor";
import { dailyBudgetMessage, sortPublishedChronicles, splitRankedChronicles, submitVote } from "@/lib/storyVotes";

const roleLabels: Record<PartyRole, string> = {
  OWNER: "Proprietário",
  NARRATOR: "Narrador",
  PLAYER: "Jogador",
  SPECTATOR: "Espectador",
};

const statusLabels: Record<PartyMember["status"], string> = {
  ACTIVE: "Ativo",
  DISABLED: "Desabilitado temporariamente",
  REMOVED: "Removido",
};

function compareInProgressChronicles(a: ChronicleCard, b: ChronicleCard): number {
  const awaitingDiff = Number(b.awaitingCurrentUser ?? false) - Number(a.awaitingCurrentUser ?? false);
  if (awaitingDiff !== 0) return awaitingDiff;
  return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
}

export default function PartyArchivePage() {
  const { partyId } = useParams<{ partyId: string }>();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [party, setParty] = useState<PartyDetail>();
  const [chronicles, setChronicles] = useState<ChronicleCard[]>([]);
  const [voteSummary, setVoteSummary] = useState<StoryVoteSummary[]>([]);
  const [dailyVoteState, setDailyVoteState] = useState<DailyStoryVoteState>();
  const [votesError, setVotesError] = useState("");
  const [voteActionError, setVoteActionError] = useState("");
  const [pendingVoteChronicleId, setPendingVoteChronicleId] = useState<string>();
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
  const [spectatorInvitation, setSpectatorInvitation] = useState<PartyInvitationLink>();
  const [spectatorCopyFeedback, setSpectatorCopyFeedback] = useState(false);
  const [spectatorRegenerating, setSpectatorRegenerating] = useState(false);
  const [spectatorInviteError, setSpectatorInviteError] = useState("");
  const [message, setMessage] = useState("");
  const narrator = party?.currentUserRole === "OWNER" || party?.currentUserRole === "NARRATOR";
  const owner = party?.currentUserRole === "OWNER";

  const sortParam = searchParams.get("sort");
  const sortMode: SortMode = sortParam === "NEWEST" || sortParam === "OLDEST" ? sortParam : "RANK";

  const voteSummaryById = useMemo(() => new Map(voteSummary.map((entry) => [entry.chronicleId, entry])), [voteSummary]);

  const inProgressChronicles = useMemo(
    () => chronicles.filter((item) => !item.published).sort(compareInProgressChronicles),
    [chronicles]
  );

  // Only the Ranking mode splits published stories into a "podium" (rank 1-5) and a
  // "concluded" block with a divider between them — Newest/Oldest keep a single flat list,
  // exactly like before.
  const rankedSections = useMemo(() => {
    const published = chronicles.filter((item) => item.published);
    if (sortMode === "RANK") {
      return splitRankedChronicles(published, voteSummaryById);
    }
    return { podium: [] as ChronicleCard[], concluded: sortPublishedChronicles(published, voteSummaryById, sortMode) };
  }, [chronicles, voteSummaryById, sortMode]);

  function changeSortMode(next: SortMode) {
    const params = new URLSearchParams(searchParams.toString());
    if (next === "RANK") {
      params.delete("sort");
    } else {
      params.set("sort", next);
    }
    const query = params.toString();
    router.replace(query ? `${pathname}?${query}` : pathname);
  }

  const loadVotes = useCallback(async () => {
    try {
      const [summary, daily] = await Promise.all([
        api<StoryVoteSummary[]>(`/parties/${partyId}/story-votes/summary`),
        api<DailyStoryVoteState>(`/parties/${partyId}/story-votes/today`),
      ]);
      setVoteSummary(summary);
      setDailyVoteState(daily);
      setVotesError("");
    } catch (cause) {
      setVoteSummary([]);
      setDailyVoteState(undefined);
      setVotesError(cause instanceof Error ? cause.message : "Não foi possível carregar os votos.");
    }
  }, [partyId]);

  const load = useCallback(async () => {
    const [partyDetail, chronicleCards] = await Promise.all([
      api<PartyDetail>(`/parties/${partyId}`),
      api<ChronicleCard[]>(`/parties/${partyId}/chronicles`),
    ]);
    setParty(partyDetail);
    setChronicles(chronicleCards);
    if (partyDetail.currentUserRole === "OWNER" || partyDetail.currentUserRole === "NARRATOR") {
      setInvitation(await api<PartyInvitationLink>(`/parties/${partyId}/invitation`));
      setSpectatorInvitation(await api<PartyInvitationLink>(`/parties/${partyId}/invitation/spectator`));
    }
    await loadVotes();
  }, [partyId, loadVotes]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  async function handleVoteChange(chronicleId: string, storyTitle: string, nextUnits: number) {
    setVoteActionError("");
    setPendingVoteChronicleId(chronicleId);
    try {
      const updated = await submitVote(partyId, chronicleId, nextUnits);
      setDailyVoteState(updated);
      await loadVotes();
    } catch (cause) {
      if (cause instanceof ApiError && cause.code === "forbidden") {
        setVoteActionError("Você não pode votar nesta party.");
      } else if (cause instanceof ApiError && cause.code === "not_found") {
        setVoteActionError("Esta história não está mais disponível.");
      } else if (cause instanceof ApiError && cause.code === "story_vote_concurrency_conflict") {
        setVoteActionError(cause.message);
        await loadVotes();
      } else {
        setVoteActionError(cause instanceof Error ? cause.message : `Não foi possível atualizar seu voto em "${storyTitle}". Tente novamente.`);
      }
    } finally {
      setPendingVoteChronicleId(undefined);
    }
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    if (kind === "GAME" && isContentEmpty(initialContent)) {
      setMessage("Escreva o início da história.");
      return;
    }
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

  function buildSpectatorInviteMessage() {
    if (!party || !spectatorInvitation) return "";
    const appName = process.env.NEXT_PUBLIC_APP_NAME ?? "Narrative Platform";
    return `Por ordem solene de um Narrador, você foi nomeado(a) Observador(a) das Crônicas da party "${party.name}" no reino de "${appName}" — testemunha silenciosa das histórias, sem tomar parte na escrita. Aceite esta incumbência apresentando o seu selo: ${spectatorInvitation.inviteUrl}!`;
  }

  async function copySpectatorInviteMessage() {
    const inviteMessage = buildSpectatorInviteMessage();
    if (!inviteMessage) return;
    await navigator.clipboard.writeText(inviteMessage);
    setSpectatorCopyFeedback(true);
    window.setTimeout(() => setSpectatorCopyFeedback(false), 2000);
  }

  function sendSpectatorInviteWhatsApp() {
    const inviteMessage = buildSpectatorInviteMessage();
    if (!inviteMessage) return;
    window.open(`https://wa.me/?text=${encodeURIComponent(inviteMessage)}`, "_blank", "noopener,noreferrer");
  }

  async function regenerateSpectatorLink() {
    if (!window.confirm("Gerar um novo link invalida o link de espectador atual imediatamente. Continuar?")) return;
    setSpectatorInviteError("");
    setSpectatorRegenerating(true);
    try {
      setSpectatorInvitation(await api<PartyInvitationLink>(`/parties/${partyId}/invitation/spectator/regenerate`, { method: "POST" }));
    } catch (cause) {
      setSpectatorInviteError(cause instanceof Error ? cause.message : "Não foi possível gerar um novo link.");
    } finally {
      setSpectatorRegenerating(false);
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

  function renderChronicleCard(item: ChronicleCard, index: number) {
    const hasProgress = item.type === "GAME" && item.status === "IN_PROGRESS" && typeof item.totalTurns === "number";
    const summary = item.published ? voteSummaryById.get(item.id) : undefined;
    return (
      <Link
        href={`/app/party/${partyId}/chronicle/${item.id}?type=${item.type}`}
        key={item.id}
        className={`chronicle-card card ${index === 0 ? "featured" : ""} ${item.status !== "PUBLISHED" ? "building" : ""}`}
      >
        {summary && <RankBadge rank={summary.rank} />}
        {summary && (
          <StoryVoteControl
            storyTitle={item.title}
            unitsToday={summary.currentUserVotesToday}
            totalVotes={summary.totalVotes}
            remainingUnitsToday={dailyVoteState?.remainingUnits ?? 0}
            canVote={summary.canVote}
            disabled={pendingVoteChronicleId === item.id}
            onChange={(nextUnits) => void handleVoteChange(item.id, item.title, nextUnits)}
          />
        )}
        <div className="chronicle-card-inner">
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
        </div>
      </Link>
    );
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
            <button
              className="button primary registrar-cronica-button"
              onClick={() => setShowCreate(!showCreate)}
              title="Registrar crônica"
              aria-label="Registrar crônica"
            >
              <span aria-hidden="true">✎</span>
              <span className="registrar-cronica-label">Registrar crônica</span>
            </button>
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
          {spectatorInvitation && (
            <div className="invite-members-row">
              <span className="invite-members-label">Convite para Espectadores:</span>
              <code className="invite-members-url" title={spectatorInvitation.inviteUrl}>{spectatorInvitation.inviteUrl}</code>
              <div className="invite-members-actions">
                <button
                  type="button"
                  className="invite-icon-button"
                  title={spectatorRegenerating ? "Gerando novo link..." : "Gerar novo link de espectador"}
                  aria-label="Gerar novo link de espectador"
                  onClick={regenerateSpectatorLink}
                  disabled={spectatorRegenerating}
                >
                  ↻
                </button>
                <button
                  type="button"
                  className="invite-icon-button"
                  title={spectatorCopyFeedback ? "Copiado!" : "Copiar convite de espectador"}
                  aria-label="Copiar convite de espectador"
                  onClick={copySpectatorInviteMessage}
                >
                  {spectatorCopyFeedback ? "✓" : "⧉"}
                </button>
                <button
                  type="button"
                  className="invite-icon-button"
                  title="Enviar convite de espectador pelo WhatsApp"
                  aria-label="Enviar convite de espectador pelo WhatsApp"
                  onClick={sendSpectatorInviteWhatsApp}
                >
                  <WhatsAppIcon />
                </button>
              </div>
            </div>
          )}
          {spectatorInviteError && <small className="invite-members-error">{spectatorInviteError}</small>}
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
                    {owner && member.status === "ACTIVE" && (member.role === "NARRATOR" || member.role === "SPECTATOR") && (
                      <button className="button secondary" onClick={() => updateRole(member, "PLAYER")}>Tornar jogador</button>
                    )}
                    {owner && member.status === "ACTIVE" && (member.role === "PLAYER" || member.role === "NARRATOR") && (
                      <button className="button secondary" onClick={() => {
                        if (window.confirm(`Tornar ${member.displayName} espectador? A pessoa deixará de participar dos próximos ciclos das histórias em andamento, podendo apenas visualizá-las.`)) {
                          void updateRole(member, "SPECTATOR");
                        }
                      }}>Tornar espectador</button>
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

      {narrator && <AiTagSettingsPanel partyId={partyId} narrator={narrator} />}

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
              <div className="field">
                <span className="field-label">Início da história</span>
                <RichTextEditor
                  value={initialContent}
                  onChange={setInitialContent}
                  maxLength={10000}
                  placeholder="Escreva o primeiro fragmento. Ao criar, a história já será iniciada e passará para a próxima pessoa."
                  ariaLabel="Início da história"
                />
              </div>
            </>
          ) : (
            <fieldset>
              <legend>Quem pode editar?</legend>
              {party?.members.filter((member) => member.status === "ACTIVE" && member.role !== "SPECTATOR").map((member) => (
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

      <div className="story-votes-banner">
        {dailyVoteState && (
          <span className="story-votes-budget">
            <span className="story-votes-budget-icon" aria-hidden="true">✦</span>
            {dailyBudgetMessage(dailyVoteState)}
          </span>
        )}
        {votesError && <span className="error-message">Não foi possível carregar os votos.</span>}
        <label className="sort-select-label">
          Ordenar por
          <select className="sort-select" value={sortMode} onChange={(event) => changeSortMode(event.target.value as SortMode)}>
            <option value="RANK">Ranking</option>
            <option value="NEWEST">Mais recentes</option>
            <option value="OLDEST">Mais antigas</option>
          </select>
        </label>
      </div>
      {voteActionError && <p className="error-message">{voteActionError}</p>}

      {sortMode === "RANK" ? (
        <>
          <section className="chronicle-grid">
            {inProgressChronicles.map((item, index) => renderChronicleCard(item, index))}
            {chronicles.length === 0 && (
              <div className="empty-state card">
                <span>✦</span>
                <h2>Ainda não há registros neste arquivo.</h2>
                <p>Comece escrevendo a primeira crônica.</p>
              </div>
            )}
          </section>
          {rankedSections.podium.length > 0 && (
            <>
              <div className="celestial-divider full-width" aria-hidden="true">✦</div>
              <section className="chronicle-grid">
                {rankedSections.podium.map((item, index) => renderChronicleCard(item, index))}
              </section>
            </>
          )}
          {rankedSections.concluded.length > 0 && (
            <>
              <div className="celestial-divider full-width" aria-hidden="true">✦</div>
              <section className="chronicle-grid">
                {rankedSections.concluded.map((item, index) => renderChronicleCard(item, index))}
              </section>
            </>
          )}
        </>
      ) : (
        // No section dividers in this mode, so pending and concluded stories share a single
        // grid container — two separate <section> grids would have no gap between them (CSS
        // `gap` only spaces items within the same grid, not between sibling grids).
        <section className="chronicle-grid">
          {[...inProgressChronicles, ...rankedSections.concluded].map((item, index) => renderChronicleCard(item, index))}
          {chronicles.length === 0 && (
            <div className="empty-state card">
              <span>✦</span>
              <h2>Ainda não há registros neste arquivo.</h2>
              <p>Comece escrevendo a primeira crônica.</p>
            </div>
          )}
        </section>
      )}
    </AppShell>
  );
}
