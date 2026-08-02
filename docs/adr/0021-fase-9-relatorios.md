# ADR-0021: Fase 9 (Java) — Relatórios, escopo de exportação e extração de SpendingByCategoryCalculator

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-08-01 |
| Autor | CTO / Principal Software Architect |
| Fase | 9 (Java) — RF-037, RF-038 completos; RF-039 completo para CSV, PDF deliberadamente fora do escopo |

## Contexto

Fase 9 cobre RF-037 (gastos por categoria em período selecionável), RF-038 (comparativo entre períodos) e RF-039 (exportação de relatórios e dados de transações, "ex.: CSV/PDF").

**Duas decisões de escopo, ambas com base literal no texto do vision.md, precisam de registro explícito:**

1. **RF-039 cita "CSV/PDF" como exemplo** ("ex.:"), não como uma dupla obrigatória. PDF exigiria uma nova dependência de biblioteca (nenhuma existe no `pom.xml` atual — o projeto usa apenas Spring Data JPA, H2, `spring-security-crypto`, `jjwt`, JUnit/AssertJ) e uma decisão de layout/formatação que não está especificada em nenhum requisito. CSV, em contraste, é gerável com Java puro, sem dependência nova, e atende integralmente "dados de transações" (RF-039) — a motivação central do requisito.
2. **RF-037/RF-038 não especificam um formato de endpoint** — apenas "período selecionável" e "comparativo entre períodos (ex.: mês atual vs. mês anterior)". "Mês atual vs. mês anterior" é um exemplo de uso, não uma prescrição de que o backend deva calcular esses períodos internamente.

## Decisão

### 1. RF-039 — exportação CSV nesta fase; PDF explicitamente adiado

- Implementados dois endpoints de exportação CSV: gastos por categoria (`GET /reports/spending-by-category/export`) e dados brutos de transações (`GET /reports/transactions/export`), cobrindo tanto "relatórios" quanto "dados de transações" citados em RF-039.
- **PDF não é implementado nesta fase.** Não há biblioteca de geração de PDF no projeto, e adicionar uma introduziria uma dependência nova e uma decisão de layout visual não especificada em nenhum requisito — exatamente o tipo de escopo especulativo que este processo evita (mesmo racional já aplicado a RF-022/028/032: entregar o que está pedido agora, não antecipar o que não está). A camada de dados dos relatórios (`GetSpendingByCategoryReportUseCase`, `GetPeriodComparisonReportUseCase`, `GetTransactionsForPeriodUseCase`) é independente de formato — devolve dados estruturados; a serialização CSV vive inteiramente na camada de adaptador (`adapters/in/web`), então adicionar um serializador PDF depois não exige redesenhar nenhum caso de uso.
- CSV gerado com um formatter próprio (`adapters/in/web/CsvWriter`), sem dependência externa — separador vírgula, aspas quando o valor contém vírgula/aspas/quebra de linha (RFC 4180 simplificado), `CRLF` como terminador de linha.
- Comparativo de períodos (RF-038) **não tem exportação CSV própria nesta fase** — decisão de escopo deliberada, não uma lacuna: os dois exports já entregues (gastos por categoria, transações brutas) cobrem a motivação central de RF-039; um terceiro export para o comparativo pode ser adicionado sob demanda, com a mesma forma dos dois já existentes.

### 2. RF-037/RF-038 — período explícito, sem presets embutidos no backend

