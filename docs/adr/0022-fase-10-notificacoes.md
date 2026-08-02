# ADR-0022: Fase 10 (Java) — Notificações, entrega dos sinais represados e adiamento de RF-043

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-08-01 |
| Autor | CTO / Principal Software Architect |
| Fase | 10 (Java) — RF-040, RF-041, RF-042 completos; RF-043 remanejado para acompanhar a Fase 4.2 |

## Contexto

Fase 10 cobre RF-040 (preferências de notificação por tipo de alerta e canal), RF-041 (notificar estouro de orçamento), RF-042 (notificar gasto atípico, por desvio estatístico) e RF-043 (lembrete de transação recorrente prevista e não confirmada).

Esta fase finalmente resolve a dívida represada em três ADRs anteriores: RF-028 (ADR-0018) e RF-032 (ADR-0019) prometeram explicitamente que a **entrega** (e-mail/in-app) dos sinais já calculados ficaria para a Fase 10.

**Duas questões de escopo precisam de decisão explícita, ambas já sinalizadas por decisões anteriores do projeto:**

1. **RF-043 depende de transações recorrentes (RF-016), que não existem ainda** — RF-016 é a Fase 4.2, ainda não construída (ver ADR-0016, que decompôs a Fase 4 e deixou 4.2/4.3/4.4 pendentes). "Lembrete de transação recorrente prevista e ainda não confirmada" pressupõe a existência do conceito de transação recorrente no domínio — que simplesmente não existe no código hoje. Implementá-lo agora exigiria construir uma fundação de recorrência inteira dentro da fase de Notificações, antecipando escopo da Fase 4.2 de forma não coordenada. Mesmo padrão de dependência já resolvido para RF-022 (ADR-0017, remanejado para a Fase 4.4).
2. **RF-005 (Fase 2.3) já estabeleceu o padrão de porta para envio externo**: `PasswordResetNotifier` (não "EmailSender") + adaptador `ConsolePasswordResetNotifier` (loga, não envia de verdade) — ver ADR-0009. Nenhum provedor de e-mail real (SMTP, SES, SendGrid) foi integrado a este projeto até hoje, em nenhuma fase. Um canal EMAIL real está fora do escopo pela mesma razão que fundamentou ADR-0009: decisão de infraestrutura de produção, não de domínio.

## Decisão

### 1. RF-043 remanejado para a Fase 4.2 (terceira ocorrência do mesmo padrão)

RF-043 não é implementado nesta fase — nem mesmo como um tipo de alerta "vazio" em `AlertType`. Anunciar uma preferência configurável para um recurso que não existe (transação recorrente) apresentaria uma funcionalidade falsa ao usuário. `AlertType` desta fase tem exatamente três valores: `BUDGET_THRESHOLD`, `GOAL_THRESHOLD`, `ATYPICAL_SPENDING`. RF-043 será implementado junto da Fase 4.2, quando o conceito de transação recorrente existir — mesma resolução já aplicada a RF-022 (ADR-0017).

### 2. Canal EMAIL — porta desacoplada, sem provedor real (mesmo padrão de ADR-0009)

- Nova porta `AlertEmailNotifier` (nomeada pela intenção — "notificar sobre um alerta por e-mail" —, não "EmailSender" genérico, mesmo raciocínio de nomenclatura do ADR-0009).
- Adaptador desta fase: `ConsoleAlertEmailNotifier`, que apenas loga o alerta (SLF4J). Nenhum provedor real integrado — mesma dívida técnica já registrada e aceita em ADR-0009, agora estendida ao canal de notificações gerais.
- Falha ao "enviar" um e-mail (mesmo o adaptador de console, pensando em uma futura implementação real) **não interrompe o processamento dos demais eventos detectados** — vision.md § 6.5 (Resiliência) trata o serviço de notificação como não-crítico; um evento de e-mail com falha é capturado e logado, sem abortar a verificação inteira.

### 3. Canal IN_APP — sem porta externa, é a própria persistência

Diferente de EMAIL, o canal IN_APP não precisa de uma porta de infraestrutura — "entregar" via IN_APP é simplesmente persistir a notificação e ela aparecer em `GET /notifications`. Cada `Notification` registra em `deliveredChannels` quais canais estavam habilitados no momento da detecção; `GET /notifications` só retorna notificações onde IN_APP está presente nesse conjunto — desabilitar IN_APP para um tipo de alerta o remove da caixa de entrada dali em diante, sem apagar o registro (retido para todos os fins de deduplicação e auditoria).

### 4. Preferências (RF-040) — padrão opt-out, mesclado sob demanda

- `NotificationPreference(id, userId, alertType, channel, enabled)` — uma linha por combinação configurada explicitamente pelo usuário. Combinações não configuradas usam o padrão `enabled = true` (opt-out, não opt-in) — decisão de produto: este é um app de finanças pessoais onde alertas proativos (orçamento estourado, gasto atípico) têm valor direto ao usuário; exigir configuração explícita antes de qualquer alerta chegar reduziria a utilidade percebida no primeiro uso (mesmo raciocínio de "valor rápido no onboarding" já citado em vision.md § 12, R-03).
- `GET /notification-preferences` sempre retorna as 3 × 2 = 6 combinações completas (mesclando linhas persistidas com o padrão `true` para as ausentes) — nenhuma escrita ocorre até o usuário de fato mudar uma preferência (ao contrário do "seed automático" de categorias, RF-025, que precisa persistir porque outras entidades referenciam `categoryId`; aqui não há necessidade equivalente).
- `PUT /notification-preferences` aceita atualização parcial (lista de combinações a alterar, não as 6 obrigatoriamente).

