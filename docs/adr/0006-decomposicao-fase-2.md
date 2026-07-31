# ADR-0006: Decomposição da Fase 2 em subfases

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 2 |

## Contexto

`roadmap.md` define a Fase 2 como "Gestão de Conta de Usuário", cobrindo RF-004 (MFA), RF-005 (recuperação de senha), RF-006 (edição de perfil), RF-007 (exclusão de conta) e RF-046 (consentimento LGPD) — além do refresh token, já registrado como pendência formal da Fase 1 (ADR-0005). Implementar todo esse escopo como uma única entrega violaria o próprio critério usado para desenhar o roadmap: fases pequenas o suficiente para serem integralmente concluídas (CTO → Full Stack → QA → aprovação) antes da próxima começar.

## Decisão

A Fase 2 é decomposta em subfases, cada uma seguindo o ciclo completo de aprovação antes da seguinte:

| Subfase | Escopo | Requisitos |
|---|---|---|
| 2.1 | Refresh token e logout (fecha a pendência de RF-008 da Fase 1) | RF-008 (renovação segura) |
| 2.2 | Edição de perfil e consentimento LGPD | RF-006, RF-046 |
| 2.3 | Recuperação de senha | RF-005 |
| 2.4 | Exclusão de conta (LGPD) | RF-007 |
| 2.5 | Autenticação multifator (MFA) | RF-004 |

## Justificativa

- 2.1 é pré-requisito natural: outras subfases (ex.: exclusão de conta) devem revogar sessões ativas, o que só é possível com refresh tokens revogáveis já implementados.
- 2.2 é a menor e mais isolada (nenhuma dependência de infraestrutura nova).
- 2.3 introduz uma nova preocupação de infraestrutura (envio de notificação/e-mail), isolada atrás de uma porta — melhor tratada em subfase própria.
- 2.4 depende de 2.1 (revogação de sessão) e do padrão de exclusão/anonimização já definido para RF-045.
- 2.5 (MFA) é a mais complexa (TOTP, códigos de backup) — deixada por último dentro da Fase 2.

## Consequências

- Esta decisão não altera escopo de produto — apenas sequenciamento de entrega, mesmo espírito do ADR-0004.
- `roadmap.md` é atualizado para refletir as subfases 2.1–2.5 no lugar de uma única linha "Fase 2".
