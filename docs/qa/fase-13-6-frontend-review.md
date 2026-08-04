# QA — Revisão da Fase 13.6 (Frontend): Relatórios

| Campo | Valor |
|---|---|
| Fase | 13.6 (Frontend) — Relatórios (gastos por categoria, comparação de períodos, exportação CSV), consumindo `ReportController` |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Esta fase não introduziu nenhuma decisão arquitetural nova (aplica integralmente ADR-0025) — a única capacidade nova (download de arquivo autenticado) é uma extensão do módulo de rede já existente, não um padrão diferente. Sem novo ADR do CTO, mesmo padrão de 13.2–13.5.

## Checklist de Qualidade

- [x] A implementação atende ao escopo real do backend: gastos por categoria (com exportação CSV), comparação de dois períodos, e exportação CSV de transações.
- [x] Nenhuma regra de negócio duplicada no cliente — `percentageChange` nulo (base zero) é exibido como "variação indefinida" refletindo exatamente a semântica já documentada pelo backend, sem recalcular nada.
- [x] Testes cobrem geração de relatório, comparação de períodos (incluindo o caso de base zero), download de ambos os CSVs, e os dois caminhos de erro real do backend (46 → 49 testes de frontend; 6 novos nesta fase, 2 deles regressão dos achados abaixo).
- [x] Aderente à cláusula de frontend de `rules.md` §3: `ReportsPage.test.tsx` renderiza o `App` real com MSW na fronteira de rede, incluindo o fluxo completo de download (clique real → fetch autenticado → Blob → link programático).
- [x] Sem degradação de performance evidente (bundle: 405 KB / 121 KB gzip).
- [x] Código segue Clean Code e SOLID (ver Achados abaixo — dois corrigidos durante a revisão).
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada, incluindo a decisão de desabilitar retry de `useQuery`.
- [x] Não há introdução de funcionalidade fora do escopo — nenhuma exportação de comparação de períodos foi adicionada (o backend não expõe isso; só `spending-by-category/export` e `transactions/export` existem).

## Verificação de Execução

```
npm test    → 49 testes, 100% passando (Vitest, jsdom, MSW), 2 execuções consecutivas estáveis após as correções
npm run build → sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos
```

Verificação manual adicional contra o backend real, via `docker compose -f docker-compose.dev.yml up`: relatório de gastos por categoria com dado real, exportação CSV com os headers `Content-Disposition`/`Content-Type` reais confirmados byte a byte contra o que o frontend espera, comparação de períodos incluindo o caso real de `percentageChange: null` (categoria com valor zero no período A).

## Achados Durante a Revisão

**1. Erros de `useQuery` mostravam uma mensagem genérica em vez da mensagem real do backend (severidade: média — inconsistência real com o padrão do projeto, corrigido nesta revisão)**

`SpendingByCategorySection` e `PeriodComparisonSection` usam `useQuery` (leitura acionada por um formulário, não uma mutação) e, no caminho de erro, exibiam um texto fixo (`"Não foi possível gerar o relatório."`/`"Não foi possível comparar os períodos."`) em vez da mensagem real retornada pelo backend — inconsistente com **todas** as outras páginas do projeto (Accounts, Categories, Transactions, Budgets, Goals), que sempre mostram `error.message` de uma `ApiError` real quando disponível. **Resolução**: as duas seções agora checam `error instanceof ApiError` e exibem `error.message`, com o texto genérico apenas como fallback caso o erro não seja uma `ApiError` (ex.: falha de rede). Dois testes de regressão adicionados, cada um forçando um `400` real via MSW com uma mensagem específica e verificando que ela — não um texto genérico — aparece na tela.

**2. `useQuery` tenta de novo 3x com backoff antes de assentar em erro — os testes de erro do Achado 1 ficavam presos em "Carregando…" além do timeout padrão (severidade: média — comportamento real de produção também afetado, não só os testes, corrigido nesta revisão)**

Ao escrever os testes de regressão do Achado 1, descobri que `useQuery` (diferente de `useMutation`, que não tenta de novo por padrão) tenta a requisição de novo automaticamente até 3 vezes com backoff exponencial antes de expor o erro — o `queryClient` do projeto (`src/queryClient.ts`) nunca havia desabilitado isso, porque nenhuma tela anterior baseada em `useQuery` (Dashboard, listagens de Accounts/Categories/etc.) tinha um teste dedicado ao caminho de erro dessa chamada. **Impacto real, não só de teste**: um usuário que envie um período inválido (`startDate > endDate`) veria a tela presa em "Carregando…" por vários segundos antes do erro aparecer, já que o erro é determinístico (validação do backend) — tentar de novo nunca muda o resultado, só atrasa o feedback. **Resolução**: `queryClient` agora define `defaultOptions.queries.retry: false` globalmente. Verificado que isso não afeta nenhum caminho de sucesso já testado (suíte completa permanece verde) — apenas acelera a exposição de erros determinísticos, em todas as páginas que usam `useQuery`, não só Relatórios.

## Avaliação por Critério

**Clean Code**: `httpClient.downloadFile` documenta com um comentário o "porquê" de não poder usar um `<a href>` simples (autenticação via header, não cookie) — não o "o quê" (o código já mostra os passos). `filenameFromContentDisposition` isola a extração do nome do arquivo do header real do backend, testável e reaproveitável para os dois endpoints de exportação.

**SOLID/estrutura**: `reportsApi.ts` segue o formato dos demais módulos, com uma distinção clara entre chamadas que retornam dados (`apiRequest`) e chamadas que disparam download (`downloadFile`) — a página não precisa saber como cada uma funciona internamente.

**Testes**: a suíte cobre os três relatórios, o caso de borda de `percentageChange` nulo (verificado contra o comportamento real do backend, não inventado), e — depois da correção do Achado 1 — os caminhos de erro reais de duas das três seções. O fluxo de download é exercitado de ponta a ponta (clique real, não uma chamada direta à função) nos dois endpoints de exportação.

**Segurança**: nenhuma chamada aceita parâmetros de escopo de outro usuário; o download de arquivo usa o mesmo token/cabeçalho `Authorization` das demais chamadas, sem introduzir um caminho de autenticação paralelo.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Sem exportação CSV para a comparação de períodos — o backend não expõe esse endpoint (só `spending-by-category/export` e `transactions/export` existem); não é uma lacuna do frontend.
2. Aviso benigno do jsdom no console (`Not implemented: navigation to another Document`) durante os testes de download — comportamento esperado do ambiente de teste ao simular o clique em um link com `download`, não indica um problema real; documentado no README para não ser confundido com uma falha futura.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Dois achados reais foram identificados e corrigidos durante esta revisão: uma inconsistência de UX (mensagem de erro genérica em vez da real, divergindo do padrão já estabelecido em todas as outras páginas) e um comportamento de produção real (retry automático de `useQuery` atrasando desnecessariamente a exposição de um erro determinístico) — este último descoberto organicamente ao escrever o teste de regressão do primeiro achado, e corrigido de forma a beneficiar todas as páginas do projeto que usam `useQuery`, não apenas Relatórios. Toda alegação de comportamento foi verificada com execução real contra `backend-java` via Docker Compose. Nenhum apontamento crítico adicional foi identificado.
