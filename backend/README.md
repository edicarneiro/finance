# FinancePulse Engine — Backend

Backend do [roadmap.md](../roadmap.md) — Fase 1 (**Fundação técnica + Cadastro e Login**), Fase 2.1 (**Refresh token e logout**), Fase 2.2 (**Edição de perfil e consentimento LGPD**) e Fase 2.3 (**Recuperação de senha**), cobrindo RF-001, RF-002, RF-003, RF-005, RF-006, RF-008 e RF-046 do [vision.md](../vision.md).

## Stack

TypeScript + Node.js, Express (HTTP), better-sqlite3 (persistência), bcryptjs (hash de senha), jsonwebtoken (token de acesso), Vitest (testes). Decisões e alternativas consideradas: [docs/adr/0001-stack-tecnologica-backend.md](../docs/adr/0001-stack-tecnologica-backend.md).

## Arquitetura

Arquitetura Hexagonal (Ports & Adapters), conforme [docs/adr/0002-arquitetura-hexagonal-backend.md](../docs/adr/0002-arquitetura-hexagonal-backend.md):

```
src/
  domain/
    user/                     Entidade User (id, email, passwordHash, name), value object Email,
                               ConsentRecord (audit trail append-only), PasswordResetToken (uso único),
                               PasswordPolicy, ProfilePolicy, ConsentPolicy, erros de domínio
    session/                  Entidade RefreshToken (emitido/revogado/expirado), erros de domínio
                               (nenhum destes depende de framework)
  application/
    ports/                    UserRepository, ConsentRepository, PasswordResetTokenRepository,
                               PasswordResetTokenGenerator, PasswordResetNotifier, PasswordHasher,
                               TokenService, IdGenerator, RefreshTokenRepository, RefreshTokenGenerator, Clock
    services/
      SessionIssuer.ts         Emite par (access token + refresh token) — compartilhado entre
                                login e renovação, evitando duplicação (ADR-0007)
    use-cases/                 RegisterUserUseCase, AuthenticateUserUseCase, RefreshAccessTokenUseCase,
                                LogoutUseCase, RequestPasswordResetUseCase, ResetPasswordUseCase,
                                GetProfileUseCase, UpdateProfileUseCase,
                                RecordConsentUseCase, ListConsentHistoryUseCase
  adapters/
    in/http/                   Rotas Express (authRoutes, userRoutes), middleware de autenticação,
                                tratamento de erros
    out/persistence/           SqliteUserRepository, SqliteRefreshTokenRepository, SqliteConsentRepository,
                                SqlitePasswordResetTokenRepository (produção/dev), variantes InMemory* (testes)
    out/security/               BcryptPasswordHasher, JwtTokenService, CryptoIdGenerator,
                                RandomRefreshTokenGenerator, RandomPasswordResetTokenGenerator
    out/notification/           ConsolePasswordResetNotifier (loga em vez de enviar e-mail real — ver Limitações)
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
| `POST` | `/auth/password-reset/request` | Solicita recuperação de senha (`{ email }`) — sempre `202`, exista ou não o e-mail | RF-005 |
| `POST` | `/auth/password-reset/confirm` | Confirma a troca de senha (`{ token, newPassword }`) | RF-005 |
| `GET` | `/users/me` | Retorna o perfil do usuário autenticado (`{ id, name, email }`) | RF-006 |
| `PUT` | `/users/me` | Atualiza nome e e-mail do usuário autenticado (`{ name, email }`, substituição completa) | RF-006 |
| `POST` | `/users/me/consent` | Registra um novo aceite de consentimento (`{ version }`) | RF-046 |
| `GET` | `/users/me/consent` | Lista o histórico completo de consentimentos do usuário | RF-046 |

Todas as rotas de `/users` exigem `Authorization: Bearer <token>` (a antiga rota de exemplo `GET /auth/me` da Fase 1 foi removida em favor de `GET /users/me`, que agora entrega dados reais — ver [ADR-0008](../docs/adr/0008-perfil-e-consentimento.md)).

Regras de negócio aplicadas:
- Senha mínima de 8 caracteres (`WeakPasswordError`), decisão de segurança do Full Stack — não é um requisito explícito do vision.md, sinalizado no relatório de encerramento da Fase 1 para eventual revisão do CTO/produto.
- E-mail duplicado (mesmo com capitalização diferente) é rejeitado tanto no cadastro (RF-002) quanto na edição de perfil, com erro dedicado (`DuplicateEmailError`).
- Login com e-mail inexistente e login com senha incorreta retornam o **mesmo** erro (`InvalidCredentialsError`, HTTP 401) — decisão deliberada para não permitir enumeração de contas via `/auth/login`.
- Token de acesso (JWT) expira em 15 minutos; refresh token expira em 7 dias, é de uso único (rotação a cada `/auth/refresh`) e é armazenado apenas como hash SHA-256, nunca em texto plano. Reapresentar um refresh token já rotacionado/revogado derruba **todas** as sessões do usuário — ver [ADR-0007](../docs/adr/0007-estrategia-refresh-token.md).
- Edição de perfil (`PUT /users/me`) é uma substituição completa de `{ name, email }`, não um PATCH parcial. Nome deve ter entre 1 e 100 caracteres após `trim()`.
- Consentimento é um **registro imutável e append-only** — cada aceite gera um novo registro, o histórico completo é preservado (RF-046, ADR-0008).
- Recuperação de senha (RF-005): token opaco de 256 bits, hash SHA-256 em repouso, TTL de **1 hora**, uso único. Solicitar uma nova recuperação invalida qualquer token anterior ainda válido do mesmo usuário. Uma troca de senha bem-sucedida **revoga todas as sessões ativas** do usuário (reaproveitando `RefreshTokenRepository.revokeAllForUser` da Fase 2.1). `POST /auth/password-reset/request` sempre responde `202` de forma idêntica, exista ou não o e-mail — mesma postura anti-enumeração do login (ver [ADR-0009](../docs/adr/0009-recuperacao-de-senha.md)).

## Limitações conhecidas

- Persistência via SQLite embarcado é adequada para desenvolvimento/validação do MVP; a decisão de motor de banco para produção fica para um ADR futuro (ver [docs/adr/0003-persistencia-fase-1.md](../docs/adr/0003-persistencia-fase-1.md)). Sem migração automatizada de schema ainda — mudanças de coluna (ex.: `name` adicionado na Fase 2.2) assumem banco de desenvolvimento sem dados de produção.
- Sem rate limiting nos endpoints de autenticação ainda.
- **"Preferências" (parte de RF-006) não implementadas** — vision.md não define o que constitui uma preferência de usuário; tratado como dúvida em aberto, não como lacuna de implementação (ver ADR-0008 e o relatório de encerramento da Fase 2.2).
- Sem verificação de propriedade de e-mail (nem no cadastro, nem na troca via perfil) — o sistema ainda não possui mecanismo de confirmação por e-mail.
- **`ConsolePasswordResetNotifier` apenas loga o token de recuperação** — nenhum provedor de e-mail real está integrado ainda; deve ser substituído por um adaptador real antes de qualquer deployment de produção (ver ADR-0009).
- Sem contramedida para diferença de tempo de resposta entre e-mail existente/inexistente em `/auth/password-reset/request` (*timing attack* teórico) — aceito como limitação conhecida, não corrigido por não ser requisito explícito.
- MFA (RF-004) e exclusão de conta (RF-007) ainda não implementados — ver subfases 2.4 e 2.5 em [roadmap.md](../roadmap.md).
