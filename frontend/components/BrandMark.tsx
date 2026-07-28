export function BrandMark({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`brand-mark ${compact ? "brand-mark--compact" : ""}`} aria-label="Marca da plataforma">
      <span className="brand-orbit" aria-hidden="true"><span>✦</span></span>
      {!compact && (
        <div>
          <strong>{process.env.NEXT_PUBLIC_APP_NAME ?? "Narrative Platform"}</strong>
          <small>Ferramentas para mundos em construção</small>
        </div>
      )}
    </div>
  );
}
