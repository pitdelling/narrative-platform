import { Extension } from "@tiptap/core";

export const INDENT_REM_PER_LEVEL = 2;

export const BlockIndent = Extension.create({
  name: "blockIndent",
  addGlobalAttributes() {
    return [
      {
        types: ["paragraph"],
        attributes: {
          indent: {
            default: 0,
            parseHTML: (element: HTMLElement) => {
              const match = /(-?\d+(?:\.\d+)?)rem/.exec(element.style.marginLeft || "");
              return match ? Math.round(Number(match[1]) / INDENT_REM_PER_LEVEL) : 0;
            },
            renderHTML: (attributes: { indent?: number }) => {
              if (!attributes.indent) return {};
              return { style: `margin-left: ${attributes.indent * INDENT_REM_PER_LEVEL}rem` };
            },
          },
        },
      },
    ];
  },
});
