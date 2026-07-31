"use client";

import DOMPurify from "isomorphic-dompurify";

const ALLOWED_TAGS = ["b", "strong", "i", "em", "u", "s", "ul", "ol", "li", "p", "br", "span"];
const ALLOWED_ATTR = ["style", "data-kind"];

interface RichTextViewerProps {
  html: string;
  className?: string;
}

export function RichTextViewer({ html, className }: RichTextViewerProps) {
  const clean = DOMPurify.sanitize(html, { ALLOWED_TAGS, ALLOWED_ATTR });
  return <div className={className} dangerouslySetInnerHTML={{ __html: clean }} />;
}
