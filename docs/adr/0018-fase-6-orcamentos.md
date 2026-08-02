# ADR-0018: Fase 6 (Java) — Orçamentos, período recorrente e adiamento da entrega de alertas (RF-028)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | 6 (Java) — RF-026, RF-027, RF-029 completos; RF-028 parcial (sinal calculado, entrega adiada) |

## Contexto

Fase 6 cobre RF-026 (criação de orçamentos por categoria e período), RF-027 (percentual de consumo em tempo real), RF-028 (notificar ao atingir limiares configuráveis) e RF-029 (histórico de desempenho de períodos anteriores), sobre RN-004 ("um orçamento é sempre associado a uma categoria e a um período recorrente; o consumo é recalculado a cada nova transação relevante").

**Mesmo padrão de dependência já resolvido em ADR-0016 e ADR-0017 foi encontrado aqui**: RF-028 exige "notificar o usuário", mas a infraestrutura de notificação (RF-040–043) é a Fase 10 — que, no próprio `roadmap.md`, **depende da Fase 6** ("Fases 6, 7"), confirmando que a entrega de notificações foi sempre planejada para vir depois do sinal que a dispara. Não há adaptador de e-mail, push ou notificação in-app no backend Java hoje.

## Decisão

### 1. Modelo de domínio

- `Budget`: `id`, `userId`, `categoryId`, `limitAmount` (`BigDecimal`, sempre positivo), `periodType` (`MONTHLY`, `WEEKLY`, `CUSTOM`), `customPeriodStart`/`customPeriodEnd` (apenas para `CUSTOM`), `alertThresholds` (lista de percentuais, ex.: `[80, 100]`, padrão `[80, 100]` se omitido), `createdAt`.
- **`categoryId` e `periodType` são imutáveis após a criação** — mesma decisão já aplicada a `Account.type`/`Account.currency` (ADR-0014) e `Category.parentCategoryId` (ADR-0017): mudar a categoria ou o tipo de período de um orçamento já em consumo tornaria o histórico (RF-029) ambíguo. Apenas `limitAmount` e `alertThresholds` são editáveis via `PUT`.
- **Interpretação de "período recorrente" (RN-004) por tipo**:
  - `MONTHLY`/`WEEKLY`: **recorrentes de fato** — não armazenam datas fixas; o período vigente é sempre calculado a partir da data atual (mês civil corrente / semana ISO corrente, segunda a domingo). Isso é o que torna o orçamento "sempre atual" sem exigir um job de renovação.
  - `CUSTOM`: um intervalo de datas fixo definido pelo usuário (`customPeriodStart`/`customPeriodEnd`), **não recorrente**. RF-026 lista "customizado" como uma opção de período junto com mensal/semanal; a leitura mais direta de "customizado" é um intervalo definido pelo usuário, não uma recorrência com passo arbitrário — que exigiria uma unidade de repetição não especificada em nenhum requisito. Consequência assumida: RF-029 (histórico de períodos anteriores) não se aplica a orçamentos `CUSTOM` — não há "período anterior" de um intervalo único (retorna lista vazia, não erro).
- Apenas transações do tipo **EXPENSE** contam para o consumo — RF-026 fala em "limite de gasto"; receitas não consomem o orçamento (decisão direta do texto do requisito, não uma extensão de escopo).

### 2. Cálculo de período e consumo — funções puras, sem persistência de snapshot

- `BudgetPeriodCalculator`: dado um `Budget` e uma data de referência, deriva o intervalo do período vigente (ou de um período anterior, para RF-029) — sem estado, sem I/O.
- `BudgetConsumptionCalculator`: dado um `Budget`, um intervalo de período e a lista de transações de despesa da categoria nesse intervalo, calcula valor consumido, percentual e quais `alertThresholds` foram ultrapassados.
- **RF-029 é resolvido sem uma entidade de histórico persistida**: como `Transaction.date` é um registro histórico imutável, o consumo de qualquer período passado é recalculado sob demanda a partir das transações já existentes — não há necessidade de "fechar" um período e arquivar um snapshot. Mais simples e sem risco de divergência entre o snapshot e a fonte de verdade (as transações).
- Introduzida a porta `Clock` (`today(): LocalDate`), com adaptador `SystemClock` — necessária pela primeira vez no backend Java para tornar o cálculo de "período vigente" testável (mesmo padrão `Clock`/`FixedClock` já usado no backend TypeScript desde a Fase 2.1).

