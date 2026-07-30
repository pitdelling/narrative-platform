# Guia de teste local e publicação gratuita

## Tempo estimado

| Etapa | Tempo médio |
|---|---:|
| Criar Supabase e copiar a conexão | 10–20 min |
| Importar e configurar o backend no IntelliJ | 15–30 min |
| Instalar e executar o frontend | 10–20 min |
| Testar convites, turnos e bloqueios | 30–60 min |
| Enviar ao GitHub | 5–15 min |
| Publicar Cloudflare Tunnel + Vercel | 20–40 min |

Na primeira configuração, reserve aproximadamente **1h30 a 3h**. Depois disso, novos deploys são automáticos pelo GitHub.


## 1. Pré-requisitos

- IntelliJ IDEA com suporte a Maven e Java 21.
- Node.js 20.9 ou superior.
- Uma conta Supabase para PostgreSQL.
- Uma conta GitHub.
- Opcional: chave da API OpenAI.

O backend foi deliberadamente preparado para ser executado pelo IntelliJ. Não é necessário executar Maven ou Java no terminal.

## 2. Criar o banco PostgreSQL no Supabase

1. Crie um projeto Supabase separado do protótipo anterior.
2. No painel do projeto, clique em **Connect**.
3. Para o backend local, use a conexão direta se sua rede suportar IPv6. Caso contrário, copie a conexão **Session pooler**.
4. Separe os valores da conexão para preencher as variáveis abaixo.

A URL JDBC precisa ficar parecida com:

```text
jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require
```

O usuário do pooler costuma parecer:

```text
postgres.REFERENCIA_DO_PROJETO
```

Não execute manualmente o SQL da pasta de migração. O Flyway cria e atualiza as tabelas quando o backend inicia.

## 3. Configurar o backend

No IntelliJ:

1. Abra a pasta `backend` como projeto Maven.
2. Aguarde o IntelliJ importar as dependências.
3. Abra **Run > Edit Configurations**.
4. Crie uma configuração **Spring Boot** usando `NarrativePlatformApplication`.
5. Adicione estas variáveis de ambiente:

```text
DATABASE_URL=jdbc:postgresql://HOST:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres.REFERENCIA
DATABASE_PASSWORD=SUA_SENHA
JWT_SECRET=COLOQUE_UMA_CHAVE_ALEATORIA_COM_PELO_MENOS_64_CARACTERES
FRONTEND_URL=http://localhost:3000
APP_PUBLIC_URL=http://localhost:3000
OPENAI_API_KEY=
OPENAI_MODEL=gpt-5.6-luna
```

6. Execute `NarrativePlatformApplication` pelo botão Run do IntelliJ.
7. Confirme no console que o Flyway aplicou `V1__initial_schema.sql`.
8. Abra `http://localhost:8080/actuator/health` e confirme `UP`.

Sem `OPENAI_API_KEY`, as histórias terminadas continuam no estado `AI_PENDING`. Depois de configurar a chave e reiniciar o backend, o worker as processa automaticamente. O botão de regenerar fica disponível para histórias já publicadas ou que falharam.

## 4. Configurar o frontend

Na pasta `frontend`, copie `.env.example` para `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_NAME=Narrative Platform
```

No terminal da pasta `frontend`:

```powershell
npm install
npm run dev
```

Abra `http://localhost:3000`.

## 5. Roteiro mínimo de teste

1. Registre uma conta de narrador.
2. Crie uma party.
3. Gere dois ou mais convites individuais.
4. Abra cada convite em uma janela anônima e crie contas de jogador.
5. Crie uma história-jogo com um ciclo.
6. Confirme que o iniciador consegue escrever primeiro.
7. Em outra conta, confirme que os blocos anteriores chegam sem conteúdo quando ainda não é a vez dela.
8. Salve um rascunho, limpe-o e salve novamente.
9. Publique e confirme que o turno avança.
10. Teste o skip pelo participante atual e pelo narrador.
11. No perfil do narrador, use revelar; a interface esconde novamente após 10 segundos.
12. Termine a ordem e confirme o estado `AI_PENDING` ou `PUBLISHED`, dependendo da configuração da API.
13. Desabilite um segmento, informe o motivo e regenere.
14. Crie uma história escrita, dê permissão a um jogador e adquira o bloqueio de edição.
15. Em outra conta autorizada, confirme a mensagem indicando quem está editando.

## 6. Build do frontend

```powershell
npm run lint
npm run build
```

## 7. Build do backend pelo IntelliJ

Não use Maven no terminal.

1. Abra o painel **Maven**.
2. Selecione `Lifecycle > clean`.
3. Selecione `Lifecycle > test`.
4. Selecione `Lifecycle > package`.
5. Ou use **Build > Build Project** para uma verificação rápida.

## 8. Enviar o projeto ao GitHub

Crie um repositório vazio no GitHub. No terminal da pasta raiz do projeto:

```powershell
git init
git add .
git commit -m "Initial narrative platform MVP"
git branch -M main
git remote add origin URL_DO_REPOSITORIO
git push -u origin main
```

Confirme no GitHub que nenhum `.env.local`, senha de banco, JWT secret ou chave de API foi enviado.

## 9. Publicar o backend através de Cloudflare Tunnel

O backend não é hospedado em uma plataforma gerenciada (não usamos mais Render). Ele roda onde você decidir (servidor próprio, VPS etc.) e é exposto publicamente por um túnel Cloudflare (`cloudflared`), sem expor a porta diretamente.

