import { describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import { RichTextViewer } from "@/components/RichTextViewer";

describe("RichTextViewer", () => {
  it("renders allowed formatting tags", () => {
    const { container } = render(<RichTextViewer html="<p>Hello <b>bold</b> and <i>italic</i></p>" />);
    expect(container.querySelector("b")).toHaveTextContent("bold");
    expect(container.querySelector("i")).toHaveTextContent("italic");
  });

  it("renders lists", () => {
    const { container } = render(<RichTextViewer html="<ul><li>one</li><li>two</li></ul>" />);
    expect(container.querySelectorAll("li")).toHaveLength(2);
  });

  it("keeps a color-only style on a span", () => {
    const { container } = render(<RichTextViewer html='<span style="color:#ff0000">red</span>' />);
    const span = container.querySelector("span");
    expect(span?.getAttribute("style")).toContain("color");
  });

  it("strips script tags and inline event handlers", () => {
    const { container } = render(
      <RichTextViewer html={'<p>Safe</p><script>window.__xss = true;</script><img src=x onerror="window.__xss = true">'} />,
    );
    expect(container.querySelector("script")).toBeNull();
    expect(container.querySelector("img")).toBeNull();
    expect(container.innerHTML).not.toContain("onerror");
    expect(container.textContent).toContain("Safe");
  });

  it("strips javascript: hrefs since links are not an allowed tag at all", () => {
    const { container } = render(<RichTextViewer html='<a href="javascript:alert(1)">click</a>' />);
    expect(container.querySelector("a")).toBeNull();
  });

  it("applies the given className to the wrapping element", () => {
    const { container } = render(<RichTextViewer html="<p>text</p>" className="segment-content" />);
    expect(container.firstElementChild).toHaveClass("segment-content");
  });
});
