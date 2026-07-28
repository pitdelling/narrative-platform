"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import type { PartySummary } from "@/lib/types";
import { AppShell } from "@/components/AppShell";

export default function DashboardPage() {
  const router = useRouter();
  const [parties, setParties] = useState<PartySummary[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try { setParties(await api<PartySummary[]>("/parties")); }
    catch { router.push("/"); }
  }, [router]);
  useEffect(() => { const timeoutId = window.setTimeout(() => { void load(); }, 0); return () => window.clearTimeout(timeoutId); }, [load]);

  async function create(event: FormEvent) {
    event.preventDefault(); setError("");
    try {
      const party = await api<PartySummary>("/parties", { method: "POST", body: JSON.stringify({ name, description }) });
      router.push(`/app/party/${party.id}`);
    } catch (e) { setError(e instanceof Error ? e.message : "Falha ao criar party."); }
  }

  return <AppShell>
    <header className="page-header"><div><p className="eyebrow">Mundos em construção</p><h1>Suas parties</h1><p>Uma conta, diferentes papéis e histórias.</p></div></header>
    <section className="party-grid">
      {parties.map((party) => <Link key={party.id} href={`/app/party/${party.id}`} className="party-card card"><span className="party-sigil">✦</span><h2>{party.name}</h2><p>{party.description || "Nenhuma descrição registrada."}</p><small>{party.role}</small></Link>)}
      <form className="party-card card create-card" onSubmit={create}><h2>Criar nova party</h2><label>Nome<input value={name} onChange={(e) => setName(e.target.value)} required /></label><label>Descrição<textarea value={description} onChange={(e) => setDescription(e.target.value)} /></label>{error && <p className="error-message">{error}</p>}<button className="button primary">Criar party</button></form>
    </section>
  </AppShell>;
}
