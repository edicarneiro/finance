# CTO — Aprovação da Fase 2.5.1 (MFA — Cadastro e Gestão)

| Campo | Valor |
|---|---|
| Fase | 2.5.1 — RF-004 (cadastro/gestão) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado |

> Conforme `rules.md` § 7, este parecer é o encerramento formal da fase do ponto de vista dos agentes — emitido após a aprovação de qualidade do QA.

## Escopo revisado

RF-004 — cadastro, confirmação, desativação e consulta de status de MFA via TOTP, **sem** alterar o fluxo de login (reservado para a Fase 2.5.2), conforme ADR-0011.

## Insumos considerados

- [docs/qa/fase-02-5-1-review.md](../qa/fase-02-5-1-review.md) — **aprovado**, com uma lacuna de teste em código de segurança encontrada e corrigida na própria revisão.
- [ADR-0011](../adr/0011-mfa-totp.md).
- Código-fonte em `backend/src/domain/user/`, `backend/src/application/`, `backend/src/adapters/`, `backend/src/composition/`.

## Verificação de aderência arquitetural

- [x] `MfaCredential` corretamente modelada como entidade própria em `domain/user/`, sem dependência de `otplib` ou de mecanismos de criptografia — mantém a regra de dependência `adapters → application → domain`.
- [x] Criptografia do segredo TOTP isolada inteiramente no adaptador de persistência (`SqliteMfaCredentialRepository`), nunca vazando para domínio/aplicação — mesmo princípio de isolamento já usado para hash de refresh/reset tokens.
- [x] `TotpService` e `SecretCipher` corretamente desenhadas como portas de propósito único, sem misturar geração/verificação TOTP com criptografia de segredo (responsabilidades distintas, SRP preservado).
- [x] Correção do QA (`confirm(id, confirmedAt)`/`disable(id, disabledAt)` em vez de `update(credential)` genérico) está alinhada com o padrão arquitetural já estabelecido em `RefreshTokenRepository` e `PasswordResetTokenRepository` — **endosso a correção como aderência, não como desvio**.
- [x] Decomposição em 2.5.1/2.5.2 respeitada — nenhuma alteração foi feita em `AuthenticateUserUseCase` ou no fluxo de `/auth/login` nesta subfase.
- [x] `MFA_ENCRYPTION_KEY` tratada com a mesma disciplina de `JWT_SECRET` (obrigatória, validada no bootstrap, nunca hardcoded).
- [x] Nenhum desvio arquitetural adicional registrado pelo QA — o achado foi uma lacuna de teste, não uma divergência de arquitetura.

## Decisão

**A Fase 2.5.1 está aprovada.** Cadastro e gestão de MFA estão completos e corretos, incluindo proteção adequada do segredo em repouso. Reforço a mensagem já registrada no README e no ADR: **MFA ainda não é efetivo em termos de segurança de login** até a Fase 2.5.2 ser concluída — essa fase entrega apenas a fundação (cadastro/gestão), não a funcionalidade de ponta a ponta prometida por RF-004.

Conforme processo definido pelo stakeholder, esta aprovação encerra o ciclo interno dos agentes; a autorização para iniciar a Fase 2.5.2 (integração com o login) permanece com o stakeholder.
