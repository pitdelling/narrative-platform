"use client";

import { useState } from "react";
import { ThemeToggle } from "@/components/ThemeToggle";
import { WhatsAppIcon } from "@/components/WhatsAppIcon";
import { DragonPanel } from "@/components/DragonPanel";

const FAKE_PARTY_NAME = "Os Cinzentos do Vale";
const FAKE_INVITE_URL = "https://narrative-platform.example.com/invite/9f3a7c21-preview-token-for-visual-check";
const FAKE_STORY = `A neblina cobria o vale antes do amanhecer, e foi ali, entre as pedras cobertas de musgo, que os Cinzentos encontraram o primeiro sinal de que algo havia mudado no reino. O bardo, ainda com a poeira da estrada nas botas, insistiu em registrar cada detalhe: o brilho fraco vindo da torre abandonada, o silêncio estranho dos corvos, o cheiro de ferro queimado que vinha do rio.

Foi Elenor quem primeiro quebrou o silêncio, apontando para as marcas na terra — pegadas grandes demais para serem de homem, pequenas demais para serem de gigante. Ninguém dormiu bem naquela noite, e quando o sol finalmente rompeu por entre as nuvens baixas, a party já estava de pé, armas em punho, prontos para descer ao vale e descobrir a verdade por trás da neblina.`;

function buildInviteMessage(inviteUrl: string) {
  const appName = process.env.NEXT_PUBLIC_APP_NAME ?? "Narrative Platform";
  return `Por ordem de um Narrador, o bardo do reino convida você para a party "${FAKE_PARTY_NAME}" no reino de "${appName}". Para participar, basta apresentar o seu convite: ${inviteUrl}!`;
}

function InviteMembersPanel({ inviteUrl }: { inviteUrl: string }) {
  const [copyFeedback, setCopyFeedback] = useState(false);
  const [regenerating, setRegenerating] = useState(false);

  async function copyInviteMessage() {
    await navigator.clipboard.writeText(buildInviteMessage(inviteUrl));
    setCopyFeedback(true);
    window.setTimeout(() => setCopyFeedback(false), 2000);
  }

  function sendInviteWhatsApp() {
    window.open(`https://wa.me/?text=${encodeURIComponent(buildInviteMessage(inviteUrl))}`, "_blank", "noopener,noreferrer");
  }

  function fakeRegenerate() {
    setRegenerating(true);
    window.setTimeout(() => setRegenerating(false), 800);
  }

  return (
    <div className="invite-members-panel card">
      <div className="invite-members-row">
        <span className="invite-members-label">Convite para Membros:</span>
        <code className="invite-members-url" title={inviteUrl}>{inviteUrl}</code>
        <div className="invite-members-actions">
          <button type="button" className="invite-icon-button" title={regenerating ? "Gerando novo link..." : "Gerar novo link"} aria-label="Gerar novo link" onClick={fakeRegenerate} disabled={regenerating}>↻</button>
          <button type="button" className="invite-icon-button" title={copyFeedback ? "Copiado!" : "Copiar convite"} aria-label="Copiar convite" onClick={copyInviteMessage}>{copyFeedback ? "✓" : "⧉"}</button>
          <button type="button" className="invite-icon-button" title="Enviar convite pelo WhatsApp" aria-label="Enviar convite pelo WhatsApp" onClick={sendInviteWhatsApp}><WhatsAppIcon /></button>
        </div>
      </div>
    </div>
  );
}

export default function DevPreviewPage() {
  return (
    <main className="main-content">
      <p className="eyebrow">Preview isolado (sem backend) — apagar depois de validar</p>
      <h1 style={{ fontSize: "2rem" }}>Dev Preview</h1>
      <p style={{ color: "var(--muted)", marginBottom: "1.5rem" }}>
        Redimensione a janela do navegador para testar o truncamento do link e o ornamento decorativo.
      </p>

      <ThemeToggle />

      <h2 style={{ marginTop: "2.5rem" }}>1. Convite para Membros + Gerenciar membros (largura total)</h2>
      <InviteMembersPanel inviteUrl={FAKE_INVITE_URL} />
      <details className="members-panel card">
        <summary>Gerenciar membros</summary>
        <div className="membership-explanation">
          <p><strong>Desabilitar</strong> é temporário: bloqueia o acesso e permite reativação.</p>
          <p><strong>Remover</strong> é permanente para o vínculo atual: o histórico é preservado, mas a pessoa só volta por um novo convite.</p>
        </div>
        <div className="member-list">
          <div className="member-row member-active">
            <div>
              <strong>Elenor Vasth</strong>
              <small>@elenor · Narrador · Ativo</small>
            </div>
          </div>
          <div className="member-row member-active">
            <div>
              <strong>Bram Correia</strong>
              <small>@bram · Jogador · Ativo</small>
            </div>
          </div>
        </div>
      </details>

      <h2 style={{ marginTop: "2.5rem" }}>2. Mesmo componente, container estreito (força as reticências)</h2>
      <div style={{ maxWidth: "380px" }}>
        <InviteMembersPanel inviteUrl={FAKE_INVITE_URL} />
      </div>

      <h2 style={{ marginTop: "2.5rem" }}>3. Painel de dragões (alargue a janela para +1300px)</h2>
      <article className="generated-story card">
        <div className="story-prose-frame">
          <div className="story-prose-column">
            <p className="eyebrow">Versão do Cronista · v1</p>
            <h2>A Neblina do Vale</h2>
            <div className="story-prose">{FAKE_STORY}</div>
          </div>
          <DragonPanel />
        </div>
      </article>

      <h2 style={{ marginTop: "2.5rem" }}>4. Painel de dragões espremido (testa a sangria de 250px)</h2>
      <p style={{ color: "var(--muted)" }}>
        Container forçado para 130px de largura — o dragão deve continuar existindo (parcialmente
        oculto pelo recorte), sem desaparecer e sem gerar scroll horizontal.
      </p>
      <div className="card" style={{ width: "130px", height: "340px", padding: "0", display: "flex" }}>
        <DragonPanel />
      </div>
    </main>
  );
}
