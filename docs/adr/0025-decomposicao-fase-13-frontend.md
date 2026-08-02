# ADR-0025: Decomposição da Fase 13 (Frontend Web) e Arquitetura de Frontend

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-08-01 |
| Autor | CTO / Principal Software Architect |
| Fase | 13 (Frontend Web) — decomposta em 13.1 a 13.9; esta ADR cobre a decisão de decomposição, a arquitetura técnica de frontend e a Fase 13.1 (Fundação + Autenticação) |

## Contexto

roadmap.md tratava a Fase 13 como uma única entrada ("Frontend Web (MVP) — Interface para todas as funcionalidades acima"). Isso viola o princípio de granularidade que rege este roadmap desde o ADR-0006 (decomposição da Fase 2) e o ADR-0016 (decomposição da Fase 4): **uma fase deve ser pequena o suficiente para ser integralmente concluível** (CTO → Full Stack → QA → aprovação) antes que a próxima comece. O backend levou 12 fases numeradas (mais subfases) para cobrir a mesma superfície funcional que a Fase 13 propõe consumir de uma vez. Construir toda a interface em uma única entrega violaria esse princípio na mesma proporção que motivou as decomposições anteriores.

Adicionalmente, vision.md § 12 é explícito: *"Este documento não define stack tecnológica, linguagens, frameworks... essas decisões pertencem à fase de arquitetura técnica detalhada"* — nenhuma decisão de stack de frontend foi tomada até aqui. vision.md Seção 10.1 define apenas o papel conceitual da "Camada de Cliente": *"web responsiva no MVP... toda a lógica de negócio reside no backend; o cliente é uma camada de apresentação e interação"*.

Antes de desenhar a arquitetura de frontend, foi necessário auditar a superfície real de API exposta por `backend-java` (não presumir com base no vision.md ou no backend TypeScript legado) — a mesma disciplina de "nunca inventar funcionalidade de backend que não existe" já aplicada em todas as fases anteriores (RF-022, RF-028, RF-032, RF-043). Essa auditoria encontrou uma lacuna relevante:

- `backend-java` expõe apenas `POST /auth/register` e `POST /auth/login` (ver `AuthController`). **Não existem endpoints de refresh token, logout, recuperação de senha (RF-005), MFA (RF-004) ou edição de perfil (RF-006)** — esses requisitos foram implementados no backend TypeScript legado (`backend/`, Fases 1 e 2.1–2.5.2 originais), mas nunca migrados para `backend-java`, que começou diretamente na Fase 3 (Contas e Carteiras, ver ADR-0014), pulando deliberadamente a trilha de migração M1–M2.5.2 (ainda `⬜ Não iniciada`, ver roadmap.md).
- O token JWT emitido por `backend-java` expira em 15 minutos (`JwtTokenServiceAdapter.ACCESS_TOKEN_TTL`), sem qualquer mecanismo de renovação.
- `backend-java` não possui nenhuma configuração de CORS (`WebMvcConfig` só registra o `AuthenticationInterceptor`; não há `CorsConfiguration`/`@CrossOrigin` em lugar algum do código-fonte). Uma aplicação frontend servida de uma origem distinta (ex.: `http://localhost:5173` em desenvolvimento) seria bloqueada pelo navegador em toda chamada `fetch`, tornando o backend literalmente inacessível ao frontend sem essa peça.

## Decisão

### 1. A Fase 13 é decomposta em 13.1 a 13.9, uma subfase por área funcional já entregue pelo backend

Mesmo padrão de granularidade do ADR-0006/ADR-0016 — cada subfase entrega uma área de tela completa e utilizável antes de passar à próxima:

| Subfase | Escopo | Consome (backend-java) |
|---|---|---|
| 13.1 | Fundação técnica + Autenticação (registro, login, logout local, rotas protegidas, shell/layout) | `AuthController` |
| 13.2 | Contas e Categorias | `AccountController`, `CategoryController` |
| 13.3 | Transações | `TransactionController` |
| 13.4 | Orçamentos e Metas | `BudgetController`, `GoalController` |
| 13.5 | Dashboard e Pulse Score | `DashboardController` |
| 13.6 | Relatórios (incl. exportação CSV) | `ReportController` |
| 13.7 | Notificações e preferências | `NotificationController` |
| 13.8 | Privacidade/LGPD (exportação de dados, consentimento, exclusão de conta) | `PrivacyController`, `UserController` |
| 13.9 | Backoffice (suporte, suspensão/reativação, audit log) | `BackofficeController` |

