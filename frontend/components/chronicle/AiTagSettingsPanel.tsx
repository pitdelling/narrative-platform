"use client";

import { useState } from "react";
import { api } from "@/lib/api";
import type { CanonCategory } from "@/lib/types";

interface AiTagSettingsPanelProps {
  partyId: string;
  narrator: boolean;
}

const DEFAULT_COLOR = "#7665a7";

interface Draft {
  name: string;
  description: string;
  color: string;
}

const EMPTY_DRAFT: Draft = { name: "", description: "", color: DEFAULT_COLOR };

export function AiTagSettingsPanel({ partyId, narrator }: AiTagSettingsPanelProps) {
  const [categories, setCategories] = useState<CanonCategory[]>();
  const [loaded, setLoaded] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [editingId, setEditingId] = useState<string | "new" | null>(null);
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [rowError, setRowError] = useState<Record<string, string>>({});
  const [busyId, setBusyId] = useState<string | null>(null);

  async function refresh() {
    try {
      const result = await api<CanonCategory[]>(`/parties/${partyId}/ai-tag-settings`);
      setCategories(result);
      setLoadError("");
    } catch (cause) {
      setLoadError(cause instanceof Error ? cause.message : "Não foi possível carregar as categorias.");
    }
  }

  async function load() {
    if (loaded) return;
    setLoaded(true);
    await refresh();
  }

  function startAdd() {
    setEditingId("new");
    setDraft(EMPTY_DRAFT);
  }

  function startEdit(category: CanonCategory) {
    setEditingId(category.id);
    setDraft({ name: category.name, description: category.description ?? "", color: category.color });
    setRowError((current) => ({ ...current, [category.id]: "" }));
  }

  function cancelEdit() {
    if (editingId) setRowError((current) => ({ ...current, [editingId]: "" }));
    setEditingId(null);
    setDraft(EMPTY_DRAFT);
  }

  async function saveEdit() {
    if (!editingId) return;
    const key = editingId;
    setBusyId(key);
    setRowError((current) => ({ ...current, [key]: "" }));
    try {
      const body = JSON.stringify({
        name: draft.name.trim(),
        description: draft.description.trim() || undefined,
        color: draft.color,
      });
      if (key === "new") {
        await api(`/parties/${partyId}/ai-tag-settings`, { method: "POST", body });
      } else {
        await api(`/parties/${partyId}/ai-tag-settings/${key}`, { method: "PUT", body });
      }
      await refresh();
      setEditingId(null);
      setDraft(EMPTY_DRAFT);
    } catch (cause) {
      setRowError((current) => ({
        ...current,
        [key]: cause instanceof Error ? cause.message : "Não foi possível salvar a categoria.",
      }));
    } finally {
      setBusyId(null);
    }
  }

  async function remove(category: CanonCategory) {
    if (!window.confirm(`Excluir a categoria "${category.name}"? Essa ação não pode ser desfeita.`)) return;
    setBusyId(category.id);
    try {
      await api(`/parties/${partyId}/ai-tag-settings/${category.id}`, { method: "DELETE" });
      await refresh();
    } catch (cause) {
      setRowError((current) => ({
        ...current,
        [category.id]: cause instanceof Error ? cause.message : "Não foi possível excluir a categoria.",
      }));
    } finally {
      setBusyId(null);
    }
  }

  async function move(category: CanonCategory, direction: "move-up" | "move-down") {
    setBusyId(category.id);
    try {
      await api(`/parties/${partyId}/ai-tag-settings/${category.id}/${direction}`, { method: "POST" });
      await refresh();
    } catch (cause) {
      setRowError((current) => ({
        ...current,
        [category.id]: cause instanceof Error ? cause.message : "Não foi possível reordenar a categoria.",
      }));
    } finally {
      setBusyId(null);
    }
  }

  if (!narrator) return null;

  return (
    <details
      className="card ai-tag-settings-panel"
      onToggle={(event) => {
        if ((event.target as HTMLDetailsElement).open) void load();
      }}
    >
      <summary>Tags do mapa do cânone</summary>
      <p>Crie as categorias de elementos que a IA deve identificar nas próximas histórias finalizadas.</p>
      {!categories ? (
        loadError ? <p className="error-message">{loadError}</p> : <p className="canon-map-state">Carregando categorias...</p>
      ) : (
        <>
          {categories.length === 0 && editingId !== "new" && (
            <p className="canon-map-state">Nenhuma categoria configurada ainda.</p>
          )}
          {categories.map((category, index) => (
            <div key={category.id}>
              {editingId === category.id ? (
                <CategoryEditRow
                  draft={draft}
                  setDraft={setDraft}
                  onSave={saveEdit}
                  onCancel={cancelEdit}
                  busy={busyId === category.id}
                  error={rowError[category.id]}
                />
              ) : (
                <div className="category-row">
                  <div className="category-row-info">
                    <strong>{category.name}</strong>
                    {category.description && <small>{category.description}</small>}
                    {rowError[category.id] && <span className="error-message">{rowError[category.id]}</span>}
                  </div>
                  <span className="canon-color-swatch" style={{ background: category.color }} aria-hidden="true" />
                  <div className="category-row-actions">
                    <button
                      type="button"
                      className="invite-icon-button"
                      title="Mover para cima"
                      aria-label="Mover categoria para cima"
                      onClick={() => move(category, "move-up")}
                      disabled={index === 0 || busyId === category.id}
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      className="invite-icon-button"
                      title="Mover para baixo"
                      aria-label="Mover categoria para baixo"
                      onClick={() => move(category, "move-down")}
                      disabled={index === categories.length - 1 || busyId === category.id}
                    >
                      ↓
                    </button>
                    <button
                      type="button"
                      className="invite-icon-button"
                      title="Editar categoria"
                      aria-label="Editar categoria"
                      onClick={() => startEdit(category)}
                      disabled={busyId === category.id}
                    >
                      ✎
                    </button>
                    <button
                      type="button"
                      className="invite-icon-button"
                      title="Excluir categoria"
                      aria-label="Excluir categoria"
                      onClick={() => remove(category)}
                      disabled={busyId === category.id}
                    >
                      ✕
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
          {editingId === "new" && (
            <CategoryEditRow
              draft={draft}
              setDraft={setDraft}
              onSave={saveEdit}
              onCancel={cancelEdit}
              busy={busyId === "new"}
              error={rowError.new}
            />
          )}
          <p className="ai-tag-settings-note">
            As mudanças serão usadas em novas gerações. Mapas já gerados preservam a configuração anterior.
          </p>
          {editingId === null && (
            <div className="ai-tag-settings-actions">
              <button type="button" className="button secondary" onClick={startAdd}>Adicionar categoria</button>
            </div>
          )}
        </>
      )}
    </details>
  );
}

interface CategoryEditRowProps {
  draft: Draft;
  setDraft: (draft: Draft) => void;
  onSave: () => void;
  onCancel: () => void;
  busy: boolean;
  error?: string;
}

function CategoryEditRow({ draft, setDraft, onSave, onCancel, busy, error }: CategoryEditRowProps) {
  return (
    <div className="category-row-edit">
      <input
        type="text"
        placeholder="Nome da categoria"
        value={draft.name}
        onChange={(event) => setDraft({ ...draft, name: event.target.value })}
        maxLength={160}
      />
      <input
        type="text"
        placeholder="Descrição (opcional)"
        value={draft.description}
        onChange={(event) => setDraft({ ...draft, description: event.target.value })}
        maxLength={500}
      />
      <input
        type="color"
        aria-label="Cor da categoria"
        value={draft.color}
        onChange={(event) => setDraft({ ...draft, color: event.target.value })}
      />
      <button
        type="button"
        className="invite-icon-button"
        title="Salvar categoria"
        aria-label="Salvar categoria"
        onClick={onSave}
        disabled={busy || !draft.name.trim()}
      >
        ✓
      </button>
      <button
        type="button"
        className="invite-icon-button"
        title="Cancelar"
        aria-label="Cancelar edição da categoria"
        onClick={onCancel}
        disabled={busy}
      >
        ✕
      </button>
      {error && <span className="error-message">{error}</span>}
    </div>
  );
}
