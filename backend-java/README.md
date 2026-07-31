# FinancePulse Engine — Backend (Java)

Backend Java do [roadmap.md](../roadmap.md), cobrindo:
- **M1** (equivalente à Fase 1 original: **Fundação técnica + Cadastro e Login**) — RF-001, RF-002, RF-003, RF-008.
- **Fase 3** (**Contas e Carteiras Financeiras**, primeira fase construída diretamente em Java, sem equivalente TypeScript anterior) — RF-009 a RF-013.

> Por decisão do stakeholder, a Fase 3 foi iniciada **antes** da conclusão de M2.1–M2.5.2 (refresh token, logout, perfil/consentimento, recuperação de senha, exclusão de conta, MFA) — ver [ADR-0014](../docs/adr/0014-fase-3-contas-carteiras-java.md). O backend TypeScript ([backend/](../backend/)) permanece intacto e é a referência funcional para as fases ainda não migradas. `rules.md` § 7 registra que, a partir de 2026-07-31, a aprovação do stakeholder deixou de ser um bloqueio obrigatório entre fases.

## Stack

Java 25, Spring Boot 3.5.4, Maven, Spring Data JPA + H2 embarcado (persistência), `spring-security-crypto` isolado — **não** o starter completo `spring-boot-starter-security` (hash de senha via `BCryptPasswordEncoder`), `jjwt` (token de acesso), JUnit 5 + AssertJ (testes, dublês escritos à mão em vez de Mockito). Decisões e alternativas consideradas: [docs/adr/0013-migracao-java-spring-boot.md](../docs/adr/0013-migracao-java-spring-boot.md) (stack original) e [docs/adr/0015-upgrade-java-25.md](../docs/adr/0015-upgrade-java-25.md) (atualização de Java 17 para Java 25).

## Arquitetura

Arquitetura Hexagonal (Ports & Adapters) — mesma regra de dependência do backend TypeScript ([ADR-0002](../docs/adr/0002-arquitetura-hexagonal-backend.md)), com o contêiner do Spring como mecanismo de composição em vez de um composition root manual ([ADR-0013](../docs/adr/0013-migracao-java-spring-boot.md)):

```
src/main/java/com/financepulse/engine/
  domain/
    user/                     Entidade User (id, email, passwordHash, name, createdAt), value object Email,
                               PasswordPolicy, erros de domínio — zero anotação/dependência de framework
      errors/                 InvalidEmailException, WeakPasswordException, DuplicateEmailException,
                               InvalidCredentialsException
    account/                  Entidade Account (id, userId, type, name, currency, balance, archived, createdAt),
                               AccountType (enum), value object Currency (ISO 4217), AccountPolicy
      errors/                 InvalidAccountNameException, InvalidCurrencyException, AccountNotFoundException
  application/
    ports/                    UserRepository, PasswordHasher, TokenService, IdGenerator, AccountRepository
                               (interfaces Java puras — AccountRepository escopa toda leitura/escrita por
                               userId na própria assinatura, reforço estrutural de RF-047)
    usecases/                 RegisterUserUseCase, AuthenticateUserUseCase — também sem anotação de framework
      account/                CreateAccountUseCase, UpdateAccountUseCase, ArchiveAccountUseCase,
                               ListAccountsUseCase, GetConsolidatedBalanceUseCase
  adapters/
    in/web/                   AuthController (POST /auth/register, /auth/login), AccountController
                               (rotas protegidas /accounts/**), DTOs, GlobalExceptionHandler,
                               AuthenticatedUserResolver + AuthenticationInterceptor (aplicação de RF-008 —
                               primeira rota protegida do backend Java, ver ADR-0014)
    out/persistence/          UserJpaEntity/SpringDataUserJpaRepository/JpaUserRepositoryAdapter,
                               AccountJpaEntity/SpringDataAccountJpaRepository/JpaAccountRepositoryAdapter
    out/security/             BCryptPasswordHasherAdapter, JwtTokenServiceAdapter, UuidIdGeneratorAdapter
  composition/
    UseCaseConfiguration.java Raiz de composição da camada de aplicação — os casos de uso não têm anotação
                               Spring (regra da Arquitetura Hexagonal), então são instanciados explicitamente
                               aqui via @Bean, em vez de descobertos por @Component
    WebMvcConfig.java         Registra AuthenticationInterceptor para as rotas /accounts/**
  FinancepulseEngineApplication.java      Classe main (Spring Boot)
```

Regra de dependência: `adapters → application → domain`, nunca o inverso (`rules.md` § 1) — idêntica à do backend TypeScript, apenas o mecanismo de composição muda.

## Como rodar

Pré-requisitos: JDK 25 e Maven (ver nota nas Limitações conhecidas sobre as instalações usadas neste ambiente de desenvolvimento).

```bash
export FINANCEPULSE_JWT_SECRET=troque-em-producao   # opcional em dev, tem default em application.properties
mvn spring-boot:run
```

## Como testar

```bash
mvn test
```

86 testes (JUnit 5), cobrindo domínio, casos de uso (com dublês em memória), adaptadores contra tecnologia real (H2 real via `@DataJpaTest`, BCrypt real, JWT real) e smoke tests de ponta a ponta contra a raiz de composição real (`AuthControllerTest`, `AccountControllerTest` — mesma disciplina do `container.integration.test.ts` do backend TypeScript, `rules.md` § 3).

