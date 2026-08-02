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
    HomePage.tsx           # placeholder até a Fase 13.5 (Dashboard)
    AuthForm.module.css
    AccountsPage.tsx / AccountsPage.test.tsx / AccountsPage.module.css   # Fase 13.2
    CategoriesPage.tsx / CategoriesPage.test.tsx / CategoriesPage.module.css   # Fase 13.2
    TransactionsPage.tsx / TransactionsPage.test.tsx / TransactionsPage.module.css   # Fase 13.3
  components/
    AppShell.tsx           # layout autenticado: nav (Início/Contas/Categorias/Transações) com logout, banner de sessão expirada
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

**Equivalente frontend do smoke test de composition root do backend** (`rules.md` §3, formalizado em ADR-0025): o frontend não tem banco de dados para substituir por dublê — sua única fronteira externa real é a rede. Todo fluxo completo de tela tem ao menos um teste que renderiza o `App` real, usa os hooks reais (`useAuth`, TanStack Query real, roteamento real) e intercepta a chamada apenas na camada de rede via MSW (`src/test/server.ts`), nunca mockando um hook de dados ou o cliente HTTP diretamente. `AccountsPage.test.tsx`/`CategoriesPage.test.tsx`/`TransactionsPage.test.tsx` cobrem criação, listagem, edição, arquivamento/exclusão e os erros de negócio reais (ex.: categoria com subcategorias, transação em conta arquivada); `httpClient.test.ts` cobre o módulo de rede isoladamente.

**O cache do TanStack Query é limpo entre testes** (`queryClient.clear()` em `test/setup.ts`) — sem isto, dados em cache de um teste anterior no mesmo arquivo vazam para o teste seguinte (a `<select>` de conta podia mostrar uma opção de uma conta que já não existia mais no MSW após o reset de estado), causando falhas intermitentes que só apareciam ao rodar a suíte completa, não em isolamento. `queryClient` foi extraído para seu próprio módulo (`src/queryClient.ts`) justamente para os testes poderem importá-lo e limpá-lo.

**Verificação manual contra o backend real**: além da suíte automatizada, o fluxo de registro/login, o handshake de CORS, o CRUD completo de contas e categorias, e o CRUD completo de transações (incluindo o bloqueio real de valor zero e de lançamento em conta arquivada) foram verificados com `backend-java` rodando de verdade — não só contra o MSW.

## Limitações conhecidas

- **Sem telas de MFA (RF-004), recuperação de senha (RF-005) ou edição de perfil (RF-006)** — `backend-java` não expõe esses endpoints ainda (só existem no backend TypeScript legado, nunca migrado). Ver ADR-0025.
- **Sessão expira em 15 minutos sem aviso prévio nem renovação silenciosa** — limitação herdada do backend (sem refresh token); o frontend apenas detecta o `401` resultante e desloga, não tenta contornar.
- **CORS liberado por padrão só para `http://localhost:5173`** (configurável via `FINANCEPULSE_CORS_ALLOWED_ORIGINS` no backend) — o domínio de produção precisa ser adicionado quando existir.
- **Contas arquivadas não podem ser reativadas pela UI** — o backend não expõe um endpoint de desarquivamento nesta fase (RF-013).
- **Saldo consolidado é exibido sempre em BRL** — `GetConsolidatedBalanceUseCase` soma sem agrupar por moeda, refletindo a premissa de moeda única do MVP já documentada no próprio backend, não uma escolha nova do frontend.
- **Sem filtro/busca de transações** — a listagem é sempre por conta (`GET /transactions?accountId=`); RF-018 (filtro/busca) é a Fase 4.3 do backend, ainda não construída.
- **Sem transações recorrentes nem importação CSV/OFX** — RF-016 (Fase 4.2) e RF-019–022 (Fase 4.4) do backend ainda não foram construídos.
