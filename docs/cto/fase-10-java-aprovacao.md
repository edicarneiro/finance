# CTO — Aprovação da Fase 10 (Java): Notificações

| Campo | Valor |
|---|---|
| Fase | 10 (Java) — RF-040, RF-041, RF-042 completos (entrega represada de RF-028/RF-032 também efetivada); RF-043 fora do escopo (ver ADR-0022) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

RF-040 (preferências de notificação por tipo de alerta × canal), RF-041 (estouro de orçamento) e RF-042 (gasto atípico por desvio estatístico), além da entrega finalmente efetivada dos sinais represados de RF-028 (ADR-0018, Fase 6) e RF-032 (ADR-0019, Fase 7). RF-043 permanece fora do escopo, remanejado para acompanhar a Fase 4.2. Conforme delimitado em [ADR-0022](../adr/0022-fase-10-notificacoes.md) e [roadmap.md](../../roadmap.md) — Fase 10.

## Insumos considerados

- [docs/qa/fase-10-java-review.md](../qa/fase-10-java-review.md) — parecer de qualidade do QA: **aprovado**, com um achado real de severidade média (notificação duplicada para o mesmo marco de meta) identificado e corrigido durante a própria revisão, e uma nota de performance não bloqueante.
- [ADR-0022](../adr/0022-fase-10-notificacoes.md) — decisão de adiar RF-043, reaproveitar `ListBudgetsUseCase`/`ListGoalsUseCase`, e replicar o padrão de porta desacoplada de e-mail já estabelecido em ADR-0009.
- [ADR-0009](../adr/0009-recuperacao-de-senha.md) — precedente direto para a decisão de nomear a porta pela intenção (`AlertEmailNotifier`, não "EmailSender") e usar um adaptador de console para esta fase.
- Código-fonte em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue o padrão já estabelecido (`domain/ → application/ → adapters/`), estendida com `domain/notification/`, `application/usecases/notification/`, `application/services/{AtypicalSpendingDetector,NotificationPreferenceResolver}`, `adapters/out/notification/`.
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/notification/` ou nos use cases/serviços de notificação importa Spring, JPA ou Jackson. `ConsoleAlertEmailNotifier` (único ponto que usa SLF4J diretamente para "enviar") vive corretamente em `adapters/out/notification/`.
- [x] **RF-043 corretamente adiado, não simulado**: nenhum valor "vazio" foi adicionado a `AlertType` para um recurso (transação recorrente) que não existe no domínio — concordo que anunciar uma preferência configurável para uma funcionalidade inexistente seria pior do que simplesmente não implementá-la ainda. Mesma disciplina já aplicada a RF-022 (ADR-0017).
- [x] **Porta `AlertEmailNotifier` corretamente desacoplada**: nomeada pela intenção, não pelo mecanismo — mesmo raciocínio de `PasswordResetNotifier` (ADR-0009), aplicado pela primeira vez fora do fluxo de autenticação. `ConsoleAlertEmailNotifier` é o único adaptador, sem provedor real — dívida técnica assumida conscientemente, não uma omissão.
- [x] **Primeira composição de caso de uso por outro caso de uso deste projeto, avaliada e aprovada**: `CheckNotificationsUseCase` depende de `ListBudgetsUseCase`/`ListGoalsUseCase` em vez de duplicar a orquestração de repositórios pela terceira vez. Verifiquei que ambos os casos de uso reaproveitados são estritamente de leitura (nenhum efeito colateral oculto seria introduzido por essa composição) — esta é a condição que torna a decisão segura, e deve ser tratada como pré-requisito para qualquer futura composição semelhante, não uma licença geral para acoplar casos de uso indiscriminadamente.
- [x] **Deduplicação por `eventKey` estruturalmente correta**: verifiquei que a chave inclui `periodStart` para orçamentos (permitindo renotificação em cada novo período recorrente) e é estável para metas/transações (nunca renotifica o mesmo marco). A unicidade é reforçada tanto em memória (dublê de teste) quanto via `UniqueConstraint` no schema H2 (`user_id, event_key`).
- [x] **Endosso à correção do Achado 1 do QA** (notificação duplicada de meta atingida): a correção é mínima e cirúrgica — pula apenas o limiar 100 quando `achieved()` já é verdadeiro, preservando o caso em que `achieved` precisa disparar sozinho (meta sem limiar 100 configurado, mas ultrapassada). Boa captura de um defeito real de experiência do usuário antes de chegar a produção.
- [x] `rules.md` § 3 atendido: `NotificationControllerTest` inclui um fluxo de ponta a ponta completo (orçamento real, transação real via HTTP, detecção real, leitura/marcação da caixa de entrada) contra a raiz de composição real.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009).
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Ausência de provedor de e-mail real (`ConsoleAlertEmailNotifier`) é dívida técnica conscientemente assumida, mesma classe já registrada para `ConsolePasswordResetNotifier` desde a Fase 2.3 (ADR-0009) — ambas precisam ser substituídas antes de qualquer implantação de produção.
- Ausência de scheduler dedicado para `POST /notifications/check` é aceita pela mesma razão já registrada para o Pulse Score (ADR-0020) — nenhuma infraestrutura de job em background existe em nenhuma parte do backend Java ainda; introduzi-la agora anteciparia a Camada de Integração Assíncrona (vision.md § 10.5) fora do escopo desta fase.
- Busca redundante de transações entre as três sub-rotinas de `CheckNotificationsUseCase` (nota do QA) é aceita como oportunidade de otimização futura, não uma dívida urgente — sem evidência de impacto real no volume esperado do MVP.
- RF-043 permanece formalmente rastreado como pendente, vinculado à Fase 4.2 — terceira ocorrência do mesmo padrão de dependência já resolvido para RF-022.

## Decisão

**A Fase 10 (Java) está aprovada.** A implementação entrega preferências de notificação configuráveis (RF-040), detecção e entrega de estouro de orçamento (RF-041) e gasto atípico (RF-042), finalmente fechando os sinais represados de RF-028 e RF-032 das Fases 6 e 7. A porta de e-mail segue corretamente o precedente arquitetural já estabelecido (ADR-0009), e a primeira composição de caso de uso por outro caso de uso deste projeto foi avaliada e considerada segura. O parecer de qualidade do QA foi favorável, destacando a correção, durante a própria revisão, de um defeito real de experiência do usuário (notificação duplicada de meta). Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
