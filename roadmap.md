# FinancePulse Engine — Roadmap de Implementação (roadmap.md)

| Campo | Valor |
|---|---|
| Documento | roadmap.md |
| Versão | 1.0 |
| Status | Vigente |
| Autor | CTO / Principal Software Architect |
| Data | 2026-07-30 |

> Este documento traduz o escopo do MVP e o roadmap de produto definidos no [vision.md](vision.md) (Seções 14 e 15) em **fases de implementação executáveis**, na granularidade exigida pelo processo de colaboração dos agentes ([README.md](README.md)): cada fase é pequena o suficiente para ser **integralmente concluída** (CTO → Full Stack → QA → aprovação) antes que a próxima comece.
>
> Nenhuma fase abaixo introduz requisito não previsto no vision.md. A quebra em fases é uma decisão de sequenciamento de entrega — não uma alteração de escopo.

---

## Convenção de Status

| Status | Significado |
|---|---|
| ⬜ Não iniciada | Ainda não houve trabalho de nenhum agente |
| 🔵 Em andamento | Ciclo CTO → Full Stack → QA em execução |
| 🟡 Aguardando aprovação | Ciclo concluído, aguardando aprovação do stakeholder para prosseguir |
| ✅ Concluída | Aprovada pelo stakeholder |

---

## Trilha de Migração — Java / Spring Boot (ADR-0013)

> Por decisão do stakeholder, o backend está sendo migrado de TypeScript/Node.js para **Java + Spring Boot** (ver [ADR-0013](docs/adr/0013-migracao-java-spring-boot.md)). A migração é incremental, fase por fase, espelhando exatamente os limites das fases já concluídas em TypeScript — mesmo ciclo completo (CTO → Full Stack → QA → aprovação) e mesma exigência de aprovação do stakeholder entre fases. **O backend TypeScript (`backend/`) permanece intacto** até a migração completa ser aprovada. Código Java em `backend-java/`.

| Fase | Nome | Equivalente TS | Requisitos | Depende de | Status |
|---|---|---|---|---|---|
| M1 | Fundação técnica + Cadastro e Login (Java) | Fase 1 | RF-001, RF-002, RF-003, RF-008 | — | 🟡 Aguardando aprovação |
| M2.1 | Refresh token e logout (Java) | Fase 2.1 | RF-008 (renovação segura) | M1 | ⬜ Não iniciada |
| M2.2 | Edição de perfil e consentimento LGPD (Java) | Fase 2.2 | RF-006, RF-046 | M2.1 | ⬜ Não iniciada |
| M2.3 | Recuperação de senha (Java) | Fase 2.3 | RF-005 | M2.1 | ⬜ Não iniciada |
| M2.4 | Exclusão de conta (Java) | Fase 2.4 | RF-007 | M2.1 | ⬜ Não iniciada |
| M2.5.1 | MFA — cadastro e gestão (Java) | Fase 2.5.1 | RF-004 | M2.1 | ⬜ Não iniciada |
| M2.5.2 | MFA — integração com login (Java) | Fase 2.5.2 | RF-004 | M2.5.1 | ⬜ Não iniciada |

**Nota (histórica)**: esta trilha previa originalmente que as fases seguintes do roadmap (Fase 3 em diante) só passariam a ser construídas diretamente em Java a partir de M2.5.2, quando a migração alcançasse paridade com o backend TypeScript. Por decisão do stakeholder em 2026-07-31, a Fase 3 foi antecipada e construída diretamente em Java **antes** da conclusão de M2.1–M2.5.2 — ver [ADR-0014](docs/adr/0014-fase-3-contas-carteiras-java.md). M2.1–M2.5.2 permanecem `⬜ Não iniciada`, sem previsão fixa de retomada.

### Histórico da Trilha de Migração

| Fase | Aprovação CTO | Aprovação Stakeholder | ADRs | Revisão QA |
|---|---|---|---|---|
| M1 | ✅ [docs/cto/fase-m1-aprovacao.md](docs/cto/fase-m1-aprovacao.md) | *aguardando* | [0013](docs/adr/0013-migracao-java-spring-boot.md) | [docs/qa/fase-m1-review.md](docs/qa/fase-m1-review.md) |

