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

function buildRow(index: number, availableWidth: number, metrics: DragonMetrics): HTMLDivElement {
  const row = document.createElement("div");
  row.className = index % 2 === 0 ? "dragon-row" : "dragon-row dragon-row--reverse";

  let remaining = availableWidth;
  let hasDragon = false;

  while (remaining >= metrics.minimum) {
    if (hasDragon) {
      if (remaining < metrics.gap + metrics.minimum) break;
      remaining -= metrics.gap;
    }
    const units = Math.min(3, Math.floor((remaining - metrics.fixed) / metrics.unit));
    if (units < 1) break;
    row.appendChild(buildDragon(units));
    remaining -= metrics.fixed + units * metrics.unit;
    hasDragon = true;
  }

  return row;
}

export function DragonPanel() {
  const innerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const inner = innerRef.current;
    if (!inner) return;

    let frame = 0;

    function recalculate() {
      if (!inner) return;
      const styles = getComputedStyle(inner);
      const headWidth = parseFloat(styles.getPropertyValue("--dragon-head-width"));
      const tailWidth = parseFloat(styles.getPropertyValue("--dragon-tail-width"));
      const unitWidth = parseFloat(styles.getPropertyValue("--dragon-unit-width"));
      const gap = parseFloat(styles.getPropertyValue("--dragon-gap"));
      const rowHeight = parseFloat(styles.getPropertyValue("--dragon-row-height"));
      const rowGap = parseFloat(styles.getPropertyValue("--dragon-row-gap"));
      const fixed = headWidth + tailWidth;
      const metrics: DragonMetrics = { fixed, unit: unitWidth, gap, minimum: fixed + unitWidth };

      const { width: availableWidth, height: availableHeight } = inner.getBoundingClientRect();
      const rowCount = Math.max(0, Math.floor((availableHeight + rowGap) / (rowHeight + rowGap)));

      const fragment = document.createDocumentFragment();
      for (let index = 0; index < rowCount; index += 1) {
        fragment.appendChild(buildRow(index, availableWidth, metrics));
      }
      inner.replaceChildren(fragment);
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
      <div className="dragon-panel__inner" ref={innerRef} />
    </div>
  );
}
