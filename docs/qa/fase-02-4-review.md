# QA — Revisão da Fase 2.4 (Exclusão de Conta)

| Campo | Valor |
|---|---|
| Fase | 2.4 — RF-007 |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-30 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Aderência arquitetural é atribuição do CTO.

## Checklist de Qualidade

- [x] RF-007 implementado: exclusão com confirmação explícita (reautenticação por senha) e anonimização.
- [x] Nenhuma regra de negócio ou restrição do vision.md violada.
- [x] Isolamento multi-tenant preservado — `userId` sempre de `getAuthenticatedUserId(req)`, nunca do corpo da requisição.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Dados pessoais (e-mail, nome, senha) irreversivelmente substituídos por valores não identificáveis/inutilizáveis; senha anonimizada é um hash bcrypt real de um valor aleatório (não um sentinel malformado — evita comportamento indefinido do comparador de hash).
- [x] Testes cobrem caminho principal, confirmação incorreta, bloqueio de login pós-exclusão, revogação de sessões, invalidação de token de recuperação pendente, e validação de entrada na borda HTTP.
- [x] Código segue Clean Code e SOLID.
- [x] Aderente às regras definidas em `rules.md`, **incluindo a nova exigência do § 3** (smoke test do composition root estendido nesta fase, já que `SqliteUserRepository.update()` ganhou mais uma coluna).
- [x] Documentação técnica (`backend/README.md`) atualizada e suficiente.
- [x] Nenhuma funcionalidade fora do escopo (MFA/RF-004 corretamente não tocado).

## Verificação de Execução

```
npm run typecheck    → OK, sem erros
npm test              → 38 arquivos de teste, 181 testes, 100% passando
npm run test:coverage → 97,78% statements | 94,44% branches | 100% funções | 97,75% linhas
```

## Achado Durante a Revisão (encontrado → corrigido nesta mesma revisão)

**Severidade**: Média (inconsistência de integridade de dados, não falha de isolamento entre usuários).

**Descrição**: `GetProfileUseCase`, `UpdateProfileUseCase` e `RecordConsentUseCase` não verificavam `user.isDeleted()`. Combinado com a limitação já conhecida e documentada de tokens JWT stateless (ADR-0010 — um token de acesso emitido antes da exclusão continua válido por até 15 minutos), isso permitia que o **próprio usuário**, usando seu próprio token ainda válido, editasse nome/e-mail ou registrasse um novo consentimento em uma conta já anonimizada — sem, no entanto, conseguir reautenticar-se (o hash de senha permanece inutilizável). Não é uma falha de isolamento entre contas (nenhum outro usuário é afetado), mas é uma inconsistência: uma conta marcada como excluída não deveria continuar mutável.

**Ação**: adicionado `|| user.isDeleted()` ao guard de "usuário não encontrado" nos três use cases, tratando uma conta anonimizada como inexistente para qualquer operação que dependa de identidade ativa. `ListConsentHistoryUseCase` foi deliberadamente **não** alterado — é uma leitura histórica pura (não muta estado, não revela identidade "atual"), consistente com o próprio desenho de RF-046 de preservar o histórico mesmo após exclusão. Testes de regressão adicionados aos três use cases corrigidos.

**Status**: ✅ Corrigido e validado nesta revisão.

## Verificação da Exigência de `rules.md` § 3

`SqliteUserRepository.update()` ganhou a coluna `deleted_at` nesta fase. Confirmado que `composition/container.integration.test.ts` foi estendido (não apenas os testes unitários de `SqliteUserRepository.test.ts`) para exercitar exclusão de conta através do composition root real — incluindo login pós-exclusão rejeitado via HTTP real. A regra criada na Fase 2.3 está funcionando como pretendido.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Cobertura residual não coberta**: mesmos padrões já aceitos (guardas defensivas inalcançáveis, caminho 500 genérico). `DeleteAccountUseCase.ts` linha 34 (`UserNotFoundError` defensivo — inalcançável via API real, já que `requireAuth` só produz um `userId` de um usuário que existia no momento da emissão do token).
2. **Limitação de token stateless** (JWT válido por até 15 min após exclusão) — documentada e aceita em ADR-0010, meramente estreitada (não eliminada) pelo achado corrigido acima. Continua sendo dívida técnica conhecida, não uma pendência desta revisão.

## Suspeita de Problema Arquitetural

Nenhuma. O achado é uma lacuna de validação de invariante entre use cases relacionados, não uma divergência da arquitetura aprovada.

## Parecer

**Aprovado**, após a correção do achado de integridade de dados. RF-007 está completo, com anonimização consistentemente aplicada em todas as operações que dependem de uma identidade de usuário ativa.
