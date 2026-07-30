import type { ChronicleStatus, GeneratedStory } from "@/lib/types";
import { formatDate } from "@/lib/format";
import { DragonPanel } from "@/components/DragonPanel";

interface AiAdaptationPanelProps {
  status: ChronicleStatus;
  generatedStory?: GeneratedStory;
}

export function AiAdaptationPanel({ status, generatedStory }: AiAdaptationPanelProps) {
  return (
    <section className="ai-adaptation-panel card" aria-labelledby="ai-adaptation-heading">
      <div className="section-heading">
        <h2 id="ai-adaptation-heading">Adaptação da história</h2>
      </div>
      <p className="ai-adaptation-intro">Uma interpretação gerada a partir da thread original.</p>
      {generatedStory ? <GeneratedStoryContent story={generatedStory} /> : <AiAdaptationEmptyState status={status} />}
    </section>
  );
}

function GeneratedStoryContent({ story }: { story: GeneratedStory }) {
  return (
    <div className="story-prose-frame">
      <div className="story-prose-column">
        <p className="eyebrow">Adaptação gerada por IA · v{story.version}</p>
        <h3>{story.title}</h3>
        <div className="story-prose">{story.content}</div>
        <p className="ai-adaptation-timestamp">
          Gerada em <time dateTime={story.createdAt}>{formatDate(story.createdAt)}</time>
        </p>
      </div>
      <DragonPanel />
    </div>
  );
}

function AiAdaptationEmptyState({ status }: { status: ChronicleStatus }) {
  const message = emptyStateMessage(status);
  return <p className="ai-adaptation-empty">{message}</p>;
}

function emptyStateMessage(status: ChronicleStatus): string {
  switch (status) {
    case "AI_PENDING":
      return "Sem uma chave de IA configurada, a thread permanece concluída e o trabalho aguarda processamento.";
    case "AI_PROCESSING":
      return "O Cronista está tecendo a adaptação agora.";
    case "FAILED":
      return "A adaptação por IA falhou. A thread original permanece intacta e disponível para leitura.";
    case "ARCHIVED":
      return "Esta crônica foi arquivada sem uma adaptação gerada.";
    default:
      return "Adaptação indisponível.";
  }
}
