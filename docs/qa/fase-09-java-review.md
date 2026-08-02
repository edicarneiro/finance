# QA — Revisão da Fase 9 (Java): Relatórios

| Campo | Valor |
|---|---|
| Fase | 9 (Java) — RF-037, RF-038 completos; RF-039 completo para CSV, PDF fora do escopo (ver ADR-0021) |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0021) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-037 (gastos por categoria em período selecionável), RF-038 (comparativo entre dois períodos) e RF-039 (exportação CSV de relatório e de dados de transações) do vision.md. PDF está deliberadamente fora do escopo, documentado em ADR-0021, não uma lacuna silenciosa.
- [x] Não há violação de regra de negócio ou restrição do vision.md — período inválido (`startDate` após `endDate`) é rejeitado explicitamente em vez de reordenado silenciosamente.
- [x] Isolamento multi-tenant (RF-047) verificado — todos os três casos de uso desta fase usam `TransactionRepository.findAllByUserId`/`CategoryRepository.findAllByUserId`/`AccountRepository.findAllByUserId`, escopados por `userId`. Testado explicitamente em `GetSpendingByCategoryReportUseCaseTest.doesNotMixTransactionsFromAnotherUser` e `ReportControllerTest.aUserNeverSeesAnotherUsersTransactionsInAReport`.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009) — toda a Fase 9 é leitura/agregação/exportação.
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (332 testes; 29 novos nesta fase).
- [x] Sem degradação de performance evidente para o estágio atual — mesma estratégia de "uma consulta, filtro em memória" já usada pela Fase 8.
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`ReportControllerTest` como smoke test contra a raiz de composição real, incluindo exportação CSV real via HTTP).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo — nenhuma exportação em PDF foi implementada (decisão de ADR-0021), nenhuma exportação CSV do comparativo de períodos foi adicionada sem necessidade concreta.

## Verificação de Execução

```
mvn test → 332 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4)
```

## Achados Durante a Revisão

**1. `CsvWriter` vulnerável a CSV/Formula Injection — identificado e corrigido durante a própria implementação, antes desta revisão formal (severidade: alta se não corrigida — vulnerabilidade real de segurança)**

Os dois endpoints de exportação (`/reports/spending-by-category/export`, `/reports/transactions/export`) incluem campos de texto livre controlados pelo usuário — `description` da transação (RF-014), nome de categoria/conta e tags (RF-017) — em um arquivo CSV tipicamente aberto no Excel/Google Sheets. Um valor começando com `=`, `+`, `-`, `@` ou tabulação é interpretado como fórmula por essas ferramentas (classe de vulnerabilidade conhecida como [CSV/Formula Injection](https://owasp.org/www-community/attacks/CSV_Injection)); em versões vulneráveis do Excel isso pode levar a execução de comando via `=cmd|'/c calc'!A1` ou fórmulas semelhantes, se o usuário (ou um terceiro que injete dados via alguma integração futura) conseguir controlar esse texto. **Resolução**: `CsvWriter.escape` agora prefixa qualquer valor iniciado por um desses caracteres com um apóstrofo, forçando interpretação como texto — implementado e testado (`CsvWriterTest.neutralizesAFormulaInjectionAttemptStartingWithAnEqualsSign` e variações) antes desta revisão formal ser escrita, seguindo a instrução de corrigir imediatamente ao identificar código inseguro. Não é mais uma pendência.

**2. Sem cabeçalho BOM UTF-8 no CSV (severidade: baixa — nota de usabilidade, não bloqueante)**

Ao abrir o CSV exportado diretamente por duplo clique no Excel (em vez de importar explicitamente como UTF-8), nomes de categoria/conta com acentuação podem ser exibidos incorretamente em versões mais antigas do Excel no Windows, que assumem `latin1`/`cp1252` por padrão sem um BOM. Documentado como limitação conhecida no README (não uma lacuna silenciosa); a correção (prefixar o corpo com `﻿`) é trivial de adicionar depois se houver relato de usuário.

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`neutralizeFormulaInjection`, `ReportPeriod.contains`, `SpendingByCategoryCalculator`); a decisão de não reordenar datas invertidas e a de escopo de CSV-only estão documentadas tanto em Javadoc quanto em ADR-0021.

**SOLID**: `SpendingByCategoryCalculator` e `PeriodComparisonCalculator` são funções puras e isoladas (sem I/O, sem dependência de framework); `SpendingByCategoryCalculator` é a mesma implementação usada pela Fase 8 (extraída, não duplicada — verifiquei que `GetDashboardUseCase` não mantém uma segunda cópia da lógica de agrupamento por categoria após o refactor). A serialização CSV (`CsvWriter`) vive inteiramente em `adapters/in/web`, mantendo os casos de uso (`application/usecases/report/`) agnósticos de formato de exportação — confirmei que nenhum dos três casos de uso desta fase importa `CsvWriter` ou qualquer classe de `adapters/`. Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/report/` ou `application/{usecases/report,services}`.

**Testes**: `PeriodComparisonCalculatorTest` cobre o caso de borda mais importante da fórmula (categoria presente em apenas um dos dois períodos, `percentageChange` nulo quando a base é zero) — exatamente o tipo de teste que pegaria uma divisão por zero não tratada. `ReportControllerTest` inclui exportação CSV real via HTTP com verificação de conteúdo (não apenas status HTTP), e um teste dedicado de rejeição de período invertido (HTTP 400). A suíte de `CsvWriterTest` cobre tanto o escaping RFC 4180 padrão quanto a neutralização de injeção de fórmula, incluindo o caso de não-regressão (um valor que apenas contém um hífen no meio, não no início, não deve ser prefixado).

**Segurança**: `userId` nunca aceito do corpo/parâmetros da requisição; toda leitura escopada por `userId` nas portas de repositório. A vulnerabilidade de CSV injection (Achado 1) foi identificada e corrigida antes desta revisão, com cobertura de teste específica.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Sem BOM UTF-8 no CSV (Achado 2 acima) — nota de usabilidade, não uma falha funcional.
2. Sem exportação CSV do comparativo de períodos (RF-038) — decisão de escopo explícita (ADR-0021), pode ser adicionada sob demanda.
3. Corrida de leitura concorrente durante a geração de um relatório muito grande não é um risco novo — mesma classe de característica já aceita em `GetDashboardUseCase` (Fase 8), que também busca todas as transações do usuário em uma única consulta.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Uma vulnerabilidade de segurança real (CSV/Formula Injection) foi identificada e corrigida durante a própria implementação, com cobertura de teste dedicada — tratada com a mesma prioridade de um defeito funcional, conforme esperado para qualquer funcionalidade que gere arquivos abertos por ferramentas de terceiros a partir de dados controlados pelo usuário. Um achado de usabilidade de baixa severidade foi registrado como nota não bloqueante. Nenhum apontamento de regra de negócio ou de isolamento multi-tenant foi identificado.
