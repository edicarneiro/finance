# CTO — Aprovação da Fase 13.4 (Frontend): Orçamentos e Metas

| Campo | Valor |
|---|---|
| Fase | 13.4 (Frontend) — CRUD de orçamentos e metas, consumindo `BudgetController`/`GoalController` |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado |

> Conforme `rules.md` § 7, este é o parecer formal de encerramento de fase. **Nenhum novo ADR foi produzido** — ADR-0025 já cobre a arquitetura de toda a Fase 13, e 13.4 não introduziu nenhuma decisão arquiteturalmente significativa, mesmo raciocínio já registrado nas aprovações de 13.2 e 13.3.

## Escopo revisado

CRUD de orçamentos (criação com período mensal/semanal/customizado, edição de limite e limiares, exclusão, histórico de períodos anteriores) e CRUD de metas financeiras (criação com associação exclusiva a conta ou categoria, edição, exclusão), consumindo `BudgetController` e `GoalController` reais de `backend-java`. Conforme [roadmap.md](../../roadmap.md) — Fase 13.4.

## Insumos considerados

- [docs/qa/fase-13-4-frontend-review.md](../qa/fase-13-4-frontend-review.md) — parecer de qualidade do QA: **aprovado**, com um achado de Clean Code (duplicação de função utilitária) identificado e corrigido.
- [ADR-0025](../adr/0025-decomposicao-fase-13-frontend.md) — arquitetura e stack já definidas, integralmente reaplicadas.
- Contrato real de `BudgetController`/`GoalController` (`backend-java`), incluindo a regra de associação exclusiva de meta (`InvalidGoalAssociationException`) e a distinção entre período recorrente (mensal/semanal) e customizado — verificados diretamente no código-fonte do backend antes da implementação.
- Código-fonte em `frontend/src/`.

## Verificação de aderência arquitetural

- [x] Nenhum padrão arquitetural novo introduzido — `budgetsApi.ts`/`goalsApi.ts` seguem o formato já estabelecido dos demais módulos de API.
- [x] **Seletor de associação de meta (conta/categoria) verificado como affordance de UI, não duplicação de regra**: o backend continua sendo a única fonte de verdade para `InvalidGoalAssociationException` — confirmado manualmente contra o backend real que enviar ambas as associações ou nenhuma retorna a mensagem de erro exata do servidor, não uma mensagem inventada no cliente.
- [x] **Exibição condicional dos campos de período customizado do orçamento verificada como progressive disclosure, não validação client-side da regra de negócio** — o backend permanece responsável por `InvalidBudgetPeriodException`.
- [x] **Endosso à correção do Achado 1 do QA (duplicação de `toThresholdList`)**: correção correta e proporcional — extração para um módulo utilitário compartilhado, sem introduzir abstração além do necessário.
- [x] Nenhuma regra de negócio duplicada no cliente.
- [x] Equivalente de frontend a `rules.md` §3 atendido.
- [x] Nenhuma funcionalidade de movimentação financeira real introduzida (RN-009) — orçamentos e metas são estruturas de planejamento, não execução de pagamento.
- [x] Nenhum desvio arquitetural registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- `formatCurrency` permanece duplicada (com assinaturas distintas) entre páginas de fases diferentes — dívida de estilo menor, não urgente; não justifica reabrir páginas de fases já aprovadas apenas por isso.
- Nenhuma dívida técnica nova introduzida por esta fase além do já registrado nas fases anteriores (13.1–13.3) e em ADR-0025/ADR-0026.

## Decisão

**A Fase 13.4 (Frontend) está aprovada.** O CRUD de orçamentos e metas reflete fielmente as regras de negócio mais específicas do backend nesta área — associação exclusiva de meta e período customizado de orçamento — através de affordances de UI corretas, sem duplicar validação. O parecer de qualidade do QA foi favorável, com uma correção pontual de Clean Code. Toda alegação de comportamento foi verificada com execução real contra o backend via Docker Compose, incluindo os casos de erro de negócio. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7, esta aprovação encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de aprovação explícita adicional em separado.
