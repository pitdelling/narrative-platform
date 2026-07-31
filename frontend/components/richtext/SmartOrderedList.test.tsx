import { describe, expect, it } from "vitest";
import { Editor } from "@tiptap/core";
import StarterKit from "@tiptap/starter-kit";
import { SmartOrderedList, sinkListSmart, liftListSmart } from "@/components/richtext/SmartOrderedList";

function makeEditor(html: string) {
  return new Editor({
    extensions: [StarterKit.configure({ link: false, orderedList: false }), SmartOrderedList],
    content: html,
  });
}

describe("sinkListSmart", () => {
  it("flips a freshly nested list from decimal to alpha", () => {
    const editor = makeEditor('<ol data-kind="decimal"><li>One</li><li>Two</li></ol>');
    editor.commands.setTextSelection(editor.state.doc.content.size - 1);

    sinkListSmart(editor);

    expect(editor.getHTML()).toContain('data-kind="alpha"');
    editor.destroy();
  });

  it("flips a freshly nested list from alpha back to decimal one level deeper", () => {
    const editor = makeEditor(
      '<ol data-kind="decimal"><li>One<ol data-kind="alpha"><li>a</li><li>b</li></ol></li></ol>',
    );
    editor.commands.setTextSelection(editor.state.doc.content.size - 1);

    sinkListSmart(editor);

    const html = editor.getHTML();
    expect(html).toContain('data-kind="decimal"');
    // the newly created depth-3 list should be decimal even though its immediate parent (depth 2) is alpha
    expect(html.match(/data-kind="decimal"/g)?.length).toBe(2);
    editor.destroy();
  });

  it("does not flip when sinking merges into an already-correctly-kinded nested list", () => {
    const editor = makeEditor(
      '<ol data-kind="decimal"><li>One<ol data-kind="alpha"><li>a</li></ol></li><li>Two</li></ol>',
    );
    editor.commands.setTextSelection(editor.state.doc.content.size - 1);

    sinkListSmart(editor);

    const html = editor.getHTML();
    expect(html.match(/data-kind="alpha"/g)?.length).toBe(1);
    expect(html).toContain("<li><p>a</p></li><li><p>Two</p></li>");
    editor.destroy();
  });
});

describe("liftListSmart", () => {
  it("lifts a nested item back out to its parent list", () => {
    const editor = makeEditor(
      '<ol data-kind="decimal"><li>One<ol data-kind="alpha"><li>a</li></ol></li></ol>',
    );
    editor.commands.setTextSelection(editor.state.doc.content.size - 1);

    liftListSmart(editor);

    expect(editor.getHTML()).not.toContain('data-kind="alpha"');
    editor.destroy();
  });
});