### 3. RF-028 — sinal calculado agora, entrega adiada para a Fase 10

Esta fase entrega o **sinal**: cada orçamento listado inclui `thresholdsCrossed` (quais limiares configurados já foram ultrapassados pelo consumo atual). A **entrega** (e-mail, push, notificação in-app) fica para a Fase 10, quando a infraestrutura de notificação (RF-040–043) existir — construir um canal de entrega agora, sem RF-040–043, seria especulativo, mesmo raciocínio já aplicado a RF-022 (ADR-0017).

### 4. Isolamento multi-tenant (RF-047)

Mesmo padrão já estabelecido: toda leitura/escrita de `Budget` é escopada por `userId` na própria assinatura do repositório. Criar um orçamento exige que a categoria informada pertença ao usuário autenticado (mesma validação já aplicada a `Transaction`).

### 5. Orçamento em categoria de nível superior **não** agrega gastos de subcategorias (achado na revisão de QA)

A Fase 5 introduziu subcategorias (ADR-0017). Um orçamento criado sobre uma categoria de nível superior (ex.: "Alimentação") considera **apenas** transações lançadas diretamente naquela categoria — gastos lançados em uma subcategoria seguida (ex.: "Restaurante", filha de "Alimentação") **não** contam para esse orçamento. Cada orçamento é isolado à categoria exata que ele referencia, sem soma da árvore.

Esta é uma decisão consciente, não um gap silencioso: nenhum requisito (RF-026 a RF-029, RN-004) menciona agregação hierárquica de orçamento, e implementá-la exigiria decidir uma segunda regra não pedida (um orçamento na subcategoria "Restaurante" também deveria contar em dobro para o orçamento de "Alimentação"? Seria permitido orçar pai e filho simultaneamente?) sem base no vision.md para resolver essas perguntas. Fica registrado como candidato a revisão de produto, não como defeito.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Persistir um snapshot de consumo ao final de cada período (job agendado) | Desnecessário — o consumo de qualquer período é sempre recalculável a partir de `Transaction.date`, sem risco de a fonte de verdade divergir do snapshot. Adiciona complexidade (agendamento, reprocessamento) sem benefício real no estágio atual. |
| Implementar RF-028 com um canal de notificação simplificado (ex.: log) já nesta fase | Repetiria o padrão de RF-022: construir infraestrutura sem que o requisito real (RF-040–043) exista, arriscando retrabalho quando a Fase 10 definir o formato real de notificação. |
| Tratar `CUSTOM` como recorrente com um passo de N dias configurável | Nenhum requisito pede essa unidade de repetição; adicionaria um campo e uma lógica de cálculo sem necessidade demonstrada. |
| Tornar `categoryId`/`periodType` editáveis | Mudaria a semântica do histórico (RF-029) de forma ambígua sem requisito que peça essa flexibilidade. |
| Orçamento em categoria-pai agregar automaticamente subcategorias | Nenhum requisito pede a agregação, e a regra levanta perguntas de produto (dupla contagem, orçar pai e filho simultaneamente) sem resposta no vision.md — melhor tratar como decisão de produto futura do que resolver arbitrariamente agora. |

## Consequências

- `roadmap.md` mantém RF-028 vinculado à Fase 6 (sinal) e à Fase 10 (entrega) — não uma pendência solta.
- Introduzida a primeira porta `Clock` do backend Java — precedente para qualquer cálculo futuro sensível a "data atual" (ex.: RF-043, lembretes de transação recorrente prevista, Fase 4.2/10).
- `TransactionRepository` ganha `findAllByCategoryIdAndUserId`, reaproveitável por relatórios futuros (RF-037–039, Fase 9) que também agregam por categoria.
- Orçamentos não agregam subcategorias — se o produto decidir que devem, será uma mudança de comportamento em fase futura, não uma correção de bug.
