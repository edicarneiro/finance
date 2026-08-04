# FinancePulse Engine — Frontend

Interface web responsiva do FinancePulse Engine (vision.md Seção 10.1 — "Camada de Cliente"), consumindo a API real de `backend-java`. Construída em fases (13.1 a 13.9, ver [ADR-0025](../docs/adr/0025-decomposicao-fase-13-frontend.md) e [roadmap.md](../roadmap.md)) — este README é atualizado a cada subfase concluída.

## Stack

- **React 18 + TypeScript**, via **Vite**.
- **React Router v6** — roteamento e rotas protegidas.
- **TanStack Query** — cache e estado de chamadas remotas.
- **React Hook Form + Zod** — validação de formulário no cliente (nunca substitui a validação do backend, apenas dá feedback imediato).
- **CSS Modules** — sem biblioteca de componentes de terceiros.
- **Vitest + React Testing Library + MSW** — testes (ver seção "Como testar").

Decisões e alternativas consideradas: [ADR-0025](../docs/adr/0025-decomposicao-fase-13-frontend.md).

## Arquitetura

```
src/
  api/
    httpClient.ts       # único módulo que fala com a rede: injeta Authorization, trata 401, parseia ErrorResponse
    authApi.ts           # funções tipadas para /auth/register e /auth/login
    accountsApi.ts        # funções tipadas para /accounts (Fase 13.2)
    categoriesApi.ts      # funções tipadas para /categories (Fase 13.2)
    transactionsApi.ts    # funções tipadas para /transactions (Fase 13.3)
    budgetsApi.ts          # funções tipadas para /budgets (Fase 13.4)
    goalsApi.ts             # funções tipadas para /goals (Fase 13.4)
    dashboardApi.ts          # funções tipadas para /dashboard (Fase 13.5)
    reportsApi.ts             # funções tipadas para /reports, incl. downloads CSV (Fase 13.6)
    notificationsApi.ts        # funções tipadas para /notification-preferences e /notifications (Fase 13.7)
    privacyApi.ts                # funções tipadas para /privacy/export, /privacy/consents e DELETE /users/me (Fase 13.8)
    backofficeApi.ts               # funções tipadas para /backoffice/users/{userId}/* (Fase 13.9)
    httpClient.test.ts
  auth/
    authContextInstance.ts  # o React Context em si (separado por causa do fast-refresh do React)
    AuthContext.tsx      # AuthProvider — estado de autenticação, login/registro/logout, reage a sessão expirada
    AuthContext.test.tsx
    useAuth.ts            # hook de consumo do contexto
  routes/
    ProtectedRoute.tsx        # redireciona para /login quando não autenticado
    AuthenticatedLayout.tsx    # renderiza AppShell + <Outlet/> para toda rota autenticada (Fase 13.2)
  pages/
    LoginPage.tsx / LoginPage.test.tsx
    RegisterPage.tsx / RegisterPage.test.tsx
    AuthForm.module.css
    AccountsPage.tsx / AccountsPage.test.tsx / AccountsPage.module.css   # Fase 13.2
    CategoriesPage.tsx / CategoriesPage.test.tsx / CategoriesPage.module.css   # Fase 13.2
    TransactionsPage.tsx / TransactionsPage.test.tsx / TransactionsPage.module.css   # Fase 13.3
    BudgetsPage.tsx / BudgetsPage.test.tsx / BudgetsPage.module.css   # Fase 13.4
    GoalsPage.tsx / GoalsPage.test.tsx / GoalsPage.module.css   # Fase 13.4
    DashboardPage.tsx / DashboardPage.test.tsx / DashboardPage.module.css   # Fase 13.5 — agora a rota "/" (HomePage removida)
    ReportsPage.tsx / ReportsPage.test.tsx / ReportsPage.module.css   # Fase 13.6
    NotificationsPage.tsx / NotificationsPage.test.tsx / NotificationsPage.module.css   # Fase 13.7
    PrivacyPage.tsx / PrivacyPage.test.tsx / PrivacyPage.module.css   # Fase 13.8
    BackofficePage.tsx / BackofficePage.test.tsx / BackofficePage.module.css   # Fase 13.9
  components/
    AppShell.tsx           # layout autenticado: nav (Dashboard/Contas/Categorias/Transações/Orçamentos/Metas/Relatórios/Notificações) com logout, banner de sessão expirada
    AppShell.module.css
  test/
    setup.ts               # liga o servidor MSW para todos os testes; limpa o cache do TanStack Query entre testes
    server.ts               # handlers MSW simulando o formato real de resposta de backend-java
    renderApp.tsx            # helper: renderiza o App real a partir de um caminho
  queryClient.ts              # instância única do QueryClient — módulo próprio para os testes poderem limpar o cache
  App.tsx                    # composição de rotas (equivalente ao composition root do backend)
```

