# FinancePulse Engine — Backend

Backend do [roadmap.md](../roadmap.md) — Fase 1 (**Fundação técnica + Cadastro e Login**) e Fase 2.1 (**Refresh token e logout**), cobrindo RF-001, RF-002, RF-003 e RF-008 do [vision.md](../vision.md).

## Stack

TypeScript + Node.js, Express (HTTP), better-sqlite3 (persistência), bcryptjs (hash de senha), jsonwebtoken (token de acesso), Vitest (testes). Decisões e alternativas consideradas: [docs/adr/0001-stack-tecnologica-backend.md](../docs/adr/0001-stack-tecnologica-backend.md).

## Arquitetura

Arquitetura Hexagonal (Ports & Adapters), conforme [docs/adr/0002-arquitetura-hexagonal-backend.md](../docs/adr/0002-arquitetura-hexagonal-backend.md):

```
src/
  domain/
    user/                     Entidade User, value object Email, PasswordPolicy, erros de domínio
    session/                  Entidade RefreshToken (emitido/revogado/expirado), erros de domínio
                               (ambos sem dependência de framework)
  application/
    ports/                    UserRepository, PasswordHasher, TokenService, IdGenerator,
                               RefreshTokenRepository, RefreshTokenGenerator, Clock
    services/
      SessionIssuer.ts         Emite par (access token + refresh token) — compartilhado entre
                                login e renovação, evitando duplicação (ADR-0007)
    use-cases/                 RegisterUserUseCase, AuthenticateUserUseCase,
                                RefreshAccessTokenUseCase, LogoutUseCase
  adapters/
    in/http/                   Rotas Express, middleware de autenticação, tratamento de erros
    out/persistence/           SqliteUserRepository, SqliteRefreshTokenRepository (produção/dev),
                                variantes InMemory* (testes)
    out/security/               BcryptPasswordHasher, JwtTokenService, CryptoIdGenerator,
                                RandomRefreshTokenGenerator
    out/time/                   SystemClock
  composition/container.ts     Composition root — único módulo que conhece use cases E adaptadores concretos
  server.ts                    Montagem do app Express a partir de dependências já injetadas
  index.ts                     Bootstrap (lê variáveis de ambiente, sobe o servidor)
  test-support/                Dublês de teste reutilizados entre unit tests (Fake*, Sequential*, FixedClock) —
                                nunca importado por código de produção
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

## Endpoints

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| `POST` | `/auth/register` | Cadastra um novo usuário (`{ email, password }`) | RF-001, RF-002 |
| `POST` | `/auth/login` | Autentica e emite `{ token, refreshToken }` | RF-003, RF-008 |
| `POST` | `/auth/refresh` | Troca um refresh token válido por um novo par `{ token, refreshToken }` (rotação) | RF-008 |
| `POST` | `/auth/logout` | Revoga um refresh token (`{ refreshToken }`), idempotente — `204 No Content` | RF-008 |
| `GET` | `/auth/me` | Rota protegida de exemplo — valida o token via header `Authorization: Bearer <token>` | RF-008 |

Regras de negócio aplicadas:
- Senha mínima de 8 caracteres (`WeakPasswordError`), decisão de segurança do Full Stack — não é um requisito explícito do vision.md, sinalizado no relatório de encerramento da Fase 1 para eventual revisão do CTO/produto.
- E-mail duplicado (mesmo com capitalização diferente) é rejeitado (RF-002), com erro dedicado (`DuplicateEmailError`).
- Login com e-mail inexistente e login com senha incorreta retornam o **mesmo** erro (`InvalidCredentialsError`, HTTP 401) — decisão deliberada para não permitir enumeração de contas via `/auth/login`. O endpoint `/auth/register`, por definição funcional do RF-002, não tem essa mesma proteção.
- Token de acesso (JWT) expira em 15 minutos; refresh token expira em 7 dias, é de uso único (rotação a cada `/auth/refresh`) e é armazenado apenas como hash SHA-256, nunca em texto plano. Reapresentar um refresh token já rotacionado/revogado derruba **todas** as sessões do usuário (sinal de possível roubo de token) — ver [ADR-0007](../docs/adr/0007-estrategia-refresh-token.md).

## Limitações conhecidas

- Persistência via SQLite embarcado é adequada para desenvolvimento/validação do MVP; a decisão de motor de banco para produção fica para um ADR futuro (ver [docs/adr/0003-persistencia-fase-1.md](../docs/adr/0003-persistencia-fase-1.md)).
- Sem rate limiting nos endpoints de autenticação ainda — mitigação recomendada para o risco de enumeração de e-mail via `/auth/register` e para tentativas de força bruta em `/auth/login` e `/auth/refresh`.
- MFA (RF-004), recuperação de senha (RF-005), edição de perfil (RF-006), exclusão de conta (RF-007) e consentimento LGPD (RF-046) ainda não implementados — ver subfases 2.2 a 2.5 em [roadmap.md](../roadmap.md).
