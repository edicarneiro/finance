# CTO — Aprovação da Fase 2.3 (Recuperação de Senha)

| Campo | Valor |
|---|---|
| Fase | 2.3 — RF-005 |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-07-30 |
| Resultado | ✅ Aprovado |

> Conforme `rules.md` § 7, este parecer é o encerramento formal da fase do ponto de vista dos agentes — emitido após a aprovação de qualidade do QA.

## Escopo revisado

RF-005 (recuperação de senha via token de e-mail com expiração limitada), conforme ADR-0009.

## Insumos considerados

- [docs/qa/fase-02-3-review.md](../qa/fase-02-3-review.md) — **aprovado**, com um achado crítico encontrado e corrigido na própria revisão.
- [ADR-0009](../adr/0009-recuperacao-de-senha.md).
- Código-fonte em `backend/src/domain/user/`, `backend/src/application/`, `backend/src/adapters/`, `backend/src/composition/`.

## Avaliação do achado crítico

O QA identificou que `SqliteUserRepository.update()` (Fase 2.2) não persistia `password_hash`, o que teria tornado `ResetPasswordUseCase` um no-op silencioso em produção — a senha nunca mudaria de fato, apesar da API responder sucesso. **Endosso a análise de causa raiz do QA**: o problema não foi apenas um bug pontual, mas uma lacuna estrutural — nenhum teste de integração exercitava os adaptadores de produção reais (SQLite/bcrypt/JWT) ponta a ponta, apenas os use cases com dublês em memória.

**Decisão de governança**: formalizei em `rules.md` § 3 a exigência de um smoke test contínuo contra o composition root real (`composition/container.integration.test.ts`, já criado nesta fase), estendido sempre que uma fase futura adicionar um método a um repositório Sqlite já existente. Isso não é apenas documentação do incidente — é uma mudança de processo vinculante para todas as fases seguintes.

## Verificação de aderência arquitetural

- [x] `PasswordResetToken` corretamente modelado como entidade própria em `domain/user/`, com justificativa explícita para não compartilhar abstração com `RefreshToken` (ADR-0009, "regra de três" não satisfeita).
- [x] `PasswordResetNotifier` como porta de intenção ("notificar sobre recuperação"), não de mecanismo ("enviar e-mail") — permite trocar `ConsolePasswordResetNotifier` por um provedor real sem alterar aplicação/domínio.
- [x] Reaproveitamento correto de `RefreshTokenRepository.revokeAllForUser` (Fase 2.1) no `ResetPasswordUseCase`, evitando duplicar lógica de revogação de sessão.
- [x] Reaproveitamento correto de `PasswordPolicy`/`assertStrongPassword` (Fase 1) para validar a nova senha.
- [x] Postura anti-enumeração em `/auth/password-reset/request` consistente com o precedente já estabelecido em `/auth/login` (Fase 1).
- [x] Nenhum desvio arquitetural adicional registrado pelo QA nesta fase — o achado foi de implementação (SQL incompleto), não de arquitetura.

## Decisão

**A Fase 2.3 está aprovada.** RF-005 está completo e corrigido, agora validado também contra os adaptadores de produção reais via o novo smoke test do composition root — que passa a ser exigência permanente do processo (`rules.md` § 3).

Conforme processo definido pelo stakeholder, esta aprovação encerra o ciclo interno dos agentes; a autorização para iniciar a Fase 2.4 (exclusão de conta) permanece com o stakeholder.