**Nenhuma regra de negócio é duplicada no frontend.** Validação de formulário (Zod) verifica apenas forma de entrada (campo obrigatório, formato de e-mail, tamanho mínimo de senha) — a fonte de verdade de toda regra de domínio continua sendo `backend-java`. Erros retornados pelo backend (`{ error: string }`) são sempre exibidos ao usuário, nunca substituídos por uma mensagem genérica do cliente.

### Autenticação (Fase 13.1)

- `backend-java` expõe apenas `POST /auth/register` e `POST /auth/login` — **não há refresh token, logout no servidor, MFA, recuperação de senha ou edição de perfil** (RF-004/005/006 não migrados de `backend/`, ver ADR-0025). O token JWT expira em 15 minutos sem renovação.
- O token é mantido em `sessionStorage` (não `localStorage`), deliberadamente: não sobrevive ao fechamento da aba, reduzindo a janela de exposição para dados financeiros.
- Logout é inteiramente local (limpa o token) — não existe endpoint de logout no backend para chamar.
- Qualquer chamada autenticada que receba `401` aciona automaticamente o mesmo fluxo de logout + um aviso de "sessão expirada" (`httpClient.onSessionExpired`) — nunca tenta mascarar a ausência de renovação silenciosa.

### Contas e Categorias (Fase 13.2)

- `AccountsPage`: lista de contas, saldo consolidado, criação (tipo/nome/moeda/saldo inicial), edição de nome inline e arquivamento com confirmação em duas etapas (sem `window.confirm`, para manter o fluxo testável). Contas arquivadas perdem as ações de editar/arquivar — o backend não permite reverter o arquivamento nesta fase (RF-013 não inclui desarquivamento).
- `CategoriesPage`: lista hierárquica (categorias de topo com suas subcategorias aninhadas), criação, edição de nome e exclusão. O seletor de "categoria pai" só oferece categorias de topo — o backend limita a hierarquia a 2 níveis (`InvalidCategoryHierarchyException`); isso é só uma affordance de UI (evita um erro previsível), não uma duplicação da regra — o backend continua sendo a autoridade.
- Exclusão de categoria com subcategorias ou transações associadas é bloqueada pelo backend (`CategoryHasSubcategoriesException`/`CategoryHasTransactionsException`) — a mensagem de erro real do servidor é exibida, sem lógica cliente-side para prever o bloqueio.
- Nenhuma nova decisão arquitetural foi necessária nesta subfase — aplica integralmente os padrões de ADR-0025 (TanStack Query para estado remoto, Zod só para forma, MSW na fronteira de rede).

### Transações (Fase 13.3)

- `TransactionsPage`: a listagem do backend (`GET /transactions?accountId=`) é sempre por conta — não existe ainda filtro/busca (RF-018, Fase 4.3 do backend, não construída) — a tela reflete essa limitação real, oferecendo um seletor de conta em vez de simular um filtro que a API não suporta.
- Valor da transação é sempre positivo — a direção (receita/despesa) vem do campo `type`, nunca do sinal do valor (mesma regra de `TransactionPolicy.assertPositiveAmount` no backend); a validação Zod (`.positive()`) só antecipa esse feedback, não o substitui.
- Lançar uma transação em uma conta arquivada é bloqueado pelo backend (`ArchivedAccountException`) — a mensagem real do servidor é exibida; o formulário não tenta prever/bloquear isso no cliente.
- Nenhuma nova decisão arquitetural foi necessária — mesmos padrões de ADR-0025.

### Orçamentos e Metas (Fase 13.4)

