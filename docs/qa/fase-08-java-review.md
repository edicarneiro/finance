# QA — Revisão da Fase 8 (Java): Dashboard e Pulse Score

| Campo | Valor |
|---|---|
| Fase | 8 (Java) — RF-033, RF-035, RF-036 completos; RF-034 com fórmula provisória (RN-006) |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0020) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-033 (painel: saldo, fluxo de caixa, distribuição de gastos), RF-035 (histórico) e RF-036 (explicabilidade por fator) do vision.md. RF-034 entrega uma fórmula provisória e versionada (`formulaVersion`), com a pendência formal de RN-006/§17.5 preservada e não escondida.
- [x] Não há violação de regra de negócio ou restrição do vision.md — nenhum sinal fora dos quatro citados em § 4.8 foi adicionado (progresso de meta foi deliberadamente excluído, verificado no código).
- [x] Isolamento multi-tenant (RF-047) verificado — `TransactionRepository.findAllByUserId` (nova capacidade desta fase) é escopada por `userId` na própria assinatura; `PulseScoreSnapshot` idem. Testado explicitamente em `DashboardControllerTest.aUserNeverSeesAnotherUsersDashboardData` e `JpaPulseScoreRepositoryAdapterTest.doesNotFindSnapshotsBelongingToAnotherUser`.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009) — todo o dashboard é leitura/agregação.
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (300 testes; 30 novos nesta fase, incluindo cobertura exaustiva de `PulseScoreCalculator` com valores exatos, não apenas "não lança exceção").
- [x] Sem degradação de performance evidente — `GetDashboardUseCase` busca `findAllByUserId` uma única vez e reutiliza a lista em memória para saldo, saldo passado, fluxo de caixa, distribuição por categoria e cálculo de orçamentos, em vez do padrão N+1 já aceito em `GetConsolidatedBalanceUseCase` (uma consulta por conta) — uma melhoria incidental, não uma regressão.
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`DashboardControllerTest` como smoke test contra a raiz de composição real, com transações reais via HTTP refletidas no saldo/fluxo de caixa/Pulse Score).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo — nenhum scheduler/cron foi construído (decisão explícita de ADR-0020, não uma omissão silenciosa); nenhum canal de notificação de Pulse Score foi implementado.

## Verificação de Execução

```
mvn test → 300 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4), após a correção do Achado 1
```

## Achados Durante a Revisão

**1. Regressão real em `Goal`/`ListGoalsUseCase`: `createdAt` usava `Instant.now()` não controlado por `Clock`, quebrando o filtro "transações desde a criação da meta" (severidade: alta no sentido de já estar quebrando a suíte — corrigido nesta revisão)**

Ao rodar a suíte completa após implementar a Fase 8, `ListGoalsUseCaseTest.categoryBasedGoalProgressSumsTransactionsSinceGoalCreation` (um teste da Fase 7, não tocado por esta fase) começou a falhar: esperava `currentAmount = 500.00`, recebeu `0`. Investigação: `Goal.create()` gravava `createdAt` via `Instant.now()` (relógio de parede real), ignorando o parâmetro `today` (`LocalDate`, vindo de `Clock`) já recebido pelo método para validar o prazo. `ListGoalsUseCase.categoryBasedAmount` deriva `since = goal.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()` para filtrar transações — como o teste usa `FixedClock(2026-07-31)` para `today`, mas `createdAt` vinha do relógio real, a mudança de data do sistema (2026-07-31 → 2026-08-01, ocorrida durante esta sessão de trabalho) fez `since` saltar para depois da data da transação de teste, excluindo-a do cálculo. **Este é exatamente o item que a revisão de QA da Fase 7 já havia identificado e registrado como nota de design não bloqueante** ("Achado 2" em `docs/qa/fase-07-java-review.md`) — na época avaliado como risco teórico ("se o servidor for reimplantado em um fuso diferente"); a passagem real do tempo durante esta sessão demonstrou que o risco também se manifesta sem trocar de servidor, apenas com a data avançando. **Resolução**: `Goal.create()` agora deriva `createdAt` do parâmetro `today` (`today.atStartOfDay(ZoneOffset.UTC).toInstant()`) em vez de `Instant.now()`, e `ListGoalsUseCase` compara usando `ZoneOffset.UTC` (fixo) em vez de `ZoneId.systemDefault()` (implícito) — escrita e leitura agora usam a mesma referência de fuso, e ambas são controláveis por `FixedClock` em teste. A suíte completa (300 testes) passa de forma determinística após a correção. Nenhuma outra entidade (`User`, `Account`, `Category`, `Transaction`, `Budget`) usa `createdAt` em lógica de negócio — o escopo da correção foi propositalmente restrito a `Goal`, o único caso real.