---

## Parte 1 — MVP (vision.md Seção 14)

O MVP foi decomposto em 13 fases, seguindo a ordem de dependência natural do domínio: fundação técnica e identidade primeiro, depois dados financeiros centrais (contas, transações), depois as camadas de valor agregado (categorização, orçamentos, metas, dashboard), depois camadas de suporte (relatórios, notificações, privacidade, backoffice), e por fim a interface web consumindo a API já estabilizada.

| Fase | Nome | Requisitos do vision.md | Depende de | Status |
|---|---|---|---|---|
| 1 | Fundação técnica + Cadastro e Login | RF-001, RF-002, RF-003, RF-008 | — | ✅ Concluída |
| 2.1 | Refresh token e logout | RF-008 (renovação segura) | Fase 1 | ✅ Concluída |
| 2.2 | Edição de perfil e consentimento LGPD | RF-006, RF-046 | Fase 2.1 | ✅ Concluída |
| 2.3 | Recuperação de senha | RF-005 | Fase 2.1 | ✅ Concluída |
| 2.4 | Exclusão de conta (LGPD) | RF-007 | Fase 2.1 | ✅ Concluída |
| 2.5.1 | MFA — cadastro e gestão (enroll/confirm/disable/status) | RF-004 | Fase 2.1 | ✅ Concluída |
| 2.5.2 | MFA — integração com o fluxo de login | RF-004 | Fase 2.5.1 | ✅ Concluída |
| 3 | Contas e Carteiras Financeiras (Java — ver ADR-0014) | RF-009 a RF-013 | Fase 1 | 🟡 Aguardando aprovação |
| 4.1 | Transações manuais + fundação mínima de categoria (Java — ver ADR-0016) | RF-014, RF-015, RF-017, RF-025 (parcial) | Fase 3 | ✅ Concluída |
| 4.2 | Transações recorrentes | RF-016 | Fase 4.1 | ⬜ Não iniciada |
| 4.3 | Filtro e busca de transações | RF-018 | Fase 4.1 | ⬜ Não iniciada |
| 4.4 | Importação CSV/OFX, duplicatas, revisão e categorização automática | RF-019 a RF-022 | Fase 4.1, Fase 5 | ⬜ Não iniciada |
| 5 | Categorização — CRUD completo e subcategorias (Java — ver ADR-0017) | RF-023, RF-024 (já satisfeito por RF-015) | Fase 4.1 | ✅ Concluída |
| 6 | Orçamentos (Java — ver ADR-0018) | RF-026, RF-027, RF-028, RF-029 completos (RF-028: sinal calculado aqui, entrega efetivada na Fase 10) | Fase 5 | ✅ Concluída |
| 7 | Metas Financeiras (Java — ver ADR-0019) | RF-030, RF-031, RF-032 completos (RF-032: sinal calculado aqui, entrega efetivada na Fase 10) | Fase 4.1 | ✅ Concluída |
| 8 | Dashboard e Pulse Score (Java — ver ADR-0020) | RF-033, RF-035, RF-036 completos; RF-034 com fórmula provisória (RN-006 — definição formal pendente) | Fases 4, 5, 6, 7 | ✅ Concluída |
| 9 | Relatórios (Java — ver ADR-0021) | RF-037, RF-038 completos; RF-039 completo para CSV (PDF fora do escopo) | Fases 4, 5 | ✅ Concluída |
| 10 | Notificações (Java — ver ADR-0022) | RF-040, RF-041, RF-042 completos (entrega represada de RF-028/RF-032 efetivada); RF-043 pendente, vinculado à Fase 4.2 | Fases 6, 7 | ✅ Concluída |
| 11 | Privacidade e Conformidade (LGPD) (Java — ver ADR-0023) | RF-044, RF-045, RF-046 completos (RF-045 sob política de retenção provisória, pendente de validação jurídica — RN-008) | Fase 3 | ✅ Concluída |
| 12 | Multi-tenancy Hardening e Backoffice (Java — ver ADR-0024) | RF-047 a RF-050 completos (RF-049/050 como versão mínima manual) | Todas as anteriores | ✅ Concluída |
| 13 | Frontend Web (MVP) — decomposta em 13.1 a 13.9 (ver ADR-0025) | Interface para todas as funcionalidades acima | Fases 1 a 12 | 🔵 Em andamento |

