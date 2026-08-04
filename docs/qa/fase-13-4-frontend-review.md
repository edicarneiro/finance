# QA — Revisão da Fase 13.4 (Frontend): Orçamentos e Metas

| Campo | Valor |
|---|---|
| Fase | 13.4 (Frontend) — CRUD de orçamentos e metas, consumindo `BudgetController`/`GoalController` |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Esta fase não introduziu nenhuma decisão arquitetural nova (aplica integralmente ADR-0025) — mesmo padrão já usado em 13.2/13.3, sem novo ADR do CTO.

## Checklist de Qualidade

- [x] A implementação atende ao escopo real do backend: CRUD de orçamentos (com período mensal/semanal/customizado e histórico) e CRUD de metas (com associação exclusiva a conta ou categoria).
- [x] Nenhuma regra de negócio duplicada no cliente — a exibição condicional dos campos de período customizado e o seletor "Vincular a" (conta/categoria) são affordances de UI que evitam erros previsíveis, não reimplementações das regras (`InvalidBudgetPeriodException`, `InvalidGoalAssociationException` continuam sendo decididas pelo backend).
- [x] Testes cobrem listagem, criação (incluindo os dois casos de associação de meta), edição, exclusão e histórico de orçamento (39 testes de frontend no total; 9 novos nesta fase).
- [x] Aderente à cláusula de frontend de `rules.md` §3: `BudgetsPage.test.tsx`/`GoalsPage.test.tsx` renderizam o `App` real com MSW na fronteira de rede.
- [x] Sem degradação de performance evidente (bundle: 395 KB / 118 KB gzip).
- [x] Código segue Clean Code e SOLID (ver Achado 1 abaixo — corrigido durante a revisão).
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada.
- [x] Não há introdução de funcionalidade fora do escopo.

## Verificação de Execução

```
npm test    → 39 testes, 100% passando (Vitest, jsdom, MSW), 3 execuções consecutivas estáveis
npm run build → sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos
```

Verificação manual adicional contra o backend real (não apenas MSW), via `docker compose -f docker-compose.dev.yml up`: criação de orçamento mensal, listagem, atualização (`HTTP 200` corpo vazio, consistente com o achado da Fase 13.2), histórico de 3 períodos; criação de meta associada a conta, tentativa de associar a conta E categoria simultaneamente (`400`, mensagem exata `"Informe exatamente uma associação..."`), tentativa sem nenhuma associação (mesma mensagem), atualização e exclusão de meta.

## Achados Durante a Revisão

**1. `toThresholdList` duplicada verbatim entre `BudgetsPage.tsx` e `GoalsPage.tsx` (severidade: baixa — Clean Code, corrigido nesta revisão)**

A função que converte uma string "80, 100" em `[80, 100]` (usada para os limiares de alerta de orçamentos e metas) foi implementada de forma idêntica, cópia-e-cola, nos dois arquivos. **Resolução**: extraída para `src/utils/thresholds.ts`, importada por ambas as páginas. Suíte completa (39 testes) confirmada passando após a extração, sem alteração de comportamento.

## Avaliação por Critério

**Clean Code**: após a correção do Achado 1, não há mais duplicação exata entre os módulos desta fase. `formatCurrency` continua duplicada entre `AccountsPage`/`TransactionsPage` (com parâmetro de moeda) e `BudgetsPage`/`GoalsPage` (BRL fixo) — não são implementações idênticas (assinaturas diferentes) e tocam páginas de fases já aprovadas anteriormente; registrado como item não bloqueante em vez de forçar um refactor mais amplo fora do escopo desta fase.

**SOLID/estrutura**: `budgetsApi.ts`/`goalsApi.ts` seguem o mesmo formato dos módulos de API anteriores. O seletor de associação de meta (`associationType`) isola a decisão de qual campo enviar inteiramente no componente de formulário, sem vazar essa lógica para o módulo de API (que só aceita `accountId`/`categoryId` já resolvidos).

**Testes**: a suíte cobre os dois casos de associação de meta (conta e categoria) separadamente, não apenas o caminho feliz de um deles — importante porque é a regra de negócio mais específica desta fase. O teste de "campos de período customizado só aparecem quando selecionado" verifica a affordance de UI diretamente, sem depender de uma chamada de rede.

**Segurança**: nenhuma chamada aceita escopo de outro usuário do cliente; valores monetários (`limitAmount`, `targetAmount`) seguem o mesmo padrão de validação client-side (`.positive()`) já usado em Contas/Transações, sempre como complemento, nunca substituto da validação do backend.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. `formatCurrency` duplicada (com assinaturas diferentes) entre páginas de fases distintas — não corrigido nesta revisão por tocar páginas já aprovadas em fases anteriores sem necessidade correspondente nesta fase.
2. Edição de orçamento/meta não usa validação Zod (apenas inputs controlados simples) — consistente com o padrão já estabelecido nas edições inline de Contas/Categorias/Transações (nenhuma dessas usa Zod na edição também), não uma inconsistência nova.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real de Clean Code (duplicação verbatim de uma função utilitária entre dois arquivos novos desta fase) foi identificado e corrigido durante a revisão. A regra de negócio mais específica desta fase — associação exclusiva de meta a conta ou categoria — foi verificada tanto via teste automatizado quanto manualmente contra o backend real, incluindo os dois casos de erro (ambas as associações e nenhuma). Nenhum apontamento crítico adicional foi identificado.
