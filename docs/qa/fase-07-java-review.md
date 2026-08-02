# QA — Revisão da Fase 7 (Java): Metas Financeiras

| Campo | Valor |
|---|---|
| Fase | 7 (Java) — RF-030, RF-031 completos; RF-032 parcial (sinal — entrega na Fase 10) |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0019) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-030 (criação com valor-alvo/prazo/associação a conta ou categoria) e RF-031 (cálculo de progresso) do vision.md. RF-032 entrega o sinal (`thresholdsCrossed`, `achieved`), conforme escopo definido em ADR-0019.
- [x] Não há violação de regra de negócio ou restrição do vision.md — associação conta-ou-categoria verificada como mutuamente exclusiva (`GoalPolicy.assertValidAssociation`) e imutável após a criação (`Goal.withDetails` não aceita novo `accountId`/`categoryId`).
- [x] Isolamento multi-tenant (RF-047) verificado — criação de meta valida que a conta/categoria referenciada pertence ao usuário autenticado; leitura/edição/exclusão seguem o mesmo padrão anti-enumeração (HTTP 404 idêntico para meta inexistente e meta de outro usuário) já usado nos demais recursos.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009) — `currentAmount` é sempre derivado de saldo/transações já existentes, nunca escrito de volta.
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (270 testes; 50 novos nesta fase, incluindo 2 de regressão adicionados durante esta revisão — ver Achado 1).
- [x] Sem degradação de performance evidente para o estágio atual.
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`GoalControllerTest` como smoke test contra a raiz de composição real, com verificação de progresso via saldo de conta real e rejeição de conflitos de associação).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo — nenhum canal de notificação foi implementado (RF-032 permanece só sinal, conforme ADR-0019).

## Verificação de Execução

```
mvn test → 270 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4), após a correção do Achado 1
```

## Achados Durante a Revisão

**1. String em branco em `accountId`/`categoryId` produzia HTTP 404 em vez de HTTP 400 (severidade: baixa — corrigido nesta revisão)**

`CreateGoalRequest.accountId`/`categoryId` não têm `@NotBlank` (correto — exatamente um dos dois é obrigatório, não ambos). Porém `CreateGoalUseCase.execute` verificava apenas `!= null` antes de consultar o repositório de conta/categoria, enquanto `GoalPolicy.assertValidAssociation` trata string em branco (`""`, `"  "`) como ausente (`isBlank()`). Um cliente que enviasse `{"accountId": "", "categoryId": "cat-123"}` disparava uma busca por uma conta de id `""`, que nunca existe — retornando `AccountNotFoundException` (HTTP 404, "conta não encontrada") em vez do erro correto de validação (HTTP 400, se o caso fosse realmente inválido) ou, neste caso, deveria simplesmente ter sido tratado como "sem conta, meta baseada em categoria" e ter sucesso. **Resolução**: `CreateGoalUseCase` agora normaliza string em branco para `null` antes de qualquer verificação de posse ou chamada a `Goal.create`, alinhando o caso de uso com a semântica já usada pelo domínio. Dois testes de regressão foram adicionados (`treatsABlankAccountIdAsAbsent...`, `rejectsBothAssociationsBlankTheSameAsBothAbsent`). Corrigido e verificado nesta revisão, não é mais uma pendência.

**2. Conversão `Instant` → `LocalDate` via `ZoneId.systemDefault()` em `ListGoalsUseCase.categoryBasedAmount` (severidade: baixa — nota de design, não bloqueante)**

`since = goal.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()` é o único ponto do backend Java que converte um `Instant` de auditoria (`createdAt`) em uma data de negócio (`LocalDate`) usando o fuso horário padrão da JVM. `SystemClock.today()` também usa `LocalDate.now()` (implicitamente o mesmo fuso), então o comportamento é internamente consistente dentro de um mesmo deployment — mas a dependência do fuso horário padrão da JVM (em vez de um fuso fixo e explícito, ou de reaproveitar a porta `Clock`) é implícita e não documentada. Se o servidor for reimplantado em um fuso diferente, metas criadas perto da meia-noite poderiam, em teoria, deslocar a fronteira "desde a criação" em um dia. Não é um defeito de código — é comportamento determinístico e correto para o ambiente atual — mas vale registrar como dívida técnica de design para quando o backend for implantado em produção com fuso horário explícito.

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`GoalProgressCalculator.thresholdsCrossed`, `accountBasedAmount`/`categoryBasedAmount`, `isAccountBased()`); comentários existentes documentam decisões não óbvias (por que a associação é imutável, por que `GoalPolicy` duplica `BudgetPolicy`).

**SOLID**: `GoalProgressCalculator` é uma função pura e isolada (sem I/O, sem dependência de framework), testável exaustivamente sem dublês de infraestrutura — inclusive os casos de borda relevantes (atingida exatamente no alvo, atingida acima do alvo, vencida somente quando não atingida, `currentAmount` negativo para metas de categoria). `ListGoalsUseCase` compõe `AccountBalanceCalculator` (reaproveitado, não duplicado) e `GoalProgressCalculator` em vez de recalcular lógica já existente. Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/goal/` ou `application/{usecases/goal,services}`.

**Testes**: `GoalProgressCalculatorTest` cobre limites exatos (currentAmount == target, currentAmount > target, overdue apenas quando não atingida) — exatamente o tipo de teste que pegaria um erro de `>=` vs `>` nesse cálculo. `GoalControllerTest` inclui um teste de ponta a ponta que cria uma conta com saldo real via HTTP e verifica que `GET /goals` reflete `achieved: true` e o limiar de 80% cruzado — a verificação mais importante desta fase. `JpaGoalRepositoryAdapterTest` cobre metas baseadas em conta e em categoria separadamente, isolamento por usuário, e reload dos `progressAlertThresholds` via `@ElementCollection`.

**Segurança**: `userId` nunca aceito do corpo da requisição; criação de meta valida que a conta/categoria referenciada pertence ao usuário autenticado (não apenas que existe) — testado explicitamente, incluindo o caso "conta existe mas pertence a outro usuário" (`rejectsAnAccountBelongingToAnotherUser`). Acesso a meta de outro usuário retorna o mesmo erro "não encontrada" (HTTP 404) de uma meta inexistente, verificado em `GoalControllerTest.aUserCannotAccessAnotherUsersGoal`.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Corrida de criação/edição concorrente (TOCTOU) em `Goal` — mesma classe de risco já registrada em todas as fases anteriores.
2. Fuso horário implícito na conversão `Instant` → `LocalDate` de `Goal.createdAt` (Achado 2 acima) — dívida de design a revisitar quando o ambiente de produção/fuso horário for definido.
3. Progresso de meta por categoria conta apenas transações lançadas a partir da criação da meta — comportamento intencional (documentado em ADR-0019 e no README), mas pode surpreender um usuário que já tinha transações relevantes na categoria antes de criar a meta.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real de severidade baixa foi identificado e corrigido durante esta revisão (Achado 1 — normalização de string em branco em `CreateGoalUseCase`, com testes de regressão adicionados). Um segundo achado foi registrado como nota de design não bloqueante (Achado 2 — fuso horário implícito). Nenhum apontamento crítico ou de alta severidade foi identificado. Os itens não bloqueantes são extensões de dívidas técnicas já conhecidas ou decisões de escopo já documentadas.
