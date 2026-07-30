# Auditoria Permanente do Projeto

> Este arquivo é o registro permanente de auditoria, planejamento, decisões e progresso técnico da Narrative Platform. Todo console futuro do Claude Code deve lê-lo integralmente, junto com todos os arquivos `CLAUDE.md` aplicáveis, antes de alterar o projeto.

---

## 1. Protocolo obrigatório para consoles futuros

Todo console futuro que trabalhar neste projeto deve, antes de qualquer alteração:

1. Ler todos os `CLAUDE.md` aplicáveis (raiz, `backend/CLAUDE.md`, `frontend/CLAUDE.md`).
2. Ler este arquivo (`docs/PROJECT_AUDIT.md`) integralmente.
3. Verificar `git status` com comandos somente de leitura.
4. Preservar qualquer alteração manual/local ainda não commitada — nunca descartar.
5. Executar apenas o escopo do console atual; não expandir sem necessidade real.
6. Atualizar este arquivo ao final do console (nova entrada datada, nunca sobrescrever histórico).
7. Responder e planejar em português; código-fonte, comentários e documentação técnica normal em inglês.
8. Não apagar histórico deste arquivo — apenas adicionar/atualizar estados com data.
9. Não afirmar sucesso (build, teste, deploy) sem evidência real de execução.
10. Distinguir sempre: **confirmado** (visto no código), **inferido** (deduzido, não testado), **planejado** (ainda não implementado) e **desconhecido** (não foi possível verificar).

### Regras para atualização futura deste arquivo

1. Nunca apagar entradas históricas.
2. Nunca substituir uma decisão antiga sem registrar que ela foi revisada.
3. Registrar novas conclusões como entradas datadas.
4. Atualizar estados quando uma feature for implementada.
5. Preservar a diferença entre confirmado / inferido / planejado / desconhecido.
6. Registrar todas as migrations novas.
7. Registrar endpoints adicionados ou alterados.
8. Registrar decisões de segurança.
9. Registrar testes adicionados e testes realmente executados, separadamente.
10. Registrar problemas encontrados durante cada console.
11. Registrar alterações manuais identificadas no início do console.
12. Não incluir secrets, senhas, tokens ou valores sensíveis.
13. Não transformar o arquivo em uma cópia integral do código.
14. Manter o arquivo útil, objetivo e incremental.

---

## 2. Metadados da auditoria

- **Data/hora**: 2026-07-30.
- **Branch**: `main`.
- **Commit no início do console**: `90882d4` ("feat(chronicle): fit dragon rows dynamically and frame the panel").
- **Working tree no início**: limpo (`nothing to commit, working tree clean`).
- **Executado por**: Claude Code (console de auditoria).
- **Escopo**: auditoria geral somente leitura do repositório inteiro (raiz, `backend/`, `frontend/`, infraestrutura, migrations, testes) **mais** um conjunto de limpezas pontuais explicitamente autorizadas pelo utilizador durante este mesmo console (ver §12, console nº1).
- **Restrições aplicadas**: sem Maven/Gradle/Java CLI, sem iniciar Spring Boot, sem executar migrations, sem alterar banco de dados, sem comandos Docker que criem/parem/modifiquem containers, sem instalar dependências, sem alterar `.env`, sem `git push`, sem descartar alterações locais. Um único commit local foi explicitamente autorizado pelo utilizador para as ações do §12.

Nenhum secret foi reproduzido neste arquivo. `backend/src/main/resources/application-local.yml` (não versionado, listado em `backend/src/main/resources/.gitignore`) contém uma chave OpenAI e um segredo JWT com aparência real — não reproduzidos aqui; recomenda-se ao utilizador confirmar que essas credenciais não são reaproveitadas em produção e, por precaução, rotacioná-las.

---

## 3. Resumo executivo

**Estado geral**: o projeto é um MVP funcional de uma plataforma narrativa colaborativa para parties de RPG, com um único módulo em produção real de features ("Arquivo do Cronista"): contas, parties, convite reutilizável por party, crônicas escritas (com bloqueio de edição) e crônicas-jogo por turnos com moderação do narrador e geração final por IA. A infraestrutura documentada (Render) está desatualizada; a real é Vercel (frontend) + Cloudflare Tunnel (backend), corrigida neste console.

**Maturidade**: sólida no núcleo de jogo (parties, membros, convites, turnos, ciclos, segmentos ocultos/removidos, IA), mas com lacunas de qualidade transversais: praticamente zero cobertura de testes de backend (1 arquivo de teste em todo o projeto) e de frontend (2 arquivos, ambos sobre tema), nenhuma proteção de rate limiting, nenhum cabeçalho de segurança (CSP/HSTS), e nenhuma infraestrutura de eventos/outbox genérica (a fila de IA é o único padrão de job existente).

