import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// jsdom has no ResizeObserver; DragonPanel (and anything that renders it) needs a stub to mount in tests.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver ??= ResizeObserverStub as unknown as typeof ResizeObserver;

// jsdom has no layout engine, so ProseMirror (Tiptap's editor core) can't measure real positions/rects.
// These stubs let its DOM-change detection run without throwing, so typing in tests works.
document.elementFromPoint ??= () => null;
Range.prototype.getBoundingClientRect ??= () => ({
  x: 0, y: 0, top: 0, left: 0, right: 0, bottom: 0, width: 0, height: 0, toJSON() { return this; },
});
Range.prototype.getClientRects ??= () => ({
  item: () => null,
  length: 0,
  [Symbol.iterator]: function* () {},
}) as unknown as DOMRectList;

afterEach(() => {
  cleanup();
  window.localStorage.clear();
});
