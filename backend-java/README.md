# FinancePulse Engine — Backend (Java)

Backend Java do [roadmap.md](../roadmap.md), cobrindo:
- **M1** (equivalente à Fase 1 original: **Fundação técnica + Cadastro e Login**) — RF-001, RF-002, RF-003, RF-008.
- **Fase 3** (**Contas e Carteiras Financeiras**, primeira fase construída diretamente em Java, sem equivalente TypeScript anterior) — RF-009 a RF-013.
- **Fase 4.1** (**Transações manuais + fundação mínima de categoria**) — RF-014, RF-015, RF-017, RF-025 (parcial).
- **Fase 5** (**Categorização — CRUD completo e subcategorias**) — RF-023, RF-024 (já satisfeito por RF-015).
- **Fase 6** (**Orçamentos**) — RF-026, RF-027, RF-029 completos; RF-028 agora completo (sinal calculado na Fase 6, entrega efetivada na Fase 10).
- **Fase 7** (**Metas Financeiras**) — RF-030, RF-031 completos; RF-032 agora completo (sinal calculado na Fase 7, entrega efetivada na Fase 10).
- **Fase 8** (**Dashboard e Pulse Score**) — RF-033, RF-035 completos; RF-034 com fórmula provisória (RN-006 declara a fórmula real como pendência de produto/ciência de dados); RF-036 completo (explicabilidade por fator).
- **Fase 9** (**Relatórios**) — RF-037, RF-038 completos; RF-039 completo para exportação em CSV (PDF deliberadamente fora do escopo, ver ADR-0021).
- **Fase 10** (**Notificações**) — RF-040, RF-041, RF-042 completos (entrega represada de RF-028/RF-032 também efetivada aqui); RF-043 fora do escopo (depende de transações recorrentes, RF-016/Fase 4.2, ver ADR-0022).
- **Fase 11** (**Privacidade e Conformidade — LGPD**) — RF-044, RF-045, RF-046 completos. RF-045 exigiu construir o mecanismo mínimo de exclusão/anonimização de conta (equivalente a RF-007, ainda não migrado por si só) — ver ADR-0023.
- **Fase 12** (**Multi-tenancy Hardening e Backoffice**, última fase da Parte 1/MVP do roadmap) — RF-047, RF-048, RF-049, RF-050 completos. RF-049/RF-050 como **versão mínima manual**, decisão já explicitamente sancionada por vision.md § 16 — ver ADR-0024.

> Por decisão do stakeholder, a Fase 3, a Fase 4.1, a Fase 5, a Fase 6, a Fase 7, a Fase 8, a Fase 9, a Fase 10, a Fase 11 e a Fase 12 foram iniciadas **antes** da conclusão de M2.1–M2.5.2 (refresh token, logout, edição de perfil, recuperação de senha, MFA — a exclusão de conta em si, RF-007/RF-045, foi construída na Fase 11 por ser inseparável de RF-045) — ver [ADR-0014](../docs/adr/0014-fase-3-contas-carteiras-java.md), [ADR-0016](../docs/adr/0016-decomposicao-fase-4-e-dependencia-categoria.md), [ADR-0017](../docs/adr/0017-fase-5-categorizacao.md), [ADR-0018](../docs/adr/0018-fase-6-orcamentos.md), [ADR-0019](../docs/adr/0019-fase-7-metas-financeiras.md), [ADR-0020](../docs/adr/0020-fase-8-dashboard-pulse-score.md), [ADR-0021](../docs/adr/0021-fase-9-relatorios.md), [ADR-0022](../docs/adr/0022-fase-10-notificacoes.md), [ADR-0023](../docs/adr/0023-fase-11-privacidade-lgpd.md) e [ADR-0024](../docs/adr/0024-fase-12-multitenancy-hardening-backoffice.md). O backend TypeScript ([backend/](../backend/)) permanece intacto e é a referência funcional para as fases ainda não migradas. `rules.md` § 7 registra que, a partir de 2026-07-31, a aprovação do stakeholder deixou de ser um bloqueio obrigatório entre fases.

## Stack

Java 25, Spring Boot 3.5.4, Maven, Spring Data JPA — **PostgreSQL 16** em desenvolvimento (via Docker Compose, perfil `dev`) e **H2 em memória** exclusivamente na suíte de testes (ADR-0026); `spring-security-crypto` isolado — **não** o starter completo `spring-boot-starter-security` (hash de senha via `BCryptPasswordEncoder`), `jjwt` (token de acesso), JUnit 5 + AssertJ (testes, dublês escritos à mão em vez de Mockito). Decisões e alternativas consideradas: [docs/adr/0013-migracao-java-spring-boot.md](../docs/adr/0013-migracao-java-spring-boot.md) (stack original), [docs/adr/0015-upgrade-java-25.md](../docs/adr/0015-upgrade-java-25.md) (atualização de Java 17 para Java 25) e [docs/adr/0026-containerizacao-dev-postgresql.md](../docs/adr/0026-containerizacao-dev-postgresql.md) (containerização + PostgreSQL).

## Arquitetura

Arquitetura Hexagonal (Ports & Adapters) — mesma regra de dependência do backend TypeScript ([ADR-0002](../docs/adr/0002-arquitetura-hexagonal-backend.md)), com o contêiner do Spring como mecanismo de composição em vez de um composition root manual ([ADR-0013](../docs/adr/0013-migracao-java-spring-boot.md)):

