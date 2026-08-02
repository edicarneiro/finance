# CTO — Aprovação da Fase 7 (Java): Metas Financeiras

| Campo | Valor |
|---|---|
| Fase | 7 (Java) — RF-030, RF-031 completos; RF-032 parcial (sinal — entrega na Fase 10) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

RF-030 (criação de metas com valor-alvo, prazo e associação a conta ou categoria), RF-031 (cálculo de progresso em tempo real), e RF-032 parcial (sinal de limiares/conclusão atingidos — entrega adiada para a Fase 10), conforme delimitado em [ADR-0019](../adr/0019-fase-7-metas-financeiras.md) e [roadmap.md](../../roadmap.md) — Fase 7.

## Insumos considerados

- [docs/qa/fase-07-java-review.md](../qa/fase-07-java-review.md) — parecer de qualidade do QA: **aprovado**, com um achado real de baixa severidade corrigido durante a própria revisão (normalização de string em branco em `CreateGoalUseCase`) e uma nota de design não bloqueante registrada (fuso horário implícito na conversão `Instant → LocalDate`).
- [ADR-0019](../adr/0019-fase-7-metas-financeiras.md) — decisão de associação mutuamente exclusiva conta/categoria, dois modos de cálculo de progresso, adiamento de RF-032, e a duplicação deliberada de `GoalPolicy` em relação a `BudgetPolicy`.
- Código-fonte em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue o padrão já estabelecido (`domain/ → application/ → adapters/`), estendida com `domain/goal/`, `application/usecases/goal/`, `application/services/GoalProgressCalculator`.
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/goal/` ou nos use cases/serviços de meta importa Spring, JPA ou Jackson.
- [x] **Associação conta-ou-categoria verificada em código, não apenas em documentação**: `GoalPolicy.assertValidAssociation` rejeita tanto "ambas" quanto "nenhuma"; `Goal.accountId`/`categoryId` não têm setter e `withDetails` não os aceita como parâmetro — imutabilidade estrutural, não apenas convencional.
- [x] **Reaproveitamento correto de `AccountBalanceCalculator`**: `ListGoalsUseCase` reaproveita o mesmo serviço puro já usado por `ListAccountsUseCase`/`GetConsolidatedBalanceUseCase` para metas baseadas em conta, em vez de duplicar o cálculo de saldo — confirmei que não há uma segunda implementação de "saldo atual" no código.
- [x] **`GoalPolicy` duplica `BudgetPolicy` deliberadamente (ADR-0019)**: concordo com a decisão de não introduzir uma dependência cruzada entre `domain.goal` e `domain.budget`, nem uma abstração compartilhada prematura, para ~10 linhas de validação de limiares — consistente com o precedente já estabelecido no backend TypeScript (RefreshToken/PasswordResetToken/MfaChallenge) e citado corretamente em ADR-0019.
- [x] Endosso à correção do achado de QA (string em branco tratada como presença de conta/categoria): a normalização em `CreateGoalUseCase.blankToNull` alinha o caso de uso com a semântica já usada por `GoalPolicy.assertValidAssociation`, sem introduzir validação nova no domínio — mudança mínima e no lugar certo.
- [x] `rules.md` § 3 atendido: `GoalControllerTest` inclui um teste de ponta a ponta que cria uma conta com saldo real via HTTP e verifica que `GET /goals` reflete `achieved`/`thresholdsCrossed` corretamente, exercitando `Account`, `Transaction`, `Goal` e o cálculo de progresso juntos contra a raiz de composição real.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009) — `currentAmount` é sempre derivado, nunca persistido.
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Os itens não bloqueantes do parecer de QA (corrida de concorrência TOCTOU, fuso horário implícito em `ListGoalsUseCase.categoryBasedAmount`) são aceitos como dívida técnica consciente, consistentes com o padrão já aceito em todas as fases anteriores. O item de fuso horário fica registrado para revisão quando o ambiente de produção e seu fuso horário forem definidos formalmente — não é urgente enquanto o backend roda em um único ambiente com fuso horário consistente entre `SystemClock` e a conversão de `Goal.createdAt`.
- Progresso de meta por categoria conta apenas transações desde a criação da meta — decisão de produto implícita razoável (evita que transações passadas "infladas" retroativamente contem para uma meta nova), mas vale confirmar com o stakeholder se esse é o comportamento desejado antes de expor a UI ao usuário final (Fase 13).
- RF-032 permanece uma pendência formalmente rastreada (sinal entregue, canal de entrega não) — vinculada à Fase 10 tanto em `roadmap.md` quanto neste ADR, mesmo padrão já usado para RF-022 (ADR-0017) e RF-028 (ADR-0018).

## Decisão

**A Fase 7 (Java) está aprovada.** A implementação entrega metas financeiras com associação exclusiva e imutável a conta ou categoria, cálculo de progresso corretamente derivado (nunca armazenado) reaproveitando `AccountBalanceCalculator` já existente, e o parecer de qualidade do QA foi favorável após a correção, durante a própria revisão, de um achado real de baixa severidade (normalização de string em branco). Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
