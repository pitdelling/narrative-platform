import { describe, expect, it } from "vitest";
import { chronicleStatusLabels, formatDate, formatDateTime, turnStatusLabels } from "@/lib/format";

describe("date formatting", () => {
  const iso = "2026-03-05T14:30:00.000Z";

  it("formats a date without time", () => {
    expect(formatDate(iso)).toBe(new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date(iso)));
  });

  it("formats a date with time", () => {
    expect(formatDateTime(iso)).toBe(
      new Intl.DateTimeFormat("pt-BR", { dateStyle: "long", timeStyle: "short" }).format(new Date(iso)),
    );
  });
});

describe("status label dictionaries", () => {
  it("has a Portuguese label for every chronicle status", () => {
    expect(chronicleStatusLabels.IN_PROGRESS).toBe("Em andamento");
    expect(chronicleStatusLabels.PUBLISHED).toBe("História finalizada");
    expect(chronicleStatusLabels.FAILED).toBe("Falha na adaptação");
  });

  it("has a Portuguese label for every turn status", () => {
    expect(turnStatusLabels.SKIPPED).toBe("Pulado");
    expect(turnStatusLabels.EXPIRED).toBe("Expirado");
    expect(turnStatusLabels.SUBMITTED).toBe("Enviado");
  });
});