```
src/main/java/com/financepulse/engine/
  domain/
    user/                     Entidade User (id, email, passwordHash, name, createdAt, deletedAt — RF-045/
                               RF-007, anonimização via `anonymize()`, não exclusão física, ver ADR-0023 —,
                               role [RF-049/050, `CUSTOMER`/`SUPPORT_OPERATOR`, ver ADR-0024], suspendedAt
                               [RF-050, bloqueio reversível via `suspend()`/`reactivate()`, distinto da
                               anonimização]), value object Email, PasswordPolicy, enum Role, entidade
                               ConsentRecord (id, userId, version, acceptedAt — RF-046, trilha append-only)
                               — zero anotação/dependência de framework
      errors/                 InvalidEmailException, WeakPasswordException, DuplicateEmailException,
                               InvalidCredentialsException, InvalidConsentVersionException,
                               UserNotFoundException, AccountSuspendedException
    backoffice/                Entidade AuditLogEntry (id, operatorUserId, targetUserId, action, details,
                               createdAt — RF-048, trilha append-only), enum AuditAction (VIEWED_USER_DATA,
                               SUSPENDED_ACCOUNT, REACTIVATED_ACCOUNT)
      errors/                 ForbiddenException (papel insuficiente, HTTP 403)
    account/                  Entidade Account (id, userId, type, name, currency, balance [saldo inicial —
                               ver RN-001 abaixo], archived, createdAt), AccountType (enum), value object
                               Currency (ISO 4217), AccountPolicy
      errors/                 InvalidAccountNameException, InvalidCurrencyException, AccountNotFoundException,
                               ArchivedAccountException
    category/                 Entidade Category (id, userId, name, parentCategoryId, createdAt) — hierarquia
                               limitada a 2 níveis, parentCategoryId imutável após a criação (ver ADR-0017)
      errors/                 InvalidCategoryNameException, CategoryNotFoundException,
                               InvalidCategoryHierarchyException, CategoryHasSubcategoriesException,
                               CategoryHasTransactionsException
    transaction/               Entidade Transaction (id, userId, accountId, categoryId, type, amount, date,
                               description, tags, createdAt), TransactionType (enum), TransactionPolicy
      errors/                 InvalidAmountException, TransactionNotFoundException
    budget/                   Entidade Budget (id, userId, categoryId, limitAmount, periodType,
                               customPeriodStart/End, alertThresholds, createdAt) — categoryId e periodType
                               imutáveis após a criação (ver ADR-0018), BudgetPeriodType (enum), BudgetPolicy
      errors/                 InvalidBudgetLimitException, InvalidAlertThresholdException,
                               InvalidBudgetPeriodException, BudgetNotFoundException
    goal/                     Entidade Goal (id, userId, name, targetAmount, deadline, accountId **ou**
                               categoryId [mutuamente exclusivos e imutáveis], progressAlertThresholds,
                               createdAt), GoalPolicy — ver ADR-0019
      errors/                 InvalidGoalNameException, InvalidGoalTargetException,
                               InvalidGoalDeadlineException, InvalidGoalAssociationException,
                               InvalidGoalThresholdException, GoalNotFoundException
    pulsescore/               Entidade PulseScoreSnapshot (id, userId, scoreDate, overallScore, score por
                               fator [3 opcionais + balanceTrend sempre presente], formulaVersion, createdAt)
                               — no máximo um snapshot por (userId, scoreDate), nunca editável pelo usuário
                               (RN-005, ver ADR-0020)
    report/
      errors/                 InvalidReportPeriodException — único invariante de domínio desta área (sem
                               entidade persistida, ver ADR-0021)
    notification/             Entidade Notification (id, userId, alertType, eventKey [identidade de
                               deduplicação], message, deliveredChannels, read, createdAt), entidade
                               NotificationPreference (id, userId, alertType, channel, enabled), enums
                               AlertType (BUDGET_THRESHOLD, GOAL_THRESHOLD, ATYPICAL_SPENDING — RF-043 fora
                               do escopo, ver ADR-0022) e NotificationChannel (IN_APP, EMAIL)
      errors/                 NotificationNotFoundException
  application/
    ports/                    UserRepository (agora também com update, para anonimização), PasswordHasher,
                               TokenService, IdGenerator, AccountRepository, CategoryRepository,
                               TransactionRepository (agora também com findAllByUserId, usado por
                               agregações), BudgetRepository, GoalRepository, PulseScoreRepository,
                               NotificationRepository, NotificationPreferenceRepository, AlertEmailNotifier
                               (nomeada pela intenção, não "EmailSender" — mesmo padrão de
                               PasswordResetNotifier, ADR-0009), ConsentRepository (append-only — apenas
                               save/findAllByUserId), AuditLogRepository (append-only — apenas
                               save/findAllByTargetUserId, RF-048), Clock (interfaces Java puras — toda
                               porta de dado financeiro escopa leitura/escrita por userId na própria
                               assinatura, reforço estrutural de RF-047)
    services/
      AccountBalanceCalculator.java   Deriva o saldo atual de uma conta (saldo inicial + transações) —
                               função pura, sem I/O; usada por ListAccountsUseCase/GetConsolidatedBalanceUseCase,
                               ListGoalsUseCase (metas por conta) e GetDashboardUseCase (saldo consolidado e
                               tendência de saldo do Pulse Score)
      BudgetPeriodCalculator.java     Deriva o intervalo do período vigente/anterior de um orçamento
                               (RN-004) — função pura; MONTHLY/WEEKLY recorrentes, CUSTOM fixo (ADR-0018)
      BudgetConsumptionCalculator.java   RF-027 (percentual em tempo real) + RF-028 (limiares ultrapassados,
                               sinal) — função pura, considera apenas transações EXPENSE; reaproveitada por
                               GetDashboardUseCase para o fator "consistência orçamentária" do Pulse Score
      GoalProgressCalculator.java     RF-031 (percentual de progresso) + RF-032 (limiares/conclusão, sinal)
                               — função pura; quem calcula currentAmount (saldo de conta ou soma líquida de
                               categoria) é o caso de uso (ListGoalsUseCase), que tem acesso aos repositórios
      PulseScoreCalculator.java       RF-034/RF-036 (ver ADR-0020) — função pura, fórmula **provisória**
                               (`FORMULA_VERSION = "pulse-v0-provisional"`) combinando quatro sinais com
                               pesos iguais: consistência orçamentária, taxa de poupança, diversificação de
                               gastos (índice de Herfindahl-Hirschman invertido) e tendência de saldo (o
                               único sempre presente); os demais são omitidos quando não há dado suficiente
                               no período (sem orçamentos, sem receita, sem despesas)
      SpendingByCategoryCalculator.java   RF-033/RF-037 — função pura; agrupa despesas por categoria e
                               calcula percentual do total. Extraída nesta fase de uma implementação até
                               então inline em GetDashboardUseCase (Fase 8), agora reaproveitada também
                               pelos relatórios (ver ADR-0021) — mesmo cálculo, fontes de transações diferentes
      PeriodComparisonCalculator.java     RF-038 — função pura; compara totais (receita/despesa/líquido) e
                               gastos por categoria (delta, variação percentual) entre dois períodos
                               quaisquer, fornecidos pelo chamador
      ReportPeriod.java               Record com construtor compacto validador — intervalo explícito
                               fornecido pelo cliente (nunca um preset "mês atual" calculado internamente);
                               rejeita `startDate` após `endDate` (`InvalidReportPeriodException`), sem
                               reordenar silenciosamente (ver ADR-0021)
      AtypicalSpendingDetector.java   RF-042 — função pura, fórmula estatística **provisória**: sinaliza uma
                               despesa quando excede a média histórica da categoria em mais de 2
                               desvios-padrão, exigindo amostra mínima de 5 transações (dado insuficiente
                               nunca é atípico por padrão, ver ADR-0022)
      NotificationPreferenceResolver.java   RF-040 — função pura; única fonte da verdade sobre o padrão
                               opt-out (`enabled = true` quando não configurado explicitamente)
    usecases/                 RegisterUserUseCase, AuthenticateUserUseCase — também sem anotação de framework
      account/                CreateAccountUseCase, UpdateAccountUseCase, ArchiveAccountUseCase,
                               ListAccountsUseCase, GetConsolidatedBalanceUseCase (as duas últimas agora também
                               dependem de TransactionRepository — ver RN-001 abaixo)
      category/                ListCategoriesUseCase (com seed automático das categorias padrão, RF-025),
                               CreateCategoryUseCase, UpdateCategoryUseCase, DeleteCategoryUseCase (RF-023)
      transaction/              CreateTransactionUseCase, UpdateTransactionUseCase, DeleteTransactionUseCase,
                               ListTransactionsUseCase (por conta — filtro/busca completo é RF-018, Fase 4.3)
      budget/                   CreateBudgetUseCase, UpdateBudgetUseCase, DeleteBudgetUseCase,
                               ListBudgetsUseCase (RF-027/RF-028, período vigente),
                               GetBudgetHistoryUseCase (RF-029, períodos anteriores recalculados sob demanda)
      goal/                     CreateGoalUseCase, UpdateGoalUseCase, DeleteGoalUseCase,
                               ListGoalsUseCase (RF-031/RF-032, progresso via conta ou categoria)
      dashboard/                GetDashboardUseCase (RF-033/RF-034/RF-036 — agrega saldo, fluxo de caixa,
                               distribuição de gastos e Pulse Score atual; persiste um snapshot diário do
                               Pulse Score a cada chamada, ver ADR-0020), GetPulseScoreHistoryUseCase
                               (RF-035 — lista snapshots já persistidos, sem recálculo)
      report/                   GetSpendingByCategoryReportUseCase (RF-037), GetPeriodComparisonReportUseCase
                               (RF-038), GetTransactionsForPeriodUseCase (RF-039 — dados brutos enriquecidos
                               com nome de conta/categoria, consumido pela exportação CSV)
      notification/             GetNotificationPreferencesUseCase/UpdateNotificationPreferencesUseCase
                               (RF-040), CheckNotificationsUseCase (RF-041/RF-042 e entrega represada de
                               RF-028/RF-032 — reaproveita ListBudgetsUseCase/ListGoalsUseCase em vez de
                               duplicar orquestração, primeiro caso de uso deste projeto a depender de
                               outro caso de uso, ver ADR-0022), ListNotificationsUseCase/
                               MarkNotificationReadUseCase (caixa de entrada in-app)
      user/                     DeleteAccountUseCase (RF-045/RF-007 — anonimização via reautenticação por
                               senha, ver ADR-0023), RecordConsentUseCase/ListConsentHistoryUseCase
                               (RF-046), ExportUserDataUseCase (RF-044 — agrega todas as áreas já existentes
                               do backend Java em um único documento; nunca inclui passwordHash)
      backoffice/               GetUserForSupportUseCase (RF-049 — reaproveita ExportUserDataUseCase,
                               segunda composição de caso de uso deste projeto, ver ADR-0022/0024),
                               SuspendAccountUseCase/ReactivateAccountUseCase (RF-050), GetAuditLogUseCase
                               (RF-048) — todos verificam `Role.SUPPORT_OPERATOR` do operador autenticado
                               (`OperatorAuthorization`, checagem interna ao pacote) e, exceto a própria
                               leitura do log, gravam uma entrada em AuditLogRepository
  adapters/
    in/web/                   AuthController, AccountController, CategoryController, TransactionController,
                               BudgetController, GoalController, DashboardController, ReportController,
                               NotificationController, UserController, PrivacyController, BackofficeController
                               (rotas protegidas /accounts/**, /categories/**, /transactions/**, /budgets/**,
                               /goals/**, /dashboard/**, /reports/**, /notification-preferences/**,
                               /notifications/**, /users/**, /privacy/**, /backoffice/**), CsvWriter
                               (serialização CSV própria, sem dependência externa — RF-039, ver ADR-0021),
                               DTOs, GlobalExceptionHandler, AuthenticatedUserResolver +
                               AuthenticationInterceptor (aplicação de RF-008, ver ADR-0014)
    out/persistence/          UserJpaEntity (agora também com role, suspendedAt)/SpringDataUserJpaRepository/
                               JpaUserRepositoryAdapter, ConsentRecordJpaEntity/
                               SpringDataConsentJpaRepository/JpaConsentRepositoryAdapter,
                               AuditLogEntryJpaEntity/SpringDataAuditLogJpaRepository/
                               JpaAuditLogRepositoryAdapter,
                               AccountJpaEntity/SpringDataAccountJpaRepository/JpaAccountRepositoryAdapter,
                               CategoryJpaEntity/SpringDataCategoryJpaRepository/JpaCategoryRepositoryAdapter,
                               TransactionJpaEntity (tags via @ElementCollection eager — ver Limitações)/
                               SpringDataTransactionJpaRepository/JpaTransactionRepositoryAdapter,
                               BudgetJpaEntity (alertThresholds via @ElementCollection eager)/
                               SpringDataBudgetJpaRepository/JpaBudgetRepositoryAdapter,
                               GoalJpaEntity (progressAlertThresholds via @ElementCollection eager)/
                               SpringDataGoalJpaRepository/JpaGoalRepositoryAdapter,
                               PulseScoreSnapshotJpaEntity (unique constraint em user_id+score_date, upsert
                               no adaptador)/SpringDataPulseScoreJpaRepository/JpaPulseScoreRepositoryAdapter,
                               NotificationJpaEntity (deliveredChannels via @ElementCollection eager, unique
                               constraint em user_id+event_key)/SpringDataNotificationJpaRepository/
                               JpaNotificationRepositoryAdapter, NotificationPreferenceJpaEntity (unique
                               constraint em user_id+alert_type+channel, upsert no adaptador)/
                               SpringDataNotificationPreferenceJpaRepository/
                               JpaNotificationPreferenceRepositoryAdapter
    out/security/             BCryptPasswordHasherAdapter, JwtTokenServiceAdapter, UuidIdGeneratorAdapter
    out/time/                 SystemClock (primeira porta Clock do backend Java, ver ADR-0018)
    out/notification/         ConsoleAlertEmailNotifier — apenas loga o alerta (SLF4J), nenhum provedor de
                               e-mail real integrado ainda (mesmo padrão de ConsolePasswordResetNotifier do
                               backend TypeScript, ver ADR-0009/ADR-0022)
  composition/
    UseCaseConfiguration.java Raiz de composição da camada de aplicação — os casos de uso não têm anotação
                               Spring (regra da Arquitetura Hexagonal), então são instanciados explicitamente
                               aqui via @Bean, em vez de descobertos por @Component
    WebMvcConfig.java         Registra AuthenticationInterceptor para /accounts/**, /transactions/**,
                               /categories/**, /budgets/**, /goals/**, /dashboard/**, /reports/**,
                               /notification-preferences/**, /notifications/**, /users/**, /privacy/**,
                               /backoffice/**
  FinancepulseEngineApplication.java      Classe main (Spring Boot)
```

