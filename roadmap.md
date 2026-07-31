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

## Parte 1 — MVP (vision.md Seção 14)

O MVP foi decomposto em 13 fases, seguindo a ordem de dependência natural do domínio: fundação técnica e identidade primeiro, depois dados financeiros centrais (contas, transações), depois as camadas de valor agregado (categorização, orçamentos, metas, dashboard), depois camadas de suporte (relatórios, notificações, privacidade, backoffice), e por fim a interface web consumindo a API já estabilizada.

| Fase | Nome | Requisitos do vision.md | Depende de | Status |
|---|---|---|---|---|
| 1 | Fundação técnica + Cadastro e Login | RF-001, RF-002, RF-003, RF-008 | — | ✅ Concluída |
| 2.1 | Refresh token e logout | RF-008 (renovação segura) | Fase 1 | 🟡 Aguardando aprovação |
| 2.2 | Edição de perfil e consentimento LGPD | RF-006, RF-046 | Fase 2.1 | ⬜ Não iniciada |
| 2.3 | Recuperação de senha | RF-005 | Fase 2.1 | ⬜ Não iniciada |
| 2.4 | Exclusão de conta (LGPD) | RF-007 | Fase 2.1 | ⬜ Não iniciada |
| 2.5 | Autenticação multifator (MFA) | RF-004 | Fase 2.1 | ⬜ Não iniciada |
| 3 | Contas e Carteiras Financeiras | RF-009 a RF-013 | Fase 1 | ⬜ Não iniciada |
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
| 2.1 | ✅ [docs/cto/fase-02-1-aprovacao.md](docs/cto/fase-02-1-aprovacao.md) | *aguardando* | [0006](docs/adr/0006-decomposicao-fase-2.md), [0007](docs/adr/0007-estrategia-refresh-token.md) | [docs/qa/fase-02-1-review.md](docs/qa/fase-02-1-review.md) |
