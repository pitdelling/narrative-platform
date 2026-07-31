"use client";

import { useEffect } from "react";
import { EditorContent, useEditor, type Editor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { TextStyle, BackgroundColor } from "@tiptap/extension-text-style";
import { Color } from "@tiptap/extension-color";
import { SmartOrderedList, SmartListKeymap, sinkListSmart, liftListSmart, type SmartListKind } from "@/components/richtext/SmartOrderedList";
import { BlockIndent } from "@/components/richtext/BlockIndent";

export function isContentEmpty(html: string): boolean {
  return html.replace(/<[^>]*>/g, "").trim().length === 0;
}

interface RichTextEditorProps {
  value: string;
  onChange: (html: string) => void;
  readOnly?: boolean;
  placeholder?: string;
  maxLength?: number;
  ariaLabel?: string;
}

function toggleSmartList(editor: Editor, kind: SmartListKind) {
  const activeKind = editor.isActive("orderedList") ? (editor.getAttributes("orderedList").kind as SmartListKind) : null;
  if (activeKind === kind) {
    editor.chain().focus().toggleOrderedList().run();
    return;
  }
  if (activeKind) {
    editor.chain().focus().updateAttributes("orderedList", { kind }).run();
    return;
  }
  editor.chain().focus().toggleOrderedList().updateAttributes("orderedList", { kind }).run();
}

function indentBlock(editor: Editor) {
  editor.commands.focus();
  if (editor.isActive("orderedList") || editor.isActive("bulletList")) {
    sinkListSmart(editor);
    return;
  }
  const current = (editor.getAttributes("paragraph").indent as number) || 0;
  editor.chain().focus().updateAttributes("paragraph", { indent: current + 1 }).run();
}

function outdentBlock(editor: Editor) {
  editor.commands.focus();
  if (editor.isActive("orderedList") || editor.isActive("bulletList")) {
    liftListSmart(editor);
    return;
  }
  const current = (editor.getAttributes("paragraph").indent as number) || 0;
  editor.chain().focus().updateAttributes("paragraph", { indent: Math.max(0, current - 1) }).run();
}

export function RichTextEditor({ value, onChange, readOnly = false, placeholder, maxLength, ariaLabel }: RichTextEditorProps) {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({ link: false, orderedList: false }),
      SmartOrderedList,
      SmartListKeymap,
      BlockIndent,
      TextStyle,
      Color,
      BackgroundColor,
    ],
    content: value,
    editable: !readOnly,
    immediatelyRender: false,
    onUpdate: ({ editor: instance }) => onChange(instance.getHTML()),
    editorProps: {
      attributes: {
        class: "rte-content",
        ...(ariaLabel ? { "aria-label": ariaLabel } : {}),
        ...(placeholder ? { "data-placeholder": placeholder } : {}),
      },
    },
  });

  useEffect(() => {
    if (!editor) return;
    if (editor.getHTML() !== value) editor.commands.setContent(value, { emitUpdate: false });
  }, [editor, value]);

  useEffect(() => {
    editor?.setEditable(!readOnly);
  }, [editor, readOnly]);

  useEffect(() => {
    if (!editor) return;
    const syncEmptyState = () => editor.view.dom.classList.toggle("is-empty", editor.isEmpty);
    syncEmptyState();
    editor.on("update", syncEmptyState);
    return () => {
      editor.off("update", syncEmptyState);
    };
  }, [editor]);

  if (!editor) return null;

  return (
    <div className="rte">
      {!readOnly && (
        <div className="rte-toolbar">
          <div className="rte-toolbar-group">
            <button
              type="button"
              className={`invite-icon-button${editor.isActive("bold") ? " active" : ""}`}
              title="Negrito"
              aria-label="Negrito"
              onClick={() => editor.chain().focus().toggleBold().run()}
            >
              <strong>B</strong>
            </button>
            <button
              type="button"
              className={`invite-icon-button${editor.isActive("italic") ? " active" : ""}`}
              title="Itálico"
              aria-label="Itálico"
              onClick={() => editor.chain().focus().toggleItalic().run()}
            >
              <em>I</em>
            </button>
            <button
              type="button"
              className={`invite-icon-button${editor.isActive("underline") ? " active" : ""}`}
              title="Sublinhado"
              aria-label="Sublinhado"
              onClick={() => editor.chain().focus().toggleUnderline().run()}
            >
              <span style={{ textDecoration: "underline" }}>U</span>
            </button>
            <button
              type="button"
              className={`invite-icon-button${editor.isActive("strike") ? " active" : ""}`}
              title="Riscado"
              aria-label="Riscado"
              onClick={() => editor.chain().focus().toggleStrike().run()}
            >
              <span style={{ textDecoration: "line-through" }}>S</span>
            </button>
          </div>

          <span className="rte-toolbar-sep" aria-hidden="true" />

          <div className="rte-toolbar-group">
            <button
              type="button"
              className={`invite-icon-button${editor.isActive("bulletList") ? " active" : ""}`}
              title="Lista com marcadores"
              aria-label="Lista com marcadores"
              onClick={() => editor.chain().focus().toggleBulletList().run()}
            >
              •
            </button>
            <button
              type="button"
              className={`invite-icon-button${editor.isActive("orderedList", { kind: "decimal" }) ? " active" : ""}`}
              title="Enumeração de números"
              aria-label="Enumeração de números"
              onClick={() => toggleSmartList(editor, "decimal")}
            >
              1.
            </button>
            <button
              type="button"
              className={`invite-icon-button${editor.isActive("orderedList", { kind: "alpha" }) ? " active" : ""}`}
              title="Enumeração de letras minúsculas"
              aria-label="Enumeração de letras minúsculas"
              onClick={() => toggleSmartList(editor, "alpha")}
            >
              a)
            </button>
          </div>

          <span className="rte-toolbar-sep" aria-hidden="true" />

          <div className="rte-toolbar-group">
            <input
              type="color"
              title="Cor do texto"
              aria-label="Cor do texto"
              value={editor.getAttributes("textStyle").color || "#000000"}
              onChange={(event) => editor.chain().focus().setColor(event.target.value).run()}
            />
            <input
              type="color"
              title="Cor de fundo"
              aria-label="Cor de fundo"
              value={editor.getAttributes("textStyle").backgroundColor || "#ffffff"}
              onChange={(event) => editor.chain().focus().setBackgroundColor(event.target.value).run()}
            />
          </div>

          <span className="rte-toolbar-sep" aria-hidden="true" />

          <div className="rte-toolbar-group">
            <button
              type="button"
              className="invite-icon-button"
              title="Recuar bloco"
              aria-label="Recuar bloco"
              onClick={() => outdentBlock(editor)}
            >
              ⇤
            </button>
            <button
              type="button"
              className="invite-icon-button"
              title="Avançar bloco"
              aria-label="Avançar bloco"
              onClick={() => indentBlock(editor)}
            >
              ⇥
            </button>
          </div>
        </div>
      )}
      <EditorContent editor={editor} />
      {maxLength && (
        <span className="rte-char-count">{editor.getText().length} / {maxLength} caracteres</span>
      )}
    </div>
  );
}
