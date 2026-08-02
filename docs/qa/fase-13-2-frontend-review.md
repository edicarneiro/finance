# QA — Revisão da Fase 13.2 (Frontend): Contas e Categorias

| Campo | Valor |
|---|---|
| Fase | 13.2 (Frontend) — CRUD de contas e categorias, consumindo `AccountController`/`CategoryController` |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Esta fase não introduziu nenhuma decisão arquitetural nova (aplica integralmente ADR-0025) — não há um novo ADR do CTO para esta subfase, apenas a aprovação formal de encerramento.

## Checklist de Qualidade

- [x] A implementação atende ao escopo de 13.2: listagem/criação/edição/arquivamento de contas, saldo consolidado, listagem/criação/edição/exclusão de categorias com hierarquia de 2 níveis.
- [x] Nenhuma regra de negócio duplicada no cliente — o filtro do seletor de "categoria pai" (só oferece categorias de topo) é uma affordance de UI, não uma reimplementação da validação (`InvalidCategoryHierarchyException` continua sendo a autoridade); erros de negócio (categoria com subcategorias/transações) são sempre a mensagem real do backend.
- [x] Testes cobrem caminho principal, edição, arquivamento/exclusão e os erros de negócio reais (25 testes de frontend no total; 11 novos nesta fase, incluindo dois testes de regressão para os Achados abaixo).
- [x] Aderente à cláusula de frontend de `rules.md` §3: `AccountsPage.test.tsx`/`CategoriesPage.test.tsx` renderizam o `App` real (rotas, `AuthProvider`, TanStack Query reais) e interceptam somente na rede via MSW.
- [x] Sem degradação de performance evidente (bundle: 374 KB / 115 KB gzip).
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada com as novas páginas, módulos de API e limitações.
- [x] Não há introdução de funcionalidade fora do escopo (ex.: nenhum desarquivamento de conta, já que o backend não o expõe).

## Verificação de Execução

```
npm test    → 25 testes, 100% passando (Vitest, jsdom, MSW), após as correções dos Achados 1 e 2
npm run build → sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos
mvn test (backend-java) → 444 testes, 100% passando (nenhuma mudança de backend nesta fase)
```

Verificação manual adicional contra o backend real (não apenas MSW): fluxo completo de conta (criar → listar → renomear → arquivar) e de categoria (criar categoria de topo → criar subcategoria → tentar excluir o pai com subcategoria, bloqueado → renomear subcategoria → excluir a subcategoria) executados via `curl` contra `backend-java` rodando de verdade, confirmando que os formatos de requisição/resposta (incluindo os `HTTP 200` com corpo vazio dos `PUT`) e as mensagens de erro batem exatamente com o que o frontend espera.

## Achados Durante a Revisão

**1. `httpClient.apiRequest` quebrava em qualquer resposta `200` com corpo vazio — não apenas `204` (severidade: alta — bug real que afetaria todo `PUT` de atualização, corrigido nesta revisão)**

`AccountController.update`/`CategoryController.update` (e vários outros endpoints já existentes, ex.: `NotificationController.updatePreferences`) respondem `ResponseEntity.ok().build()` — HTTP `200` com corpo vazio — não `204 No Content`. `httpClient.apiRequest` só tratava `204` como "sem corpo para parsear"; para qualquer outro status de sucesso, chamava `response.json()` incondicionalmente, que lança uma exceção de parsing (`Unexpected end of JSON input`) sobre um corpo vazio. Isto foi descoberto organicamente pelo teste de edição de nome de conta (`AccountsPage.test.tsx`), que falhava mostrando "Não foi possível salvar." mesmo com a chamada de rede tecnicamente bem-sucedida. **Impacto real**: qualquer tela que chamasse `updateAccount`/`updateCategory` contra o backend de verdade teria o mesmo erro — não era um problema exclusivo do MSW. **Resolução**: `apiRequest` agora lê o corpo como texto primeiro e só faz `JSON.parse` se houver conteúdo, cobrindo `204` e `200`-vazio uniformemente. Dois testes de regressão adicionados a `httpClient.test.ts` (`200` vazio resolve para `undefined`; `200` com corpo ainda é parseado normalmente). Confirmado também via verificação manual contra o backend real (`PUT /accounts/{id}` retornando `HTTP 200` com `Content-Length: 0`).

**2. `AppShell` ganhou links de navegação (`NavLink`) nesta fase, e os testes de `AuthContext.test.tsx` que renderizavam `AppShell` fora de um `<Router>` quebraram (severidade: média — regressão de teste real, corrigida nesta revisão)**

Ao adicionar navegação (`Contas`/`Categorias`) ao `AppShell` para esta fase, os dois testes de `expiração de sessão` em `AuthContext.test.tsx` (que renderizam `AuthProvider` + `AppShell` isoladamente, sem passar pelo `App`/`BrowserRouter`) passaram a falhar com `useLocation() may be used only in the context of a <Router> component` — `NavLink` depende de contexto de roteamento. **Resolução**: os dois testes agora envolvem a árvore renderizada em um `<MemoryRouter>`. Não é um bug de produção (o `AppShell` real sempre roda dentro do `BrowserRouter` do `App`) — um teste que teria continuado "verde" incorretamente se não fosse pela suíte já existente pegando a quebra imediatamente.

## Avaliação por Critério

**Clean Code**: `formatCurrency` centraliza a formatação monetária; o comentário sobre o filtro de "categoria pai" explica o "porquê" (evitar um erro previsível sem duplicar a regra) — não o "o quê". `AuthenticatedLayout` extrai o padrão repetido de envolver toda rota autenticada em `AppShell`, evitando repetição no `App.tsx`.

**SOLID/estrutura**: `accountsApi.ts`/`categoriesApi.ts` seguem exatamente o mesmo formato de `authApi.ts` (funções puras, sem estado) — consistência entre módulos de API. `AccountRow`/`CategoryRow` isolam o estado local de edição/confirmação por linha, sem vazar para o componente de página pai além do callback `onChanged` (invalidação de cache).

**Testes**: a suíte cobre os cinco fluxos centrais de cada página (listar, criar, editar, arquivar/excluir, erro de negócio do backend) sempre via `App` real + MSW. O teste do bloqueio de exclusão de categoria com subcategoria é particularmente valioso — exercita uma regra de negócio real do backend através da UI completa, não apenas do módulo de API isoladamente.

**Segurança**: nenhuma chamada de conta/categoria aceita `userId` do cliente — o escopo por usuário é inteiramente definido pelo token enviado (`Authorization: Bearer`), consistente com o resto do projeto. Nenhum dado sensível novo é armazenado no cliente além do que já existia (token em `sessionStorage`).

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Contas arquivadas não podem ser reativadas pela UI — o backend não expõe esse endpoint nesta fase (RF-013), documentado no README.
2. Saldo consolidado sempre formatado como BRL — reflete a premissa de moeda única do MVP já documentada no próprio `GetConsolidatedBalanceUseCase` do backend, não uma nova limitação introduzida pelo frontend.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real e potencialmente sério (parsing de resposta quebrando em qualquer `PUT` de atualização contra o backend real, não apenas em teste) foi identificado e corrigido durante esta revisão, com dois testes de regressão dedicados e confirmação adicional via chamada manual ao backend real. A correção beneficia todas as subfases seguintes que também vão consumir endpoints com resposta `200` vazia. Nenhum apontamento crítico adicional foi identificado.
