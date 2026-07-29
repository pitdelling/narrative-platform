# Identidade Visual — Arcane Editorial (Claro / Escuro)

Este documento lista todas as cores usadas no frontend (`frontend/app/globals.css`), tanto no modo claro (padrão) quanto no modo escuro, no formato `#hex (nome) -> uso`. As variáveis `-rgb` são a mesma cor em formato `r,g,b`, usadas quando o CSS precisa aplicar transparência (`rgba(var(--x-rgb), .3)` etc.) em vários níveis de opacidade diferentes.

Cada cor tem exatamente um papel visual em cada tema — quando o papel muda de significado entre claro e escuro (ex.: `--navy` é fundo de botão, não cor de título), isso está anotado.

---

## 1. Modo claro (padrão, tema "Arcane Editorial Linen")

### Fundos e superfícies
- `#f1e4ca` (linho) `--linen` -> fundo geral da página.
- `#e5d3b2` (pergaminho) `--parchment` / `--parchment-rgb: 229,211,178` -> painéis recuados: barra lateral, trilha do seletor segmentado, fundo do selo de status, aviso de última mensagem, checkbox de permissões de edição, explicação de membership, barra mobile.
- `#fbf4e7` (marfim) `--surface` / `--surface-rgb: 251,244,231` -> fundo de cards e diálogos (`.card`, modal de conta, inputs/textarea/select).

### Texto
- `#2b2926` (tinta) `--ink` -> texto principal do corpo (parágrafos, textos de carregamento).
- `#686158` (marrom acinzentado) `--muted` -> texto secundário, legendas, texto de ajuda.
- `#102b52` (azul-marinho) `--heading` -> títulos (h1–h3), rótulos de formulário, marca, botão de texto, links de "voltar", ícones do menu lateral. *(Papel de texto — separado do papel de fundo abaixo mesmo tendo o mesmo valor no claro.)*

### Cor de destaque sólida (blocos)
- `#102b52` (azul-marinho) `--navy` -> fundo de blocos de destaque: botão primário, item ativo do menu, opção ativa do seletor segmentado. Sempre combinado com texto marfim por cima.
- `#091c37` (azul-marinho profundo) `--navy-deep` -> hover do botão primário.
- `#fbf4e7` (marfim) `--ivory` -> cor do texto sobre os blocos de destaque acima (nunca muda entre temas, pois sempre fica sobre um fundo escuro).

### Acentos decorativos
- `#c29042` (dourado) `--gold` / `--gold-rgb: 194,144,66` -> anel da marca, divisor celestial, borda de card em destaque, textura do ícone de crônica, linha conectora da thread, borda do marcador de turno.
- `#a76d3d` (cobre) `--copper` / `--copper-rgb: 167,109,61` -> texto de destaque secundário: "eyebrow" (rótulo pequeno em maiúsculas), metadado da party, status "em andamento"/"processando IA", fundo do aviso de bloqueio de edição.
- `#7665a7` (violeta) `--violet` / `--violet-rgb: 118,101,167` + `#57477f` (violeta escuro) `--violet-strong` -> selo de status "publicado" (fundo violeta claro translúcido + texto violeta escuro).
- `#627a66` (verde) `--green` -> mensagens de sucesso.
- `#9a4f47` (terracota/vermelho) `--danger` / `--danger-rgb: 154,79,71` -> mensagens de erro, contorno de botão perigoso, borda do aviso de ação.

### Bordas, sombra e foco
- `rgba(151,112,58,.22)` (marrom-dourado translúcido) `--border` / `--border-rgb: 151,112,58` -> bordas finas padrão de cards, inputs, divisórias.
- `rgba(49,38,24,.08)` `--shadow-rgb: 49,38,24` -> base da sombra de elevação de cards.
- `rgba(9,28,55,.x)` `--overlay-rgb: 9,28,55` -> fundo escurecido de modais e do menu mobile (invariável entre os dois temas, pois um "véu" por trás de um diálogo deve continuar escuro nos dois casos).
- `rgb(16,43,82)` `--emphasis-rgb: 16,43,82` -> anel de foco de inputs e destaque do compositor de turno ativo (`.turn-composer`), veias decorativas do "fragmento oculto" e do card "em construção".

### Estados neutros (cinza — nunca mudam de família de cor)
- `#777777` (cinza) `--disabled-fg`, `rgba(186,183,175,.65)` `--disabled-bg`, `90,90,90` `--disabled-rgb` -> segmento desabilitado pelo narrador (mantém o filtro `grayscale` já existente).
- `#666666` (cinza) `--neutral-fg`, `#d7d5cf` `--neutral-bg`, `80,80,80` `--neutral-rgb` -> turnos pulados ou expirados.
- `rgba(100,100,100,.055)` `--removed-wash` -> fundo esmaecido da linha de um membro removido.

---

## 2. Modo escuro — "Arcane Editorial Nocturne"

### Fundos e superfícies
- `#0d1117` (quase preto, tom azulado) `--linen` -> fundo geral da página.
- `#121923` (azul-ardósia bem escuro) `--parchment` / `--parchment-rgb: 18,25,35` -> mesmos painéis recuados do modo claro (barra lateral a 92% de opacidade, trilha do seletor, selo de status, etc.).
- `#1a2432` (ardósia) `--surface` / `--surface-rgb: 26,36,50` -> fundo de cards e diálogos (a 96% de opacidade), com um leve brilho interno de 1px (`inset 0 1px 0 rgba(255,255,255,.025)`) para dar sensação de superfície elevada.

