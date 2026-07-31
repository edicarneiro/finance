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
| 4 | Transações (manuais e importação) | RF-014 a RF-021 | Fase 3 | ⬜ Não iniciada |
| 5 | Categorização | RF-022 a RF-025 | Fase 4 | ⬜ Não iniciada |
| 6 | Orçamentos | RF-026 a RF-029 | Fase 5 | ⬜ Não iniciada |
| 7 | Metas Financeiras | RF-030 a RF-032 | Fase 4 | ⬜ Não iniciada |
| 8 | Dashboard e Pulse Score | RF-033 a RF-036 | Fases 4, 5, 6, 7 | ⬜ Não iniciada |
| 9 | Relatórios | RF-037 a RF-039 | Fases 4, 5 | ⬜ Não iniciada |
| 10 | Notificações | RF-040 a RF-043 | Fases 6, 7 | ⬜ Não iniciada |
| 11 | Privacidade e Conformidade (LGPD) | RF-044, RF-045 | Fase 3 | ⬜ Não iniciada |
| 12 | Multi-tenancy Hardening e Backoffice | RF-047 a RF-050 | Todas as anteriores | ⬜ Não iniciada |
| 13 | Frontend Web (MVP) | Interface para todas as funcionalidades acima | Fases 1 a 12 | ⬜ Não iniciada |

**Nota de sequenciamento (decisão do CTO)**: as Fases 1 a 12 entregam a API backend; a Fase 13 entrega a interface web consumindo essa API já estabilizada. Isso evita retrabalho de UI a cada mudança de contrato durante a fase de maior instabilidade de API. Ver [ADR-0004](docs/adr/0004-sequenciamento-backend-first.md).

**Nota sobre a Fase 2 (decisão do CTO)**: decomposta em subfases 2.1–2.5 para preservar a granularidade "uma fase = totalmente concluível antes da próxima". Ver [ADR-0006](docs/adr/0006-decomposicao-fase-2.md).

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
