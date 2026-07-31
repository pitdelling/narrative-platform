import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AiArtifacts } from "@/lib/types";

const POLL_INTERVAL_MS = 5000;

function isProcessing(artifacts?: AiArtifacts): boolean {
  if (!artifacts) return false;
  const pending = (status?: string) => status === "PENDING" || status === "PROCESSING";
  return ["AI_PENDING", "AI_PROCESSING"].includes(artifacts.adaptation.status)
    || pending(artifacts.canonMap?.status)
    || pending(artifacts.synopsis?.status);
}

export function useAiArtifacts(partyId: string, chronicleId: string, enabled: boolean, refreshToken: number) {
  const [artifacts, setArtifacts] = useState<AiArtifacts>();

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    let timeoutId: number;

    async function poll() {
      try {
        const result = await api<AiArtifacts>(`/parties/${partyId}/chronicles/${chronicleId}/ai-artifacts`);
        if (cancelled) return;
        setArtifacts(result);
        if (isProcessing(result)) timeoutId = window.setTimeout(poll, POLL_INTERVAL_MS);
      } catch {
        if (!cancelled) setArtifacts(undefined);
      }
    }

    void poll();
    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, [partyId, chronicleId, enabled, refreshToken]);

  return { artifacts, processing: isProcessing(artifacts) };
}