**2. `GetDashboardUseCase` é um caso de uso grande, com várias responsabilidades de agregação (severidade: baixa — nota de manutenibilidade, não bloqueante)**

O método `execute` e seus privados computam saldo consolidado, saldo passado, fluxo de caixa, distribuição por categoria, consumo de orçamentos e o Pulse Score — mais lógica que qualquer outro caso de uso do projeto até agora. Não há duplicação nem violação de regra de negócio (cada cálculo reaproveita um serviço puro já existente: `AccountBalanceCalculator`, `BudgetPeriodCalculator`, `BudgetConsumptionCalculator`, `PulseScoreCalculator`), e os testes cobrem cada seção do agregado isoladamente através dos campos do `Output`. Ainda assim, é o caso de uso mais complexo do backend Java em número de colaboradores (5 repositórios/portas). Registrado como nota de manutenibilidade para acompanhar caso a Fase 9 (Relatórios) precise de agregações semelhantes — se um terceiro consumidor de "todas as transações do usuário agregadas" aparecer, vale extrair um serviço de agregação dedicado (regra de três, mesmo racional já aplicado a outras decisões do projeto).

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`budgetHealthScore`, `spendingDiversification`, `balanceTrend`); a fórmula provisória do Pulse Score está documentada extensivamente no Javadoc da classe e em ADR-0020, deixando claro que não é a definição final de produto.

**SOLID**: `PulseScoreCalculator` é uma função pura e isolada (sem I/O, sem dependência de framework), testável exaustivamente sem dublês de infraestrutura — a suíte cobre cada fator individualmente (saturação, piso, omissão por falta de dado) além do caso combinado. `GetDashboardUseCase` compõe serviços já existentes (`AccountBalanceCalculator`, `BudgetPeriodCalculator`, `BudgetConsumptionCalculator`) em vez de duplicá-los — nenhuma segunda implementação de "saldo atual" ou "consumo de orçamento" foi introduzida. Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/pulsescore/` ou `application/{usecases/dashboard,services/PulseScoreCalculator}`.

**Testes**: `PulseScoreCalculatorTest` usa valores numéricos exatos e escolhidos deliberadamente "redondos" para tornar as asserções legíveis e verificáveis à mão, cobrindo saturação (100/0), omissão de fator por falta de dado, e o caso em que só um fator (tendência de saldo) está disponível. `DashboardControllerTest` inclui um teste de ponta a ponta que cria transações reais via HTTP e verifica que saldo, fluxo de caixa, distribuição por categoria e Pulse Score refletem exatamente esses dados — a verificação mais importante desta fase. `GetDashboardUseCaseTest.persistsExactlyOneSnapshotPerUserPerDayEvenAcrossMultipleCalls` verifica diretamente o comportamento de upsert que resolve RN-005 sem scheduler (decisão central de ADR-0020).

**Segurança**: `userId` nunca aceito do corpo da requisição; toda agregação é escopada por `userId` nas portas de repositório, incluindo a nova `TransactionRepository.findAllByUserId`. Isolamento entre usuários testado explicitamente tanto no nível de adaptador (`JpaPulseScoreRepositoryAdapterTest`, `JpaTransactionRepositoryAdapterTest`) quanto de ponta a ponta (`DashboardControllerTest`).

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. `GetDashboardUseCase` como caso de uso grande (Achado 2 acima) — nota de manutenibilidade, não uma violação atual de nenhum princípio.
2. Lacunas no histórico de Pulse Score em dias sem chamada a `GET /dashboard` — decisão de escopo explícita (ADR-0020), não uma falha.
3. Fórmula do Pulse Score provisória (RN-006) — pendência de produto/ciência de dados já sinalizada no próprio vision.md, não uma lacuna de implementação desta fase.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real e ativo (não apenas teórico) foi identificado e corrigido durante esta revisão — uma regressão na Fase 7 que só se manifestou porque a data real do sistema avançou durante esta sessão de trabalho, confirmando um risco que a revisão de QA da própria Fase 7 já havia sinalizado como não bloqueante. A correção (Achado 1) é mínima, cirúrgica, e restrita à única entidade que de fato usa `createdAt` em lógica de negócio. Um segundo achado foi registrado como nota de manutenibilidade não bloqueante. Nenhum apontamento crítico de segurança ou de regra de negócio foi identificado nesta fase.
