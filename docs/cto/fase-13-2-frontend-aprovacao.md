# CTO — Aprovação da Fase 13.2 (Frontend): Contas e Categorias

| Campo | Valor |
|---|---|
| Fase | 13.2 (Frontend) — CRUD de contas e categorias, consumindo `AccountController`/`CategoryController` |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e `rules.md` § 7, este é o parecer formal de encerramento de fase. **Nenhum novo ADR foi produzido para esta subfase** — ADR-0025 já declara explicitamente que decide "a arquitetura técnica válida para todas as subfases" da Fase 13 e que "o CTO não precisa reabrir esta ADR salvo decisão nova". Verifiquei que 13.2 não introduziu nenhuma decisão arquiteturalmente significativa (stack, testes, tratamento de erro e organização de pastas seguem integralmente o que já estava definido) — abrir um ADR apenas para preencher o padrão seria processo sem conteúdo real, o que o próprio ADR-0025 já antecipou e dispensou.

## Escopo revisado

CRUD de contas (criação, listagem, edição de nome, arquivamento, saldo consolidado) e categorias (criação com hierarquia de 2 níveis, listagem, edição, exclusão), consumindo `AccountController` e `CategoryController` reais de `backend-java`. Conforme [roadmap.md](../../roadmap.md) — Fase 13.2.

## Insumos considerados

- [docs/qa/fase-13-2-frontend-review.md](../qa/fase-13-2-frontend-review.md) — parecer de qualidade do QA: **aprovado**, com um achado real de alta severidade (parsing de resposta HTTP quebrando em qualquer `PUT` de atualização retornando `200` com corpo vazio) identificado e corrigido durante a própria revisão, com testes de regressão e confirmação manual contra o backend real.
- [ADR-0025](../adr/0025-decomposicao-fase-13-frontend.md) — arquitetura e stack já definidas, integralmente reaplicadas nesta subfase.
- Contrato real de `AccountController`/`CategoryController` (`backend-java`), incluindo os formatos de resposta de cada verbo HTTP (`201`+corpo para criação, `200`+corpo vazio para atualização, `204` para arquivamento/exclusão) — verificados diretamente no código-fonte antes da implementação, não presumidos.
- Código-fonte em `frontend/src/`.

## Verificação de aderência arquitetural

- [x] Nenhum padrão arquitetural novo introduzido — `accountsApi.ts`/`categoriesApi.ts` seguem exatamente o formato de `authApi.ts` (ADR-0025); páginas usam TanStack Query e Zod exatamente como especificado.
- [x] **Endosso à correção do Achado 1 do QA (bug de parsing de resposta vazia)**: concordo que era um bug real de produção, não um artefato de teste — qualquer chamada a um endpoint de atualização (`PUT`) que responde `200` vazio (padrão usado em múltiplos controllers do backend, não só contas/categorias) quebraria da mesma forma contra o backend real. A correção (ler o corpo como texto antes de decidir se há JSON para parsear) é a forma correta e mínima de tratar essa ambiguidade de `200` vazio vs. `200` com corpo, sem precisar de um contrato mais rígido do lado do backend. Verificação manual confirmou o formato real (`Content-Length: 0` em `HTTP 200`) antes de aceitar a correção como completa.
- [x] **Filtro do seletor de "categoria pai" (só oferece categorias de topo) verificado como affordance de UI, não duplicação de regra**: o backend continua sendo a única fonte de verdade para a validação de hierarquia (`InvalidCategoryHierarchyException`); o frontend apenas evita apresentar uma opção que sempre falharia.
- [x] Nenhuma regra de negócio duplicada no cliente — validado da mesma forma que em 13.1.
- [x] Equivalente de frontend a `rules.md` §3 atendido — `AccountsPage.test.tsx`/`CategoriesPage.test.tsx` exercitam o `App` real com MSW na fronteira de rede.
- [x] Nenhuma funcionalidade de movimentação financeira real introduzida (RN-009) — contas e categorias são apenas metadados/estrutura, sem lançamento de valores reais.
- [x] Nenhum desvio arquitetural registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Contas arquivadas não podem ser reativadas pela UI — limitação do próprio backend (RF-013 não inclui desarquivamento), não uma dívida desta fase.
- Saldo consolidado sempre exibido em BRL, refletindo a premissa de moeda única do backend (`GetConsolidatedBalanceUseCase`) — decisão de produto já aceita, não uma nova dívida.
- A correção do Achado 1 (parsing de corpo vazio) é uma correção estrutural no módulo de rede compartilhado (`httpClient.ts`) — reduz risco para todas as subfases seguintes (13.3–13.9), que vão inevitavelmente chamar outros endpoints com o mesmo padrão de resposta.

## Decisão

**A Fase 13.2 (Frontend) está aprovada.** O CRUD de contas e categorias está corretamente implementado sobre a arquitetura já definida em ADR-0025, sem necessidade de nenhuma decisão arquitetural nova. O parecer de qualidade do QA foi favorável, destacando a correção de um bug real e potencialmente sério no módulo de rede compartilhado — descoberto organicamente pela suíte de testes e confirmado contra o backend real antes de ser considerado resolvido. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7, esta aprovação encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de aprovação explícita adicional em separado.