**Principais áreas completas**: autenticação username/senha, parties e papéis (`OWNER`/`NARRATOR`/`PLAYER`), convite reutilizável por party, crônicas escritas com lock de 24h, crônicas-jogo com turnos/ciclos/expiração/skip/finalização, visibilidade de segmentos (ocultos, revelação do narrador, remoção/moderação), fila de jobs de IA (funciona sem `OPENAI_API_KEY`), tema visual "Arcane Editorial Nocturne" (claro/escuro/sistema).

**Principais áreas pendentes**: votação (histórias e tags) — totalmente ausente, greenfield; PWA e Web Push — totalmente ausente, greenfield; notificações internas — totalmente ausente, greenfield; extração automática de tags — ausente; mini-história manual — ausente; badges/ranking — ausente.

**Principais riscos**: (a) `/dev-preview` era uma rota de desenvolvimento sem guarda de ambiente, reestruturada neste console (ver §12) para nunca mais ser commitada; (b) cobertura de testes quase nula em toda a base; (c) ausência de rate limiting em endpoints sensíveis (login, registro, regeneração de convite); (d) tabelas órfãs no schema (`audit_events`, `party_invites`) sem código Java correspondente; (e) documentação de infraestrutura estava desatualizada (Render), corrigida neste console.

**Recomendação principal de sequência**: antes de iniciar qualquer feature nova de votação, notificação ou PWA/push, tratar a dívida técnica transversal (testes de backend nas áreas de turno/IA, rate limiting básico, `Clock` bean para testabilidade) reduziria risco, mas não é bloqueante. Tecnicamente, a sequência mais segura é: (1) revisão integrada das funcionalidades existentes, (2) página da história finalizada + extração de tags (dependem de dados de IA já existentes), (3) votos em histórias e tags (podem ser paralelos entre si, mesmo padrão arquitetural), (4) auditoria + base de PWA (pré-requisito técnico de Web Push), (5) Web Push e notificações (dependem do PWA), (6) revisão final. Ver §6 para o detalhamento item a item da lista de 19 itens fornecida pelo utilizador.

---

## 4. Arquitetura atual confirmada

### Frontend
- Next.js **16.2.11**, React **19.2.0**, App Router (`frontend/app/`), sem Pages Router.
- Rotas: `/` (`frontend/app/page.tsx`, login+registro combinados), `/invite/[token]` (`frontend/app/invite/[token]/page.tsx`), `/app` (`frontend/app/app/page.tsx`, dashboard de parties), `/app/party/[partyId]` (`frontend/app/app/party/[partyId]/page.tsx`, arquivo da party), `/app/party/[partyId]/chronicle/[chronicleId]` (`frontend/app/app/party/[partyId]/chronicle/[chronicleId]/page.tsx`, branch `GameView`/`WrittenView` por `?type=`).
- Layout único: `frontend/app/layout.tsx` (fontes, script inline anti-flash de tema, `ThemeProvider`). Não há layouts aninhados por rota.
- "Shell" autenticado é um componente cliente, não um layout: `frontend/components/AppShell.tsx` — busca `GET /auth/me` a cada montagem e redireciona para `/` em falha; não há `middleware.ts` nem guarda de rota centralizada.
- Cliente HTTP: `frontend/lib/api.ts` (`api<T>()`), injeta `Authorization: Bearer` a partir de `frontend/lib/auth.ts` (token em `localStorage`, chave `narrative-platform-token`), sempre `cache: "no-store"`, converte `ProblemDetail` em `ApiError` tipado.
- Sem `loading.tsx`/`error.tsx` em nenhuma rota; estados de loading/erro/vazio são markup manual por página.
- Tema **Arcane Editorial Nocturne**: confirmado em `frontend/app/globals.css` (tokens `:root` + bloco `:root[data-theme="dark"]`), mecanismo em `frontend/lib/theme.ts`, `frontend/lib/theme-init-script.ts`, `frontend/components/ThemeProvider.tsx`, `frontend/components/ThemeToggle.tsx`. **Concluído — não deve ser reimplementado.**
- PWA/service worker/manifest/Push: **ausentes** (sem `frontend/public/`, sem `manifest`, sem `sw.js`, zero uso de `Notification`/`PushManager` no código).
- Votação (histórias/tags): **ausente** (zero ocorrências de "vote"/"voto"/"ranking"/"badge").
- IA no frontend: renderização inline em `frontend/app/app/party/[partyId]/chronicle/[chronicleId]/page.tsx` (bloco `.generated-story`, botão de regeneração, estados `AI_PENDING`/`AI_PROCESSING`); tipo `GeneratedStory` em `frontend/lib/types.ts`.
- Convite no frontend: painel real em `frontend/app/app/party/[partyId]/page.tsx` (link reutilizável, regeneração), com botão de WhatsApp (`frontend/components/WhatsAppIcon.tsx`, deep link `wa.me`) mantido deliberadamente por decisão do utilizador (§12).
- Testes: Vitest (`frontend/vitest.config.ts`), apenas 2 arquivos — `frontend/components/ThemeToggle.test.tsx`, `frontend/lib/theme.test.ts`. Sem testes de `lib/api.ts`, `lib/auth.ts`, `AppShell` ou páginas.