### Decomposição da Fase 13 (ver ADR-0025)

| Fase | Nome | Consome (backend-java) | Depende de | Status |
|---|---|---|---|---|
| 13.1 | Fundação técnica + Autenticação | `AuthController` | Fase 12 | ✅ Concluída |
| 13.2 | Contas e Categorias | `AccountController`, `CategoryController` | Fase 13.1 | ✅ Concluída |
| 13.3 | Transações | `TransactionController` | Fase 13.2 | ✅ Concluída |
| 13.4 | Orçamentos e Metas | `BudgetController`, `GoalController` | Fase 13.3 | ⬜ Não iniciada |
| 13.5 | Dashboard e Pulse Score | `DashboardController` | Fase 13.4 | ⬜ Não iniciada |
| 13.6 | Relatórios (incl. exportação CSV) | `ReportController` | Fase 13.3 | ⬜ Não iniciada |
| 13.7 | Notificações e preferências | `NotificationController` | Fase 13.1 | ⬜ Não iniciada |
| 13.8 | Privacidade/LGPD (exportação, consentimento, exclusão de conta) | `PrivacyController`, `UserController` | Fase 13.1 | ⬜ Não iniciada |
| 13.9 | Backoffice (suporte, suspensão/reativação, audit log) | `BackofficeController` | Fase 13.1 | ⬜ Não iniciada |

**Nota de sequenciamento (decisão do CTO)**: as Fases 1 a 12 entregam a API backend; a Fase 13 entrega a interface web consumindo essa API já estabilizada. Isso evita retrabalho de UI a cada mudança de contrato durante a fase de maior instabilidade de API. Ver [ADR-0004](docs/adr/0004-sequenciamento-backend-first.md).

**Nota sobre a Fase 2 (decisão do CTO)**: decomposta em subfases 2.1–2.5 para preservar a granularidade "uma fase = totalmente concluível antes da próxima". Ver [ADR-0006](docs/adr/0006-decomposicao-fase-2.md).

**Nota sobre a Fase 4 (decisão do CTO)**: decomposta em subfases 4.1–4.4 pelo mesmo motivo da Fase 2, com uma resolução adicional: RN-002 exige que toda transação tenha uma categoria, mas o CRUD completo de categorias é RF-023 (Fase 5, que dependeria da Fase 4). A Fase 4.1 introduz uma fundação mínima de categoria (somente leitura, com seed automático de categorias padrão) só para satisfazer RN-002/RF-025, sem antecipar o escopo da Fase 5. Ver [ADR-0016](docs/adr/0016-decomposicao-fase-4-e-dependencia-categoria.md).

**Nota sobre a Fase 5 (decisão do CTO)**: RF-022 (categorização automática) foi remanejado da Fase 5 para a Fase 4.4 — pelo texto do vision.md, é um recurso do fluxo de **importação** (RF-019–021), que só existe na Fase 4.4; construí-lo antes da importação existir não teria consumidor real, já que toda transação manual exige categoria explícita desde a Fase 4.1. A Fase 5 entrega o CRUD completo de categorias/subcategorias (RF-023); RF-024 já estava satisfeito desde a Fase 4.1 (edição geral de transação permite recategorizar). Ver [ADR-0017](docs/adr/0017-fase-5-categorizacao.md).

**Nota sobre a Fase 6 (decisão do CTO)**: RF-028 (notificar ao atingir limiares) foi entregue parcialmente — o sinal (`thresholdsCrossed`) é calculado nesta fase, mas a entrega (e-mail/push/in-app) depende da infraestrutura de notificação da Fase 10 (RF-040–043), que no próprio roadmap já depende da Fase 6. Mesmo padrão de resolução de dependência do ADR-0016/0017. Ver [ADR-0018](docs/adr/0018-fase-6-orcamentos.md).

