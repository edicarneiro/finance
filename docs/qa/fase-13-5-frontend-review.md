# QA — Revisão da Fase 13.5 (Frontend): Dashboard e Pulse Score

| Campo | Valor |
|---|---|
| Fase | 13.5 (Frontend) — Dashboard e Pulse Score, consumindo `DashboardController` |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Esta fase não introduziu nenhuma decisão arquitetural nova (aplica integralmente ADR-0025) — mesmo padrão já usado em 13.2–13.4, sem novo ADR do CTO.

## Checklist de Qualidade

- [x] A implementação atende ao escopo real do backend: saldo consolidado, fluxo de caixa por janela de dias, gastos por categoria, Pulse Score com fatores explicáveis, e histórico do Pulse Score.
- [x] `DashboardPage` substitui corretamente o placeholder (`HomePage`) na rota `/`, conforme o próprio texto do placeholder já anunciava.
- [x] Nenhuma regra de negócio duplicada no cliente — a fórmula do Pulse Score não é recalculada no cliente, apenas exibida com a ressalva de que é provisória (mesma informação que o backend já expõe via `formulaVersion`).
- [x] Testes cobrem carregamento com dados, troca de janela de período, histórico do Pulse Score e estado vazio (43 testes de frontend no total; 4 novos nesta fase).
- [x] Aderente à cláusula de frontend de `rules.md` §3: `DashboardPage.test.tsx` renderiza o `App` real com MSW na fronteira de rede.
- [x] Sem degradação de performance evidente (bundle: 399 KB / 119 KB gzip).
- [x] Código segue Clean Code e SOLID (ver Achados abaixo — dois corrigidos durante a revisão).
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada, incluindo o efeito de a rota `/` mudar de dono.
- [x] Não há introdução de funcionalidade fora do escopo — nenhuma biblioteca de gráficos foi adicionada sem decisão do CTO; histórico e gastos por categoria são exibidos como lista.

## Verificação de Execução

```
npm test    → 43 testes, 100% passando (Vitest, jsdom, MSW), 2 execuções consecutivas estáveis após as correções
npm run build → sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos
```

Verificação manual adicional contra o backend real, via `docker compose -f docker-compose.dev.yml up`: dashboard vazio do usuário semeado (saldo, fluxo e Pulse Score zerados/base), depois com uma conta e uma transação reais lançadas — saldo consolidado, fluxo de caixa, gastos por categoria e Pulse Score todos refletindo os dados reais corretamente.

## Achados Durante a Revisão

**1. Trocar a rota `/` de `HomePage` para `DashboardPage` quebrava três testes de outras telas (severidade: alta — suíte de testes, corrigido nesta revisão)**

`LoginPage.test.tsx` e `RegisterPage.test.tsx` verificavam o redirecionamento pós-autenticação buscando o texto `"Bem-vindo ao FinancePulse"` (da `HomePage`, agora removida) — passaram a falhar. Mais importante: `DashboardPage` faz chamadas reais a `GET /dashboard` e `GET /dashboard/pulse-score/history` ao montar; como `test/server.ts` não tinha handlers para essas rotas, **qualquer teste que chegasse a `/` autenticado** (incluindo o teste de logout em `AuthContext.test.tsx`) passaria a falhar com um erro de requisição não tratada (`onUnhandledRequest: 'error'`), não apenas os testes desta fase. **Resolução**: adicionados handlers MSW padrão para as duas rotas (sempre ativos, não só nos testes do Dashboard), com estado configurável via `seedDashboard`/`seedPulseScoreHistory`; as duas asserções desatualizadas foram trocadas para verificar o heading real `"Dashboard"`.

**2. O seletor de período estava posicionado dentro de "Fluxo de caixa", mas também recalculava "Gastos por categoria" sem indicação visual (severidade: média — UX real, corrigido nesta revisão)**

Verifiquei o backend (`GetDashboardUseCase`) e confirmei que `spendingByCategory` é calculado com a **mesma janela de dias** (`since = today.minusDays(windowDays)`) que `cashFlow` — ambos vêm da mesma chamada `GET /dashboard?days=N`. A implementação inicial colocava o seletor "Período" dentro da seção "Fluxo de caixa", dando a impressão de que ele só afetava aquela seção; na prática, mudar o período também recalculava silenciosamente "Gastos por categoria", uma seção visualmente separada mais abaixo — um usuário real não teria como saber disso. **Resolução**: o seletor foi movido para fora das duas seções, com o rótulo explícito "Período do resumo (fluxo de caixa e gastos por categoria)", deixando o escopo compartilhado visível. O teste correspondente foi renomeado e atualizado para refletir a correção.

## Avaliação por Critério

**Clean Code**: `FACTOR_LABELS` centraliza a tradução dos nomes de fator reais do backend (`budgetConsistency`, `savingsRate`, `spendingDiversification`, `balanceTrend` — verificados em `PulseScoreCalculator.java`, não inventados) para rótulos em português, com fallback para o nome bruto caso o backend adicione um fator novo sem o frontend ser atualizado.

**SOLID/estrutura**: `dashboardApi.ts` segue o mesmo formato dos demais módulos de API. `DashboardPage` é somente leitura (sem mutações) — corretamente mais simples que as páginas de CRUD anteriores, sem abstração desnecessária para o padrão de formulário/edição que não se aplica aqui.

**Testes**: a suíte cobre o estado com dados, a troca de período (incluindo a correção do Achado 2), o histórico do Pulse Score e o estado vazio de gastos por categoria. O achado de que a mudança de rota afetava testes de OUTRAS fases foi descoberto justamente por rodar a suíte completa, não apenas os testes novos — reforça o valor de sempre rodar tudo, não só o que foi escrito na fase atual.

**Segurança**: nenhuma chamada aceita parâmetros de escopo de outro usuário; a URL da API sempre usa o `days` como um inteiro simples, sem risco de injeção.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Sem biblioteca de gráficos — histórico do Pulse Score e gastos por categoria são listas, não gráficos visuais. Decisão consciente de não introduzir uma nova dependência sem uma decisão do CTO; pode ser revisitado em uma fase futura dedicada, se desejado.
2. `formatCurrency` continua duplicada entre páginas de fases diferentes (já registrado como item não bloqueante em 13.4) — não expandido nesta fase.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Dois achados reais foram identificados e corrigidos durante esta revisão: um de alto impacto na suíte de testes (troca de dono da rota `/` quebrando testes de fases anteriores, não apenas desta fase) e um de UX real (escopo compartilhado de um controle de período sem indicação visual, confirmado contra o código-fonte do backend). Ambos confirmados resolvidos com a suíte completa (43 testes) passando de forma estável. Nenhum apontamento crítico adicional foi identificado.