### Backend
- Spring Boot **4.1.0**, Java **21**, Maven (`backend/pom.xml`). Pacote raiz `com.narrativeplatform`.
- 5 bounded contexts em `backend/src/main/java/com/narrativeplatform/app/`: `auth`, `party`, `invitation`, `chronicle`, `aijob`. Não existem contexts de `voting`, `notification` ou `webpush`.
- Controllers: `AuthController` (`/api/auth`), `PartyController` (`/api/parties`), `InvitationController` (`/api/parties/{partyId}/invitation`, `/api/invites/{token}`), `ChronicleController` (`/api/parties/{partyId}/chronicles`, inclui `/regenerate` de IA).
- Serviço autoritativo de turno/ciclo/expiração/finalização: `GameChronicleService` (`backend/src/main/java/com/narrativeplatform/app/chronicle/services/GameChronicleService.java`) — `GameRunEntity.currentSequence` + o único `GameTurnEntity` `ACTIVE` é a fonte de verdade; `advance()`/`completeRun()` finalizam a run e enfileiram IA; `expireTurns()` (`@Scheduled(fixedDelay=60_000)`) varre expirações; `requireCurrentTurn` também expira turnos preguiçosamente antes da varredura.
- Autorização: `PartyAccessService` (`requireActiveMember/requireNarrator/requireOwner`) e `ChronicleAccessService`, sem `@PreAuthorize`/interceptor — chamadas explícitas em cada serviço.
- Erros: `GlobalExceptionHandler` (`@RestControllerAdvice`) + hierarquia `DomainException` (`BadRequestException`, `ForbiddenException`, `NotFoundException`, `ConflictException`, `TurnExpiredException`, `AiNotConfiguredException`), tudo via `ProblemDetail`.
- IA: `AiJobService`/`AiJobStateService`/`AiJobProcessor` (`@Scheduled(fixedDelay=15_000)`)/`OpenAiClient` (`backend/src/main/java/com/narrativeplatform/shared/integrations/OpenAiClient.java`). Funciona sem `OPENAI_API_KEY` (jobs ficam `PENDING`; regeneração explícita responde `503 ai_not_configured`).
- Sem `Clock` bean (todo timestamp é `Instant.now()` direto). Sem rate limiting. Sem `ApplicationEventPublisher`/outbox genérico — a tabela `ai_jobs` é o único padrão de fila existente.
- Testes: **1 único arquivo** em todo o backend — `backend/src/test/java/com/narrativeplatform/app/invitation/services/InvitationServiceTest.java`.

### Base de dados
- PostgreSQL via Flyway, migrations em `backend/src/main/resources/db/migration/`: `V1__initial_schema.sql`, `V2__game_turn_sequence_deferrable.sql`, `V3__party_invitation_links.sql` (detalhamento em §8).
- Tabelas órfãs (sem entidade JPA): `audit_events` (parece outbox abandonado, colunas `event_type`/`payload_json` sem código Java) e `party_invites` (modelo antigo de convite de uso único, substituído por `party_invitation_links`).

### Infraestrutura
- **Frontend**: Vercel — **confirmado pelo utilizador nesta sessão** (não estava documentado em nenhum arquivo do repositório antes deste console).
- **Backend**: hospedado onde o processo Spring Boot roda, exposto publicamente via **Cloudflare Tunnel** (`cloudflared`) — **confirmado pelo utilizador nesta sessão**; não havia nenhuma menção a Cloudflare em todo o repositório antes deste console (confirmado por grep).
- **Render**: não é mais usado. `render.yaml` (raiz) foi removido e `SETUP-PTBR.md` foi corrigido neste console (§12).
- `backend/Dockerfile`: build multi-stage (`maven:3.9.11-eclipse-temurin-21` → `eclipse-temurin:21-jre`), expõe porta 8080. Continua válido como forma de empacotar o backend, independentemente de onde ele seja executado.
- Sem Docker Compose, sem CI/CD (`.github/workflows` inexistente) em nenhum ponto do repositório.