- `GET /reports/spending-by-category?startDate=&endDate=` (RF-037) e `GET /reports/period-comparison?periodAStart=&periodAEnd=&periodBStart=&periodBEnd=` (RF-038) recebem datas explícitas do cliente, em vez de o backend assumir "mês atual" ou "mês anterior" internamente. "Mês atual vs. mês anterior" (o exemplo do RF-038) é responsabilidade do cliente calcular e enviar como datas — o backend permanece genérico para qualquer comparação de dois intervalos, não apenas mês-a-mês.
- Um período inválido (`startDate` após `endDate`) é rejeitado explicitamente (`InvalidReportPeriodException`, HTTP 400) via um novo `ReportPeriod` (record com construtor compacto validador, `application/services/ReportPeriod.java`) — decisão deliberada de **não** reordenar silenciosamente as datas, o que mascararia um erro real do cliente.
- Sem novo estado persistido: ao contrário da Fase 8 (Pulse Score, que precisa de snapshot diário para ter histórico), os relatórios desta fase sempre operam sobre um período explícito fornecido pela chamada — não há noção de "evolução ao longo do tempo sem fim definido" que exigisse persistência. Mesmo racional de "derivar, não persistir" já aplicado a `Budget`/`Goal` (ADR-0018/0019).

### 3. Extração de `SpendingByCategoryCalculator` (refatoração, não mudança de comportamento)

- A lógica de "agrupar despesas por categoria e calcular percentual do total" já existia, inline, em `GetDashboardUseCase` (Fase 8). Esta fase precisa exatamente da mesma lógica para RF-037 (mesmo cálculo, aplicado a um período explícito em vez de uma janela rolante). Em vez de duplicar pela segunda vez, foi extraída para `application/services/SpendingByCategoryCalculator.java`, função pura reaproveitada por ambos os casos de uso.
- `GetDashboardUseCase` foi refatorado para usar o novo serviço — comportamento idêntico, verificado pela suíte de testes da Fase 8 (`GetDashboardUseCaseTest`, `DashboardControllerTest`) permanecendo verde sem alteração.

### 4. Isolamento multi-tenant (RF-047)

Mesmo padrão já estabelecido: toda leitura usada pelos relatórios (`Transaction`, `Account`, `Category`) é escopada por `userId`. Nenhum dado de outro usuário pode aparecer em um relatório ou exportação.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Implementar exportação em PDF nesta fase | Exigiria uma dependência nova (nenhuma biblioteca de PDF existe no projeto) e decisões de layout visual não especificadas em nenhum requisito — vision.md já cita "CSV/PDF" como exemplo, não como par obrigatório. |
| Backend calcular automaticamente "mês atual" e "mês anterior" para RF-038 | Reduziria a generalidade do endpoint a um único caso de uso; datas explícitas cobrem o exemplo do RF-038 e qualquer outra comparação de períodos sem lógica adicional. |
| Reordenar silenciosamente `startDate`/`endDate` quando invertidos | Mascararia um erro real de entrada do cliente — rejeitar explicitamente (HTTP 400) é consistente com a disciplina de validação já aplicada a outras entradas do projeto (ex.: `InvalidGoalDeadlineException`). |
| Persistir um "relatório" como entidade (semelhante a `PulseScoreSnapshot`) | Não há requisito de "evolução histórica" para RF-037/038 como existe para o Pulse Score (RF-035) — cada chamada já recebe um período explícito e completo; não há necessidade de reconstruir "o relatório de um dia específico" depois. |
| Adicionar exportação CSV para o comparativo de períodos (RF-038) nesta mesma fase | Os dois exports já entregues cobrem a motivação central de RF-039 ("relatórios e dados de transações"); adicionar um terceiro sem uma necessidade concreta seria escopo especulativo — pode ser adicionado sob demanda, mesma forma dos dois existentes. |

## Consequências

- `roadmap.md` registra RF-039 como "CSV completo, PDF fora do escopo desta fase" — não como uma limitação escondida.
- A separação entre casos de uso (dados estruturados) e serialização CSV (camada de adaptador) permite adicionar um formato de exportação novo (PDF, XLSX) futuramente sem alterar `application/`.
- `SpendingByCategoryCalculator` passa a ser a única implementação de "agrupar despesas por categoria" no backend Java — qualquer fase futura que precise do mesmo cálculo deve reaproveitá-lo, não duplicá-lo pela terceira vez.