Regra de dependência: `adapters → application → domain`, nunca o inverso (`rules.md` § 1) — idêntica à do backend TypeScript, apenas o mecanismo de composição muda.

## Como rodar

Guia completo de ambiente local (Docker Compose, incluindo frontend + PostgreSQL): ver [README.md da raiz do repositório](../README.md#como-rodar-o-projeto-localmente) — é o caminho recomendado.

Para rodar só este backend fora de container (requer JDK 25 — confirme `JAVA_HOME` — e um PostgreSQL acessível; o `postgres` do Docker Compose funciona, exposto em `localhost:5433`):

```bash
docker compose -f ../docker-compose.dev.yml up -d postgres   # só o banco
export FINANCEPULSE_JWT_SECRET=troque-em-producao            # opcional em dev, tem default
POSTGRES_PORT=5433 ./mvnw spring-boot:run
```

O perfil `dev` (`application-dev.yml`) é ativado por padrão — sem ele não há ambiente de produção configurado neste projeto ainda. Ao subir com esse perfil, `DevDataSeeder` popula (de forma idempotente) um usuário de teste (`dev@financepulse.local` / `DevPassword1`) e suas categorias padrão — desligável com `FINANCEPULSE_SEED_ENABLED=false`.

## Como testar

```bash
mvn test
```

446 testes (JUnit 5), cobrindo domínio, casos de uso (com dublês em memória, incluindo `FixedClock` para tornar o cálculo de período de orçamento, progresso de meta, Pulse Score e detecção de notificações determinístico), adaptadores contra tecnologia real (H2 real via `@DataJpaTest`, BCrypt real, JWT real) e smoke tests de ponta a ponta contra a raiz de composição real (`AuthControllerTest`, `AccountControllerTest`, `CategoryControllerTest`, `TransactionControllerTest`, `BudgetControllerTest`, `GoalControllerTest`, `DashboardControllerTest`, `ReportControllerTest`, `NotificationControllerTest`, `UserControllerTest`, `PrivacyControllerTest`, `BackofficeControllerTest`, `CorsConfigurationTest`, `DevDataSeederTest` — mesma disciplina do `container.integration.test.ts` do backend TypeScript, `rules.md` § 3), incluindo `MultiTenantIsolationHardeningTest` — suíte consolidada de RF-047 verificando, em um único arquivo, que nenhum dado de um usuário alcança outro, em toda área do produto.

## Endpoints

| Método | Rota | Descrição | Requisito |
|---|---|---|---|
| `POST` | `/auth/register` | Cadastra um novo usuário (`{ email, password }`); responde `201 { userId }` | RF-001, RF-002 |
| `POST` | `/auth/login` | Autentica com e-mail/senha; responde `200 { token }` | RF-003, RF-008 |
| `POST` | `/accounts` | Cria uma conta financeira (`{ type, name, currency, initialBalance }`); responde `201 { accountId }` | RF-009 |
| `GET` | `/accounts` | Lista as contas do usuário autenticado (ativas e arquivadas), cada uma com seu saldo **atual** (saldo inicial + transações) | RF-010, RF-011 |
| `GET` | `/accounts/balance/consolidated` | Soma o saldo atual de todas as contas ativas do usuário; responde `{ consolidatedBalance }` | RF-012 |
| `PUT` | `/accounts/{id}` | Renomeia uma conta (`{ name }`) — único campo editável | RF-010 |
| `POST` | `/accounts/{id}/archive` | Arquiva uma conta (idempotente); responde `204 No Content` | RF-010, RF-013 |
| `GET` | `/categories` | Lista as categorias do usuário (nível superior e subcategorias); semeia automaticamente um conjunto padrão na primeira chamada, se vazio | RF-025 (parcial) |
| `POST` | `/categories` | Cria uma categoria (`{ name, parentCategoryId? }`); responde `201 { categoryId }` | RF-023 |
| `PUT` | `/categories/{id}` | Renomeia uma categoria (`{ name }`) — único campo editável | RF-023 |
| `DELETE` | `/categories/{id}` | Exclui definitivamente uma categoria, se não tiver subcategorias nem transações associadas; responde `204 No Content` | RF-023 |
| `POST` | `/transactions` | Registra uma transação manual (`{ accountId, categoryId, type, amount, date, description, tags }`); responde `201 { transactionId }` | RF-014, RF-017 |
| `GET` | `/transactions?accountId=` | Lista as transações de uma conta do usuário | — (leitura mínima; RF-018 completo é Fase 4.3) |
| `PUT` | `/transactions/{id}` | Substitui todos os campos de uma transação (não é PATCH parcial) | RF-015 |
| `DELETE` | `/transactions/{id}` | Exclui definitivamente uma transação; responde `204 No Content` | RF-015 |
| `POST` | `/budgets` | Cria um orçamento (`{ categoryId, limitAmount, periodType, customPeriodStart?, customPeriodEnd?, alertThresholds? }`); responde `201 { budgetId }` | RF-026 |
| `GET` | `/budgets` | Lista os orçamentos do usuário, cada um com o consumo do período **vigente** (RF-027) e os limiares já ultrapassados (RF-028, sinal — entrega efetivada via `POST /notifications/check`, Fase 10) | RF-026, RF-027, RF-028 |
| `PUT` | `/budgets/{id}` | Atualiza `limitAmount`/`alertThresholds` — categoria e tipo de período são imutáveis | RF-026 |
| `DELETE` | `/budgets/{id}` | Exclui definitivamente um orçamento; responde `204 No Content` | RF-026 |
| `GET` | `/budgets/{id}/history?periods=` | Desempenho dos `periods` períodos anteriores (padrão 6, máximo 24); vazio para orçamentos `CUSTOM` | RF-029 |
| `POST` | `/goals` | Cria uma meta financeira (`{ name, targetAmount, deadline, accountId? ou categoryId?, progressAlertThresholds? }`); responde `201 { goalId }` | RF-030 |
| `GET` | `/goals` | Lista as metas do usuário, cada uma com o progresso calculado (`currentAmount`, `progressPercentage`, `thresholdsCrossed`, `achieved`, `overdue`) | RF-030, RF-031, RF-032 |
| `PUT` | `/goals/{id}` | Atualiza `name`/`targetAmount`/`deadline`/`progressAlertThresholds` — `accountId`/`categoryId` são imutáveis | RF-030 |
| `DELETE` | `/goals/{id}` | Exclui definitivamente uma meta; responde `204 No Content` | RF-030 |
| `GET` | `/dashboard?days=` | Saldo consolidado, fluxo de caixa da janela (`days`, padrão 30, máximo 365), distribuição de gastos por categoria e o Pulse Score atual com detalhamento por fator (`pulseScore.factors`); recalcula e persiste o snapshot diário do Pulse Score a cada chamada (ver ADR-0020) | RF-033, RF-034 (fórmula provisória), RF-036 |
| `GET` | `/dashboard/pulse-score/history?days=` | Evolução histórica do Pulse Score (`days`, padrão 90, máximo 365) a partir dos snapshots já persistidos, sem recálculo | RF-035 |
| `GET` | `/reports/spending-by-category?startDate=&endDate=` | Gastos por categoria no período explícito informado (JSON) | RF-037 |
| `GET` | `/reports/spending-by-category/export?startDate=&endDate=` | O mesmo relatório acima, em CSV (`Content-Disposition: attachment`) | RF-039 (CSV) |
| `GET` | `/reports/period-comparison?periodAStart=&periodAEnd=&periodBStart=&periodBEnd=` | Compara totais (receita/despesa/líquido) e gastos por categoria (delta, variação %) entre dois períodos quaisquer — "mês atual vs. mês anterior" é um exemplo de uso, não um preset calculado pelo backend | RF-038 |
| `GET` | `/reports/transactions/export?startDate=&endDate=` | Dados brutos de transações do período, enriquecidos com nome de conta/categoria, em CSV | RF-039 (CSV) |
| `GET` | `/notification-preferences` | Lista as 3 × 2 combinações (`AlertType` × `NotificationChannel`), mesclando preferências configuradas com o padrão `enabled: true` | RF-040 |
| `PUT` | `/notification-preferences` | Atualiza uma ou mais combinações (`[{ alertType, channel, enabled }]`) — atualização parcial | RF-040 |
| `POST` | `/notifications/check` | Detecta estouros de orçamento, limiares/conclusão de meta e gastos atípicos ainda não notificados; entrega via canais habilitados e responde com as notificações recém-criadas | RF-041, RF-042 (e entrega represada de RF-028/RF-032) |
| `GET` | `/notifications?unreadOnly=` | Caixa de entrada in-app — só notificações entregues via `IN_APP`, mais recentes primeiro | RF-040 |
| `PUT` | `/notifications/{id}/read` | Marca uma notificação como lida (idempotente) | RF-040 |
| `DELETE` | `/users/me` | Exclui (anonimiza) a própria conta — exige `{ password }` como confirmação explícita; responde `204 No Content` | RF-045, RF-007 |
| `GET` | `/privacy/export` | Documento único com todos os dados pessoais e financeiros do usuário (perfil, contas, transações, categorias, orçamentos, metas, histórico de Pulse Score, notificações, preferências, consentimentos) — nunca inclui senha/hash | RF-044 |
| `POST` | `/privacy/consents` | Registra um novo aceite de consentimento (`{ version }`); responde `201 { id, version, acceptedAt }` | RF-046 |
| `GET` | `/privacy/consents` | Histórico completo de consentimentos do usuário, mais recente primeiro | RF-046 |
| `GET` | `/backoffice/users/{id}` | Dados completos do usuário-alvo (mesmo formato de `GET /privacy/export`) — exige papel `SUPPORT_OPERATOR`; registrado em audit log | RF-049, RF-048 |
| `POST` | `/backoffice/users/{id}/suspend` | Suspende a conta do usuário-alvo (`{ reason? }`), reversível — exige `SUPPORT_OPERATOR`; registrado em audit log | RF-050, RF-048 |
| `POST` | `/backoffice/users/{id}/reactivate` | Reverte uma suspensão — exige `SUPPORT_OPERATOR`; registrado em audit log | RF-050, RF-048 |
| `GET` | `/backoffice/users/{id}/audit-log` | Trilha de acessos administrativos ao usuário-alvo, mais recente primeiro — exige `SUPPORT_OPERATOR`; a própria consulta não gera uma nova entrada | RF-048 |

Todas as rotas de `/accounts`, `/categories`, `/transactions`, `/budgets`, `/goals`, `/dashboard`, `/reports`, `/notification-preferences`, `/notifications`, `/users`, `/privacy` e `/backoffice` exigem `Authorization: Bearer <token>` (`AuthenticationInterceptor`, ver ADR-0014) — as rotas `/backoffice/**` exigem adicionalmente o papel `SUPPORT_OPERATOR`, verificado dentro do caso de uso (HTTP 403 caso contrário).

Regras de negócio aplicadas:
- Senha mínima de 8 caracteres (`WeakPasswordException`); e-mail duplicado (mesmo com capitalização diferente) é rejeitado no cadastro (RF-002); login com e-mail inexistente e login com senha incorreta retornam o **mesmo** erro (`InvalidCredentialsException`, HTTP 401); token de acesso (JWT) expira em 15 minutos — todas idênticas ao equivalente da Fase 1 original em TypeScript.
- **RN-001 (estendido nesta fase, ver ADR-0016)**: `Account.balance` armazenado representa exclusivamente o **saldo inicial**. O saldo atual (RF-011) é sempre derivado — `saldoInicial + receitas − despesas` das transações da conta —, nunca um campo mutável. `GET /accounts` e `GET /accounts/balance/consolidated` já refletem transações reais.
- **RF-010**: apenas o campo `name` de uma conta é editável após a criação; `type` e `currency` são imutáveis.
- **RF-013**: única forma de remoção de conta é o arquivamento (`archive`), idempotente. Uma conta arquivada **não aceita novas transações** (`ArchivedAccountException`, HTTP 400) — decisão desta fase (ADR-0016).
- **RF-012**: saldo consolidado soma o saldo atual de todas as contas ativas em um único total, sem agrupar por moeda — vision.md assume operação em moeda única (BRL) para o MVP; multi-moeda é Pós-MVP.
- **RN-002/RF-025**: toda transação exige uma categoria existente do próprio usuário. `GET /categories` semeia automaticamente um conjunto fixo de 8 categorias padrão na primeira consulta de um usuário sem nenhuma (RF-025).
- **RF-023 (categorias e subcategorias)**: hierarquia limitada a 2 níveis — uma subcategoria não pode ter subcategorias (`InvalidCategoryHierarchyException`). Apenas o `name` é editável após a criação; `parentCategoryId` é imutável (mesma decisão de `Account.type`/`Account.currency`, ADR-0014). Exclusão é bloqueada se a categoria tiver subcategorias (`CategoryHasSubcategoriesException`) ou transações associadas (`CategoryHasTransactionsException`) — preserva RN-002, já que apagar uma categoria em uso deixaria transações com referência inválida. Categorias sem uso podem ser excluídas definitivamente (ao contrário de contas, não há "arquivamento" de categoria).
- **RF-024 (recategorização manual)**: já satisfeito desde a Fase 4.1 por `PUT /transactions/{id}` (edição geral já permite trocar `categoryId`, inclusive para uma subcategoria) — nenhum endpoint dedicado foi necessário (ver ADR-0017).
- **RF-022 (categorização automática)**: **não implementado nesta fase** — pelo texto do vision.md é um recurso do fluxo de importação (RF-019–021), que só existe na Fase 4.4. Será implementado junto com ela (ver ADR-0017).
- **RF-014/RF-015**: transação tem valor (sempre positivo — `InvalidAmountException` se ≤ 0; o sinal é implícito pelo campo `type`, INCOME ou EXPENSE), data, conta, categoria, descrição opcional e tags livres (RF-017). Edição substitui todos os campos (não é PATCH parcial). Exclusão é definitiva (ao contrário de contas, transações não têm arquivamento).
- **RF-047 (isolamento multi-tenant)**: toda leitura/escrita de conta, categoria, transação ou orçamento é escopada por `userId` na própria assinatura do repositório, não apenas por checagem em código de aplicação. Acessar/editar/excluir um recurso de outro usuário retorna o mesmo erro "não encontrado" (HTTP 404) de um recurso inexistente — postura anti-enumeração consistente em todo o projeto. Criar uma transação/orçamento referenciando conta/categoria de outro usuário é rejeitado explicitamente no caso de uso, não apenas confiado ao isolamento de leitura.
- **RN-004/RF-026**: um orçamento é sempre associado a uma categoria e um `periodType` (`MONTHLY`, `WEEKLY` ou `CUSTOM`), ambos imutáveis após a criação. `MONTHLY`/`WEEKLY` são recorrentes — o período vigente é sempre calculado a partir da data atual (mês civil / semana ISO segunda a domingo), nunca armazenado. `CUSTOM` é um intervalo de datas fixo, não recorrente — sem "períodos anteriores" (RF-029 retorna lista vazia). Apenas transações **EXPENSE** consomem o orçamento.
- **RF-027/RF-028**: o percentual de consumo é sempre recalculado em tempo real a partir das transações da categoria no período vigente — nenhum valor de consumo é persistido. `alertThresholds` (padrão `[80, 100]`) definem os limiares configuráveis; `GET /budgets` retorna `thresholdsCrossed` com os limiares já ultrapassados (o sinal, ver ADR-0018) — a entrega (e-mail/in-app) agora é efetivada por `POST /notifications/check` (RF-040–042, Fase 10, ver ADR-0022).
- **RF-029**: o histórico de períodos anteriores é recalculado sob demanda a partir de `Transaction.date` — nenhum snapshot de período fechado é persistido, então o histórico nunca diverge da fonte de verdade.
- **Orçamento não agrega subcategorias**: um orçamento em uma categoria de nível superior considera apenas transações lançadas diretamente nela — gastos em uma subcategoria não contam para o orçamento da categoria-pai. Decisão registrada em ADR-0018 (achado na revisão de QA), não uma lacuna silenciosa.
- **RF-030 (criação de meta)**: uma meta tem `name`, `targetAmount` (positivo), `deadline` (estritamente futura em relação a `Clock.today()` — `InvalidGoalDeadlineException` se hoje ou passado) e é associada a **exatamente uma** de `accountId` ou `categoryId`, nunca ambas nem nenhuma (`InvalidGoalAssociationException`) — verificação de posse da conta/categoria referenciada é feita no caso de uso, seguindo o mesmo padrão anti-enumeração de transações/orçamentos. A associação é imutável após a criação; apenas `name`, `targetAmount`, `deadline` e `progressAlertThresholds` são editáveis (`PUT /goals/{id}`).
- **RF-031 (progresso da meta)**: `currentAmount` é sempre derivado, nunca persistido — para metas associadas a conta, reaproveita `AccountBalanceCalculator` (saldo atual da conta); para metas associadas a categoria, soma `receitas − despesas` das transações da categoria lançadas **desde a criação da meta** (`Goal.createdAt`), não desde o início dos tempos. `progressPercentage` é calculado por `GoalProgressCalculator`, mesma função pura usada para `thresholdsCrossed`/`achieved`/`overdue`.
- **RF-032 (alertas de meta)**: `progressAlertThresholds` (padrão `[80, 100]`) segue a mesma validação de `Budget.alertThresholds` — decisão deliberada de duplicar a lógica (`GoalPolicy` não depende de `BudgetPolicy`) em vez de acoplar os agregados `domain.goal` e `domain.budget`, mesmo raciocínio de "regra de três" já aplicado a RefreshToken/PasswordResetToken/MfaChallenge no backend TypeScript (ver ADR-0019). `GET /goals` retorna `thresholdsCrossed` e `achieved` (o sinal, ver ADR-0019) — a entrega agora é efetivada por `POST /notifications/check`, mesmo padrão de RF-028 (ver ADR-0022).
- **RF-033 (painel consolidado)**: `GET /dashboard` deriva saldo consolidado, fluxo de caixa (receita, despesa, líquido) e distribuição de gastos por categoria — todos calculados sob demanda a partir de contas/transações reais na janela solicitada (`days`), nenhum valor persistido para esta parte do painel.
- **RF-034/RN-006 (Pulse Score — fórmula provisória)**: RN-006 declara formalmente que a composição exata do Pulse Score é uma decisão de produto/ciência de dados ainda pendente (vision.md § 17.5) — esta fase implementa uma fórmula transparente e determinística usando os quatro sinais citados em vision.md § 4.8 (consistência orçamentária, taxa de poupança, diversificação de gastos, tendência de saldo), pesos iguais, identificada por `formulaVersion: "pulse-v0-provisional"`. **Isto não substitui a definição formal pendente** — é uma implementação de referência necessária para entregar RF-033–036 nesta fase (ver ADR-0020). Um sinal é omitido do cálculo quando não há dado suficiente no período (sem orçamentos, sem receita, sem despesas); tendência de saldo está sempre presente, então o score final é sempre calculável, mesmo para um usuário novo sem histórico.
- **RN-005 (Pulse Score não editável, recálculo periódico)**: não existe endpoint de escrita para `PulseScoreSnapshot` — o único caminho de escrita é o próprio cálculo, disparado por `GET /dashboard`. No máximo um snapshot é persistido por usuário por dia civil (upsert por `user_id`+`score_date`) — satisfaz "recalculado periodicamente" sem exigir um scheduler dedicado, que não existe nesta fase (ver Limitações e ADR-0020).
- **RF-035 (evolução histórica)**: `GET /dashboard/pulse-score/history` lista os snapshots já persistidos, sem recalcular nada — histórico é estritamente o que já foi gravado por chamadas anteriores a `GET /dashboard`.
- **RF-036 (explicabilidade)**: `GET /dashboard` retorna `pulseScore.factors`, o detalhamento por sinal (nome, score 0–100, peso) que compôs o `overallScore` — a mesma chamada que calcula o score já entrega a explicação, sem endpoint adicional.
- **RF-037 (gastos por categoria em período selecionável)**: `GET /reports/spending-by-category` recebe `startDate`/`endDate` explícitos do cliente — sempre recalculado sob demanda a partir de `Transaction`, reaproveitando `SpendingByCategoryCalculator` (o mesmo cálculo usado pelo dashboard, extraído nesta fase, ver ADR-0021).
- **RF-038 (comparativo entre períodos)**: `GET /reports/period-comparison` recebe dois pares de datas (`periodAStart/End`, `periodBStart/End`) — o backend não assume "mês atual vs. mês anterior" internamente; esse é apenas o exemplo de uso citado no requisito, calculável pelo cliente com quaisquer datas. Retorna totais de cada período e, por categoria, `delta` (B − A) e `percentageChange` (`null` quando o valor no período A é zero — base zero torna a variação percentual indefinida, não calculada como infinito).
- **Validação de período (decisão desta fase, ver ADR-0021)**: `startDate` após `endDate`, em qualquer relatório, é rejeitado explicitamente (`InvalidReportPeriodException`, HTTP 400) via `ReportPeriod` — as datas nunca são reordenadas silenciosamente.
- **RF-039 (exportação — CSV completo, PDF fora do escopo)**: `GET /reports/spending-by-category/export` e `GET /reports/transactions/export` retornam CSV (`Content-Disposition: attachment`), serializado por um `CsvWriter` próprio sem dependência externa. PDF não foi implementado — decisão de escopo explícita, não uma lacuna silenciosa (ver ADR-0021): vision.md cita "CSV/PDF" como exemplo, não como par obrigatório, e adicionar PDF exigiria uma biblioteca nova e decisões de layout não especificadas em nenhum requisito.
- **RF-040 (preferências de notificação)**: 3 tipos de alerta (`BUDGET_THRESHOLD`, `GOAL_THRESHOLD`, `ATYPICAL_SPENDING` — RF-043 fora do escopo, ver abaixo) × 2 canais (`IN_APP`, `EMAIL`). Combinações não configuradas usam o padrão `enabled: true` (opt-out, decisão de produto registrada em ADR-0022) — `GET /notification-preferences` sempre retorna as 6 combinações completas, mesclando o que foi persistido com o padrão, sem escrever nada até o usuário de fato alterar uma preferência.
- **RF-041/RF-042/entrega represada de RF-028 e RF-032**: `POST /notifications/check` detecta, para o usuário autenticado: limiares de orçamento cruzados (reaproveitando `ListBudgetsUseCase`), limiares/conclusão de meta (reaproveitando `ListGoalsUseCase`) e despesas atípicas (via `AtypicalSpendingDetector`, janela de 30 dias). Cada evento tem uma `eventKey` determinística — chamar `/notifications/check` repetidamente nunca duplica uma notificação já criada; orçamentos recorrentes voltam a poder notificar a cada novo período (`periodStart` faz parte da chave). Uma notificação é sempre persistida (mesmo com ambos os canais desabilitados, para fins de deduplicação), mas só aparece em `GET /notifications` se `IN_APP` estava habilitado no momento da detecção. Falha ao enviar e-mail é capturada e logada — não interrompe a checagem dos demais eventos (vision.md § 6.5, serviço de notificação não-crítico).
- **RF-042 (fórmula estatística provisória)**: RF-042 pede "desvio estatístico do padrão histórico", sem especificar o método — `AtypicalSpendingDetector` usa média + 2 desvios-padrão das despesas anteriores na mesma categoria, com amostra mínima de 5 transações (dado insuficiente nunca é atípico por padrão). Mesmo espírito de transparência do `PulseScoreCalculator` (ADR-0020): constantes nomeadas, documentadas, ajustáveis sem redesenho.
- **Sem provedor de e-mail real (mesma dívida técnica de ADR-0009)**: `ConsoleAlertEmailNotifier` apenas loga o alerta (SLF4J) — nenhum SMTP/SES/SendGrid integrado a este projeto ainda, em nenhuma fase. Porta `AlertEmailNotifier` isola essa troca futura sem custo de retrabalho no domínio/aplicação (ver ADR-0022).
- **RF-043 fora do escopo**: "lembrete de transação recorrente prevista e ainda não confirmada" pressupõe transações recorrentes (RF-016), que não existem no domínio ainda (Fase 4.2, não construída) — nem mesmo um tipo de alerta "vazio" foi adicionado a `AlertType`, para não anunciar uma funcionalidade inexistente. Será implementado junto da Fase 4.2 (ver ADR-0022, mesma resolução já aplicada a RF-022/ADR-0017).
- **RF-045/RF-007 (exclusão de conta — anonimização, não exclusão física)**: `DELETE /users/me` exige `{ password }` como confirmação explícita (reautenticação). `User` ganha `anonymize()`: `email` é substituído por um valor sintético único (`deleted-{id}@anonymized.financepulse.internal`), `name` é limpo, `passwordHash` é substituído por um hash inutilizável, `deletedAt` é registrado — a linha permanece no banco (id preservado). `AuthenticateUserUseCase` passa a verificar `isDeleted()` (defesa em profundidade). Reautenticação incorreta e conta já excluída retornam o mesmo erro (`InvalidCredentialsException`, HTTP 401) — mesma postura anti-enumeração do login.
- **RF-045 — dados financeiros retidos, apenas `User` anonimizado (posição provisória, ver ADR-0023)**: `Account`, `Transaction`, `Category`, `Budget`, `Goal`, `Notification`, `NotificationPreference`, `PulseScoreSnapshot` e `ConsentRecord` **não são apagados nem anonimizados** na exclusão de conta — permanecem vinculados ao mesmo `userId`, agora sem identificação pessoal associada. RN-008/vision.md § 17.2 (dúvida #7) já declaram formalmente que a política de retenção pós-exclusão é uma pendência jurídica não resolvida; esta é uma posição conservadora e reversível (nada é apagado fisicamente), não a palavra final sobre retenção de dados.
- **RF-046 (registro de consentimento)**: `ConsentRecord(id, userId, version, acceptedAt)` é uma trilha append-only — nunca atualizada ou apagada, inclusive após a exclusão de conta (preserva a evidência de conformidade passada). `version` é fornecida pelo cliente; o conteúdo jurídico da política em si não é modelado nem armazenado pelo sistema.
- **RF-044 (exportação de dados pessoais — JSON completo)**: `GET /privacy/export` agrega perfil, contas, transações, categorias, orçamentos, metas, histórico de Pulse Score, notificações, preferências de notificação e histórico de consentimento em um único documento — tudo que o backend Java já armazena para o usuário. `passwordHash` nunca é incluído. Apenas JSON — vision.md cita "ex.: JSON/CSV" como exemplo, não par obrigatório (mesmo raciocínio de RF-039/ADR-0021).
- **RF-047 (isolamento multi-tenant reforçado)**: nenhum código funcional novo — todo repositório já escopa leitura/escrita por `userId` desde a Fase 3. Esta fase entrega `MultiTenantIsolationHardeningTest`, uma suíte consolidada que verifica, para cada área de dado do produto (contas, transações, categorias, orçamentos, metas, dashboard, relatórios, notificações, privacidade), que um usuário nunca alcança dados de outro — complementa (não substitui) os testes de isolamento pontuais já existentes em cada controller (ver ADR-0024).
- **RF-048 (audit log de acesso administrativo)**: `AuditLogEntry(id, operatorUserId, targetUserId, action, details, createdAt)` — trilha append-only, escopada especificamente a acesso **administrativo/backoffice** (não ao autoacesso do usuário, já coberto por RF-047). Toda chamada a `GetUserForSupportUseCase`/`SuspendAccountUseCase`/`ReactivateAccountUseCase` grava uma entrada; **consultar o próprio audit log não gera uma nova entrada nele mesmo** (evita ruído recursivo).
- **RF-049/RF-050 (RBAC mínimo, versão manual — vision.md § 16 sanciona explicitamente)**: `User` ganha `role` (`CUSTOMER`/`SUPPORT_OPERATOR`, padrão `CUSTOMER`). **Não existe endpoint para promover um usuário a `SUPPORT_OPERATOR`** — a promoção é manual/fora de banda (alteração direta no banco), a "versão mínima manual" que vision.md § 16 já autoriza para estas duas RFs. `GetUserForSupportUseCase` (RF-049) reaproveita `ExportUserDataUseCase` (Fase 11) em vez de duplicar a agregação — segunda composição de caso de uso deste projeto (a primeira foi `CheckNotificationsUseCase`, ADR-0022).
- **RF-050 (suspensão — reversível, distinta de RF-045)**: `User` ganha `suspendedAt`, `suspend()`/`reactivate()` — bloqueio de acesso puro, não altera `email`/`name`/`passwordHash` (ao contrário da anonimização de RF-045, que é deliberadamente irreversível). `AuthenticateUserUseCase` retorna `AccountSuspendedException` (HTTP 403) para uma conta suspensa — **distinta** de `InvalidCredentialsException` (401) usada para conta excluída: uma conta suspensa continua existindo e um retorno acionável é intencional aqui, ao contrário da postura anti-enumeração usada para exclusão. A senha é sempre validada **antes** da checagem de suspensão, para que ninguém descubra o estado de uma conta sem antes provar que conhece a senha.

## Limitações conhecidas

- **Persistência de desenvolvimento é PostgreSQL 16 via Docker Compose desde ADR-0026** (H2 permanece exclusivo da suíte de testes, por velocidade/determinismo — ver ADR-0026 "Alternativas Consideradas"). Nenhuma ferramenta de migração versionada (Flyway/Liquibase) — schema gerenciado por `spring.jpa.hibernate.ddl-auto=update`, registrado como dívida técnica explícita na mesma ADR: adotar migração versionada exigiria reconstruir o histórico de schema de 13 fases já implementadas, tratado como decisão própria e futura.
- **Sem hot-reload do backend em container** — alterações exigem `docker compose -f docker-compose.dev.yml up --build`; para iteração rápida, rode o backend fora do container (`./mvnw spring-boot:run`) contra o `postgres` do compose (ver "Como rodar" acima).
- Sem refresh token, logout, edição de perfil, recuperação de senha ou MFA — fora do escopo das fases já feitas (equivalentes às Fases 2.1, 2.2 [exceto consentimento], 2.3 e 2.5 do backend TypeScript), deliberadamente adiadas por decisão do stakeholder (ver ADR-0014). A exclusão de conta (RF-007/RF-045) **foi construída na Fase 11** por ser inseparável de RF-045 (ver ADR-0023) — o restante de "gestão de conta" continua adiado. Sessões não podem ser renovadas nem revogadas até M2.1 ser retomado.
- Sem rate limiting em nenhum endpoint — mesma limitação já conhecida e aceita no backend TypeScript.
- Corrida de criação/edição concorrente (TOCTOU) não tratada explicitamente em nenhum fluxo de escrita (contas, categorias ou transações) — sem controle de concorrência otimista (`@Version`); mesma classe de limitação estrutural já registrada no backend TypeScript e no `docs/qa/fase-03-java-review.md`.
- **Sem filtro/busca de transações** (RF-018) — `GET /transactions` só lista por `accountId`; filtrar por categoria, período, valor ou tags fica para a Fase 4.3.
- **Sem transações recorrentes** (RF-016, Fase 4.2) nem **importação CSV/OFX** (RF-019 a RF-021, Fase 4.4) — ver decomposição em [ADR-0016](../docs/adr/0016-decomposicao-fase-4-e-dependencia-categoria.md).
- **Sem categorização automática** (RF-022) — adiada para acompanhar a Fase 4.4, único gatilho real do requisito (ver ADR-0017).
- **Sem re-parentar categorias** — mover uma subcategoria para outro pai, ou promovê-la a categoria de nível superior, não é suportado; `parentCategoryId` é imutável após a criação.
- **Sem re-tipar orçamento** — trocar a categoria ou o `periodType` de um orçamento já criado não é suportado; `categoryId`/`periodType` são imutáveis.
- **Orçamentos `CUSTOM` não têm histórico** (RF-029) — `GET /budgets/{id}/history` retorna lista vazia para eles, por definição (intervalo único, não recorrente).
- **Sem re-tipar meta** — trocar a conta/categoria associada a uma meta já criada não é suportado; `accountId`/`categoryId` são imutáveis. Trocar de conta para categoria (ou vice-versa) exige excluir e recriar a meta.
- **Progresso de meta por categoria conta desde a criação da meta, não desde sempre** — se o usuário já tinha transações na categoria antes de criar a meta, elas não entram no `currentAmount`; apenas transações lançadas a partir de `Goal.createdAt` contam. `Goal.createdAt` é derivado da mesma referência de `Clock` usada para validar o prazo (não de `Instant.now()`), e comparado a `LocalDate` sempre em UTC — corrigido nesta fase após uma regressão real detectada ao rodar a suite completa (ver `docs/qa/fase-08-java-review.md`).
- **Fórmula do Pulse Score é provisória** (`formulaVersion: "pulse-v0-provisional"`) — RN-006/vision.md § 17.5 declaram que a composição exata é uma decisão de produto/ciência de dados ainda pendente; esta fórmula é uma implementação de referência transparente, não a definição final (ver ADR-0020).
- **Histórico de Pulse Score tem lacunas** — não há scheduler/cron nesta fase; um snapshot só é gravado nos dias em que `GET /dashboard` é chamado. Um usuário que não abre o dashboard por vários dias não terá pontos de histórico nesse intervalo (ver ADR-0020).
- **Pulse Score não considera progresso de metas** — apenas os quatro sinais citados em vision.md § 4.8 (consistência orçamentária, taxa de poupança, diversificação de gastos, tendência de saldo) compõem o score; metas (Fase 7) não são um sinal do cálculo.
- **RF-039 sem exportação em PDF** — apenas CSV foi implementado; PDF exigiria uma dependência nova (nenhuma biblioteca de geração de PDF existe no projeto) e decisões de layout não especificadas em nenhum requisito (ver ADR-0021).
- **Sem exportação CSV do comparativo de períodos** — apenas gastos por categoria e transações brutas têm rota `/export`; o comparativo (RF-038) tem apenas JSON nesta fase. Pode ser adicionado sob demanda, mesma forma dos exports já existentes (ver ADR-0021).
- **CSV de relatórios não tem cabeçalho `BOM` UTF-8** — leitores mais antigos do Excel podem interpretar acentos incorretamente ao abrir o arquivo diretamente por duplo clique (abrir via importação de texto com codificação UTF-8 explícita contorna o problema); aceitável para o MVP, revisitar se houver relato de usuário.
- **Sem provedor de e-mail real** — `ConsoleAlertEmailNotifier` apenas loga (SLF4J); nenhum SMTP/SES/SendGrid integrado. Deve ser substituído antes de qualquer implantação de produção — mesma dívida técnica já registrada para `ConsolePasswordResetNotifier` (ADR-0009), agora também aplicável a `AlertEmailNotifier` (ver ADR-0022).
- **Detecção de notificações sem scheduler dedicado** — `POST /notifications/check` só roda quando chamado; não há job periódico ativo nesta fase. Um usuário que nunca chama esse endpoint nunca recebe notificações, mesmo que eventos reais (orçamento estourado, meta atingida) tenham ocorrido. Mesma limitação já aceita para o Pulse Score (ADR-0020), agora também aplicada às notificações (ver ADR-0022).
- **RF-043 fora do escopo** — lembrete de transação recorrente depende de RF-016 (Fase 4.2), que não existe no domínio ainda; nem um tipo de alerta "vazio" foi anunciado em `AlertType` para evitar apresentar uma funcionalidade inexistente (ver ADR-0022).
- **Detecção de gasto atípico limitada a uma janela de 30 dias** — transações mais antigas que não foram avaliadas em nenhuma chamada anterior a `/notifications/check` nunca são retroativamente sinalizadas como atípicas além dessa janela.
- **RF-045 — dados financeiros não são apagados nem anonimizados na exclusão de conta** — apenas `User` é anonimizado; `Account`/`Transaction`/`Category`/`Budget`/`Goal`/etc. permanecem intactos, vinculados ao mesmo `userId`. Posição provisória e conservadora, pendente da validação jurídica já formalmente registrada em RN-008/vision.md § 17.2 (dúvida #7) — pode exigir revisão quando essa pendência for resolvida (ver ADR-0023).
- **Tokens de acesso (JWT) emitidos antes da exclusão de conta continuam válidos até a expiração natural** (15 minutos) — sem lista de revogação de access tokens (o backend Java, ao contrário do TypeScript, nem tem a camada de sessão/refresh token da Fase 2.1 para mitigar). `GET`s autenticados com um token antigo nesse intervalo veriam o perfil já anonimizado, não dados de outro usuário — sem risco de confidencialidade, mas a exclusão não é instantaneamente refletida em chamadas já autenticadas (ver ADR-0023, mesma limitação de ADR-0010).
- **Campos de texto livre não são anonimizados na exclusão de conta** — `Transaction.description`, `Goal.name` etc. podem conter informação pessoal digitada pelo usuário; apenas os campos estruturais de `User` (email, nome, senha) são anonimizados. Nenhum requisito exige mais do que isso hoje; registrado como limitação, não uma lacuna silenciosa (ver ADR-0023).
- **Sem exportação em CSV para RF-044** — apenas JSON; mesmo raciocínio de RF-039 (ADR-0021), aplicado agora a ADR-0023.
- **Sem endpoint para promover um usuário a `SUPPORT_OPERATOR`** — a promoção é manual/fora de banda (alteração direta no banco), exatamente a "versão mínima manual" que vision.md § 16 autoriza explicitamente para RF-049/050. Um fluxo de provisionamento adequado (com sua própria trilha de auditoria de "quem promoveu quem") fica para quando houver decisão de produto sobre governança de operadores (ver ADR-0024).
- **Backoffice sem painel administrativo** — apenas endpoints REST mínimos (`GET/POST /backoffice/users/{id}/...`); nenhuma interface visual foi construída (Fase 13 é Frontend, ainda não iniciada, e mesmo assim não há indicação de que cobriria backoffice).
- **Consultar o próprio audit log não é auditado** — decisão deliberada para evitar ruído recursivo; o log em si já é a trilha (ver ADR-0024).
- **Um operador com o próprio acesso revogado (suspenso/excluído) ainda pode agir com um JWT emitido antes da revogação, até a expiração natural (15 minutos)** — `OperatorAuthorization` verifica `isSuspended()`/`isDeleted()` a cada chamada (corrigido durante a revisão de QA desta fase), fechando a janela para o estado já persistido no banco; o que permanece em aberto é a mesma limitação estrutural de token stateless já aceita em todo o projeto (ADR-0010/0023) — aqui com um risco maior, por afetar contas de terceiros, não apenas a própria conta do operador.
- `TransactionJpaEntity.tags` usa fetch **eager** (`@ElementCollection(fetch = FetchType.EAGER)`) em vez do padrão lazy do JPA — necessário porque o adaptador mapeia a entidade para o domínio fora do escopo de uma sessão Hibernate ativa (mesmo padrão de todos os outros adaptadores deste projeto, que não usam `@Transactional` em métodos de leitura). Aceitável para uma coleção pequena (poucas tags por transação); reavaliar se o volume de tags crescer.
- Nem Maven nem um JDK vieram pré-instalados neste ambiente Windows de desenvolvimento por padrão. Durante a Fase M1 (Java 17), o Maven foi baixado manualmente de `archive.apache.org` e mantido fora do repositório (`backend-java-tools/` no `.gitignore`). A partir de ADR-0015 (Java 25), o build usa um JDK 25 e uma instalação de Maven já presentes no ambiente (`~/.jdks/jdk-25.0.2`, `~/.maven/maven-3.10.0-rc-1` — esta última deixada por uma ferramenta externa de modernização, ver nota abaixo). Qualquer novo ambiente de desenvolvimento precisará de um JDK 25 e Maven próprios até que o setup seja documentado em uma ferramenta de provisionamento.
- **`backend-java/.github/modernize/java-upgrade/`** é o diretório de sessão de uma ferramenta externa de "App Modernization for Java" que tentou, sem coordenação com o processo de governança deste projeto, realizar esta mesma atualização para Java 25 — sua tentativa falhou e não está sob controle de versão deste processo. Não foi removido unilateralmente; decisão sobre mantê-lo, ignorá-lo via `.gitignore` ou removê-lo cabe ao stakeholder.