### Integrações
- OpenAI: `shared/integrations/OpenAiClient`, configurável por `OPENAI_API_KEY`/`OPENAI_MODEL`/`OPENAI_BASE_URL`, opcional.
- Resend (e-mail) e WhatsApp Business API: **não existem no backend** — confirmado por grep (`Resend|wa\.me|WhatsApp|InviteChannelType|PartyInviteEntity`, zero ocorrências). Um documento antigo (já removido, ver §12) recomendava essa remoção; aparentemente já aconteceu sem atualização da documentação correspondente.

### Testes (visão consolidada)
- Backend: 1 arquivo de teste (`InvitationServiceTest`), sem testes de controller, repository ou de turnos/IA/segmentos.
- Frontend: 2 arquivos de teste, ambos sobre tema (`ThemeToggle`, `theme.ts`).
- Nenhum teste end-to-end em nenhum lugar do repositório.

---

## 5. Funcionalidades confirmadas como implementadas

| Funcionalidade | Estado | Evidência | Arquivos principais | Observações |
|---|---|---|---|---|
| Registro/login username+senha | Concluída | `AuthController`, Argon2id via Spring Security | `backend/.../auth/controllers/AuthController.java`, `AuthService` | — |
| Parties + papéis (`OWNER`/`NARRATOR`/`PLAYER`) | Concluída | `PartyEntity`, `PartyMemberEntity`, `PartyAccessService` | `backend/.../party/**` | — |
| Convite reutilizável por party | Concluída | Migration `V3`, `PartyInvitationLinkEntity`, `InvitationService` | `backend/.../invitation/**`, `frontend/app/app/party/[partyId]/page.tsx` | Substituiu o modelo antigo de convite único; documentação antiga (já removida) ainda descrevia o modelo velho. |
| Crônicas escritas + lock de edição 24h | Concluída | `WrittenStoryDocumentEntity`, `WrittenChronicleService` | `backend/.../chronicle/**` | — |
| Crônicas-jogo (turnos, ciclos 1–3, ordem embaralhada) | Concluída | `GameRunEntity`, `GameTurnEntity`, `GameChronicleService.create` | `backend/.../chronicle/services/GameChronicleService.java` | Ordem: criador primeiro, depois embaralhado, repetido por ciclo. |
| Expiração/skip de turno | Concluída | `expireTurns()` scheduled + `requireCurrentTurn` lazy | idem | — |
| Entrada de membro em jogo em andamento | Concluída | `insertPartyMemberIntoActiveRuns` | idem | Motivou a migration `V2` (constraint deferrable). |
| Segmentos ocultos / revelação do narrador / moderação | Concluída | `GameSegmentEntity`, `SegmentRevisionEntity`, `toHiddenResponse/toPublicDisabledResponse` | idem | Revelação de 10s é regra de frontend; backend trata dado revelado como intencional. |
| Fila de jobs de IA (geração da história final) | Concluída | `AiJobEntity`, `AiJobStateService`, `AiJobProcessor`, `OpenAiClient` | `backend/.../aijob/**`, `shared/integrations/OpenAiClient.java` | Só existe para crônicas-jogo, não para crônicas escritas. |
| Tema Arcane Editorial Nocturne (claro/escuro/sistema) | Concluída | Ver §4 (Frontend) | `frontend/app/globals.css`, `frontend/lib/theme*.ts`, `frontend/components/Theme*.tsx` | Não reimplementar. |
| Extração automática de tags | Ausente | Grep sem ocorrências | — | Greenfield. |
| Mini-história manual | Ausente | Grep sem ocorrências | — | Greenfield. |
| Votos em histórias (backend/frontend) | Ausente | Grep sem ocorrências, sem tabela | — | Greenfield. |
| Lista, ranking e badges | Ausente | Grep sem ocorrências | — | Greenfield; depende de votos. |
| Votos em tags (backend/frontend) | Ausente | Grep sem ocorrências | — | Greenfield; depende de extração de tags. |
| PWA instalável / manifest / service worker | Ausente | Sem `frontend/public/`, sem manifest, sem SW | — | Greenfield. |
| Notificações internas (backend) | Ausente | Sem entidade/tabela/serviço | — | Greenfield. |
| Assinaturas Web Push + VAPID | Ausente | Sem entidade/config | — | Greenfield; depende de PWA. |
| Página da história finalizada (estado dedicado) | Parcial | Mesmo endpoint de detalhe serve todos os status; sem rota pública de compartilhamento | `frontend/app/app/party/[partyId]/chronicle/[chronicleId]/page.tsx` | Requer validação manual/visual para confirmar completude desejada pelo produto. |
| E-mail (Resend) / WhatsApp Business (envio automático) | Ausente no backend / Legada no frontend | Grep sem ocorrências no backend; botão WhatsApp ainda ativo no frontend | `frontend/components/WhatsAppIcon.tsx` | Mantido deliberadamente no frontend por decisão do utilizador (§12) — não é lacuna, é escolha de produto. |
| `/dev-preview` (sandbox de FE sem backend) | Reestruturada neste console | Ver §12 | `.claude/commands/fe-preview.md`, `frontend/CLAUDE.md` | Deixou de ser rastreada pelo git; recriável sob demanda pelo comando `/fe-preview`. |