Esta ADR decide a arquitetura técnica válida para todas as subfases e implementa 13.1. Cada subfase seguinte segue o mesmo ciclo completo de agentes (CTO não precisa reabrir esta ADR salvo decisão nova).

### 2. Lacuna de backend (RF-004, RF-005, RF-006) é explicitamente fora do escopo da Fase 13

A Fase 13 constrói interface apenas para o que `backend-java` realmente expõe. **Login não terá "esqueci minha senha", cadastro não terá MFA, e não haverá tela de edição de perfil** até que esses endpoints existam em `backend-java` — construí-los no frontend seria apresentar funcionalidade que não funciona, o mesmo erro que ADR-0022 evitou deliberadamente para RF-043 ("nem um tipo de alerta vazio foi anunciado para essa funcionalidade inexistente"). Migrar RF-004/005/006 para `backend-java` é trabalho de backend, não desta fase — fica registrado como pendência para quando a trilha M2.x for retomada (ver roadmap.md).

**Consequência direta para UX**: como não há refresh token, uma sessão expira sativamente 15 minutos após o login, sem aviso prévio ou renovação silenciosa. O frontend deve tratar todo `401` como "sessão expirada", limpar o estado local e redirecionar ao login com uma mensagem clara — não deve tentar mascarar essa limitação com lógica especulativa de renovação que não tem endpoint correspondente.

### 3. CORS é adicionado a `backend-java` como pré-requisito bloqueante da Fase 13

Sem CORS configurado, nenhuma subfase de frontend funciona contra o backend real — não é uma melhoria opcional, é uma peça faltante de infraestrutura de borda, análoga à fundação mínima de categoria que a Fase 4.1 precisou construir para desbloquear RN-002 (ADR-0016). Decisão: adicionar um `CorsConfigurationSource` mínimo em `backend-java`, permitindo as origens de desenvolvimento do frontend (`http://localhost:5173`, porta padrão do Vite), métodos `GET/POST/PUT/DELETE`, header `Authorization`/`Content-Type`, sem credenciais de cookie (a autenticação é via header `Authorization: Bearer`, nunca cookie — não há necessidade de `allowCredentials`). Configurável via propriedade (`financepulse.cors.allowed-origins`), mesma disciplina de `financepulse.jwt.secret` (nada hardcoded que precise mudar entre ambientes sem recompilar).

### 4. Stack de frontend

| Decisão | Escolha | Motivo |
|---|---|---|
| Linguagem/framework | **React 18 + TypeScript**, via **Vite** | Alinhado ao ecossistema mais maduro para SPA responsiva; Vite dá dev server rápido e build de produção sem configuração manual de bundler. Nenhuma dependência de SSR/framework full-stack (Next.js etc.) — vision.md não pede SEO nem renderização no servidor, e a API já existe separada (backend-java); um framework full-stack seria complexidade não justificada. |
| Roteamento | **React Router v6** | Padrão de fato para SPA em React; suporta rotas protegidas via componente wrapper sem biblioteca adicional. |
| Dados remotos | **TanStack Query** | Cache, invalidação e estados de loading/erro por requisição sem reimplementar isso à mão em cada tela — crítico dado o número de telas que a Fase 13 completa vai cobrir (13.2–13.9). |
| Formulários/validação | **React Hook Form + Zod** | Validação de formulário no cliente **não substitui** a validação do backend (que permanece a fonte de verdade) — apenas dá feedback imediato ao usuário, reduzindo round-trips desnecessários. Nenhuma regra de negócio é duplicada no cliente; os schemas Zod validam apenas forma de entrada (campo obrigatório, formato), nunca regras de domínio (ex.: limite de orçamento, threshold de meta). |
| Estilo | **CSS Modules**, sem biblioteca de componentes de terceiros | Evita dependência pesada de design system para um MVP; CSS Modules dá escopo local sem exigir build adicional além do que o Vite já oferece. Pode ser revisitado por uma ADR futura se a velocidade de desenvolvimento das subfases seguintes justificar um kit de componentes. |
| Cliente HTTP | `fetch` nativo, envolto em um módulo `api/httpClient.ts` único | Sem Axios — `fetch` é suficiente para o volume de chamadas do projeto; um único módulo concentra injeção do header `Authorization`, tratamento uniforme de `401` (logout + redirecionamento) e parsing de `ErrorResponse` (mesmo formato `{ error: string }` já usado por `GlobalExceptionHandler` no backend). |
| Testes | **Vitest + React Testing Library + MSW (Mock Service Worker)** | Ver seção 5 abaixo — decisão explicitamente ancorada no equivalente frontend de `rules.md` §3. |

