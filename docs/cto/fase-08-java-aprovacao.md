# CTO — Aprovação da Fase 8 (Java): Dashboard e Pulse Score

| Campo | Valor |
|---|---|
| Fase | 8 (Java) — RF-033, RF-035, RF-036 completos; RF-034 com fórmula provisória (RN-006) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

RF-033 (painel consolidado: saldo, fluxo de caixa recente, distribuição de gastos por categoria), RF-034 (Pulse Score com fórmula provisória e versionada, dada a pendência formal de RN-006), RF-035 (evolução histórica) e RF-036 (explicabilidade por fator), conforme delimitado em [ADR-0020](../adr/0020-fase-8-dashboard-pulse-score.md) e [roadmap.md](../../roadmap.md) — Fase 8.

## Insumos considerados

- [docs/qa/fase-08-java-review.md](../qa/fase-08-java-review.md) — parecer de qualidade do QA: **aprovado**, com uma regressão real da Fase 7 identificada e corrigida durante a revisão (`Goal.createdAt` acoplado ao relógio de parede real em vez de `Clock`) e uma nota de manutenibilidade não bloqueante registrada.
- [ADR-0020](../adr/0020-fase-8-dashboard-pulse-score.md) — decisão sobre a fórmula provisória do Pulse Score (RN-006), a persistência de snapshot diário sem scheduler dedicado (RN-005), e o escopo de dois endpoints.
- vision.md § 4.8, RN-005, RN-006, § 17.5 — as duas pendências formais que motivaram decisões explícitas nesta fase, ambas já previstas pelo próprio vision.md, não descobertas ad hoc.
- Código-fonte em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue o padrão já estabelecido (`domain/ → application/ → adapters/`), estendida com `domain/pulsescore/`, `application/usecases/dashboard/`, `application/services/PulseScoreCalculator`.
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/pulsescore/` ou nos use cases/serviços de dashboard importa Spring, JPA ou Jackson.
- [x] **RN-006 tratada corretamente — não escondida, não fingida como resolvida**: a fórmula provisória é identificada por `formulaVersion`, documentada extensivamente em Javadoc e ADR-0020, e o README declara explicitamente que "isto não substitui a definição formal pendente". Concordo com a abordagem: implementar uma fórmula de referência transparente e versionada é preferível a bloquear toda a Fase 8 por uma pendência de produto que o próprio vision.md já classificou como resolvível depois (§17.5).
- [x] **RN-005 satisfeita estruturalmente, com a limitação corretamente documentada, não escondida**: não há endpoint de escrita para `PulseScoreSnapshot`; o upsert por `(userId, scoreDate)` garante no máximo um snapshot por dia civil. A ausência de um scheduler ativo (lacunas no histórico em dias sem acesso ao dashboard) está registrada tanto em ADR-0020 quanto no README — decisão de escopo explícita, dentro do que a infraestrutura atual do backend Java permite (sem barramento de eventos, sem cron).
- [x] **Reaproveitamento correto de serviços já existentes**: `GetDashboardUseCase` reaproveita `AccountBalanceCalculator`, `BudgetPeriodCalculator` e `BudgetConsumptionCalculator` em vez de duplicar lógica de saldo/orçamento — verifiquei que nenhuma segunda implementação desses cálculos foi introduzida.
- [x] **Endosso à correção do Achado 1 do QA** (regressão de `Goal.createdAt`): a correção é mínima, escopada apenas à entidade que realmente usa `createdAt` em lógica de negócio, e não introduz uma mudança de comportamento em nenhuma outra entidade (`User`, `Account`, `Category`, `Transaction`, `Budget` continuam usando `Instant.now()` puro, apropriado para metadado de auditoria). A escolha de `ZoneOffset.UTC` fixo (em vez de `ZoneId.systemDefault()`) em ambas as pontas (escrita em `Goal.create`, leitura em `ListGoalsUseCase`) elimina a dependência do fuso horário da JVM de deployment — resolve de forma definitiva o que a Fase 7 havia registrado como risco teórico.
- [x] `rules.md` § 3 atendido: `DashboardControllerTest` inclui um teste de ponta a ponta que cria transações reais via HTTP e verifica que saldo, fluxo de caixa, distribuição por categoria e Pulse Score refletem esses dados corretamente, exercitando `Account`, `Transaction`, `Category`, `Budget` (indiretamente) e `PulseScoreSnapshot` juntos contra a raiz de composição real.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009) — toda a Fase 8 é leitura/agregação.
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- A ausência de scheduler para recálculo diário do Pulse Score (lacunas de histórico em dias sem acesso ao dashboard) é uma dívida técnica consciente, vinculada à futura Camada de Integração Assíncrona (vision.md § 10.5) — não há infraestrutura de background job em nenhuma outra parte do backend Java ainda, então introduzi-la apenas para esta fase seria antecipar escopo de uma camada arquitetural inteira.
- A fórmula provisória do Pulse Score (`pulse-v0-provisional`) precisa ser substituída quando RN-006 for formalmente resolvida — fica registrado como obrigação de processo (não apenas comentário de código) que `FORMULA_VERSION` deve mudar nessa ocasião, preservando a rastreabilidade do histórico já calculado.
- `GetDashboardUseCase` como caso de uso com múltiplas responsabilidades de agregação (nota de manutenibilidade do QA) é aceita como está — nenhuma duplicação foi introduzida, e uma extração prematura de serviço sem um segundo consumidor real violaria a disciplina de "regra de três" já seguida consistentemente neste projeto.
- A regressão corrigida nesta fase (Achado 1 do QA) reforça um princípio geral válido para todo o backend: nenhuma lógica de negócio deve derivar datas de `Instant.now()`/`LocalDate.now()` diretamente — sempre through a porta `Clock`. Vale como lembrete para as próximas fases, não apenas para `Goal`.

## Decisão

**A Fase 8 (Java) está aprovada.** A implementação entrega o painel consolidado (RF-033), uma fórmula de Pulse Score transparente e corretamente rotulada como provisória diante de uma pendência de produto já formalmente reconhecida em vision.md (RF-034/RN-006), evolução histórica via snapshot diário sem exigir infraestrutura de scheduler ainda inexistente (RF-035/RN-005), e explicabilidade por fator na mesma chamada que calcula o score (RF-036). O parecer de qualidade do QA foi favorável após a correção, durante a própria revisão, de uma regressão real herdada da Fase 7. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
