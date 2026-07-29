"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { getToken, setToken } from "@/lib/auth";
import type { AuthResponse } from "@/lib/types";
import { BrandMark } from "@/components/BrandMark";

interface Preview { partyId: string; partyName: string; invitedBy: string; }

export default function InvitePage() {
  const { token } = useParams<{ token: string }>();
  const router = useRouter();
  const [preview, setPreview] = useState<Preview>();
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [hasToken, setHasToken] = useState(false);
  const [mode, setMode] = useState<"register" | "login">("register");

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setHasToken(Boolean(getToken()));
      void api<Preview>(`/invites/${token}`).then(setPreview).catch((error: Error) => setMessage(error.message));
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [token]);

  async function acceptExisting() {
    try { await api<void>("/invites/accept", { method: "POST", body: JSON.stringify({ token }) }); router.push(`/app/party/${preview?.partyId}`); }
    catch (e) { setMessage(e instanceof Error ? e.message : "Falha ao aceitar."); }
  }

  async function submitAccount(event: FormEvent) {
    event.preventDefault();
    try {
      if (mode === "register") {
        const auth = await api<AuthResponse>("/auth/register-from-invite", { method: "POST", body: JSON.stringify({ token, username, displayName, password }) });
        setToken(auth.token);
        router.push(`/app/party/${preview?.partyId}`);
        return;
      }
      const auth = await api<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify({ username, password }) });
      setToken(auth.token);
      await api<void>("/invites/accept", { method: "POST", body: JSON.stringify({ token }) });
      router.push(`/app/party/${preview?.partyId}`);
    } catch (e) { setMessage(e instanceof Error ? e.message : "Falha ao entrar pelo convite."); }
  }

  return <main className="auth-page single">
    <section className="auth-panel card wide">
      <BrandMark />
      <p className="eyebrow">Convite individual</p>
      <h1>{preview ? `Você foi convidado para ${preview.partyName}` : "Consultando o convite..."}</h1>
      {preview && <p>{preview.invitedBy} abriu um lugar para você nesta party.</p>}
      {hasToken ? <button className="button primary" onClick={acceptExisting}>Aceitar com minha conta</button> : (
        <>
          <div className="segmented"><button type="button" className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>Criar conta</button><button type="button" className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>Já tenho conta</button></div>
          <form onSubmit={submitAccount}>
            <label>Usuário<input value={username} onChange={(e) => setUsername(e.target.value)} required /></label>
            {mode === "register" && <label>Nome de exibição<input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required /></label>}
            <label>{mode === "register" ? "Senha nova" : "Senha"}<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} required /></label>
            <button className="button primary">{mode === "register" ? "Criar conta e entrar" : "Entrar e aceitar convite"}</button>
          </form>
        </>
      )}
      {message && <p className="error-message">{message}</p>}
    </section>
  </main>;
}
