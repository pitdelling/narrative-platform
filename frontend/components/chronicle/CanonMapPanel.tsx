"use client";

import type { AiArtifacts, CanonCategorySection, CanonTag, TagBasis } from "@/lib/types";
import { tagBasisLabels } from "@/lib/format";

interface CanonMapPanelProps {
  artifacts?: AiArtifacts;
  finished: boolean;
}

export function CanonMapPanel({ artifacts, finished }: CanonMapPanelProps) {
  if (!finished) return null;

  return (
    <details className="card canon-map-panel">
      <summary>Mapa do cânone</summary>
      <p className="canon-map-intro">
        O mapa organiza elementos encontrados na thread. Detalhes criados pela IA para completar
        aparência ou personalidade são identificados como complemento criativo.
      </p>
      <CanonMapBody artifacts={artifacts} />
    </details>
  );
}

function CanonMapBody({ artifacts }: { artifacts?: AiArtifacts }) {
  const canonMap = artifacts?.canonMap;

  if (!canonMap) {
    return <p className="canon-map-state">O mapa do cânone será preparado a partir da thread.</p>;
  }
  if (canonMap.status === "PENDING" && artifacts && !artifacts.aiConfigured) {
    return <p className="canon-map-state">O mapa do cânone não está disponível neste ambiente.</p>;
  }
  if (canonMap.status === "PENDING") {
    return <p className="canon-map-state">O mapa do cânone será preparado a partir da thread.</p>;
  }
  if (canonMap.status === "PROCESSING") {
    return <p className="canon-map-state">Organizando pessoas, lugares e outros elementos…</p>;
  }
  if (canonMap.status === "FAILED") {
    return (
      <>
        <p className="canon-map-state">Não foi possível gerar o mapa do cânone.</p>
        <p className="canon-map-empty">A thread original permanece intacta e completa.</p>
      </>
    );
  }

  const sortedCategories = [...canonMap.categories].sort((a, b) => a.displayOrder - b.displayOrder);

  if (sortedCategories.length === 0) {
    return <p className="canon-map-state">Nenhuma categoria de mapa do cânone foi configurada para esta party.</p>;
  }

  const totalTags = sortedCategories.reduce((sum, category) => sum + category.tags.length, 0);
  if (totalTags === 0) {
    return <p className="canon-map-state">Nenhum elemento foi identificado nas categorias configuradas.</p>;
  }

  return (
    <>
      {sortedCategories.map((category) => (
        <CategorySection key={category.id} category={category} />
      ))}
    </>
  );
}

function CategorySection({ category }: { category: CanonCategorySection }) {
  return (
    <div className="canon-category-section">
      <div className="canon-category-heading">
        <span className="canon-color-swatch" style={{ background: category.color }} aria-hidden="true" />
        <span>{category.name}</span>
        <span className="canon-category-count">{category.tags.length}</span>
      </div>
      {category.tags.length === 0 ? (
        <p className="canon-map-empty">Nenhum elemento identificado nesta categoria.</p>
      ) : (
        <div className="canon-tag-list">
          {category.tags.map((tag) => (
            <TagChip key={tag.id} tag={tag} />
          ))}
        </div>
      )}
    </div>
  );
}

function TagChip({ tag }: { tag: CanonTag }) {
  const sourcesLabel = tag.sourceSegmentPositions.length === 1
    ? `Trecho ${tag.sourceSegmentPositions[0]}`
    : `Trechos ${tag.sourceSegmentPositions.join(", ")}`;

  return (
    <details className="canon-tag-chip">
      <summary>
        <span>{tag.name}</span>
        <span className="canon-tag-summary-text">{tag.summary}</span>
      </summary>
      <div className="canon-tag-detail">
        <p>
          <span className="canon-tag-detail-label">Descrição visual</span>
          {tag.visualDescription} <BasisBadge basis={tag.visualBasis} />
        </p>
        {tag.personalityDescription && tag.personalityBasis && (
          <p>
            <span className="canon-tag-detail-label">Personalidade</span>
            {tag.personalityDescription} <BasisBadge basis={tag.personalityBasis} />
          </p>
        )}
        <p className="canon-tag-sources">{sourcesLabel}</p>
      </div>
    </details>
  );
}

function BasisBadge({ basis }: { basis: TagBasis }) {
  return <span className={`canon-tag-basis canon-tag-basis-${basis.toLowerCase()}`}>{tagBasisLabels[basis]}</span>;
}