**Nota sobre a Fase 7 (decisão do CTO)**: RF-032 (notificar ao atingir/aproximar de uma meta) foi entregue parcialmente, terceira ocorrência do mesmo padrão de RF-022/RF-028 — o sinal é calculado, a entrega fica para a Fase 10. Ver [ADR-0019](docs/adr/0019-fase-7-metas-financeiras.md).

**Nota sobre a Fase 8 (decisão do CTO)**: RF-034 (Pulse Score) é entregue com uma fórmula provisória e versionada (`formulaVersion: "pulse-v0-provisional"`) — RN-006 e vision.md § 17.5 declaram formalmente que a composição exata é uma decisão de produto/ciência de dados ainda pendente; a fórmula implementada é transparente e determinística (quatro sinais de § 4.8, pesos iguais), mas não substitui essa definição formal. RF-035 (histórico) depende de snapshots diários persistidos a cada chamada a `GET /dashboard` — sem scheduler dedicado nesta fase, o histórico pode ter lacunas em dias sem acesso ao dashboard (RN-005 satisfeita sem violar "não editável manualmente", mas "recálculo periódico" fica condicionado ao uso da API até a Camada de Integração Assíncrona existir). Ver [ADR-0020](docs/adr/0020-fase-8-dashboard-pulse-score.md).

**Nota sobre a Fase 9 (decisão do CTO)**: RF-039 (exportação) é entregue completa para CSV (relatório de gastos por categoria e dados brutos de transações); exportação em PDF foi deliberadamente deixada fora do escopo — vision.md cita "CSV/PDF" como exemplo, não como par obrigatório, e adicionar PDF exigiria uma dependência nova sem decisões de layout especificadas em requisito algum. RF-037/038 recebem período explícito do cliente (sem o backend assumir "mês atual"/"mês anterior" internamente). Ver [ADR-0021](docs/adr/0021-fase-9-relatorios.md).

**Nota sobre a Fase 10 (decisão do CTO)**: fecha a dívida represada de RF-028 (Fase 6) e RF-032 (Fase 7) — o sinal calculado em cada fase agora é efetivamente entregue via `POST /notifications/check` (in-app e/ou e-mail, conforme preferência do usuário, RF-040). RF-043 (lembrete de transação recorrente) permanece fora do escopo — depende de RF-016 (transações recorrentes), que só existe na Fase 4.2, ainda não construída; nem um tipo de alerta "vazio" foi anunciado para essa funcionalidade inexistente, mesma disciplina já aplicada a RF-022/ADR-0017. Canal de e-mail usa uma porta desacoplada (`AlertEmailNotifier`) com adaptador de console apenas — nenhum provedor real integrado, mesmo padrão já estabelecido para recuperação de senha (ADR-0009). Ver [ADR-0022](docs/adr/0022-fase-10-notificacoes.md).

