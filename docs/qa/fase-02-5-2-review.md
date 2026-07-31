# QA — Revisão da Fase 2.5.2 (MFA — Integração com o Login)

| Campo | Valor |
|---|---|
| Fase | 2.5.2 — RF-004 (integração com login, fecha RF-004 por completo) |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Aderência arquitetural é atribuição do CTO.

## Checklist de Qualidade

- [x] RF-004 implementado de ponta a ponta: cadastro, confirmação, desativação **e agora exigência efetiva no login**.
- [x] Contas sem MFA: nenhuma mudança de comportamento em `/auth/login` (verificado por teste dedicado).
- [x] Isolamento multi-tenant preservado — desafio de MFA sempre vinculado ao `userId` que originou o login, nunca aceito do cliente.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Desafio de MFA nunca em texto plano em repouso (hash SHA-256, mesmo padrão de refresh/reset tokens).
- [x] Testes cobrem: login sem MFA (inalterado), login com MFA (retorna desafio), conclusão via `/auth/login/mfa`, reuso de desafio já usado, desafio expirado, código incorreto (sem consumir o desafio), desafio desconhecido, validação de entrada na borda HTTP, e um teste de integração ponta a ponta com código TOTP real (RFC 6238) contra o composition root real.
- [x] Código segue Clean Code e SOLID.
- [x] Aderente às regras definidas em `rules.md`.
- [x] Documentação técnica (`backend/README.md`) atualizada — a nota de "MFA ainda não integrado ao login" da Fase 2.5.1 foi corretamente removida/substituída.
- [x] Nenhuma funcionalidade fora do escopo desta subfase.

## Verificação de Execução

```
npm run typecheck    → OK, sem erros
npm test              → 53 arquivos de teste, 263 testes, 100% passando
npm run test:coverage → 98,21% statements | 94% branches | 100% funções | 98,19% linhas
```

## Achado Crítico Durante a Revisão (encontrado → corrigido nesta mesma revisão)

**Severidade**: Crítica (falha de segurança — bypass do invariante "conta excluída não pode obter nova sessão").

**Descrição**: `DeleteAccountUseCase` (Fase 2.4) revoga refresh tokens (`revokeAllForUser`) e invalida tokens de recuperação de senha pendentes (`invalidateAllForUser`), mas **não desativava a credencial MFA** do usuário. Como `AuthenticateUserUseCase` bloqueia login para contas excluídas **antes** de sequer checar MFA, não é possível iniciar um *novo* login após a exclusão. Porém, um desafio de MFA emitido **antes** da exclusão (`mfaRequired: true`, com `challengeToken` de até 5 minutos de validade) permanecia utilizável depois: como a credencial MFA nunca era desativada, `POST /auth/login/mfa` com esse desafio ainda pendente + o código TOTP correto emitia **normalmente** um novo par de tokens de sessão para uma conta já anonimizada — reabrindo acesso a uma conta "excluída".

Cenário de exploração: usuário/atacante possui um token de acesso válido de uma sessão anterior; inicia um novo login (obtendo um `challengeToken` pendente, sem ainda ter sessão); usa o token da sessão anterior para chamar `DELETE /users/me`; em seguida completa o `challengeToken` ainda válido via `/auth/login/mfa`, obtendo uma sessão nova para a conta excluída.

**Ação**: `DeleteAccountUseCase` agora desativa a credencial MFA ativa do usuário (`mfaCredentialRepository.disable`) como parte da exclusão, no mesmo passo que já revoga sessões e invalida tokens de recuperação — reaproveitando o guard já existente em `CompleteMfaLoginUseCase` (`credential.isActive()`), que passa a rejeitar corretamente qualquer desafio pendente após a correção. Teste de regressão adicionado, reproduzindo o cenário completo (enroll → confirm → emitir desafio → excluir conta → tentar completar o desafio) e confirmando `InvalidOrExpiredMfaChallengeError`.

**Status**: ✅ Corrigido e validado nesta revisão (263/263 testes, incluindo a nova regressão).

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Cobertura residual não coberta**: mesmos padrões já aceitos (guardas defensivas inalcançáveis, caminho 500 genérico, ramos de fallback de `createdAt`/`generateSecret`).
2. **Sem rate limiting em `/auth/login/mfa`** — mesma classe de risco já documentada para `/users/me/mfa/confirm`, agora também presente no endpoint de conclusão de login.
3. **Duplicação estrutural `MfaChallenge`/`PasswordResetToken`/`RefreshToken`** — "regra de três" satisfeita, adiada conscientemente (ADR-0011, ADR-0012), não uma pendência desta revisão.

## Suspeita de Problema Arquitetural

Nenhuma. O achado foi uma lacuna de efeito colateral em um use case (uma chamada faltante), não uma divergência de arquitetura — corrigido diretamente pelo QA→Full Stack sem necessidade de arbitragem do CTO.

## Parecer

**Aprovado**, após a correção do achado crítico. RF-004 está agora **completo de ponta a ponta**, e a correção fecha um bypass real do invariante de exclusão de conta estabelecido desde a Fase 2.4.
