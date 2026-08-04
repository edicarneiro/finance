# QA — Revisão da Fase 13.7 (Frontend): Notificações e Preferências

| Campo | Valor |
|---|---|
| Fase | 13.7 (Frontend) — Notificações e preferências, consumindo `NotificationController` |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-03 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Esta fase não introduziu nenhuma decisão arquitetural nova (aplica integralmente ADR-0025) — sem novo ADR do CTO, mesmo padrão de 13.2–13.6.

## Checklist de Qualidade

- [x] A implementação atende ao escopo real do backend: matriz de preferências (3 tipos de alerta × 2 canais), verificação de notificações sob demanda, e lista de notificações com filtro de não lidas e ação de marcar como lida.
- [x] Nenhuma regra de negócio duplicada no cliente — os nomes de tipo de alerta e canal usados na UI (`BUDGET_THRESHOLD`, `GOAL_THRESHOLD`, `ATYPICAL_SPENDING`, `IN_APP`, `EMAIL`) foram verificados diretamente contra os enums reais do backend (`AlertType.java`, `NotificationChannel.java`), não inventados.
- [x] Testes cobrem preferências (carregar/alternar/salvar), verificação com resultado e com estado vazio, listagem com filtro, e marcar como lida (55 testes de frontend no total; 6 novos nesta fase, um deles regressão do achado abaixo).
- [x] Aderente à cláusula de frontend de `rules.md` §3: `NotificationsPage.test.tsx` renderiza o `App` real com MSW na fronteira de rede.
- [x] Sem degradação de performance evidente (bundle: 410 KB / 122 KB gzip).
- [x] Código segue Clean Code e SOLID (ver Achado abaixo — corrigido durante a revisão).
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada, incluindo o padrão geral (não específico desta tela) sobre sincronização de estado local com `useQuery`.
- [x] Não há introdução de funcionalidade fora do escopo — nenhuma tela para RF-043 (lembrete de transação recorrente), que o próprio backend não implementa (depende de RF-016, Fase 4.2, ainda não construída).

## Verificação de Execução

```
npm test    → 55 testes, 100% passando (Vitest, jsdom, MSW), 2 execuções consecutivas estáveis
npm run build → sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos
```

Verificação manual adicional contra o backend real, via `docker compose -f docker-compose.dev.yml up`: leitura e atualização de preferências reais, e um cenário completo de orçamento estourado (limite de R$ 100, transação de R$ 95, orçamento acumulado real do usuário de dev cruzando 80% e 100%) gerando duas notificações reais via `POST /notifications/check`, seguido de marcar uma como lida e confirmar que ela some do filtro "somente não lidas".

## Achados Durante a Revisão

**1. Estado local das preferências era resincronizado a cada mudança de `preferencesQuery.data`, não só na carga inicial (severidade: média — bug real de perda silenciosa de dados do usuário, corrigido nesta revisão)**

`PreferencesSection` mantém um estado local (`enabledByKey`) para os toggles ainda não salvos, inicializado a partir de `preferencesQuery.data` via `useEffect`. O `useEffect` original disparava a cada mudança na referência de `data`, não apenas na primeira carga. TanStack Query refaz a busca automaticamente em vários gatilhos por padrão — o mais comum é `refetchOnWindowFocus` (verdadeiro por padrão), disparado sempre que o navegador reganha o foco. **Cenário real**: um usuário alterna um ou mais toggles de preferência, muda de aba para checar algo e volta — o refetch de fundo dispara, `useEffect` roda de novo com os dados (inalterados) do servidor, e sobrescreve silenciosamente as alterações ainda não salvas, sem nenhum aviso. **Resolução**: o `useEffect` agora só sincroniza uma vez, guardado por um `useRef`; refetches subsequentes não tocam mais o estado local. Teste de regressão adicionado, simulando o refetch de fundo diretamente via `queryClient.refetchQueries` (mais geral e determinístico em teste do que simular perda/ganho de foco do navegador) e confirmando que o toggle pendente sobrevive.

## Avaliação por Critério

**Clean Code**: `preferenceKey` centraliza a chave composta (`alertType-channel`) usada tanto para ler quanto para escrever o estado local, evitando strings mágicas duplicadas. O comentário sobre a guarda de sincronização única explica o "porquê" (proteger edições não salvas de um refetch de fundo), não o "o quê".

**SOLID/estrutura**: `NotificationsPage` é dividida em três subcomponentes de seção (`PreferencesSection`, `CheckNotificationsSection`, `NotificationsListSection`), cada um com sua própria query/mutação — evita um componente monolítico gerenciando três preocupações distintas, mesmo padrão de decomposição já usado em `ReportsPage`.

**Testes**: a suíte cobre o caminho principal de cada seção mais o achado de regressão, que é particularmente valioso por testar um comportamento de **produção real** (perda de dados do usuário), não apenas um detalhe de implementação.

**Segurança**: nenhuma chamada aceita escopo de outro usuário do cliente; preferências e notificações são sempre implicitamente escopadas ao usuário autenticado pelo token, sem parâmetro de usuário exposto na API do frontend.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Sem tela para RF-043 (lembrete de transação recorrente) — o próprio backend não implementa isso ainda (depende de RF-016/Fase 4.2), não uma lacuna do frontend.
2. A "matriz" de preferências (tabela 3×2) não tem uma opção de "marcar todas"/"desmarcar todas" — não solicitado, não é uma regressão de nenhuma capacidade existente do backend.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real e genuinamente impactante para o usuário final (perda silenciosa de alterações não salvas em um refetch de fundo do TanStack Query) foi identificado e corrigido durante esta revisão, com teste de regressão que simula o gatilho de forma determinística. Toda alegação de comportamento foi verificada com execução real contra `backend-java` via Docker Compose, incluindo um cenário completo e realista de geração de notificação por orçamento estourado. Nenhum apontamento crítico adicional foi identificado.