---

## 6. Roadmap pendente auditado

Lista original fornecida pelo utilizador, cruzada com o estado real encontrado:

1. **Auditoria geral do projeto** — Executada neste console (console nº1). Ver §12.
2. **Página da história finalizada** — Estado real: parcial (ver tabela §5). Sem dependências bloqueantes; pode ser refinada agora. Nenhuma migration esperada. Reutiliza `GameChronicleService.detail()`/`WrittenChronicleService` e os componentes `GameView`/`WrittenView` já existentes.
3. **Extração automática de tags** — Ausente, greenfield. Depende de dado de IA já existente (`generated_stories`); provavelmente precisa de nova coluna/tabela de tags e um novo passo no job de IA (reaproveitar `AiJobStateService`/`AiJobProcessor` como padrão). Migration nova esperada (`V4`).
4. **Mini-história manual** — Ausente, greenfield. Não depende de IA; é uma funcionalidade de produto isolada. Migration esperada se precisar de nova tabela.
5. **Votos em histórias (backend)** — Ausente, greenfield. Não depende de votos em tags. Reutilizar `PartyAccessService`/`ChronicleAccessService` para autorização. Migration nova esperada. Risco a evitar: referenciar `segment_id`/`turn_id` (estáveis), nunca `sequence_number` (renumerado em entradas/saídas de membros).
6. **Lista, ranking e badges** — Depende de votos em histórias (item 5) já existirem. Sem migration própria além da de votos, possivelmente uma projection/view para ranking.
7. **Votos em tags (backend)** — Depende de extração de tags (item 3) já existir. Independente de votos em histórias (item 5), mas mesmo padrão arquitetural (poderiam ser desenvolvidos em paralelo depois que tags existirem).
8. **Interface de votos nas tags** — Depende do item 7 (backend) estar pronto.
9. **Revisão integrada das funcionalidades** — Recomendado antes de iniciar PWA/notificações, para consolidar o núcleo de jogo/IA/votos antes de empilhar mais uma camada (push) sobre uma base ainda não validada ponta a ponta.
10. **Auditoria para PWA e Web Push** — Já parcialmente coberta por este documento (§4, §5); um console dedicado pode aprofundar detalhes de manifest/service worker específicos quando a implementação começar.
11. **PWA instalável** — Greenfield. Pré-requisito técnico de Web Push (item 13/16). Não depende de nenhuma outra feature da lista.
12. **Notificações internas (backend)** — Greenfield. Pode ser desenvolvida com o mesmo padrão de fila usado por `aijob` (`AiJobEntity`/`AiJobStateService`/`AiJobProcessor` como modelo estrutural). Migration nova esperada.
13. **Assinaturas Web Push e VAPID** — Depende de PWA instalável (item 11) para existir um service worker registrado. Migration nova esperada (tabela de subscriptions).
14. **Ativação de notificações — frontend** — Depende dos itens 12 e 13 (backend) existirem.
15. **Eventos do jogo e notificações** — Pontos de gancho já identificados no código: `GameChronicleService.advance()`/`completeRun()`, `InvitationService.acceptForUser`. Depende de notificações internas (item 12) existirem como infraestrutura.
16. **Worker de Web Push** — Depende de assinaturas Web Push (item 13) e notificações internas (item 12). Pode espelhar `AiJobProcessor` como padrão de scheduled dispatcher.
17. **Preferências e aviso de expiração** — Depende de notificações (item 12) e Web Push (item 13/16) existirem.
18. **Central de notificações** — Depende de notificações internas (item 12) existirem; é majoritariamente frontend consumindo endpoints já definidos no item 12.
19. **Revisão final de PWA, segurança e documentação** — Deve vir por último, depois que PWA/push/notificações estiverem implementados; é o momento de tratar cabeçalhos de segurança (CSP/HSTS) que hoje não existem em lugar nenhum do projeto.

