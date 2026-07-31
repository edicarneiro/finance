# QA — Revisão da Fase 1 (Fundação Técnica + Cadastro e Login)

| Campo | Valor |
|---|---|
| Fase | 1 — RF-001, RF-002, RF-003, RF-008 |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-30 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADRs) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança, observabilidade e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-001, RF-002, RF-003 e RF-008 do vision.md.
- [x] Não há violação de regra de negócio ou restrição do vision.md.
- [x] N/A nesta fase: isolamento multi-tenant (RF-047) — ainda não há dado financeiro de outro usuário a isolar; torna-se aplicável a partir da Fase 3.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Senha nunca armazenada ou logada em texto plano; hash via bcrypt.
- [x] Autenticação aplicada corretamente na rota protegida de exemplo (`GET /auth/me`).
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (38 testes, 97,5% statements / 97,2% branches).
- [x] Sem degradação de performance perceptível para o escopo (operações simples de I/O local).
- [x] Código segue Clean Code (nomes revelam intenção, funções pequenas, sem duplicação, sem código morto).
- [x] Código segue SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`.
- [x] Documentação técnica entregue (`backend/README.md`) é clara e suficiente.
- [x] Não há introdução de funcionalidade fora do escopo da Fase 1.

## Verificação de Execução

```
npm run typecheck   → OK, sem erros
npm test             → 10 arquivos de teste, 38 testes, 100% passando
npm run test:coverage → 97,5% statements | 97,2% branches | 100% funções | 97,5% linhas
```

## Avaliação por Critério

**Clean Code**: nomes de classes/métodos revelam intenção (`RegisterUserUseCase.execute`, `assertStrongPassword`, `InvalidCredentialsError`); nenhuma função excede um nível de abstração; nenhum comentário explica "o quê" — os dois comentários existentes no código (`AuthenticateUserUseCase.test.ts`, `errorHandler.ts`) explicam decisões não óbvias ("por quê"), conforme `rules.md` § 2.

**SOLID**: `RegisterUserUseCase` e `AuthenticateUserUseCase` têm responsabilidade única; todas as três portas (`UserRepository`, `PasswordHasher`, `TokenService`) têm implementações substituíveis sem quebra de contrato (verificado nos próprios testes, que trocam a implementação real por dublês); interfaces são específicas ao consumidor (nenhuma porta "faz-tudo"); use cases dependem exclusivamente de abstrações — nenhuma importação de `bcryptjs`, `jsonwebtoken`, `express` ou `better-sqlite3` foi encontrada fora da camada `adapters/`.

**Testes**: pirâmide respeitada — testes de domínio/aplicação são unitários com dublês (sem I/O), testes de adaptadores validam a tecnologia real encapsulada (bcrypt real, JWT real, SQLite `:memory:` real), e um teste de integração HTTP cobre o fluxo completo register → login → rota protegida. Nenhum teste foi pulado ou comentado.

**Segurança**:
- Hash de senha com bcrypt (nunca texto plano), segredo JWT via variável de ambiente (nunca hardcoded), `.env` no `.gitignore`.
- `errorHandler` não vaza stack trace nem detalhes internos ao cliente em erros inesperados (HTTP 500 genérico; detalhe fica apenas no log de servidor).
- Login retorna o **mesmo** erro para "e-mail inexistente" e "senha incorreta" (`InvalidCredentialsError`), prevenindo enumeração de contas via login — validado por teste dedicado.

## Achado Durante a Revisão (encontrado → corrigido nesta mesma revisão)

**Severidade**: Alta (defeito de tratamento de entrada, não de arquitetura).

**Descrição**: `POST /auth/register` e `POST /auth/login` não validavam a presença/tipo dos campos `email`/`password` antes de repassá-los aos use cases. Uma requisição com corpo vazio ou campos ausentes acionava uma exceção não tratada (`TypeError` ao chamar `.trim()`/`.length` em `undefined`), resultando em **HTTP 500** — semântica incorreta para um erro de entrada do cliente, que deveria ser **HTTP 400**.

**Ação**: devolvido ao Full Stack Engineer durante esta revisão. Correção aplicada: `adapters/in/http/parseCredentials.ts`, validando o formato do corpo da requisição na borda HTTP antes de qualquer chamada a use case, com `InvalidRequestBodyError` mapeado para HTTP 400 em `errorHandler.ts`. Testes de regressão adicionados (unitários em `parseCredentials.test.ts` e de integração em `server.test.ts`). Suíte reexecutada: 38/38 testes passando.

**Status**: ✅ Corrigido e validado nesta revisão.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Cobertura residual não coberta** (`errorHandler.ts` linhas 26–27, caminho de erro 500 genérico; `JwtTokenService.ts` linha 24, ramo de payload de token estruturalmente inesperado): ambos são caminhos defensivos de baixíssima probabilidade. Não foram adicionados testes adicionais para forçar 100%, conforme `rules.md` § 3 (cobertura orientada a risco real, não a meta percentual arbitrária).
2. **Observabilidade mínima**: apenas erros não tratados são logados (via `console.error`, não estruturado). `rules.md` § 5 exige log estruturado para escrita de **dados financeiros** — ainda não aplicável nesta fase (não há dado financeiro até a Fase 3) — mas recomenda-se avaliar logging estruturado de eventos de autenticação (registro, login malsucedido) como hardening de segurança antes da Fase 2. Registrado como recomendação, não como não conformidade.
3. **Ausência de rate limiting** em `/auth/register` e `/auth/login`. Combinado com o fato de que RF-002 exige sinalizar e-mail duplicado, `/auth/register` permite, na prática, enumeração de e-mails cadastrados (comportamento funcionalmente exigido, não um bug). Recomenda-se rate limiting como mitigação em fase de hardening de segurança — não bloqueia a Fase 1, mas deve entrar no roadmap de segurança.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Todos os itens críticos e de alta severidade identificados durante a revisão foram corrigidos e revalidados. Os itens não bloqueantes acima são recomendações para fases seguintes, não pendências da Fase 1.
