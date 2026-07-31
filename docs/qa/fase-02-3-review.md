# QA — Revisão da Fase 2.3 (Recuperação de Senha)

| Campo | Valor |
|---|---|
| Fase | 2.3 — RF-005 |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-30 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Aderência arquitetural é atribuição do CTO.

## Checklist de Qualidade

- [x] RF-005 implementado: fluxo de recuperação via token de e-mail com expiração limitada.
- [x] Nenhuma regra de negócio ou restrição do vision.md violada.
- [x] Isolamento multi-tenant preservado — token de recuperação vinculado a `userId`, nunca aceito diretamente do cliente para identificar a conta.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Token de recuperação nunca armazenado em texto plano (hash SHA-256, testado explicitamente).
- [x] Testes cobrem caminho principal, anti-enumeração, expiração, reuso, revogação de sessões e validação de entrada na borda HTTP.
- [x] Código segue Clean Code e SOLID.
- [x] Aderente às regras definidas em `rules.md`.
- [x] Documentação técnica (`backend/README.md`) atualizada e suficiente.
- [x] Nenhuma funcionalidade fora do escopo (MFA/RF-004 e exclusão de conta/RF-007 corretamente não tocados).

## Verificação de Execução

```
npm run typecheck    → OK, sem erros
npm test              → 36 arquivos de teste, 159 testes, 100% passando
npm run test:coverage → 97,85% statements | 94,2% branches | 100% funções | 97,82% linhas
```

## Achado Crítico Durante a Revisão (encontrado → corrigido nesta mesma revisão)

**Severidade**: Crítica.

**Descrição**: `SqliteUserRepository.update()` (implementado na Fase 2.2 para edição de perfil) executava `UPDATE users SET email = ?, name = ? WHERE id = ?` — **sem incluir `password_hash`**. `ResetPasswordUseCase`, implementado nesta fase, chama exatamente esse método (`userRepository.update(user.withPassword(novoHash))`) para persistir a nova senha. Como todos os testes de integração (`server.test.ts`) usam repositórios em memória (`InMemoryUserRepository`, cujo `update()` substitui o objeto inteiro e portanto "mascarava" o defeito), nenhum teste exercitava esse caminho contra o adaptador SQLite real. **Em produção, o fluxo completo de recuperação de senha teria retornado sucesso (`200 OK`) sem jamais alterar a senha no banco** — a senha antiga continuaria funcionando indefinidamente e a nova nunca funcionaria.

**Causa raiz de processo**: nenhuma suíte de teste exercitava o `composition/container.ts` real (SQLite + bcrypt + JWT) ponta a ponta — toda a cobertura de integração usava adaptadores em memória, válidos para a lógica de aplicação mas cegos a bugs isolados em um adaptador de produção específico.

**Ação**: 
1. Corrigido `SqliteUserRepository.update()` para incluir `password_hash` no `UPDATE`.
2. Adicionados dois testes de regressão em `SqliteUserRepository.test.ts`: persistência da troca de senha via `update()`, e não-regressão de nome/e-mail ao atualizar apenas a senha (evita reintroduzir o inverso do bug).
3. **Adicionado `composition/container.integration.test.ts`**: um teste de fumaça que constrói o container de produção real (`buildContainer`, SQLite `:memory:`) e percorre cadastro → login → edição de perfil → recuperação de senha → login com a nova senha inteiramente através dos adaptadores reais — não apenas dos use cases com dublês. Esse teste teria falhado antes da correção e agora previne regressões da mesma classe (bug de adaptador mascarado por testes só-em-memória) em fases futuras.

**Status**: ✅ Corrigido e validado nesta revisão (159/159 testes passando, incluindo o novo smoke test).

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Cobertura residual não coberta**: mesmos padrões já aceitos em fases anteriores (guardas defensivas inalcançáveis pela API atual, caminho 500 genérico). `ResetPasswordUseCase.ts` linha 35 (`UserNotFoundError` defensivo, inalcançável sem exclusão de conta) segue o mesmo padrão.
2. **`ConsolePasswordResetNotifier` não é um provedor de e-mail real** — decisão já documentada e aceita em ADR-0009, não uma pendência desta revisão.
3. **Sem contramedida de *timing attack*** entre e-mail existente/inexistente em `/auth/password-reset/request` — limitação conhecida e documentada, não corrigida por não ser requisito explícito.

## Recomendação de Processo (para o CTO)

O achado crítico revela uma lacuna estrutural na estratégia de testes, não apenas um bug pontual: **nenhuma fase anterior tinha um teste que exercitasse o composition root real**. Recomenda-se que `container.integration.test.ts` seja mantido e expandido a cada fase futura que adicione um novo método a um repositório Sqlite existente (não apenas ao criar um novo repositório) — é exatamente esse tipo de alteração (adicionar uma coluna a uma operação `UPDATE` já existente) que os testes unitários de use case, com dublês, estruturalmente não conseguem detectar.

## Suspeita de Problema Arquitetural

Nenhuma. O achado desta revisão é um defeito de implementação em um adaptador (bug de SQL), não uma divergência arquitetural — corrigido diretamente pelo QA→Full Stack sem necessidade de arbitragem do CTO.

## Parecer

**Aprovado**, após a correção do achado crítico. RF-005 está completo e agora validado também contra os adaptadores de produção reais.
