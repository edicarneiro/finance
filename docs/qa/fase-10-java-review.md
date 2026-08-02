# QA — Revisão da Fase 10 (Java): Notificações

| Campo | Valor |
|---|---|
| Fase | 10 (Java) — RF-040, RF-041, RF-042 completos (entrega represada de RF-028/RF-032 também efetivada); RF-043 fora do escopo (ver ADR-0022) |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0022) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-040 (preferências por tipo × canal), RF-041 (estouro de orçamento) e RF-042 (gasto atípico) do vision.md, além de finalmente entregar os sinais represados de RF-028 (ADR-0018) e RF-032 (ADR-0019). RF-043 está fora do escopo, documentado (não uma lacuna silenciosa).
- [x] Não há violação de regra de negócio ou restrição do vision.md — nenhum tipo de alerta para RF-043 foi anunciado sem a funcionalidade correspondente existir.
- [x] Isolamento multi-tenant (RF-047) verificado — `NotificationRepository`/`NotificationPreferenceRepository` escopados por `userId`; `CheckNotificationsUseCase` só processa dados do usuário autenticado. Testado explicitamente (`JpaNotificationRepositoryAdapterTest`, `NotificationControllerTest.markingAnotherUsersNotificationAsReadReturnsNotFound`).
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (370 testes; 41 novos nesta fase, incluindo um teste de regressão para o Achado 1 abaixo).
- [x] Sem degradação de performance evidente para o estágio atual (ver Achado 2, não bloqueante).
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`NotificationControllerTest` como smoke test contra a raiz de composição real, com detecção real via HTTP a partir de um orçamento/transação reais).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo — nenhum provedor de e-mail real foi integrado (`ConsoleAlertEmailNotifier` apenas loga, mesmo padrão de `ConsolePasswordResetNotifier`/ADR-0009); nenhum scheduler foi construído.

## Verificação de Execução

```
mvn test → 370 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4), após a correção do Achado 1
```

## Achados Durante a Revisão

**1. Notificação duplicada para o mesmo marco quando uma meta atinge exatamente o limiar de 100% configurado (severidade: média — defeito de produto/UX real, corrigido nesta revisão)**

`CheckNotificationsUseCase.detectGoalEvents` gerava, para uma meta com `progressAlertThresholds` incluindo `100` que é efetivamente atingida, **duas notificações separadas** para o mesmo evento: uma de "atingiu 100% do valor-alvo" (via `thresholdsCrossed`) e outra de "foi atingida!" (via `achieved`). Do ponto de vista do usuário, isso é o mesmo marco comunicado duas vezes — ruído, não dois eventos distintos. O caso "achieved sem 100 configurado" (ex.: `thresholds=[80]` mas o progresso ultrapassa 100%) precisa continuar gerando o evento "achieved" independentemente, então a notificação de limiar não podia simplesmente ser removida. **Resolução**: o limiar `100` é pulado no laço de `thresholdsCrossed` especificamente quando `goal.achieved()` já é verdadeiro — a notificação "achieved" cobre esse caso sozinha; limiares menores (`80`, etc.) continuam gerando suas próprias notificações normalmente. Teste de regressão adicionado (`doesNotDuplicateANotificationForTheHundredPercentThresholdWhenTheGoalIsAlreadyAchieved`), verificando que exatamente 2 notificações (não 3) são criadas quando `thresholds=[80,100]` e a meta é atingida. Corrigido e verificado nesta revisão.

**2. `CheckNotificationsUseCase` busca transações de forma redundante entre suas três sub-rotinas de detecção (severidade: baixa — nota de performance, não bloqueante)**

