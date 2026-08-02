# ADR-0020: Fase 8 (Java) — Dashboard e Pulse Score, fórmula provisória e persistência de histórico

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-08-01 |
| Autor | CTO / Principal Software Architect |
| Fase | 8 (Java) — RF-033, RF-034, RF-035, RF-036 |

## Contexto

Fase 8 cobre RF-033 (painel consolidado: saldo total, fluxo de caixa recente, distribuição de gastos por categoria), RF-034 (calcular o Pulse Score), RF-035 (evolução histórica do Pulse Score) e RF-036 (explicabilidade básica — quais fatores impactaram o índice).

**Duas questões genuínas de projeto, ambas sinalizadas no próprio vision.md, precisam de decisão explícita antes da implementação — nenhuma é inventada nesta fase:**

1. **RN-006 declara formalmente que a fórmula do Pulse Score é uma pendência**: *"A fórmula/composição exata do Pulse Score é uma decisão de produto/ciência de dados a ser detalhada em documento técnico complementar; este documento define apenas seu papel funcional (Seção 17 — pendência formal)."* A Seção 17.5 reforça a mesma pendência como dúvida em aberto. Implementar RF-034 exige, portanto, uma fórmula concreta — mas qualquer fórmula definida agora é necessariamente provisória, não a definição de produto/ciência de dados ainda pendente.
2. **RN-005 exige recálculo periódico** ("ex.: diariamente") **e após eventos financeiros relevantes**, e proíbe edição manual. O backend Java, nesta fase, não tem infraestrutura de agendamento (cron) nem barramento de eventos (vision.md § 10, "Camada de Integração Assíncrona" — Pós-MVP/não construída ainda). Calcular e persistir um snapshot a cada leitura do dashboard, sem scheduler dedicado, é a única forma de satisfazer RN-005 com a infraestrutura atual.

## Decisão

### 1. Fórmula do Pulse Score — provisória, transparente, versionada (resolve a pendência de RN-006 sem a invalidar)

- Implementada uma fórmula determinística e documentada, usando exatamente os quatro sinais citados em vision.md § 4.8 ("consistência orçamentária, taxa de poupança, diversificação de gastos e tendência de saldo"), sem adicionar sinais não mencionados (ex.: progresso de metas foi deliberadamente **excluído** — não está listado em § 4.8).
- Cada sinal é normalizado para uma faixa 0–100; o score final é a média ponderada dos sinais **disponíveis** no período (pesos iguais nesta versão), com um sinal (tendência de saldo) sempre presente e os outros três omitidos quando não há dados suficientes para calculá-los (ex.: usuário sem orçamentos, sem receita registrada, ou sem despesas no período) — evita penalizar artificialmente um usuário por ausência de dado, não por comportamento financeiro real.
- A fórmula é identificada por uma constante `PulseScoreCalculator.FORMULA_VERSION = "pulse-v0-provisional"`, persistida em cada snapshot. **Isto não substitui a definição formal de produto/ciência de dados pendente em RN-006/§17.5** — é uma implementação de referência necessária para entregar RF-033–036 nesta fase, deliberadamente rotulada como provisória para que uma fórmula futura definitiva possa ser identificada e comparada ao histórico já calculado sem ambiguidade (ao trocar a fórmula real, `FORMULA_VERSION` muda, e o histórico antigo permanece rastreável como calculado por uma versão anterior).
- Detalhamento por sinal:
  - **Consistência orçamentária**: para cada orçamento vigente, `100` se o consumo do período está `≤ 100%`; caso contrário, `100 − excedente` (piso `0`). Score do fator = média entre todos os orçamentos do usuário. Reaproveita `BudgetConsumptionCalculator`/`BudgetPeriodCalculator` já existentes (Fase 6) — nenhuma lógica de orçamento duplicada.
  - **Taxa de poupança**: `(receita − despesa) / receita` no período da janela do dashboard; mapeada linearmente para 0–100 onde uma taxa de poupança de 50% já satura em 100 e taxas negativas saturam em 0. Excluída se não há receita registrada no período (divisão por zero é indefinida, não zero).
  - **Diversificação de gastos**: índice inverso de concentração (`1 − HHI`, Herfindahl-Hirschman, sobre a distribuição de despesas por categoria) — gasto concentrado em poucas categorias pontua baixo, gasto distribuído pontua alto. Excluída se não há despesas no período.
  - **Tendência de saldo**: comparação entre o saldo consolidado atual e o saldo consolidado no início da janela do dashboard (reaproveita `AccountBalanceCalculator` já existente, filtrando transações até a data de corte) — saldo subindo pontua acima de 50, caindo pontua abaixo. Sempre presente (sempre computável, mesmo que o resultado seja neutro para uma conta nova sem movimentação).

### 2. Persistência de snapshot diário — satisfaz RN-005 sem scheduler dedicado