### Texto
- `#e8e3da` (marfim acinzentado) `--ink` -> texto principal do corpo.
- `#aeb7c3` (azul-acinzentado) `--muted` -> texto secundário.
- `#f4ede2` (marfim quase branco) `--heading` -> títulos e rótulos — o mais claro de todos os tons de texto, para manter a hierarquia título > corpo > secundário.

### Cor de destaque sólida (blocos)
- `#315f95` (azul-marinho claro) `--navy` -> fundo dos mesmos blocos de destaque (botão primário, nav ativa, opção ativa do seletor).
- `#3d73ad` (azul-marinho ainda mais claro) `--navy-deep` -> hover do botão primário — continua mais claro que o `--navy` base, mesma lógica do claro invertida.
- `#fff8e9` (marfim) `--ivory` -> texto sobre os blocos acima.

### Acentos decorativos (clareados para manter contraste no fundo escuro)
- `#d7a95c` (dourado) `--gold` / `--gold-rgb: 215,169,92`.
- `#c98255` (cobre/terracota) `--copper` / `--copper-rgb: 201,130,85`.
- `#9c8bd8` (violeta) `--violet` / `--violet-rgb: 156,139,216` + `#d2c9f2` (lavanda) `--violet-strong` -> selo "publicado".
- `#82bd92` (verde) `--green` -> sucesso.
- `#e0827b` (vermelho salmão) `--danger` / `--danger-rgb: 224,130,123` -> erro.

### Bordas, sombra e foco
- `rgba(166,184,204,.18)` (azul-acinzentado translúcido) `--border` / `--border-rgb: 166,184,204` -> a 24% de opacidade nos inputs em repouso, 38% no hover.
- `rgba(0,0,0,.30)` `--shadow-rgb: 0,0,0` -> sombra neutra preta, mais espalhada (`0 14px 38px`) que no claro.
- `rgba(3,6,10,.x)` `--overlay-rgb: 3,6,10` -> véu de modais/menu mobile, mais escuro que no claro (não é mais o mesmo valor fixo do azul-marinho profundo original — foi ajustado para um preto quase puro específico do tema escuro).
- `rgb(215,169,92)` `--emphasis-rgb: 215,169,92` -> anel de foco (agora usando `:focus-visible`) e destaque do compositor de turno ativo — mesma família do `--gold`, trocando de azul-marinho (claro) para dourado (escuro).

### Estados neutros (cinza-azulado — continuam neutros, só invertem claro/escuro)
- `#929ca8` `--disabled-fg`, `rgba(116,126,138,.22)` `--disabled-bg`, `146,156,168` `--disabled-rgb` -> segmento desabilitado pelo narrador.
- `#aab2bc` `--neutral-fg`, `#2a323d` `--neutral-bg`, `170,178,188` `--neutral-rgb` -> turnos pulados/expirados.
- `rgba(150,160,170,.07)` `--removed-wash` -> linha de membro removido.

### Refinamentos específicos do escuro (além da simples troca de token)
- Inputs/textarea/select ganham fundo próprio `#111822` (não é um token, é um valor fixo só para o tema escuro) e placeholder em `rgba(174,183,195,.66)`.
- Botões secundário/ghost, ações de thread e botões de fechar (`.modal-close`/`.sidebar-close`) ganham um hover próprio: fundo `rgba(var(--parchment-rgb),.72)` e borda `rgba(var(--border-rgb),.34)`.
- `.turn-composer` (o compositor de turno ativo) ganha um contorno e brilho dourados: `border-color: rgba(var(--gold-rgb),.52)` + sombra dupla (`0 0 0 1px` do dourado + a sombra padrão do tema).
- `.hidden-fragment-cover` (capa do fragmento velado) usa uma listra diagonal combinando `--parchment` e `--surface`, em vez de reaproveitar só o `--parchment-rgb`.
- **Os selos de arte do card de crônica (normalmente uma exceção "não reage ao tema", ver seção 3) recebem, no escuro, cores próprias**: `.art-1` fundo `#2a3442`/ícone `--gold`; `.art-2` fundo `#26372f`/ícone `--green`; `.art-3` fundo `#3c3156`/ícone `--ivory`.

---

## 3. Exceção decorativa — só no modo claro

Os três selos de "arte" do card de crônica têm fundo próprio, independente da página (como um avatar colorido), e no modo claro são fixos, sem variável de tema:
- `.art-1`: fundo `#eee2ca`, ícone `--navy`.
- `.art-2`: fundo `#dbe2dc`, ícone `--green`.
- `.art-3`: fundo `#75679b`, ícone `--ivory`.

No modo escuro ("Nocturne"), esses três selos deixaram de ser uma exceção — ganharam suas próprias cores de fundo escurecidas (ver seção 2, "Refinamentos específicos do escuro").

---

## 4. Como ler as variáveis `-rgb`

Sempre que uma cor precisa aparecer em mais de um nível de transparência no CSS (ex.: o dourado aparece a 55%, 35%, 28%, 16% de opacidade em lugares diferentes), existe uma variável irmã `--nome-rgb: r,g,b` guardando só os três números, usada assim: `rgba(var(--gold-rgb), .35)`. Isso evita repetir o mesmo hex várias vezes e garante que, ao trocar de tema, todas as variações de transparência daquela cor mudem juntas.
