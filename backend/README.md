# FinancePulse Engine — Backend

Backend da Fase 1 do [roadmap.md](../roadmap.md): **Fundação técnica + Cadastro e Login** (RF-001, RF-002, RF-003, RF-008 do [vision.md](../vision.md)).

## Stack

TypeScript + Node.js, Express (HTTP), better-sqlite3 (persistência), bcryptjs (hash de senha), jsonwebtoken (sessão), Vitest (testes). Decisões e alternativas consideradas: [docs/adr/0001-stack-tecnologica-backend.md](../docs/adr/0001-stack-tecnologica-backend.md).

## Arquitetura

Arquitetura Hexagonal (Ports & Adapters), conforme [docs/adr/0002-arquitetura-hexagonal-backend.md](../docs/adr/0002-arquitetura-hexagonal-backend.md):

```
src/
  domain/user/              Entidade User, value object Email, PasswordPolicy, erros de domínio
                             (zero dependência de framework)
  application/
    ports/                  Interfaces: UserRepository, PasswordHasher, TokenService, IdGenerator
    use-cases/               RegisterUserUseCase, AuthenticateUserUseCase
  adapters/
    in/http/                 Rotas Express, middleware de autenticação, tratamento de erros
    out/persistence/         SqliteUserRepository (produção/dev), InMemoryUserRepository (testes)
    out/security/            BcryptPasswordHasher, JwtTokenService, CryptoIdGenerator
  composition/container.ts   Composition root — único módulo que conhece use cases E adaptadores concretos
  server.ts                  Montagem do app Express a partir de dependências já injetadas
  index.ts                   Bootstrap (lê variáveis de ambiente, sobe o servidor)
```

Regra de dependência: `adapters → application → domain`, nunca o inverso (`rules.md` § 1).

## Como rodar

```bash
cp .env.example .env   # ajuste JWT_SECRET antes de qualquer uso além de desenvolvimento local
npm install
npm run dev             # inicia o servidor com watch (tsx)
npm run build && npm start   # build de produção
```

## Como testar

```bash
npm test              # roda toda a suíte (Vitest)
npm run test:watch    # modo watch
npm run test:coverage # relatório de cobertura
npm run typecheck     # apenas checagem de tipos, sem emitir build
```

## Endpoints (Fase 1)

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| `POST` | `/auth/register` | Cadastra um novo usuário (`{ email, password }`) | RF-001, RF-002 |
| `POST` | `/auth/login` | Autentica e emite um token de sessão (`{ email, password }`) | RF-003, RF-008 |
| `GET` | `/auth/me` | Rota protegida de exemplo — valida o token via header `Authorization: Bearer <token>` | RF-008 |

Regras de negócio aplicadas:
- Senha mínima de 8 caracteres (`WeakPasswordError`), decisão de segurança do Full Stack — não é um requisito explícito do vision.md, sinalizado no relatório de encerramento da fase para eventual revisão do CTO/produto.
- E-mail duplicado (mesmo com capitalização diferente) é rejeitado (RF-002), com erro dedicado (`DuplicateEmailError`).
- Login com e-mail inexistente e login com senha incorreta retornam o **mesmo** erro (`InvalidCredentialsError`, HTTP 401) — decisão deliberada para não permitir enumeração de contas via `/auth/login`. O endpoint `/auth/register`, por definição funcional do RF-002, não tem essa mesma proteção (ver relatório de encerramento da Fase 1).

## Limitações conhecidas desta fase

- Token de sessão expira em 15 minutos, sem mecanismo de renovação (refresh token) — pendência formal para a Fase 2, registrada em [docs/adr/0005-autenticacao-e-sessao.md](../docs/adr/0005-autenticacao-e-sessao.md).
- Persistência via SQLite embarcado é adequada para desenvolvimento/validação do MVP; a decisão de motor de banco para produção fica para um ADR futuro (ver [docs/adr/0003-persistencia-fase-1.md](../docs/adr/0003-persistencia-fase-1.md)).
- Sem rate limiting nos endpoints de autenticação ainda — mitigação recomendada para o risco de enumeração de e-mail via `/auth/register` (ver relatório de encerramento da Fase 1).