- `BudgetsPage`: CRUD de orçamentos por categoria (mensal/semanal/período customizado), edição de limite e limiares de alerta, exclusão, e visualização de histórico de até 6 períodos anteriores (`GET /budgets/{id}/history`). O formulário só mostra os campos de data de período customizado quando "Período customizado" é selecionado (`periodType === 'CUSTOM'`) — progressive disclosure de UI, não validação: o backend continua rejeitando uma combinação inválida com sua própria mensagem.
- `GoalsPage`: CRUD de metas financeiras, com um seletor "Vincular a" (conta ou categoria) que garante que só um dos dois campos é enviado — o backend exige **exatamente uma** associação (`InvalidGoalAssociationException`, RF-030); esse seletor é uma affordance de UI que evita um erro previsível, não uma reimplementação da regra. Edição (`UpdateGoalRequest`) não permite alterar a associação, apenas nome/valor-alvo/prazo/limiares — o formulário de edição reflete essa limitação real da API.
- Nenhuma nova decisão arquitetural foi necessária — mesmos padrões de ADR-0025.

### Dashboard e Pulse Score (Fase 13.5)

- `DashboardPage` substitui o antigo placeholder (`HomePage`, removido) na rota `/`: saldo consolidado, fluxo de caixa (com seletor de janela — 7/30/90 dias, refaz a busca a cada mudança), Pulse Score geral com seus fatores (`budgetConsistency`, `savingsRate`, `spendingDiversification`, `balanceTrend` — nomes reais do backend, mapeados para rótulos em português), gastos por categoria e histórico do Pulse Score (com seletor de janela próprio — 30/90/180 dias).
- **A fórmula do Pulse Score é explicitamente exibida como provisória** (`formulaVersion`, ex. `pulse-v0-provisional`) com uma nota informando que a composição definitiva é uma decisão de produto pendente (RN-006) — reflete o que o próprio backend já documenta, não uma nova ressalva do frontend.
- Sem biblioteca de gráficos — histórico do Pulse Score e gastos por categoria são exibidos como lista, não como gráfico. Nenhuma decisão de charting foi tomada (ADR-0025 não cobre isso); introduzir uma biblioteca de gráficos exigiria uma decisão própria do CTO.
- Nenhuma nova decisão arquitetural foi necessária além disso — mesmos padrões de ADR-0025.

### Relatórios (Fase 13.6)

- `ReportsPage`: gastos por categoria (com exportação CSV), comparação de dois períodos (com `percentageChange` tratado explicitamente como indefinido quando a base do período A é zero, em vez de exibir `Infinity`/`NaN`), e exportação CSV de transações de um período.
- **Download de arquivo autenticado**: `<a href>` simples não funciona para os endpoints de exportação — a autenticação é via header `Authorization`, que uma navegação de link comum não envia. `httpClient.downloadFile` (novo) faz o `fetch` autenticado, lê a resposta como `Blob`, extrai o nome do arquivo do header `Content-Disposition` real do backend, e dispara o download via um `<a>` temporário com `URL.createObjectURL` — mesmo padrão de tratamento de erro/sessão expirada das demais chamadas (`ApiError`, `onSessionExpired`).
- Nenhuma nova decisão arquitetural foi necessária para o restante — mesmos padrões de ADR-0025; a única capacidade nova (download de arquivo autenticado) é uma extensão do módulo de rede já existente, não um padrão diferente.

### Notificações e preferências (Fase 13.7)

- `NotificationsPage`: matriz de preferências (3 tipos de alerta × 2 canais reais do backend — `IN_APP`/`EMAIL`, nomes verificados em `NotificationChannel.java`), botão "Verificar agora" (`POST /notifications/check`, o mesmo endpoint que calcula limiares de orçamento/meta cruzados em tempo real), e lista de notificações com filtro "somente não lidas" e ação de marcar como lida.
- **Estado local de um formulário nunca deve ser resincronizado a cada refetch de fundo do TanStack Query** — só na carga inicial. `PreferencesSection` sincroniza o estado local das preferências com `preferencesQuery.data` apenas uma vez (guarda via `useRef`); sem isso, um refetch em segundo plano (ex.: o navegador reganhando foco — `refetchOnWindowFocus` é `true` por padrão) sobrescreveria silenciosamente uma alteração ainda não salva pelo usuário. Vale como padrão para qualquer futura tela de formulário alimentada por `useQuery`.
- Nenhuma nova decisão arquitetural foi necessária — mesmos padrões de ADR-0025.