1. Rode o backend normalmente (pelo IntelliJ, ou via o `backend/Dockerfile` empacotado) na máquina/servidor escolhido, escutando na porta `8080`.
2. Instale o `cloudflared` nessa máquina e autentique-o na sua conta Cloudflare (`cloudflared tunnel login`).
3. Crie um túnel nomeado (`cloudflared tunnel create SEU-TUNEL`) e associe um hostname público a ele (`cloudflared tunnel route dns SEU-TUNEL api.seudominio.com`).
4. Configure o túnel para apontar para o backend local, por exemplo em `~/.cloudflared/config.yml`:

```yaml
tunnel: SEU-TUNEL
credentials-file: /caminho/para/SEU-TUNEL.json
ingress:
  - hostname: api.seudominio.com
    service: http://localhost:8080
  - service: http_status:404
```

5. Rode o túnel (`cloudflared tunnel run SEU-TUNEL`), ou instale-o como serviço do sistema operacional para manter ativo entre reinícios.
6. Cadastre as variáveis de ambiente do backend com o domínio público do túnel:

```text
FRONTEND_URL=https://SEU-FRONTEND.vercel.app
APP_PUBLIC_URL=https://SEU-FRONTEND.vercel.app
```

7. Copie a URL pública do backend (o hostname configurado no túnel, ex.: `https://api.seudominio.com`).

Diferente de uma instância gratuita de PaaS, o backend atrás do túnel não hiberna por inatividade — a disponibilidade depende apenas da máquina onde ele roda e do próprio `cloudflared` estarem ativos.

## 10. Publicar o frontend gratuitamente na Vercel

1. Na Vercel, escolha **Add New > Project**.
2. Importe o mesmo repositório.
3. Defina `Root Directory` como `frontend`.
4. Configure:

```text
NEXT_PUBLIC_API_URL=https://api.seudominio.com/api
NEXT_PUBLIC_APP_NAME=Narrative Platform
```

5. Faça o deploy.
6. Atualize `FRONTEND_URL` e `APP_PUBLIC_URL` (variáveis do backend, passo 6 da seção 9) com a URL final da Vercel.

## 11. OpenAI

A chave fica somente no backend como `OPENAI_API_KEY`. Nunca crie uma variável `NEXT_PUBLIC_OPENAI_API_KEY`.

O modelo é configurável por `OPENAI_MODEL`. O valor padrão do projeto é um modelo econômico atual, mas pode ser alterado sem modificar código.

## 12. Convite da party

Cada party tem um único link de convite reutilizável, visível e regenerável apenas por um narrador ativo ou pelo dono da party (`GET /api/parties/{partyId}/invitation`, `POST /api/parties/{partyId}/invitation/regenerate`). O backend não envia email nem mensagem de WhatsApp automaticamente — compartilhar o link (copiar e colar onde quiser) é responsabilidade do narrador. Regenerar o link invalida o anterior imediatamente.


## 13. Checklist depois do deploy

1. Abra `https://api.seudominio.com/actuator/health` (hostname do seu túnel Cloudflare) e aguarde até retornar `UP`.
2. Abra o frontend da Vercel e registre uma conta de narrador.
3. Crie uma party e um convite individual.
4. Abra o convite em uma janela anônima e crie o primeiro jogador.
5. Confirme, nas variáveis de ambiente do backend, que `FRONTEND_URL` corresponde exatamente ao domínio da Vercel, sem barra final.
6. Confirme que a chave OpenAI existe apenas no ambiente do backend, nunca no frontend.
7. Confirme que o `cloudflared` está ativo (como serviço do sistema) na máquina do backend, para que o túnel sobreviva a reinícios.

## 14. Atualizações futuras

Depois de qualquer alteração:

1. Teste o backend pelo IntelliJ.
2. Execute `npm run lint` e `npm run build` no frontend.
3. Faça commit e `git push`.
4. A Vercel cria um novo deploy do frontend automaticamente. O backend, por não estar em uma plataforma gerenciada, precisa ser reiniciado manualmente (ou pelo processo/serviço que você configurou) na máquina onde roda; o túnel Cloudflare não precisa ser reconfigurado, pois continua apontando para a mesma porta local.

## Checklist das melhorias de navegação e fluxo

Depois de iniciar frontend e backend, valide:

1. Em largura menor que 900 px, a navegação completa fica em um menu sanduíche e a barra lateral compacta mantém atalhos para parties, arquivo atual e conta.
2. Dentro de uma party, `Todas as parties` volta ao painel principal; dentro de uma crônica, `Voltar às crônicas` volta ao arquivo da party.
3. O rodapé do menu e a janela de conta mostram o `@username` atual.
4. A troca de senha exige a senha atual e uma nova senha diferente com pelo menos oito caracteres.
5. O proprietário pode promover jogador a narrador e rebaixar narrador a jogador. Após transferir a propriedade, o antigo proprietário vira jogador.
6. `Desabilitar` permite reativação. `Remover` não permite reativação direta; a pessoa deve aceitar um novo convite.
7. Ao criar uma história-jogo, informe título, ciclos e o primeiro trecho no mesmo formulário. A história abre com o primeiro turno já publicado e a próxima pessoa ativa.
8. Na página da história-jogo, o editor aparece no topo apenas para a pessoa da vez; abaixo aparece o último trecho visível e, em seguida, a thread completa protegida.
9. Turnos pulados e expirados aparecem inteiramente em cinza.
10. Sem `OPENAI_API_KEY`, o backend inicia normalmente. Trabalhos automáticos ficam pendentes e uma regeneração explícita retorna a mensagem de IA não configurada.
11. Ao aceitar um convite ou reativar um membro desabilitado enquanto uma história-jogo ainda está em andamento, a pessoa entra automaticamente no fim do ciclo atual dessa história (e nos ciclos seguintes, na mesma ordem), sem precisar esperar uma nova história ser criada.