A detecção de orçamento (via `ListBudgetsUseCase`) busca transações por categoria uma vez por orçamento; a detecção de meta (via `ListGoalsUseCase`) busca transações por conta/categoria uma vez por meta; a detecção de gasto atípico busca **todas** as transações do usuário separadamente. Para um usuário com muitos orçamentos/metas, isso significa buscar dados de transação repetidamente na mesma chamada a `POST /notifications/check`. Mesma classe de característica N+1 já aceita desde a Fase 6/7 (não uma regressão introduzida aqui) — não há evidência de que isso seja um problema real no volume de dados esperado para o MVP. Registrado como nota de performance para uma eventual otimização (buscar transações uma única vez e passar para as três sub-rotinas), não como bloqueio desta fase.

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`detectBudgetThresholdEvents`, `detectGoalEvents`, `detectAtypicalSpendingEvents`, `PendingEvent`); a decisão de pular o limiar 100 quando a meta já foi atingida está documentada com um comentário explicando o "porquê" (evitar notificação duplicada), não o "o quê".

**SOLID**: `AtypicalSpendingDetector` e `NotificationPreferenceResolver` são funções puras e isoladas (sem I/O, sem dependência de framework). `CheckNotificationsUseCase` reaproveita `ListBudgetsUseCase`/`ListGoalsUseCase` em vez de duplicar a orquestração de consumo/progresso pela terceira vez — primeira composição de caso de uso por outro caso de uso neste projeto, decisão justificada em ADR-0022 e verificada como correta (ambos os casos de uso reaproveitados são somente leitura, sem efeito colateral escondido). Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/notification/` ou `application/{usecases/notification,services/AtypicalSpendingDetector,services/NotificationPreferenceResolver}`.

**Testes**: `AtypicalSpendingDetectorTest` cobre o limite exato da fórmula estatística (amostra insuficiente nunca é atípica, valor igual à média com desvio zero não é atípico, valor dentro de 2 desvios-padrão é tolerado). `CheckNotificationsUseCaseTest` cobre deduplicação por `eventKey` através de chamadas repetidas, gating por canal (incluindo o caso "ambos os canais desabilitados, mas a notificação ainda é persistida para deduplicação"), e resiliência a falha de envio de e-mail (o evento mais importante de todos para um serviço declarado não-crítico em vision.md § 6.5). `NotificationControllerTest` inclui um fluxo de ponta a ponta completo: orçamento real + transação real via HTTP → detecção real → aparece na caixa de entrada não lida → marcar como lida → desaparece do filtro `unreadOnly`.

**Segurança**: `userId` nunca aceito do corpo/parâmetros da requisição; toda leitura/escrita escopada por `userId` nas portas de repositório. Marcar a notificação de outro usuário como lida retorna o mesmo erro "não encontrada" (HTTP 404) de uma notificação inexistente — postura anti-enumeração consistente com o resto do projeto, testada explicitamente.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Busca redundante de transações entre as três sub-rotinas de detecção (Achado 2 acima) — nota de performance, mesma classe de característica já aceita em fases anteriores.
2. Sem scheduler dedicado — `POST /notifications/check` só roda quando chamado (decisão de escopo explícita, ADR-0022, mesma limitação já aceita para o Pulse Score em ADR-0020).
3. Duas transações de despesa na mesma categoria, na mesma data, nunca se influenciam mutuamente na avaliação de gasto atípico (o filtro histórico usa "data estritamente anterior") — efeito de borda menor, comportamento determinístico e razoável (a ordem entre transações do mesmo dia é ambígua de qualquer forma).

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real de severidade média (notificação duplicada para o mesmo marco de meta) foi identificado e corrigido durante esta revisão, com teste de regressão dedicado. Um segundo achado foi registrado como nota de performance não bloqueante. A porta `AlertEmailNotifier` segue corretamente o precedente já estabelecido (ADR-0009) de isolar a decisão de infraestrutura de e-mail real, e a decisão de reaproveitar `ListBudgetsUseCase`/`ListGoalsUseCase` em vez de duplicar orquestração já testada é sólida. Nenhum apontamento crítico de segurança ou de regra de negócio foi identificado.
