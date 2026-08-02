# QA — Revisão da Fase 13.3 (Frontend): Transações

| Campo | Valor |
|---|---|
| Fase | 13.3 (Frontend) — CRUD de transações, consumindo `TransactionController` |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Esta fase não introduziu nenhuma decisão arquitetural nova (aplica integralmente ADR-0025) — não há um novo ADR do CTO, apenas a aprovação formal de encerramento, mesmo padrão já usado em 13.2.

## Checklist de Qualidade

- [x] A implementação atende ao escopo real do backend: listagem de transações **por conta** (não há filtro/busca geral — RF-018 é a Fase 4.3, ainda não construída no backend), criação, edição e exclusão.
- [x] Nenhuma regra de negócio duplicada no cliente — valor sempre positivo é feedback antecipado (Zod `.positive()`), não uma reimplementação de `TransactionPolicy.assertPositiveAmount`; bloqueio de lançamento em conta arquivada é inteiramente decidido pelo backend, a mensagem real é exibida sem previsão no cliente.
- [x] Testes cobrem listagem, criação, validação client-side (valor zero), erro real de negócio do backend (conta arquivada) e exclusão (30 testes de frontend no total; 5 novos nesta fase).
- [x] Aderente à cláusula de frontend de `rules.md` §3: `TransactionsPage.test.tsx` renderiza o `App` real com MSW na fronteira de rede.
- [x] Sem degradação de performance evidente (bundle: 382 KB / 116 KB gzip).
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada.
- [x] Não há introdução de funcionalidade fora do escopo — nenhum filtro/busca simulado no cliente para uma capacidade que o backend não tem (RF-018), nenhuma tela de transação recorrente ou importação (RF-016, RF-019–022, ainda não construídos no backend).

## Verificação de Execução

```
npm test    → 30 testes, 100% passando (Vitest, jsdom, MSW)
npm run build → sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos
mvn test (backend-java) → 444 testes, 100% passando (nenhuma mudança de backend nesta fase)
```

Verificação manual adicional contra o backend real: fluxo completo de transação (criar → listar por conta → atualizar → tentar valor zero, bloqueado → excluir) executado via `curl` contra `backend-java` rodando de verdade, confirmando formatos de requisição/resposta e mensagens de erro.

## Achados Durante a Revisão

**1. Cache do TanStack Query vazando entre testes no mesmo arquivo, causando falhas intermitentes (severidade: média — infraestrutura de teste, corrigida nesta revisão)**

`queryClient` era instanciado como constante de módulo dentro de `App.tsx` (`const queryClient = new QueryClient()`). Como o módulo `App.tsx` só é avaliado uma vez por arquivo de teste, todas as chamadas a `renderApp()` dentro do mesmo arquivo compartilhavam a mesma instância — e, portanto, o mesmo cache. Isso não causava problema quando a suíte de um arquivo era executada isoladamente (`vitest run TransactionsPage.test.tsx`, todos os testes passavam), mas causava falhas **intermitentes e não determinísticas** ao rodar o arquivo completo junto com o resto da suíte: um teste conseguia ler dados em cache (ex.: uma lista de contas) que pertenciam a um teste anterior já limpo do MSW (`resetTestState()`), fazendo a UI parecer travada aguardando um elemento que nunca apareceria. **Resolução**: `queryClient` foi extraído para um módulo próprio (`src/queryClient.ts`) e `test/setup.ts` agora chama `queryClient.clear()` em todo `afterEach`, ao lado do já existente `server.resetHandlers()`/`resetTestState()`. Confirmado com 3 execuções consecutivas da suíte completa sem falhas, além de execuções isoladas dos arquivos individualmente.

**Nota de julgamento do QA**: este achado não é um bug de comportamento do app em produção (a instância única de `QueryClient` é o padrão correto para uma aplicação real de página única) — é uma lacuna de isolamento de teste que só se manifestava porque os testes montam e desmontam o `App` repetidamente dentro do mesmo processo, algo que uma única execução do app real nunca faz. Registrado como achado porque a suíte de testes é o artefato de confiança desta e de todas as fases seguintes — testes intermitentes são, na prática, tão prejudiciais quanto um bug de produção não corrigido.

## Avaliação por Critério

**Clean Code**: `selectAccount` (helper de teste) documenta o "porquê" da espera em duas etapas (lista de contas assíncrona, depois formulário/categorias assíncronos) — evita que o próximo desenvolvedor reintroduza a mesma corrida ao copiar o padrão. `TransactionsForAccount`/`TransactionRow` seguem a mesma decomposição já usada em `AccountsPage`/`CategoriesPage` (linha/formulário de edição local, callback de invalidação).

**SOLID/estrutura**: `transactionsApi.ts` segue exatamente o formato dos módulos de API anteriores. A escolha de tornar a conta um parâmetro de URL (`?accountId=`) em vez de estado local reflete corretamente que a listagem do backend já é inerentemente por conta — a UI não inventa um estado que a API não suporta.

**Testes**: a suíte cobre o caminho principal (listar/criar/editar/excluir) e dois erros de negócio reais e distintos (validação client-side de valor zero vs. erro de servidor para conta arquivada) — uma boa demonstração de que o frontend não trata todo erro da mesma forma genérica.

**Segurança**: nenhuma chamada aceita `userId`/escopo de outro usuário do cliente; o valor da transação nunca é enviado com sinal negativo para simular despesa — seguindo exatamente o modelo do backend (`type` determina direção, `amount` é sempre módulo positivo).

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Sem filtro/busca de transações (RF-018) — pendência do próprio backend (Fase 4.3, não construída), não uma lacuna desta fase.
2. Sem transações recorrentes (RF-016) nem importação CSV/OFX (RF-019–022) — pendências do backend (Fases 4.2/4.4, não construídas).

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real de infraestrutura de teste (cache do TanStack Query vazando entre testes, causando falhas intermitentes) foi identificado e corrigido durante esta revisão, com a correção beneficiando a confiabilidade de toda a suíte, não apenas desta fase — confirmado por múltiplas execuções consecutivas estáveis. Nenhum apontamento crítico adicional foi identificado.
