# QA — Revisão da Fase 6 (Java): Orçamentos

| Campo | Valor |
|---|---|
| Fase | 6 (Java) — RF-026, RF-027, RF-029 completos; RF-028 parcial (sinal — entrega na Fase 10) |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0018) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-026 (criação por categoria/período), RF-027 (consumo em tempo real) e RF-029 (histórico) do vision.md. RF-028 entrega o sinal (`thresholdsCrossed`), conforme escopo definido em ADR-0018.
- [x] Não há violação de regra de negócio ou restrição do vision.md — **RN-004 verificada com atenção**: todo orçamento tem categoria e período recorrente (ou intervalo fixo para CUSTOM, decisão registrada); consumo recalculado a partir das transações reais, nunca um valor armazenado.
- [x] Isolamento multi-tenant (RF-047) verificado — criação de orçamento valida que a categoria pertence ao usuário autenticado; leitura/edição/exclusão seguem o mesmo padrão anti-enumeração já usado nos demais recursos.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (220 testes; 53 novos nesta fase), incluindo verificação matemática de datas de período (mês civil, semana ISO) com valores concretos, não apenas "não lança exceção".
- [x] Sem degradação de performance evidente para o estágio atual.
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`BudgetControllerTest` como smoke test contra a raiz de composição real, incluindo um teste que verifica consumo refletindo transação real via HTTP).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo — verificado com atenção: nenhum canal de notificação foi implementado (RF-028 permanece só sinal, conforme ADR-0018), e nenhuma agregação de subcategoria foi assumida silenciosamente (ver achado abaixo).

## Verificação de Execução

```
mvn test → 46 classes de teste, 220 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4)
```

## Achados Durante a Revisão

**1. Orçamento em categoria-pai não agrega gastos de subcategorias (severidade: média — ambiguidade de produto, não defeito de código)**

Um orçamento criado sobre uma categoria de nível superior (ex.: "Alimentação") não inclui transações lançadas em suas subcategorias (ex.: "Restaurante"). O código está correto em relação ao que foi especificado — nenhum requisito (RF-026–029, RN-004) menciona agregação hierárquica —, mas é uma expectativa razoável de usuário que vale registrar antes da aprovação, não depois. **Resolução**: CTO confirmou que esta é uma decisão consciente de escopo, não um defeito — documentada em ADR-0018 § 5 e no README, com o raciocínio de que resolver a agregação exigiria decidir regras de produto não especificadas (dupla contagem, orçar pai e filho simultaneamente). Não bloqueia esta aprovação.

**2. Ordem de `alertThresholds` não é garantida após reload do H2 (severidade: baixa)**

`BudgetJpaEntity.alertThresholds` é um `@ElementCollection` sem `@OrderColumn` — Hibernate trata como bag não ordenado; a ordem dos percentuais retornados por `GET /budgets` após persistência pode não corresponder à ordem de criação. Sem efeito funcional: `BudgetConsumptionCalculator.thresholdsCrossed` já ordena o resultado independentemente (`.sorted()`), e a ordem de um conjunto de limiares não carrega semântica própria. Os testes de adaptador já foram escritos cientes disso (`containsExactlyInAnyOrder`, não `containsExactly`). Registrado como nota, não como pendência de correção.

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`BudgetPeriodCalculator.previousPeriods`, `BudgetConsumptionCalculator.thresholdsCrossed`, `movingToADifferentAccount`-style clareza já vista em fases anteriores); comentários existentes documentam decisões não óbvias (por que MONTHLY/WEEKLY são recorrentes mas CUSTOM não; por que RF-028 é só sinal).

**SOLID**: `BudgetPeriodCalculator` e `BudgetConsumptionCalculator` são funções puras e isoladas (sem I/O, sem dependência de framework), testáveis exaustivamente sem dublês de infraestrutura; `ListBudgetsUseCase` e `GetBudgetHistoryUseCase` compõem esses serviços em vez de duplicar lógica de cálculo — nenhuma duplicação entre "consumo atual" e "consumo histórico" além do necessário (mesma função `BudgetConsumptionCalculator.calculate`, parametrizada pelo intervalo). Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/budget/` ou `application/{usecases/budget,services}`.

**Testes**: cobertura inclui verificação matemática direta de `BudgetPeriodCalculator` com datas concretas (mês civil, semana ISO segunda-a-domingo, incluindo virada de mês/ano), não apenas testes de "não lança exceção" — a suíte teria pego um erro de off-by-one na fronteira do período, que é exatamente o tipo de bug que este cálculo poderia introduzir. `BudgetControllerTest` inclui um teste de ponta a ponta que cria uma transação real via HTTP e verifica que `GET /budgets` reflete o consumo e os limiares corretos — a verificação mais importante desta fase.

**Segurança**: `userId` nunca aceito do corpo da requisição; criação de orçamento valida que a categoria pertence ao usuário autenticado (não apenas que existe) — testado explicitamente. Acesso a orçamento de outro usuário retorna o mesmo erro "não encontrado" (HTTP 404) de um orçamento inexistente.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Corrida de exclusão/edição concorrente (TOCTOU) em `Budget` — mesma classe de risco já registrada em todas as fases anteriores.
2. Parâmetro `periods` não numérico em `GET /budgets/{id}/history` retornaria HTTP 500 em vez de 400 — mesma classe de limitação de validação de borda já aceita desde a Fase M1.
3. Sem restrição de duplicidade — um usuário pode criar múltiplos orçamentos para a mesma categoria e período; não é um requisito do vision.md, mas pode gerar `GET /budgets` com entradas aparentemente redundantes.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado de severidade média foi levantado (agregação de subcategoria em orçamentos) e resolvido como decisão consciente de escopo, documentada em ADR-0018, não como defeito de implementação. Nenhum apontamento crítico ou de alta severidade foi identificado. Os itens não bloqueantes são extensões de dívidas técnicas já conhecidas.
