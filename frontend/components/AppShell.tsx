"use client";

import type { FormEvent, ReactNode } from "react";
import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { api, NetworkError } from "@/lib/api";
import { clearToken } from "@/lib/auth";
import { BrandMark } from "@/components/BrandMark";
import { ThemeToggle } from "@/components/ThemeToggle";

interface CurrentUser {
  id: string;
  username: string;
  displayName: string;
}

export function AppShell({ children, partyId }: { children: ReactNode; partyId?: string }) {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [currentUser, setCurrentUser] = useState<CurrentUser>();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [accountMessage, setAccountMessage] = useState("");
  const [accountError, setAccountError] = useState("");
  const [partyName, setPartyName] = useState<string>();
  const [lastPartyId, setLastPartyId] = useState(partyId);

  const archiveHref = partyId ? `/app/party/${partyId}` : undefined;
  const archiveActive = archiveHref ? pathname.startsWith(archiveHref) : false;
  const groupsLabel = `Grupos${partyName ? ` - ${partyName}` : ""}`;

  if (partyId !== lastPartyId) {
    setLastPartyId(partyId);
    setPartyName(undefined);
  }

  useEffect(() => {
    let active = true;
    void api<CurrentUser>("/auth/me")
      .then((user) => {
        if (active) setCurrentUser(user);
      })
      .catch((cause) => {
        if (!active) return;
        if (cause instanceof NetworkError) return;
        clearToken();
        router.push("/");
      });
    return () => {
      active = false;
    };
  }, [router]);

  useEffect(() => {
    if (!partyId) return;
    let active = true;
    void api<{ name: string }>(`/parties/${partyId}`)
      .then((party) => {
        if (active) setPartyName(party.name);
      })
      .catch(() => {
        // Silently ignore: the group name is a nice-to-have in the nav, not worth
        // breaking the shell or logging the user out over.
      });
    return () => {
      active = false;
    };
  }, [partyId]);

  function closeMenu() {
    setMenuOpen(false);
  }

  function logout() {
    clearToken();
    router.push("/");
  }

  async function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAccountError("");
    setAccountMessage("");
    if (newPassword !== confirmPassword) {
      setAccountError("A confirmação não corresponde à nova senha.");
      return;
    }
    try {
      await api("/auth/change-password", {
        method: "POST",
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setAccountMessage("Senha alterada com sucesso.");
    } catch (cause) {
      setAccountError(cause instanceof Error ? cause.message : "Não foi possível trocar a senha.");
    }
  }

  return (
    <div className="app-shell">
      <div className="mobile-rail" aria-label="Atalhos de navegação">
        <button
          className="rail-button hamburger-button"
          type="button"
          aria-label="Abrir menu"
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen(true)}
        >
          <span />
          <span />
          <span />
        </button>
        <Link className={`rail-button ${pathname === "/app" ? "active" : ""}`} href="/app" title={groupsLabel}>
          ◈
        </Link>
        {archiveHref && (
          <Link className={`rail-button ${archiveActive ? "active" : ""}`} href={archiveHref} title="Histórias">
            ✦
          </Link>
        )}
        <button className="rail-button rail-account" type="button" title="Minha conta" onClick={() => setAccountOpen(true)}>
          @
        </button>
      </div>

      {menuOpen && <button className="mobile-menu-backdrop" type="button" aria-label="Fechar menu" onClick={closeMenu} />}

      <aside className={`sidebar ${menuOpen ? "mobile-open" : ""}`}>
        <div className="sidebar-mobile-heading">
          <BrandMark />
          <button className="sidebar-close" type="button" aria-label="Fechar menu" onClick={closeMenu}>×</button>
        </div>
        <div className="desktop-brand"><BrandMark /></div>
        <nav>
          <div className="nav-group">
            <Link className={pathname === "/app" ? "active" : ""} href="/app" onClick={closeMenu} title={groupsLabel}>
              ◈ <span className="nav-label-text">{groupsLabel}</span>
            </Link>
            {archiveHref && <Link className={`nav-subitem ${archiveActive ? "active" : ""}`} href={archiveHref} onClick={closeMenu}>✦ Histórias</Link>}
          </div>
          <span className="disabled-nav">◌ Personagens <em>em breve</em></span>
          <span className="disabled-nav">⌖ Mapas e Linhagens <em>em breve</em></span>
          <span className="disabled-nav">▤ Biblioteca <em>em breve</em></span>
        </nav>
        <blockquote>“Toda história muda quem a registra.”</blockquote>
        <div className="sidebar-account">
          <strong>{currentUser?.displayName ?? "Carregando conta..."}</strong>
          <span>@{currentUser?.username ?? "..."}</span>
          <button className="button ghost" type="button" onClick={() => { setAccountOpen(true); closeMenu(); }}>Conta e senha</button>
          <button className="button ghost" type="button" onClick={logout}>Sair</button>
        </div>
      </aside>

      <main className="main-content">{children}</main>

      {accountOpen && (
        <div className="modal-layer" role="presentation" onMouseDown={(event) => {
          if (event.currentTarget === event.target) setAccountOpen(false);
        }}>
          <section className="account-modal card" role="dialog" aria-modal="true" aria-labelledby="account-title">
            <div className="modal-heading">
              <div>
                <p className="eyebrow">Conta</p>
                <h2 id="account-title">@{currentUser?.username ?? "usuário"}</h2>
                <p>{currentUser?.displayName}</p>
              </div>
              <button className="modal-close" type="button" aria-label="Fechar" onClick={() => setAccountOpen(false)}>×</button>
            </div>
            <ThemeToggle />
            <form className="password-form" onSubmit={changePassword}>
              <label>Senha atual<input type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required /></label>
              <label>Nova senha<input type="password" minLength={8} maxLength={128} value={newPassword} onChange={(event) => setNewPassword(event.target.value)} required /></label>
              <label>Confirmar nova senha<input type="password" minLength={8} maxLength={128} value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required /></label>
              {accountError && <p className="error-message">{accountError}</p>}
              {accountMessage && <p className="success-message">{accountMessage}</p>}
              <button className="button primary" type="submit">Trocar senha</button>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
