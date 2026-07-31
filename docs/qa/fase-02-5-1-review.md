# QA — Revisão da Fase 2.5.1 (MFA — Cadastro e Gestão)

| Campo | Valor |
|---|---|
| Fase | 2.5.1 — RF-004 (cadastro/gestão, sem integração com login) |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Aderência arquitetural é atribuição do CTO.

## Checklist de Qualidade

- [x] RF-004 (cadastro e gestão de MFA) implementado conforme escopo de ADR-0011.
- [x] Nenhuma regra de negócio ou restrição do vision.md violada.
- [x] Isolamento multi-tenant preservado — `userId` sempre de `getAuthenticatedUserId(req)`.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Segredo TOTP nunca em texto plano em repouso (AES-256-GCM, testado explicitamente que o ciphertext não contém o segredo original).
- [x] Cadastro e desativação exigem reautenticação por senha, consistente com o padrão já estabelecido em RF-007.
- [x] Consistência com o padrão de guarda `isDeleted()` estabelecido na Fase 2.4, aplicado a todos os novos use cases que dependem de identidade ativa.
- [x] Testes cobrem caminho principal (enroll → confirm → status), código inválido, ausência de cadastro pendente/ativo, confirmação dupla, substituição de credencial, e validação de entrada na borda HTTP.
- [x] Código segue Clean Code e SOLID.
- [x] Aderente às regras definidas em `rules.md`.
- [x] Documentação técnica (`backend/README.md`) atualizada, incluindo o aviso explícito de que a Fase 2.5.2 (integração com login) ainda não existe.
- [x] Nenhuma funcionalidade fora do escopo — login não foi alterado nesta subfase, conforme decomposição do ADR-0011.

## Verificação de Execução

```
npm run typecheck    → OK, sem erros
npm test              → 47 arquivos de teste, 231 testes, 100% passando
npm run test:coverage → 97,95% statements | 93,98% branches | 100% funções | 97,93% linhas
```

## Achado Durante a Revisão (encontrado → corrigido nesta mesma revisão)

**Severidade**: Média (lacuna de teste em código de segurança, não um defeito funcional).

**Descrição**: `AesSecretCipher` valida no construtor que a chave tem exatamente 32 bytes (`throw` caso contrário) — uma invariante de segurança importante, já que uma chave de tamanho incorreto para AES-256-GCM ou é rejeitada pelo Node ou (pior) poderia mascarar um erro de configuração silenciosamente em alguma variação futura. Nenhum teste cobria esse guard antes desta revisão.

**Ação**: adicionados dois testes de regressão (`AesSecretCipher.test.ts`): chave menor que 32 bytes e chave maior que 32 bytes, ambos devendo lançar erro na construção. Cobertura de branch do arquivo subiu de 50% para 100%.

**Status**: ✅ Corrigido e validado nesta revisão.

## Verificação da Refatoração de Design (não um achado, mas revisado)

`MfaCredentialRepository` foi desenhado com `confirm(id, confirmedAt)` e `disable(id, disabledAt)` — parâmetros explícitos, em vez de um `update(credential)` genérico que precisaria ler estado privado da entidade de domínio. Revisado e confirmado que segue exatamente o mesmo padrão já usado por `RefreshTokenRepository.revoke(id, revokedAt)` (Fase 2.1) e `PasswordResetTokenRepository.markUsed(id, usedAt)` (Fase 2.3) — consistência correta, sem quebra de encapsulamento da entidade `MfaCredential`.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Cobertura residual não coberta**: mesmos padrões já aceitos em fases anteriores (guardas defensivas inalcançáveis pela API real, caminho 500 genérico, ramo de `createdAt` omitido em `enroll()`).
2. **Sem rate limiting em `/users/me/mfa/confirm`** — código TOTP de 6 dígitos poderia em tese ser testado por força bruta. Mesma limitação já registrada para os demais endpoints de autenticação desde a Fase 1; agora explicitamente mencionada no README para este endpoint específico.
3. **`MFA_ENCRYPTION_KEY` sem mecanismo de rotação** — trocar a chave após já existirem segredos cifrados invalidaria todas as credenciais existentes. Aceitável nesta fase (sem dados de produção), documentado como limitação a resolver antes de deployment real.
4. **Duplicação estrutural entre `MfaCredential`/`PasswordResetToken`/`RefreshToken`** — já registrada e conscientemente adiada em ADR-0011, não uma pendência nova desta revisão.

## Suspeita de Problema Arquitetural

Nenhuma. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado**, após a correção da lacuna de teste no `AesSecretCipher`. RF-004 (cadastro e gestão) está completo dentro do escopo desta subfase; a integração com o login (Fase 2.5.2) permanece pendente e está claramente documentada como tal, para que ninguém trate MFA como "ativo de fato" antes da conclusão.
