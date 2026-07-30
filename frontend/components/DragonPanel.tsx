"use client";

import { useEffect, useRef } from "react";

interface DragonMetrics {
  fixed: number;
  unit: number;
  gap: number;
  minimum: number;
}

function buildDragon(units: number): HTMLDivElement {
  const dragon = document.createElement("div");
  dragon.className = "dragon";

  const head = document.createElement("span");
  head.className = "dragon__head";
  dragon.appendChild(head);

  const body = document.createElement("span");
  body.className = "dragon__body";
  for (let i = 0; i < units; i += 1) {
    const unit = document.createElement("span");
    unit.className = "dragon__unit";
    body.appendChild(unit);
  }
  dragon.appendChild(body);

  const tail = document.createElement("span");
  tail.className = "dragon__tail";
  dragon.appendChild(tail);

  return dragon;
}

function distributeRow(row: HTMLElement, availableWidth: number, metrics: DragonMetrics) {
  const fragment = document.createDocumentFragment();
  let remaining = availableWidth;
  let hasDragon = false;

  while (remaining >= metrics.minimum) {
    if (hasDragon) {
      if (remaining < metrics.gap + metrics.minimum) break;
      remaining -= metrics.gap;
    }
    const units = Math.min(3, Math.floor((remaining - metrics.fixed) / metrics.unit));
    if (units < 1) break;
    fragment.appendChild(buildDragon(units));
    remaining -= metrics.fixed + units * metrics.unit;
    hasDragon = true;
  }

  row.replaceChildren(fragment);
}

export function DragonPanel() {
  const innerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const inner = innerRef.current;
    if (!inner) return;

    const rows = Array.from(inner.querySelectorAll<HTMLElement>(".dragon-row"));
    let frame = 0;

    function recalculate() {
      if (!inner) return;
      const styles = getComputedStyle(inner);
      const headWidth = parseFloat(styles.getPropertyValue("--dragon-head-width"));
      const tailWidth = parseFloat(styles.getPropertyValue("--dragon-tail-width"));
      const unitWidth = parseFloat(styles.getPropertyValue("--dragon-unit-width"));
      const gap = parseFloat(styles.getPropertyValue("--dragon-gap"));
      const fixed = headWidth + tailWidth;
      const metrics: DragonMetrics = { fixed, unit: unitWidth, gap, minimum: fixed + unitWidth };
      const availableWidth = inner.getBoundingClientRect().width;
      rows.forEach((row) => distributeRow(row, availableWidth, metrics));
    }

    const observer = new ResizeObserver(() => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(recalculate);
    });
    observer.observe(inner);
    recalculate();

    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, []);

  return (
    <div className="dragon-panel" aria-hidden="true">
      <div className="dragon-panel__inner" ref={innerRef}>
        <div className="dragon-row" />
        <div className="dragon-row dragon-row--reverse" />
        <div className="dragon-row" />
        <div className="dragon-row dragon-row--reverse" />
      </div>
    </div>
  );
}