### 5. Equivalente frontend de `rules.md` §3 (smoke test contra o composition root real)

`rules.md` §3 exige que o composition root real do backend seja exercitado por ao menos um teste de fumaça contra adaptadores de produção reais, não dublês. A tradução direta desse princípio para um frontend SPA: **o frontend não tem um banco de dados para substituir por um dublê — sua única fronteira externa real é a rede.** Portanto:

- Toda funcionalidade que envolve um fluxo completo de tela (ex.: "usuário preenche o formulário de login e é redirecionado ao dashboard") deve ter **pelo menos um teste de integração que renderiza os componentes React reais, usa os hooks reais (`useAuth`, `useMutation`/`useQuery` reais do TanStack Query), passa pelo `httpClient.ts` real, e intercepta a chamada apenas na camada de rede via MSW** — nunca mockando `useAuth` ou o hook de dados diretamente. MSW simula o backend real (mesmo formato de request/response documentado nos DTOs de `backend-java`), preservando a garantia de que o caminho de código real (parsing de resposta, tratamento de erro, atualização de estado, navegação) é exercitado de ponta a ponta.
- Testes de componente isolado (um botão, um campo validando formato) continuam podendo usar dublês/props diretas — a exigência acima é para o fluxo completo de cada tela, não para cada unidade.
- Esta regra é adicionada como uma cláusula nova em `rules.md` §3 (formalização exigida por `rules.md` §6, já que é uma interpretação estrutural que vale para todas as subfases seguintes, não uma exceção pontual).

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Construir a Fase 13 inteira como uma entrega única | Viola a granularidade "uma fase = totalmente concluível" que motivou o ADR-0006/ADR-0016; o volume de telas é maior que qualquer fase de backend já entregue. |
| Next.js (ou outro framework full-stack/SSR) | Nenhum requisito de SEO ou renderização no servidor; a API já existe e é separada — SSR adicionaria complexidade operacional (servidor Node adicional) sem benefício correspondente para este produto. |
| Redux/Zustand para estado global | TanStack Query já cobre o estado de dados remotos (a maior parte do estado da aplicação); o estado de autenticação é pequeno o suficiente para um React Context simples. Adicionar uma biblioteca de estado global geral seria abstração prematura. |
| Axios como cliente HTTP | `fetch` nativo do navegador é suficiente; Axios adicionaria uma dependência sem necessidade técnica correspondente (nenhum recurso do projeto depende de interceptors globais complexos ou compatibilidade com Node antigo). |
| Construir telas de MFA/recuperação de senha/perfil reaproveitando o backend TypeScript legado (`backend/`) | Misturaria dois backends distintos no mesmo frontend, cada um com seu próprio esquema de autenticação/token — complexidade arquitetural desproporcional. A decisão correta é registrar a lacuna e aguardar a migração desses RFs para `backend-java`, não contorná-la no frontend. |
| Adiar a configuração de CORS para quando um proxy reverso de produção existir, usando apenas o proxy de dev do Vite | Resolveria desenvolvimento mas deixaria a Fase 13 sem um caminho de produção funcional documentado, e um proxy de dev mascarar um problema real de infraestrutura não é a mesma disciplina já aplicada a outras lacunas (o CORS real precisa existir eventualmente; mais barato resolver agora, com escopo mínimo, que redescobrir depois). |

## Consequências

- `roadmap.md` passa a listar 13.1–13.9 em vez de uma única linha "Fase 13".
- `backend-java` ganha uma configuração de CORS mínima — pequena mudança de infraestrutura de borda, não uma nova funcionalidade de produto; QA da Fase 13.1 deve verificar que ela não relaxa nenhuma outra garantia de segurança (seguem exigidos `Authorization: Bearer`, sem cookies, sem `allowCredentials`).
- `rules.md` §3 ganha uma cláusula formalizando o padrão de teste de frontend (MSW na fronteira de rede, componentes/hooks reais) — vale para 13.1 e todas as subfases seguintes.
- MFA (RF-004), recuperação de senha (RF-005) e edição de perfil (RF-006) permanecem sem interface até que `backend-java` os implemente — pendência explícita, não lacuna silenciosa.
- Sessão expira em 15 minutos sem renovação — limitação herdada do backend, tratada no frontend apenas como "detectar e redirecionar", não "disfarçar".