### Privacidade/LGPD (Fase 13.8)

- `PrivacyPage`: exportação de todos os dados pessoais e financeiros (`GET /privacy/export`, resumo por contagem na tela + botão "Baixar como JSON" que monta o Blob no cliente a partir do JSON já recebido — o endpoint responde um corpo JSON comum, sem `Content-Disposition` de anexo, ao contrário das exportações CSV da Fase 13.6, então não reaproveita `httpClient.downloadFile`), registro e histórico de consentimento (`POST`/`GET /privacy/consents`), e exclusão de conta com reautenticação por senha (`DELETE /users/me`) — confirmação em duas etapas (mesmo padrão do "Arquivar" em `AccountsPage`), seguida de `logout()` + redirecionamento para `/login` em caso de sucesso. O aviso exibido ("dados financeiros não são apagados nem anonimizados") reflete literalmente a decisão já registrada no ADR-0023, não uma nova posição do frontend.
- **Achado real**: `httpClient.apiRequest` tratava *qualquer* `401` de uma chamada autenticada como sessão expirada, disparando `onSessionExpired` (logout automático + redirecionamento para `/login`). Isso é correto para a maioria das chamadas, mas `DELETE /users/me` também retorna `401` quando a senha de reautenticação está errada (`InvalidCredentialsException` do próprio `DeleteAccountUseCase`) — um erro de validação de negócio, não uma prova de que o token expirou. Sem distinção, uma senha de confirmação digitada errada deslogava o usuário silenciosamente no meio do fluxo de exclusão, em vez de mostrar a mensagem real do backend. Corrigido com uma nova opção `treatUnauthorizedAsSessionExpired` em `apiRequest` (`true` por padrão, `false` só em `privacyApi.deleteAccount`) — mesmo espírito da opção `authenticated: false` já usada em `/auth/login`/`/auth/register`, mas resolvendo um problema distinto: aqui o header `Authorization` **é** necessário (para identificar o usuário), só o efeito colateral de "sessão expirada" no 401 precisa ser suprimido.
- Nenhuma nova decisão arquitetural foi necessária além do achado acima — mesmos padrões de ADR-0025.

### Backoffice (Fase 13.9)

- `BackofficePage`: busca de um usuário por ID (`GET /backoffice/users/{userId}`, reaproveitando o mesmo formato de `UserDataExportResponse` da Fase 13.8 — `GetUserForSupportUseCase` chama internamente o mesmo `ExportUserDataUseCase`), com ações de suspender/reativar conta (`POST .../suspend`, `POST .../reactivate`, com motivo opcional) e o log de auditoria da conta consultada (`GET .../audit-log`).
- **Decisão de RBAC sem claim de papel no JWT (ver ADR-0024)**: `backend-java` não expõe o papel (`Role`) do usuário logado no token — a checagem de `SUPPORT_OPERATOR` é feita pelo backend a cada chamada, via consulta ao banco (`OperatorAuthorization.requireSupportOperator`). Como o frontend não tem como saber de antemão, sem uma chamada de rede dedicada só para isso, se o usuário logado é um operador, o link "Backoffice" no `AppShell` é **sempre exibido** para qualquer usuário autenticado — a página em si é quem impõe o controle de acesso, mostrando a mensagem real de erro 403 do backend ("Acesso negado: esta ação exige permissão de operador de suporte.") para quem tentar buscar um usuário sem ter a permissão. Isso segue a mesma disciplina já aplicada em todo o projeto: o backend é a única fonte de verdade de autorização, o frontend nunca replica ou antecipa essa decisão no cliente.
- **Achado de fidelidade do mock**: a verificação manual contra o backend real revelou que `GetUserForSupportUseCase` registra uma entrada `VIEWED_USER_DATA` no log de auditoria em **toda** consulta de suporte (RF-048) — não só nas ações de suspender/reativar. O mock inicial de `GET /backoffice/users/:userId` em `test/server.ts` não fazia isso, o que tornava o teste do estado "log vazio" irrealista (na aplicação real, o log nunca está vazio depois de uma busca bem-sucedida, porque a própria busca já gera uma entrada). Corrigido no mock para espelhar o comportamento real, e o teste correspondente foi reescrito para verificar a entrada `VIEWED_USER_DATA` gerada pela própria busca, em vez de um estado vazio que nunca ocorre na prática.
- Promoção a `SUPPORT_OPERATOR` continua manual/fora de banda (sem endpoint de autopromoção, conforme ADR-0024) — verificado nesta fase atualizando o papel do usuário de dev diretamente no Postgres via `docker exec` e restaurado para `CUSTOMER` ao final da verificação.
- Nenhuma nova decisão arquitetural foi necessária além da decisão de RBAC acima — mesmos padrões de ADR-0025.