**Sequência tecnicamente recomendada** (não segue a ordem numérica acima, pois há dependências reais): 1 (feito) → 9 → 2 → 3 → 4 → 5 e 7 (podem ser paralelos entre si) → 6 e 8 → 10 → 11 → 12 e 13 (podem ser paralelos) → 14, 15, 16, 17, 18 → 19.

---

## 7. Mapa de domínio

- **Parties**: `PartyEntity` (tabela `parties`), papéis `PartyRoleType{OWNER,NARRATOR,PLAYER}`, status `MemberStatusType{ACTIVE,DISABLED,REMOVED}` em `PartyMemberEntity` (tabela `party_members`). Serviço: `PartyService`, `PartyAccessService`.
- **Memberships**: mesma entidade acima. `DISABLED` é reversível; `REMOVED` é permanente para o vínculo (requer novo convite).
- **Invitations**: `PartyInvitationLinkEntity` (tabela `party_invitation_links`, PK = `party_id`, um link ativo por party). Serviço: `InvitationService`. Tabela antiga `party_invites` (V1) é órfã, sem entidade JPA.
- **Chronicles**: `ChronicleEntity` (tabela `chronicles`), tipos `GAME`/`WRITTEN`. Serviços: `ChronicleService` (genérico/listagem), `GameChronicleService`, `WrittenChronicleService`.
- **Turns**: `GameTurnEntity` (tabela `game_turns`), `GameRunEntity` (tabela `game_runs`), `GameDraftEntity` (tabela `game_drafts`). Fonte autoritativa: `GameChronicleService`.
- **Segments**: `GameSegmentEntity` (tabela `game_segments`), `SegmentRevisionEntity` (tabela `segment_revisions`, auditoria de edição/desabilitação).
- **AI**: `AiJobEntity` (tabela `ai_jobs`), `GeneratedStoryEntity` (tabela `generated_stories`). Serviços: `AiJobService`, `AiJobStateService`, `AiJobProcessor`, `OpenAiClient`.
- **Votes**: nenhuma entidade/tabela/serviço existe — greenfield.
- **Notifications**: nenhuma entidade/tabela/serviço existe — greenfield. `audit_events` (V1) é uma tabela órfã que parece ter sido uma tentativa abandonada de outbox genérico, sem nenhum código Java associado.

---

## 8. Migrations existentes

| Arquivo | Propósito | Tabelas | Dependências | Observações |
|---|---|---|---|---|
| `V1__initial_schema.sql` | Schema inicial completo | `users`, `parties`, `party_members`, `party_invites`, `chronicles`, `written_story_documents`, `written_story_permissions`, `game_runs`, `game_turns`, `game_drafts`, `game_segments`, `segment_revisions`, `generated_stories`, `ai_jobs`, `audit_events` | Nenhuma (base) | Contém um bloco `DO $$ ... IF NOT EXISTS ... END $$` para adicionar a FK `chronicles.current_generated_story_id → generated_stories.id` de forma idempotente (FK circular entre `chronicles` e `generated_stories`) — precedente a seguir em migrations futuras que toquem `chronicles`. `party_invites` e `audit_events` são órfãs (sem entidade JPA). |
| `V2__game_turn_sequence_deferrable.sql` | Torna `uq_game_turn_sequence` (`game_turns`, colunas `run_id, sequence_number`) `DEFERRABLE INITIALLY DEFERRED` | `game_turns` (alter) | Depende de `V1` | Necessária para permitir renumeração em massa de `sequence_number` quando membros entram/saem de jogos em andamento (`insertPartyMemberIntoActiveRuns`/`removePartyMemberFromActiveRuns`). |
| `V3__party_invitation_links.sql` | Cria o modelo de convite reutilizável por party | `party_invitation_links` (nova) | Depende de `V1` (`parties`, `users`) | Coexiste com a tabela antiga `party_invites` (V1), que ficou órfã mas não foi removida. |

**Próximo número seguro para novas migrations**: `V4__*.sql`. Nenhuma migration foi criada neste console.

**Risco de compatibilidade a observar**: qualquer migration futura que remova/renomeie `party_invites` ou `audit_events` deve primeiro confirmar (com o utilizador) se essas tabelas têm dados em produção que precisam ser preservados ou migrados, já que não há evidência de quando ou se elas deixaram de ser escritas.

---

## 9. Decisões técnicas existentes

