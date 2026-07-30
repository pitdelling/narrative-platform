"use client";

import { useState } from "react";
import { api } from "@/lib/api";
import type { CanonCategory, TagColor, TagSetting } from "@/lib/types";
import { canonCategoryLabels } from "@/lib/format";

interface AiTagSettingsPanelProps {
  partyId: string;
  narrator: boolean;
}

const categoryOrder: CanonCategory[] = ["PERSON", "PLACE", "ITEM", "SPELL", "CREATURE"];
const colorOptions: TagColor[] = ["GOLD", "COPPER", "VIOLET", "AZURE", "GREEN", "ROSE", "SLATE"];

const colorLabels: Record<TagColor, string> = {
  GOLD: "Dourado",
  COPPER: "Cobre",
  VIOLET: "Violeta",
  AZURE: "Azul",
  GREEN: "Verde",
  ROSE: "Rosa",
  SLATE: "Ardósia",
};

const categoryDescriptions: Record<CanonCategory, string> = {
  PERSON: "Seres pensantes, personagens e entidades conscientes.",
  PLACE: "Locais, ambientes, regiões e construções.",
  ITEM: "Objetos, artefatos e elementos físicos relevantes.",
  SPELL: "Magias, rituais, poderes e efeitos mágicos.",
  CREATURE: "Animais, monstros e seres não pensantes.",
};

function orderByCategory(settings: TagSetting[]): TagSetting[] {
  return categoryOrder
    .map((category) => settings.find((item) => item.category === category))
    .filter((item): item is TagSetting => item !== undefined);
}

export function AiTagSettingsPanel({ partyId, narrator }: AiTagSettingsPanelProps) {
  const [confirmed, setConfirmed] = useState<TagSetting[]>();
  const [draft, setDraft] = useState<TagSetting[]>();
  const [loaded, setLoaded] = useState(false);
  const [status, setStatus] = useState<"idle" | "saving" | "success" | "error">("idle");
  const [message, setMessage] = useState("");

  async function load() {
    if (loaded) return;
    setLoaded(true);
    try {
      const response = await api<{ settings: TagSetting[] }>(`/parties/${partyId}/ai-tag-settings`);
      const ordered = orderByCategory(response.settings);
      setConfirmed(ordered);
      setDraft(ordered);
    } catch (cause) {
      setLoaded(false);
      setStatus("error");
      setMessage(cause instanceof Error ? cause.message : "Não foi possível carregar as configurações.");
    }
  }

  function update(category: CanonCategory, patch: Partial<TagSetting>) {
    setDraft((current) => current?.map((item) => (item.category === category ? { ...item, ...patch } : item)));
    setStatus("idle");
    setMessage("");
  }

  async function save() {
    if (!draft) return;
    setStatus("saving");
    setMessage("");
    try {
      await api(`/parties/${partyId}/ai-tag-settings`, {
        method: "PUT",
        body: JSON.stringify({ settings: draft }),
      });
      setConfirmed(draft);
      setStatus("success");
      setMessage("Configurações salvas.");
    } catch (cause) {
      setDraft(confirmed);
      setStatus("error");
      setMessage(cause instanceof Error ? cause.message : "Não foi possível salvar as configurações.");
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
      <p>Escolha quais tipos de elementos a IA deve identificar nas próximas histórias finalizadas.</p>
      {!draft ? (
        <p className="canon-map-state">Carregando configurações...</p>
      ) : (
        <>
          {draft.map((setting) => (
            <div className="tag-setting-row" key={setting.category}>
              <label className="tag-setting-name checkbox">
                <input
                  type="checkbox"
                  checked={setting.enabled}
                  onChange={(event) => update(setting.category, { enabled: event.target.checked })}
                />
                <span>
                  <strong>{canonCategoryLabels[setting.category]}</strong>
                  <small>{categoryDescriptions[setting.category]}</small>
                </span>
              </label>
              <label className="tag-setting-color">
                Cor
                <select
                  value={setting.color}
                  onChange={(event) => update(setting.category, { color: event.target.value as TagColor })}
                >
                  {colorOptions.map((color) => (
                    <option key={color} value={color}>{colorLabels[color]}</option>
                  ))}
                </select>
                <span className={`canon-color-swatch canon-color-swatch-${setting.color.toLowerCase()}`} aria-hidden="true" />
              </label>
              <label>
                Ordem
                <input
                  className="tag-setting-order"
                  type="number"
                  min={0}
                  value={setting.displayOrder}
                  onChange={(event) => update(setting.category, { displayOrder: Number(event.target.value) })}
                />
              </label>
            </div>
          ))}
          <p className="ai-tag-settings-note">
            As mudanças serão usadas em novas gerações. Mapas já gerados preservam a configuração anterior.
          </p>
          <div className="ai-tag-settings-actions">
            <button type="button" className="button primary" onClick={save} disabled={status === "saving"}>
              {status === "saving" ? "Salvando..." : "Salvar configurações"}
            </button>
            {message && <span className={status === "error" ? "error-message" : "success-message"}>{message}</span>}
          </div>
        </>
      )}
    </details>
  );
}
