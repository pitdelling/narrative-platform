"use client";

import { useEffect, useState } from "react";

interface AiProcessingTimerProps {
  processing: boolean;
  startedAt?: number;
}

function formatElapsed(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${minutes}:${remainder.toString().padStart(2, "0")}`;
}

export function AiProcessingTimer({ processing, startedAt }: AiProcessingTimerProps) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    if (!processing || !startedAt) return;
    const tick = () => setElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000));
    const timeoutId = window.setTimeout(tick, 0);
    const intervalId = window.setInterval(tick, 1000);
    return () => {
      window.clearTimeout(timeoutId);
      window.clearInterval(intervalId);
    };
  }, [processing, startedAt]);

  if (!processing) return null;

  return <span className="ai-processing-timer">Gerando IA... {formatElapsed(elapsedSeconds)}</span>;
}
