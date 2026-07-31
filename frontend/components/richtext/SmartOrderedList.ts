import type { Editor } from "@tiptap/react";
import { Extension } from "@tiptap/core";
import { OrderedList as BaseOrderedList } from "@tiptap/extension-ordered-list";

export type SmartListKind = "decimal" | "alpha";

function flipKind(kind: SmartListKind): SmartListKind {
  return kind === "decimal" ? "alpha" : "decimal";
}

export const SmartOrderedList = BaseOrderedList.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      kind: {
        default: "decimal",
        parseHTML: (element: HTMLElement) => element.getAttribute("data-kind") || "decimal",
        renderHTML: (attributes: { kind?: SmartListKind }) => ({ "data-kind": attributes.kind || "decimal" }),
      },
    };
  },
});

// Tab inside an orderedList sinks the item like the default listItem keymap, then, when the
// sink created a brand-new nested list (as opposed to merging into an already-flipped one),
// flips its kind relative to its parent so depth alternates decimal ("1.") / alpha ("a)").
export function sinkListSmart(editor: Editor): boolean {
  const wasInOrderedList = editor.isActive("orderedList");
  const sunk = editor.commands.sinkListItem("listItem");
  if (!sunk || !wasInOrderedList) return sunk;

  const { $from } = editor.state.selection;
  let childDepth = -1;
  let parentDepth = -1;
  for (let depth = $from.depth; depth > 0; depth -= 1) {
    if ($from.node(depth).type.name === "orderedList") {
      if (childDepth === -1) {
        childDepth = depth;
      } else {
        parentDepth = depth;
        break;
      }
    }
  }
  if (childDepth === -1 || parentDepth === -1) return true;

  const childNode = $from.node(childDepth);
  const parentNode = $from.node(parentDepth);
  if (childNode.attrs.kind === parentNode.attrs.kind) {
    const pos = $from.before(childDepth);
    const tr = editor.state.tr.setNodeMarkup(pos, undefined, {
      ...childNode.attrs,
      kind: flipKind(parentNode.attrs.kind),
    });
    editor.view.dispatch(tr);
  }
  return true;
}

export function liftListSmart(editor: Editor): boolean {
  return editor.commands.liftListItem("listItem");
}

export const SmartListKeymap = Extension.create({
  name: "smartListKeymap",
  priority: 1000,
  addKeyboardShortcuts() {
    return {
      Tab: () => {
        if (!this.editor.isActive("orderedList")) return false;
        sinkListSmart(this.editor);
        return true;
      },
      "Shift-Tab": () => {
        if (!this.editor.isActive("orderedList")) return false;
        liftListSmart(this.editor);
        return true;
      },
    };
  },
});