| Decisão | Fonte | Impacto | Estado |
|---|---|---|---|
| Cada party tem exatamente um link de convite reutilizável, regenerável por narrador ativo ou dono | `backend/CLAUDE.md` (Domain Invariants), confirmado em `PartyInvitationLinkEntity`/`InvitationService` | Substitui modelo de convite único; simplifica UX de convite | Confirmado, em produção |
| Token bruto do convite é persistido (não só o hash) para redisplay; `token_hash` é usado na resolução pública | `backend/CLAUDE.md`, Javadoc de `PartyInvitationLinkEntity` | Trade-off de segurança deliberado e documentado | Confirmado |
| Aplicação deve iniciar sem `OPENAI_API_KEY`; jobs automáticos ficam pendentes, regeneração explícita retorna 503 `ai_not_configured` | `backend/CLAUDE.md`, `AiJobService`/`OpenAiClient` | Backend nunca quebra por falta de chave de IA | Confirmado |
| `DISABLED` é reversível, `REMOVED` é permanente para o vínculo | `backend/CLAUDE.md`, `PartyMemberEntity`/`MemberStatusType` | Fonte de verdade para reativação vs. novo convite | Confirmado |
| `sequence_number` não é estável (é renumerado); features futuras devem referenciar `segment_id`/`turn_id` | Inferido do código (`V2` + `insertPartyMemberIntoActiveRuns`) | Restrição de design para votação/qualquer feature que referencie um turno/segmento específico | Confirmado no código, decisão a ser respeitada por features novas |
| WhatsApp/Resend removidos do backend | Confirmado por grep (zero ocorrências) | Nenhum código morto de integração externa no backend | Confirmado |
| Botão de WhatsApp permanece no frontend, sem alteração | Decisão do utilizador neste console (§12) | Reverte a recomendação de um documento antigo (já removido) que sugeria remover também do frontend | Decisão confirmada nesta sessão |
| Infraestrutura real é Vercel (frontend) + Cloudflare Tunnel (backend), não Render | Confirmado pelo utilizador neste console; corrigido em `SETUP-PTBR.md` e `render.yaml` removido | Documentação agora reflete a realidade de deploy | Confirmado nesta sessão |
| `/dev-preview` deixa de ser commitado; recriável sob demanda pelo comando `/fe-preview` | Decisão do utilizador neste console (§12) | Padroniza testes visuais de frontend sem backend, sem risco de ir para produção | Confirmado nesta sessão |

---

## 10. Riscos e dívidas técnicas

**Crítico**: nenhum identificado neste console.

**Alto**:
- Cobertura de testes do backend praticamente inexistente (1 arquivo de teste em todo o projeto) — área de turnos/ciclos/IA é a mais rica em invariantes de negócio e a menos testada. *(Testes)*
- Ausência de rate limiting em endpoints públicos sensíveis (login, registro, resolução/aceite de convite). *(Segurança)*

**Médio**:
- Ausência de cabeçalhos de segurança (CSP, HSTS, X-Frame-Options) em toda a aplicação. *(Segurança)*
- Tabelas órfãs `audit_events` e `party_invites` sem entidade JPA correspondente — dívida de schema, risco de confusão em migrations futuras. *(Dados/Arquitetura)*
- `application-local.yml` local (não commitado) contém segredos em texto simples no disco do desenvolvedor — recomenda-se rotação por precaução. *(Segurança)*
- Cobertura de testes do frontend limitada a tema; `lib/api.ts`, `lib/auth.ts`, `AppShell` e páginas sem nenhum teste. *(Frontend/Testes)*

**Baixo**:
- Ausência de `Clock` bean no backend (todo timestamp é `Instant.now()` direto), dificultando testes determinísticos de expiração/turnos. *(Arquitetura)*
- Sem `ApplicationEventPublisher`/outbox genérico — qualquer notificação futura precisará ser um novo padrão de fila (reaproveitando a forma de `aijob`) ou aumentar acoplamento entre services via chamada direta. *(Arquitetura)*
- Documentação de infraestrutura estava desatualizada (Render) até este console; corrigida agora, mas vale conferir se outros documentos futuros (fora deste repositório) ainda referenciam Render. *(Infraestrutura)*

---

## 11. Perguntas em aberto

