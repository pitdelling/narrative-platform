import type { ChronicleStatus, TagBasis, Turn } from "@/lib/types";

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date(iso));
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "long", timeStyle: "short" }).format(new Date(iso));
}

export const chronicleStatusLabels: Record<ChronicleStatus, string> = {
  DRAFT: "Rascunho",
  IN_PROGRESS: "Em andamento",
  AI_PENDING: "Aguardando o Cronista",
  AI_PROCESSING: "O Cronista está trabalhando",
  PUBLISHED: "História finalizada",
  FAILED: "Falha na adaptação",
  ARCHIVED: "Arquivada",
};

export const turnStatusLabels: Record<Turn["status"], string> = {
  WAITING: "Aguardando",
  ACTIVE: "Em andamento",
  SUBMITTED: "Enviado",
  SKIPPED: "Pulado",
  EXPIRED: "Expirado",
};

export const tagBasisLabels: Record<TagBasis, string> = {
  EXPLICIT: "Descrito na thread",
  INFERRED: "Inferido pela IA",
  CREATIVE_FILL: "Complemento criativo",
};