**Nota sobre a Fase 11 (decisão do CTO)**: RF-045 (exclusão de conta) exigiu construir, dentro desta fase, o mecanismo mínimo de exclusão/anonimização de conta — funcionalmente equivalente a RF-007 (Seção 4.2, ainda não migrado por si só para o backend Java) — porque RF-045 não pode ser satisfeito sem esse mecanismo existir, ao contrário de outros adiamentos já registrados (RF-022, RF-043). Replica o design já validado no backend TypeScript (ADR-0010): anonimização, não exclusão física; reautenticação por senha como confirmação explícita. **Dados financeiros (contas, transações, orçamentos, metas etc.) não são apagados nem anonimizados** — apenas o registro `User` — posição deliberadamente provisória e conservadora, pendente da validação jurídica que RN-008/vision.md § 17.2 (dúvida #7) já registram formalmente como não resolvida. RF-046 (consentimento) replica o design de trilha append-only já validado (ADR-0008). Ver [ADR-0023](docs/adr/0023-fase-11-privacidade-lgpd.md).

**Nota sobre a Fase 12 (decisão do CTO)**: RF-047 (isolamento multi-tenant) já era estrutural desde a Fase 3 — cada porta de repositório é escopada por `userId` na própria assinatura desde então; esta fase entrega o hardening como uma suíte de teste consolidada (`MultiTenantIsolationHardeningTest`) que verifica exaustivamente, em um único lugar auditável, todas as áreas de dado do produto para dois tenants, em vez de reescrever repositórios que já estavam corretos. RF-048 (auditoria) é escopado apenas a acesso administrativo/backoffice — o autoacesso já é coberto por RF-047, e consultar o próprio log de auditoria não gera uma nova entrada (decisão deliberada, evita ruído recursivo). RF-049 (investigação de suporte) e RF-050 (suspensão/reativação de conta) são entregues como **versão mínima manual** — RBAC via um campo `Role` no usuário, sem painel administrativo nem endpoint de autopromoção — conforme vision.md § 16 sanciona explicitamente para o MVP; promoção a `SUPPORT_OPERATOR` permanece manual/fora de banda. Suspensão é modelada como reversível e estruturalmente distinta de anonimização (RF-045/Fase 11): não altera e-mail, nome ou senha. Ver [ADR-0024](docs/adr/0024-fase-12-multitenancy-hardening-backoffice.md).

**Esta fase encerra a Parte 1 (MVP) do backend Java** — RF-047 a RF-050 eram os últimos requisitos funcionais pendentes desse escopo; a Fase 13 (Frontend Web) inicia a próxima parte do roadmap.

**Nota sobre a Fase 13 (decisão do CTO)**: decomposta em subfases 13.1–13.9, uma por área funcional já entregue pelo backend, pelo mesmo motivo de granularidade do ADR-0006/ADR-0016 — o volume de telas é maior que qualquer fase de backend já entregue. A Fase 13.1 (Fundação + Autenticação) constatou que `backend-java` não expõe recuperação de senha (RF-005), MFA (RF-004) nem edição de perfil (RF-006) — esses requisitos existem apenas no backend TypeScript legado (`backend/`), nunca migrados para `backend-java` (que começou diretamente na Fase 3, ver ADR-0014, pulando a trilha M1–M2.5.2). O frontend constrói interface apenas para o que `backend-java` realmente expõe; essas três telas ficam pendentes até a migração correspondente. `backend-java` também não tinha CORS configurado — pré-requisito bloqueante adicionado nesta subfase, não uma funcionalidade nova. Ver [ADR-0025](docs/adr/0025-decomposicao-fase-13-frontend.md).

**Nota sobre a Fase 13.2 (decisão do CTO)**: nenhum ADR novo foi produzido — ADR-0025 já declara explicitamente que governa a arquitetura de todas as subfases da Fase 13 e que não precisa ser reaberta salvo decisão nova; 13.2 (CRUD de contas e categorias) não introduziu nenhuma decisão arquitetural além do que já estava definido. A revisão de QA encontrou e corrigiu um bug real no módulo de rede compartilhado (`httpClient.ts`): respostas `HTTP 200` com corpo vazio (padrão usado por vários endpoints de atualização do backend, ex.: `AccountController.update`) quebravam o parsing — corrigido de forma a beneficiar todas as subfases seguintes, que também vão consumir endpoints com esse mesmo formato de resposta.

**Nota sobre a Fase 13.3 (decisão do CTO)**: também sem ADR novo, pelo mesmo motivo de 13.2. A UI de transações reflete fielmente a limitação real do backend (listagem sempre por conta, sem filtro/busca geral — RF-018 é a Fase 4.3, ainda não construída) em vez de simular uma capacidade inexistente. A revisão de QA encontrou e corrigiu uma fonte real de falhas intermitentes na suíte de testes: o `QueryClient` do TanStack Query era uma constante de módulo de `App.tsx`, compartilhada (com cache vazando) entre todos os testes de um mesmo arquivo — extraído para módulo próprio e limpo a cada teste, beneficiando a confiabilidade da suíte para todas as subfases seguintes.

---

## Parte 2 — Pós-MVP (vision.md Seção 15)

Fases futuras, fora do escopo atual, mantidas aqui apenas para rastreabilidade. **Nenhum trabalho de implementação destas fases pode começar antes da conclusão e aprovação de todas as fases da Parte 1.**

| Fase | Nome | Referência vision.md | Status |
|---|---|---|---|
| 14 | Automação e Engajamento (Open Banking leitura, notificações avançadas) | Seção 15 — Fase 2 | ⬜ Não iniciada |
| 15 | Inteligência Financeira (insights, multi-moeda, investimentos) | Seção 15 — Fase 3 | ⬜ Não iniciada |
| 16 | Expansão de Plataforma (mobile nativo, i18n, API pública) | Seção 15 — Fase 4 | ⬜ Não iniciada |

---

## Histórico de Fases Concluídas

*(Atualizado ao final de cada fase aprovada, com link para o relatório de encerramento correspondente.)*


| Fase | Aprovação CTO | Aprovação Stakeholder | ADRs | Revisão QA |
|---|---|---|---|---|
| 1 | ✅ [docs/cto/fase-01-aprovacao.md](docs/cto/fase-01-aprovacao.md) | ✅ (autorização para iniciar a Fase 2) | [0001](docs/adr/0001-stack-tecnologica-backend.md), [0002](docs/adr/0002-arquitetura-hexagonal-backend.md), [0003](docs/adr/0003-persistencia-fase-1.md), [0004](docs/adr/0004-sequenciamento-backend-first.md), [0005](docs/adr/0005-autenticacao-e-sessao.md) | [docs/qa/fase-01-review.md](docs/qa/fase-01-review.md) |
| 2.1 | ✅ [docs/cto/fase-02-1-aprovacao.md](docs/cto/fase-02-1-aprovacao.md) | ✅ (autorização para iniciar a Fase 2.2) | [0006](docs/adr/0006-decomposicao-fase-2.md), [0007](docs/adr/0007-estrategia-refresh-token.md) | [docs/qa/fase-02-1-review.md](docs/qa/fase-02-1-review.md) |
| 2.2 | ✅ [docs/cto/fase-02-2-aprovacao.md](docs/cto/fase-02-2-aprovacao.md) | ✅ (autorização para iniciar a Fase 2.3) | [0008](docs/adr/0008-perfil-e-consentimento.md) | [docs/qa/fase-02-2-review.md](docs/qa/fase-02-2-review.md) |
| 2.3 | ✅ [docs/cto/fase-02-3-aprovacao.md](docs/cto/fase-02-3-aprovacao.md) | ✅ (autorização para iniciar a Fase 2.4) | [0009](docs/adr/0009-recuperacao-de-senha.md) | [docs/qa/fase-02-3-review.md](docs/qa/fase-02-3-review.md) |
| 2.4 | ✅ [docs/cto/fase-02-4-aprovacao.md](docs/cto/fase-02-4-aprovacao.md) | ✅ (autorização para iniciar a Fase 2.5.1) | [0010](docs/adr/0010-exclusao-de-conta.md) | [docs/qa/fase-02-4-review.md](docs/qa/fase-02-4-review.md) |
| 2.5.1 | ✅ [docs/cto/fase-02-5-1-aprovacao.md](docs/cto/fase-02-5-1-aprovacao.md) | ✅ (autorização para completar a Fase 2.5) | [0011](docs/adr/0011-mfa-totp.md) | [docs/qa/fase-02-5-1-review.md](docs/qa/fase-02-5-1-review.md) |
| 2.5.2 | ✅ [docs/cto/fase-02-5-2-aprovacao.md](docs/cto/fase-02-5-2-aprovacao.md) | ✅ (confirmada em 2026-07-31, retroativa — encerra a Fase 2 por completo) | [0012](docs/adr/0012-mfa-integracao-login.md) | [docs/qa/fase-02-5-2-review.md](docs/qa/fase-02-5-2-review.md) |
| 3 (Java) | ✅ [docs/cto/fase-03-java-aprovacao.md](docs/cto/fase-03-java-aprovacao.md) | *aguardando* | [0014](docs/adr/0014-fase-3-contas-carteiras-java.md), [0015](docs/adr/0015-upgrade-java-25.md) | [docs/qa/fase-03-java-review.md](docs/qa/fase-03-java-review.md) |
| 4.1 (Java) | ✅ [docs/cto/fase-04-1-java-aprovacao.md](docs/cto/fase-04-1-java-aprovacao.md) | *aguardando* | [0016](docs/adr/0016-decomposicao-fase-4-e-dependencia-categoria.md) | [docs/qa/fase-04-1-java-review.md](docs/qa/fase-04-1-java-review.md) |
| 5 (Java) | ✅ [docs/cto/fase-05-java-aprovacao.md](docs/cto/fase-05-java-aprovacao.md) | *aguardando* | [0017](docs/adr/0017-fase-5-categorizacao.md) | [docs/qa/fase-05-java-review.md](docs/qa/fase-05-java-review.md) |
| 6 (Java) | ✅ [docs/cto/fase-06-java-aprovacao.md](docs/cto/fase-06-java-aprovacao.md) | *aguardando* | [0018](docs/adr/0018-fase-6-orcamentos.md) | [docs/qa/fase-06-java-review.md](docs/qa/fase-06-java-review.md) |
| 7 (Java) | ✅ [docs/cto/fase-07-java-aprovacao.md](docs/cto/fase-07-java-aprovacao.md) | *aguardando* | [0019](docs/adr/0019-fase-7-metas-financeiras.md) | [docs/qa/fase-07-java-review.md](docs/qa/fase-07-java-review.md) |
| 8 (Java) | ✅ [docs/cto/fase-08-java-aprovacao.md](docs/cto/fase-08-java-aprovacao.md) | *aguardando* | [0020](docs/adr/0020-fase-8-dashboard-pulse-score.md) | [docs/qa/fase-08-java-review.md](docs/qa/fase-08-java-review.md) |
| 9 (Java) | ✅ [docs/cto/fase-09-java-aprovacao.md](docs/cto/fase-09-java-aprovacao.md) | *aguardando* | [0021](docs/adr/0021-fase-9-relatorios.md) | [docs/qa/fase-09-java-review.md](docs/qa/fase-09-java-review.md) |
| 10 (Java) | ✅ [docs/cto/fase-10-java-aprovacao.md](docs/cto/fase-10-java-aprovacao.md) | *aguardando* | [0022](docs/adr/0022-fase-10-notificacoes.md) | [docs/qa/fase-10-java-review.md](docs/qa/fase-10-java-review.md) |
| 11 (Java) | ✅ [docs/cto/fase-11-java-aprovacao.md](docs/cto/fase-11-java-aprovacao.md) | *aguardando* | [0023](docs/adr/0023-fase-11-privacidade-lgpd.md) | [docs/qa/fase-11-java-review.md](docs/qa/fase-11-java-review.md) |
| 12 (Java) | ✅ [docs/cto/fase-12-java-aprovacao.md](docs/cto/fase-12-java-aprovacao.md) | *aguardando* | [0024](docs/adr/0024-fase-12-multitenancy-hardening-backoffice.md) | [docs/qa/fase-12-java-review.md](docs/qa/fase-12-java-review.md) |
| 13.1 (Frontend) | ✅ [docs/cto/fase-13-1-frontend-aprovacao.md](docs/cto/fase-13-1-frontend-aprovacao.md) | *aguardando* | [0025](docs/adr/0025-decomposicao-fase-13-frontend.md) | [docs/qa/fase-13-1-frontend-review.md](docs/qa/fase-13-1-frontend-review.md) |
| 13.2 (Frontend) | ✅ [docs/cto/fase-13-2-frontend-aprovacao.md](docs/cto/fase-13-2-frontend-aprovacao.md) | *aguardando* | nenhum novo (ver ADR-0025) | [docs/qa/fase-13-2-frontend-review.md](docs/qa/fase-13-2-frontend-review.md) |
| 13.3 (Frontend) | ✅ [docs/cto/fase-13-3-frontend-aprovacao.md](docs/cto/fase-13-3-frontend-aprovacao.md) | *aguardando* | nenhum novo (ver ADR-0025) | [docs/qa/fase-13-3-frontend-review.md](docs/qa/fase-13-3-frontend-review.md) |