### 5. Detecção de eventos — reaproveitamento de casos de uso existentes, sem duplicar orquestração

- `CheckNotificationsUseCase` (`POST /notifications/check`) depende diretamente de `ListBudgetsUseCase` e `ListGoalsUseCase` já existentes (Fases 6 e 7) para obter `thresholdsCrossed`/`achieved`, em vez de duplicar a orquestração de saldo/consumo/progresso pela terceira vez. **Primeira ocorrência de um caso de uso compondo outro caso de uso neste projeto** — decisão deliberada: ambos os casos de uso reaproveitados são somente leitura (sem efeito colateral), e a alternativa (duplicar ~20 linhas de orquestração de repositórios) reproduziria exatamente a lógica que `ListBudgetsUseCase`/`ListGoalsUseCase` já entregam testada e aprovada.
- Deduplicação por `eventKey` determinístico (`"budget:{id}:period:{periodStart}:threshold:{n}"`, `"goal:{id}:threshold:{n}"`, `"goal:{id}:achieved"`, `"transaction:{id}:atypical"`), verificado via `NotificationRepository.existsByUserIdAndEventKey` antes de criar uma nova notificação — chamar `/notifications/check` repetidamente é idempotente; o mesmo evento nunca gera duas notificações. Orçamentos recorrentes (MONTHLY/WEEKLY) voltam a poder notificar a cada novo período, porque `periodStart` entra na chave.
- **Sem scheduler dedicado** (mesma limitação já aceita em ADR-0020 para o Pulse Score): a detecção só roda quando `POST /notifications/check` é chamado — não há job periódico ativo nesta fase. Documentado como limitação conhecida, não uma lacuna silenciosa.

### 6. RF-042 (gasto atípico) — fórmula estatística provisória e transparente

- `AtypicalSpendingDetector`: para cada transação de despesa recente (janela de 30 dias, mesma janela padrão já usada no dashboard), compara o valor com a média + 2×desvio-padrão das despesas anteriores do usuário na mesma categoria. Exige no mínimo 5 transações históricas na categoria antes de avaliar qualquer coisa — dado insuficiente nunca é tratado como "atípico por padrão" (mesmo racional de omissão por dado insuficiente já usado no Pulse Score, ADR-0020).
- **Fórmula provisória, mesmo espírito de transparência do Pulse Score**: RF-042 pede "desvio estatístico do padrão histórico", mas não especifica o método exato (2-sigma é uma escolha comum e razoável, não uma imposição do requisito). Constantes nomeadas e documentadas (`MIN_SAMPLE_SIZE = 5`, `SIGMA_MULTIPLIER = 2`) para facilitar ajuste futuro sem redesenho.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Implementar uma fundação mínima de transação recorrente para viabilizar RF-043 nesta fase | Repetiria, sem coordenação, o escopo que pertence à Fase 4.2 — mesmo raciocínio que evitou antecipar RF-022 na Fase 5 (ADR-0017). |
| Integrar um provedor de e-mail real (SMTP/SES) agora | Decisão de infraestrutura de produção fora do escopo de qualquer fase de domínio deste projeto até hoje — mesmo raciocínio de ADR-0009. |
| Detectar gasto atípico em tempo real, no momento da criação da transação (`CreateTransactionUseCase`) | Acoplaria um caso de uso estável e já aprovado (Fase 4.1) a `NotificationRepository`/`AlertEmailNotifier`/`NotificationPreferenceRepository`, misturando duas responsabilidades. Detecção via `POST /notifications/check` mantém `CreateTransactionUseCase` inalterado e seguinda o mesmo padrão "detectar sob demanda" já usado para orçamentos e metas. |
| Duplicar a lógica de progresso de orçamento/meta dentro de `CheckNotificationsUseCase` em vez de reaproveitar `ListBudgetsUseCase`/`ListGoalsUseCase` | Duplicaria pela terceira vez uma orquestração já testada — sem ganho, apenas risco de divergência entre o que `GET /budgets`/`GET /goals` mostra e o que dispara uma notificação. |
| Preferências com padrão opt-in (`enabled = false` até o usuário habilitar) | Reduziria o valor percebido no primeiro uso — um usuário nunca notificado sobre um orçamento estourado até configurar manualmente as preferências não experimenta o valor central desta fase. |

## Consequências

- `roadmap.md` registra RF-043 como pendente, vinculado à Fase 4.2 — mesmo padrão de rastreamento já usado para RF-022.
- `ConsoleAlertEmailNotifier` precisa ser substituído por um adaptador real antes de qualquer implantação de produção — dívida técnica registrada, mesma classe já aceita para `ConsolePasswordResetNotifier` (ADR-0009).
- `CheckNotificationsUseCase` é o primeiro caso de uso deste projeto a depender de outro caso de uso em vez de apenas portas/serviços — padrão a ser seguido (não uma exceção pontual) sempre que uma nova funcionalidade precisar exatamente dos mesmos sinais que um caso de uso de leitura já existente e sem efeito colateral já calcula.
