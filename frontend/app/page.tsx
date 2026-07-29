"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { setToken } from "@/lib/auth";
import type { AuthResponse } from "@/lib/types";
import { BrandMark } from "@/components/BrandMark";

export default function HomePage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(""); setLoading(true);
    try {
      const data = mode === "login"
        ? await api<AuthResponse>("/auth/login", { method: "POST", body: JSON.stringify({ username, password }) })
        : await api<AuthResponse>("/auth/register-narrator", { method: "POST", body: JSON.stringify({ username, displayName, password }) });
      setToken(data.token);
      router.push("/app");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Não foi possível entrar.");
    } finally { setLoading(false); }
  }

  return (
    <main className="auth-page">
      <section className="auth-story">
        <BrandMark />
        <h1>Seu segundo arquivo para mundos que ainda estão sendo escritos.</h1>
        <p>Organize parties, crie crônicas colaborativas e preserve cada escolha que passou a fazer parte da campanha.</p>
        <div className="celestial-divider">✦</div>
      </section>
      <section className="auth-panel card">
        <p className="eyebrow">Arquivo do Cronista</p>
        <h2>{mode === "login" ? "Entrar" : "Crie sua conta"}</h2>
        <form onSubmit={submit}>
          <label>Usuário<input value={username} onChange={(e) => setUsername(e.target.value)} required /></label>
          {mode === "register" && <label>Nome de exibição<input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required /></label>}
          <label>Senha<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} required /></label>
          {error && <p className="error-message">{error}</p>}
          <button className="button primary" disabled={loading}>{loading ? "Aguarde..." : mode === "login" ? "Entrar" : "Criar conta"}</button>
        </form>
        <button className="text-button" onClick={() => setMode(mode === "login" ? "register" : "login")}>
          {mode === "login" ? "Quero criar uma conta" : "Já tenho uma conta"}
        </button>
        <small>Contas de jogador são criadas apenas por um convite individual.</small>
      </section>
    </main>
  );
}