## Como rodar

Pré-requisito: `backend-java` rodando em `http://localhost:8080` (ver [backend-java/README.md](../backend-java/README.md)), com CORS liberado para a origem do frontend (já configurado por padrão para `http://localhost:5173`, ver `financepulse.cors.allowed-origins`). Guia completo de ambiente local (backend + frontend): ver [README.md da raiz do repositório](../README.md#como-rodar-o-projeto-localmente).

```bash
npm install
npm run dev
```

`.env.development` já é versionado com `VITE_API_BASE_URL=http://localhost:8080` — nenhuma cópia manual é necessária para o caminho padrão. Use `.env.example` como referência para um `.env`/`.env.local` próprio se o backend não estiver em `localhost:8080`.

## Como testar

```bash
npm test          # roda a suíte uma vez
npm run test:watch
npm run build      # tsc -b && vite build — valida tipos e o bundle de produção
npm run lint
```

**Equivalente frontend do smoke test de composition root do backend** (`rules.md` §3, formalizado em ADR-0025): o frontend não tem banco de dados para substituir por dublê — sua única fronteira externa real é a rede. Todo fluxo completo de tela tem ao menos um teste que renderiza o `App` real, usa os hooks reais (`useAuth`, TanStack Query real, roteamento real) e intercepta a chamada apenas na camada de rede via MSW (`src/test/server.ts`), nunca mockando um hook de dados ou o cliente HTTP diretamente. `AccountsPage.test.tsx`/`CategoriesPage.test.tsx`/`TransactionsPage.test.tsx`/`DashboardPage.test.tsx`/`ReportsPage.test.tsx`/`NotificationsPage.test.tsx`/`PrivacyPage.test.tsx`/`BackofficePage.test.tsx` cobrem criação, listagem, edição, arquivamento/exclusão e os erros de negócio reais (ex.: categoria com subcategorias, transação em conta arquivada, senha de reautenticação incorreta, usuário sem permissão de operador); `httpClient.test.ts` cobre o módulo de rede isoladamente.

**O download de arquivo autenticado (`httpClient.downloadFile`) funciona em jsdom sem stub adicional** — `URL.createObjectURL`/`Blob` já são suportados pelo ambiente de teste usado neste projeto; os testes de exportação CSV em `ReportsPage.test.tsx` clicam no botão real e aguardam a ausência de erro, exercitando o fluxo completo (fetch autenticado → parsing do `Content-Disposition` → criação do Blob → clique programático no link). Um aviso benigno do jsdom (`Not implemented: navigation to another Document`) aparece no console durante esses testes — é o jsdom reagindo ao clique no link com `download`, não uma falha real; os testes passam normalmente.

**`/dashboard` e `/dashboard/pulse-score/history` têm handlers MSW padrão sempre ativos** (`test/server.ts`), não só nos testes do `DashboardPage` — a partir desta fase, `DashboardPage` é o que renderiza em `/` após qualquer login/registro bem-sucedido, então testes de outras telas (`LoginPage.test.tsx`, `RegisterPage.test.tsx`, `AuthContext.test.tsx`) também passam por essas chamadas. O dashboard é um agregado calculado inteiramente pelo backend a partir de outros recursos — o mock não recalcula a partir de accounts/transactions/etc. já semeados, apenas expõe um estado configurável via `seedDashboard`/`seedPulseScoreHistory`, como a resposta real da API já viria pronta.

**O cache do TanStack Query é limpo entre testes** (`queryClient.clear()` em `test/setup.ts`) — sem isto, dados em cache de um teste anterior no mesmo arquivo vazam para o teste seguinte (a `<select>` de conta podia mostrar uma opção de uma conta que já não existia mais no MSW após o reset de estado), causando falhas intermitentes que só apareciam ao rodar a suíte completa, não em isolamento. `queryClient` foi extraído para seu próprio módulo (`src/queryClient.ts`) justamente para os testes poderem importá-lo e limpá-lo.

**`useQuery` não tenta de novo em caso de erro** (`retry: false` em `queryClient.ts`, ao contrário do padrão do TanStack Query, que tenta 3x com backoff) — a maioria dos erros deste app vem de validação real do backend (`ApiError`, determinística: tentar de novo produz o mesmo erro), então o retry automático só atrasa o usuário ver a mensagem real. `useMutation` já não tenta de novo por padrão, então isso só afeta leituras (`useQuery`) — encontrado na Fase 13.6 ao testar o primeiro caminho de erro de uma tela baseada em `useQuery` (as fases anteriores só tinham telas de leitura sem um fluxo de erro dedicado testado).

**Verificação manual contra o backend real**: além da suíte automatizada, o fluxo de registro/login, o handshake de CORS, o CRUD completo de contas, categorias, transações, orçamentos, metas, o dashboard, os relatórios, notificações (incluindo um cenário real de orçamento estourado, gerando notificações de 80% e 100% do limite, e o fluxo completo de marcar como lida refletindo no filtro "somente não lidas"), privacidade (exportação real de dados do usuário de dev, registro e listagem de consentimento, e o `401`/"E-mail ou senha inválidos." real de uma tentativa de exclusão com senha errada) e — nesta fase — backoffice (promoção manual do usuário de dev a `SUPPORT_OPERATOR` via SQL direto no Postgres, busca real de um segundo usuário registrado para o teste, suspensão e reativação reais refletidas no log de auditoria, o `403`/"Acesso negado..." real de uma tentativa de um usuário sem a permissão, e o `404`/"Usuário não encontrado." real de um ID inexistente) foram verificados com `backend-java` rodando de verdade via Docker Compose — não só contra o MSW. A exclusão com senha *correta* (Fase 13.8) não foi exercitada contra o backend real deliberadamente: anonimizaria o usuário de dev seedado (`dev@financepulse.local`), do qual a verificação desta fase ainda dependia; esse caminho está coberto pelo teste automatizado com MSW. O papel do usuário de dev foi restaurado para `CUSTOMER` ao final da verificação desta fase.

## Limitações conhecidas

- **Sem telas de MFA (RF-004), recuperação de senha (RF-005) ou edição de perfil (RF-006)** — `backend-java` não expõe esses endpoints ainda (só existem no backend TypeScript legado, nunca migrado). Ver ADR-0025.
- **Sessão expira em 15 minutos sem aviso prévio nem renovação silenciosa** — limitação herdada do backend (sem refresh token); o frontend apenas detecta o `401` resultante e desloga, não tenta contornar.
- **CORS liberado por padrão só para `http://localhost:5173`** (configurável via `FINANCEPULSE_CORS_ALLOWED_ORIGINS` no backend) — o domínio de produção precisa ser adicionado quando existir.
- **Contas arquivadas não podem ser reativadas pela UI** — o backend não expõe um endpoint de desarquivamento nesta fase (RF-013).
- **Saldo consolidado é exibido sempre em BRL** — `GetConsolidatedBalanceUseCase` soma sem agrupar por moeda, refletindo a premissa de moeda única do MVP já documentada no próprio backend, não uma escolha nova do frontend.
- **Sem filtro/busca de transações** — a listagem é sempre por conta (`GET /transactions?accountId=`); RF-018 (filtro/busca) é a Fase 4.3 do backend, ainda não construída.
- **Sem transações recorrentes nem importação CSV/OFX** — RF-016 (Fase 4.2) e RF-019–022 (Fase 4.4) do backend ainda não foram construídos.