- Cada chamada a `GET /dashboard` recalcula o Pulse Score e grava (ou sobrescreve) um `PulseScoreSnapshot` único por `(userId, scoreDate)` — no máximo um snapshot por usuário por dia civil, upsert idempotente na camada de adaptador.
- **Isto é uma decisão de escopo explícita, não uma implementação completa de RN-005**: não há job periódico ativo nesta fase — o histórico (RF-035) só ganha um ponto em um dia se o usuário (ou algum processo) chamar `GET /dashboard` naquele dia. Um usuário que não abre o dashboard por uma semana não terá pontos de histórico nessa semana. Isto é aceitável para o MVP e documentado como limitação conhecida (não uma lacuna silenciosa) — a introdução de um scheduler real fica para quando a Camada de Integração Assíncrona (vision.md § 10.5) for construída.
- **RN-005 ("não pode ser editado manualmente")** é satisfeita estruturalmente: não existe endpoint de escrita direta para `PulseScoreSnapshot` — o único caminho de escrita é o próprio cálculo, disparado por uma leitura.

### 3. Endpoints — dois, não quatro

- `GET /dashboard?days=` (padrão 30, máximo 365): RF-033 (saldo consolidado, fluxo de caixa da janela, distribuição de gastos por categoria) **e** RF-034/RF-036 (Pulse Score atual com detalhamento por fator, já que a explicabilidade é apenas retornar os fatores junto do total — não justifica um endpoint dedicado separado).
- `GET /dashboard/pulse-score/history?days=` (padrão 90, máximo 365): RF-035 — lista `{ date, score, formulaVersion }` a partir dos snapshots já persistidos, sem recálculo.
- Nenhum endpoint de escrita para Pulse Score (reforça RN-005).

### 4. Nova capacidade de leitura em `TransactionRepository`

- Adicionado `findAllByUserId(String userId)` — necessário porque a agregação do dashboard (fluxo de caixa, distribuição de gastos, cálculo de tendência de saldo) precisa de todas as transações do usuário através de contas, não de uma conta ou categoria específica como os métodos existentes (`findAllByAccountIdAndUserId`, `findAllByCategoryIdAndUserId`). Mesmo padrão de porta escopada por `userId` (RF-047).

### 5. Isolamento multi-tenant (RF-047)

Mesmo padrão já estabelecido: toda leitura usada pela agregação (`Account`, `Transaction`, `Category`, `Budget`, `PulseScoreSnapshot`) é escopada por `userId`. Nenhum dado de outro usuário pode influenciar o cálculo de saldo, fluxo de caixa, distribuição de gastos ou Pulse Score.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Adiar toda a Fase 8 até que a fórmula real do Pulse Score seja definida por produto/ciência de dados | RN-006/§17.5 já preveem que a fórmula é uma pendência formal a ser resolvida depois — o próprio vision.md não bloqueia a Fase 8 nela; RF-033 (dashboard sem Pulse Score) por si só já é entregável, e uma fórmula provisória transparente e versionada entrega valor incremental sem fingir resolver a pendência de produto. |
| Não persistir nenhum snapshot — computar o Pulse Score sempre em tempo real, sem histórico (mesmo padrão de `Budget`/`Goal`) | Inviabilizaria RF-035 (evolução histórica) por completo — ao contrário de consumo de orçamento ou progresso de meta, "histórico do Pulse Score" não é recalculável a partir do estado atual; exige um ponto persistido por dia. |
| Construir um scheduler (cron) dedicado para recalcular o Pulse Score de todos os usuários diariamente | Exigiria infraestrutura (job scheduler, execução em background) que não existe em nenhuma outra parte do backend Java nesta fase — anteciparia a "Camada de Integração Assíncrona" (vision.md § 10.5), fora do escopo desta fase. Recalcular a cada leitura do dashboard é suficiente para o MVP e foi documentado como limitação, não escondido. |
| Incluir progresso de metas como um quinto sinal do Pulse Score | Não está listado em vision.md § 4.8 ("consistência orçamentária, taxa de poupança, diversificação de gastos e tendência de saldo") — adicionar seria inventar escopo não pedido. |
| Persistir o detalhamento por fator (`budgetConsistency`, `savingsRate`, etc.) no histórico exposto por RF-035 | RF-035 pede apenas "evolução histórica do Pulse Score" (o valor agregado ao longo do tempo); RF-036 (explicabilidade) já é satisfeita pelo detalhamento retornado em `GET /dashboard` para o cálculo **atual**. Expor o detalhamento histórico completo seria escopo não pedido; os quatro fatores por data continuam persistidos na tabela (não descartados), então nada impede expor isso depois se um requisito pedir. |

## Consequências

- `PulseScoreCalculator.FORMULA_VERSION` precisa ser incrementado sempre que a fórmula mudar — inclusive quando a definição formal de produto/ciência de dados (RN-006) finalmente chegar. Isto fica registrado como obrigação de processo, não apenas como comentário de código.
- O histórico de Pulse Score (RF-035) terá lacunas nos dias em que o dashboard não foi acessado — limitação conhecida, aceita para o MVP, revisitável quando a Camada de Integração Assíncrona existir.
- `roadmap.md` registra RF-034 como "fórmula provisória, pendente de definição formal de produto/ciência de dados (RN-006)" — não como "completo" no sentido de fórmula final.
