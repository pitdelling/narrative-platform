"use client";

import { useState } from "react";
import type { Segment } from "@/lib/types";
import { isContentEmpty, RichTextEditor } from "@/components/RichTextEditor";

interface EditSegmentModalProps {
  open: boolean;
  segment: Segment | null;
  onClose: () => void;
  onSave: (content: string, reason: string) => void | Promise<void>;
}

export function EditSegmentModal({ open, segment, onClose, onSave }: EditSegmentModalProps) {
  const [content, setContent] = useState(segment?.content ?? "");
  const [reason, setReason] = useState("Edited by narrator.");
  const [lastSegmentId, setLastSegmentId] = useState(segment?.id);
  const [saving, setSaving] = useState(false);

  if (segment?.id !== lastSegmentId) {
    setLastSegmentId(segment?.id);
    setContent(segment?.content ?? "");
    setReason("Edited by narrator.");
  }

  if (!open || !segment) return null;

  async function save() {
    setSaving(true);
    try {
      await onSave(content, reason);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      className="modal-layer"
      role="presentation"
      onMouseDown={(event) => {
        if (event.currentTarget === event.target) onClose();
      }}
    >
      <section className="story-modal edit-segment-modal card" role="dialog" aria-modal="true" aria-labelledby="edit-segment-heading">
        <button className="modal-close" type="button" aria-label="Fechar" onClick={onClose}>
          ×
        </button>
        <div className="story-modal-scroll">
          <div className="modal-heading">
            <p className="eyebrow" id="edit-segment-heading">Editar trecho</p>
          </div>
          <RichTextEditor value={content} onChange={setContent} maxLength={10000} ariaLabel="Editar conteúdo do trecho" />
          <label>Motivo
            <input type="text" value={reason} onChange={(event) => setReason(event.target.value)} />
          </label>
          <div className="modal-actions">
            <button className="button primary" onClick={save} disabled={saving || isContentEmpty(content)}>Salvar</button>
            <button className="button secondary" onClick={onClose} disabled={saving}>Cancelar</button>
          </div>
        </div>
      </section>
    </div>
  );
}