- Onde exatamente o processo do backend roda hoje (VPS própria, servidor doméstico, outro provedor) por trás do Cloudflare Tunnel? O utilizador confirmou o uso de Cloudflare, mas não o host subjacente nem um arquivo de configuração `cloudflared` versionado no repositório.
- As tabelas órfãs `party_invites` e `audit_events` têm dados em produção que precisam ser preservados, migrados ou podem ser dropados com segurança em uma migration futura?
- Existe algum SLA/expectativa de disponibilidade para o backend que deveria influenciar decisões de infraestrutura (ex.: monitoramento, health checks automatizados além do `/actuator/health` já exposto)?
- A "página da história finalizada" (item 2 do roadmap) tem um design/spec definido além do que já existe hoje, ou o próximo console deve começar por levantar requisitos de produto antes de codificar?

---

## 12. Histórico dos consoles

### Console nº1 — Auditoria geral inicial + limpeza de infraestrutura/documentação

- **Data**: 2026-07-30.
- **Objetivo original**: auditoria geral somente leitura do repositório e inicialização deste arquivo.
- **Objetivo ampliado durante o console**: o utilizador, após revisar o plano, autorizou explicitamente ações adicionais de limpeza (fora do escopo somente-leitura original): remoção de `render.yaml`, atualização de `SETUP-PTBR.md` para refletir Vercel + Cloudflare Tunnel (em vez de Render), absorção do conteúdo relevante de 3 documentos legados neste arquivo seguida da exclusão deles, e reestruturação da rota `/dev-preview` do frontend.
- **Arquivos lidos**: `CLAUDE.md` (raiz), `backend/CLAUDE.md`, `README.md` (raiz, backend, frontend), `PROJECT-STATUS.md`, `CODEBASE-AUDIT-PRE-IMPLEMENTATION.md`, `PWA-PUSH-AUDIT.md`, `SETUP-PTBR.md`, `render.yaml`, `.gitignore` (raiz), `backend/pom.xml` (via exploração), `backend/src/main/resources/application.yml` (via exploração), migrations `V1`/`V2`/`V3`, `frontend/package.json` (via exploração), `frontend/app/dev-preview/page.tsx`, e leitura ampla de código-fonte de `backend/src/main/java/com/narrativeplatform/**` e `frontend/app`, `frontend/components`, `frontend/lib` via 3 agentes de exploração somente leitura.
- **Arquivos alterados/criados/apagados**:
  - Criado: `docs/PROJECT_AUDIT.md` (este arquivo).
  - Apagado: `render.yaml` (raiz).
  - Apagados (conteúdo relevante absorvido acima): `CODEBASE-AUDIT-PRE-IMPLEMENTATION.md`, `PWA-PUSH-AUDIT.md`, `PROJECT-STATUS.md`.
  - Modificado: `SETUP-PTBR.md` (Render → Cloudflare Tunnel).
  - Modificado: `.gitignore` (raiz) — adicionada entrada para `frontend/app/dev-preview/`.
  - Removido do tracking do git: `frontend/app/dev-preview/page.tsx`.
  - Criado: `frontend/CLAUDE.md`.
  - Criado: `.claude/commands/fe-preview.md`.
- **Conclusões**: ver §3 (resumo executivo), §5 (funcionalidades) e §10 (riscos). O núcleo de jogo/IA está maduro; votação, PWA/push e notificações são greenfield; a maior dívida transversal é a quase total ausência de testes de backend.
- **Testes executados**: nenhum (console de auditoria/documentação; execução de Maven/Java/testes não autorizada neste console).
- **Testes não executados**: toda a suíte de backend (Maven) e frontend (`npm run lint`/`npm run build`/`vitest`) — não executados neste console.
- **Próximo passo**: ver §13.

---

## 13. Próximo console recomendado

Recomenda-se que o próximo console execute o item **9 da lista original (revisão integrada das funcionalidades)** antes de iniciar qualquer feature nova. Justificativa: o núcleo de jogo (turnos, ciclos, segmentos, IA) tem quase nenhuma cobertura de teste automatizado e é a parte mais rica em invariantes de negócio do projeto; validar esse núcleo ponta a ponta (manualmente e/ou com testes novos) antes de empilhar votação, PWA e notificações reduz o risco de construir sobre uma base não verificada. Isso não segue estritamente a ordem numérica original da lista (que colocaria "página da história finalizada" em seguida), mas a dependência técnica real justifica essa priorização — ver a sequência recomendada completa em §6.

Alternativa aceitável, se o utilizador preferir progresso de produto visível antes de revisão técnica: iniciar diretamente pelo item 2 (página da história finalizada), que não tem dependências bloqueantes e reaproveita componentes já existentes.

---

Todo console futuro deve começar lendo: (1) todos os `CLAUDE.md` aplicáveis; (2) `docs/PROJECT_AUDIT.md` (este arquivo) integralmente; (3) o estado atual do Git.
