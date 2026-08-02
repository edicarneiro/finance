# CTO — Aprovação da Fase 6 (Java): Orçamentos

| Campo | Valor |
|---|---|
| Fase | 6 (Java) — RF-026, RF-027, RF-029 completos; RF-028 parcial (sinal — entrega na Fase 10) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

RF-026 (criação de orçamentos por categoria e período), RF-027 (percentual de consumo em tempo real), RF-029 (histórico de períodos anteriores), e RF-028 parcial (sinal de limiares ultrapassados — entrega adiada para a Fase 10), conforme delimitado em [ADR-0018](../adr/0018-fase-6-orcamentos.md) e [roadmap.md](../../roadmap.md) — Fase 6.

## Insumos considerados

- [docs/qa/fase-06-java-review.md](../qa/fase-06-java-review.md) — parecer de qualidade do QA: **aprovado**, com um achado de severidade média (agregação de subcategoria) resolvido como decisão de escopo, não como defeito.
- [ADR-0018](../adr/0018-fase-6-orcamentos.md) — decisão de período recorrente, cálculo de consumo sem snapshot, adiamento de RF-028, e a decisão de não agregar subcategorias (adicionada durante esta revisão).
- Código-fonte em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue o padrão já estabelecido (`domain/ → application/ → adapters/`), estendida com `domain/budget/`, `application/usecases/budget/`, `application/services/Budget{Period,Consumption}Calculator`.
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/budget/` ou nos use cases/serviços de orçamento importa Spring, JPA ou Jackson.
- [x] **RN-004 verificada em código, não apenas em documentação**: `Budget.categoryId`/`periodType` são imutáveis (sem setter, apenas construção e `withLimitAndThresholds`, que preserva ambos); o consumo é sempre derivado (`BudgetConsumptionCalculator`), nunca um campo persistido — confirmei que nenhum caminho de código escreve um valor de "consumo" em `Budget` ou `BudgetJpaEntity`.
- [x] **Primeira porta `Clock` do backend Java, corretamente isolada**: `SystemClock` é o único adaptador de produção; testes usam `FixedClock` para determinismo — verificado que nenhum caso de uso ou serviço de cálculo chama `LocalDate.now()` diretamente, sempre through a porta.
- [x] Endosso à resolução do achado de QA (orçamento não agrega subcategorias): concordo que resolver a agregação agora exigiria decidir regras de produto (dupla contagem, orçar pai e filho) sem base no vision.md — a decisão de tratar cada orçamento como isolado à sua categoria exata é a mais simples e reversível; agregação pode ser adicionada depois sem *breaking change* de schema.
- [x] `rules.md` § 3 atendido: `BudgetControllerTest` inclui um teste de ponta a ponta que cria uma transação real via HTTP e verifica que o consumo do orçamento reflete corretamente, exercitando `Transaction`, `Budget` e os dois use cases de consumo juntos contra a raiz de composição real.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009).
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Os itens não bloqueantes do parecer de QA (corrida de concorrência, parâmetro não numérico retornando 500, ausência de restrição de duplicidade) são aceitos como dívida técnica consciente, consistentes com o padrão já aceito em todas as fases anteriores.
- A decisão de não agregar subcategorias em orçamentos (documentada em ADR-0018 § 5 durante esta revisão) é uma decisão de produto em aberto, não uma dívida técnica — fica registrada para eventual revisão quando/se o produto priorizar orçamentos hierárquicos.
- RF-028 permanece uma pendência formalmente rastreada (sinal entregue, canal de entrega não) — vinculada à Fase 10 tanto em `roadmap.md` quanto neste ADR, mesmo padrão já usado para RF-022 (ADR-0017).

## Decisão

**A Fase 6 (Java) está aprovada.** A implementação entrega orçamentos por categoria e período com cálculo de consumo em tempo real corretamente derivado (nunca armazenado), preserva RN-004 estruturalmente, introduz a primeira porta `Clock` do backend Java de forma limpa e testável, e o parecer de qualidade do QA foi favorável após a resolução documentada do achado sobre agregação de subcategorias. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