## Endpoints

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| `POST` | `/auth/register` | Cadastra um novo usuário (`{ email, password }`); responde `201 { userId }` | RF-001, RF-002 |
| `POST` | `/auth/login` | Autentica com e-mail/senha; responde `200 { token }` | RF-003, RF-008 |
| `POST` | `/accounts` | Cria uma conta financeira (`{ type, name, currency, initialBalance }`); responde `201 { accountId }` | RF-009 |
| `GET` | `/accounts` | Lista as contas do usuário autenticado (ativas e arquivadas), cada uma com seu saldo atual | RF-010, RF-011 |
| `GET` | `/accounts/balance/consolidated` | Soma o saldo de todas as contas ativas do usuário; responde `{ consolidatedBalance }` | RF-012 |
| `PUT` | `/accounts/{id}` | Renomeia uma conta (`{ name }`) — único campo editável | RF-010 |
| `POST` | `/accounts/{id}/archive` | Arquiva uma conta (idempotente); responde `204 No Content` | RF-010, RF-013 |

Todas as rotas de `/accounts` exigem `Authorization: Bearer <token>` — primeira vez que uma rota protegida existe de fato no backend Java (`AuthenticationInterceptor`, ver ADR-0014).

Regras de negócio aplicadas:
- Senha mínima de 8 caracteres (`WeakPasswordException`); e-mail duplicado (mesmo com capitalização diferente) é rejeitado no cadastro (RF-002); login com e-mail inexistente e login com senha incorreta retornam o **mesmo** erro (`InvalidCredentialsException`, HTTP 401); token de acesso (JWT) expira em 15 minutos — todas idênticas ao equivalente da Fase 1 original em TypeScript.
- **RN-001**: o saldo de uma conta só é gravável na criação (saldo inicial); não existe operação de edição direta de saldo. Como a Fase 4 (Transações) ainda não existe, o saldo atual (RF-011) é, nesta fase, sempre igual ao saldo inicial — aplicação literal de RN-001 ao estado atual do sistema, não uma simplificação (ver ADR-0014).
- **RF-010**: apenas o campo `name` é editável após a criação; `type` e `currency` são imutáveis (trocar teria efeito ambíguo sobre RF-011/RF-012 sem uma regra de conversão que não é requisito do vision.md).
- **RF-013**: única forma de remoção de conta nesta fase é o arquivamento (`archive`), que é idempotente. Não existe exclusão física — a distinção "com/sem histórico de transações" do RF-013 não é observável enquanto a Fase 4 não existir.
- **RF-012**: saldo consolidado soma todas as contas ativas em um único total, sem agrupar por moeda — vision.md assume operação em moeda única (BRL) para o MVP; multi-moeda é Pós-MVP.
- **RF-047 (isolamento multi-tenant)**: toda leitura/escrita de conta é escopada por `userId` na própria assinatura do repositório (`findByIdAndUserId`), não apenas por checagem em código de aplicação. Acessar/editar/arquivar uma conta de outro usuário retorna o mesmo erro "não encontrada" (HTTP 404) de uma conta inexistente — postura anti-enumeração consistente com o restante do projeto.

## Limitações conhecidas

- Persistência via H2 embarcado (arquivo local `financepulse-java.mv.db`, ignorado pelo git) — adequada para desenvolvimento/validação desta fase; a escolha de motor de produção segue a mesma dívida técnica já registrada no backend TypeScript (ADR-0003, revisado por ADR-0013).
- Sem refresh token, logout, edição de perfil, recuperação de senha, exclusão de conta ou MFA — fora do escopo das fases já feitas (equivalentes às Fases 2.1–2.5.2 do backend TypeScript), deliberadamente adiadas em favor da Fase 3 por decisão do stakeholder (ver ADR-0014). Sessões não podem ser renovadas nem revogadas até M2.1/M2.4 serem retomadas.
- Sem rate limiting nos endpoints de autenticação ou de contas — mesma limitação já conhecida e aceita no backend TypeScript.
- Corrida de criação concorrente (TOCTOU) não tratada explicitamente em nenhum dos fluxos de escrita — mesma classe de limitação estrutural já registrada no backend TypeScript.
- Nem Maven nem um JDK vieram pré-instalados neste ambiente Windows de desenvolvimento por padrão. Durante a Fase M1 (Java 17), o Maven foi baixado manualmente de `archive.apache.org` e mantido fora do repositório (`backend-java-tools/` no `.gitignore`). A partir de ADR-0015 (Java 25), o build usa um JDK 25 e uma instalação de Maven já presentes no ambiente (`~/.jdks/jdk-25.0.2`, `~/.maven/maven-3.10.0-rc-1` — esta última deixada por uma ferramenta externa de modernização, ver nota abaixo). Qualquer novo ambiente de desenvolvimento precisará de um JDK 25 e Maven próprios até que o setup seja documentado em uma ferramenta de provisionamento.
- **`backend-java/.github/modernize/java-upgrade/`** é o diretório de sessão de uma ferramenta externa de "App Modernization for Java" que tentou, sem coordenação com o processo de governança deste projeto, realizar esta mesma atualização para Java 25 — sua tentativa falhou e não está sob controle de versão deste processo. Não foi removido unilateralmente; decisão sobre mantê-lo, ignorá-lo via `.gitignore` ou removê-lo cabe ao stakeholder.
