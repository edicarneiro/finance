# ADR-0024: Fase 12 (Java) — Multi-tenancy Hardening e Backoffice (versão mínima manual)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-08-01 |
| Autor | CTO / Principal Software Architect |
| Fase | 12 (Java) — RF-047, RF-048, RF-049, RF-050 completos (RF-049/050 como versão mínima manual, conforme vision.md § 16) |

## Contexto

Fase 12 cobre RF-047 (isolamento multi-tenant reforçado), RF-048 (audit log de todo acesso administrativo/backoffice), RF-049 (interface interna de suporte com RBAC) e RF-050 (suspensão de conta por operador autorizado).

**vision.md já resolve o principal risco de escopo desta fase, explicitamente:** a Seção 16 ("Escopo do MVP") lista RF-047/RF-048 como MVP pleno, mas classifica RF-049/RF-050 à parte: *"Backoffice administrativo avançado (RF-049, RF-050 podem ser versão mínima manual no MVP)"*. Isto não é uma pendência a resolver — é uma instrução de escopo explícita: construir uma versão mínima e manual, não um painel administrativo completo.

## Decisão

### 1. RF-047 — reforço via suíte de testes de isolamento consolidada, não novo código funcional

- Toda porta de repositório do backend Java já escopa leitura/escrita por `userId` na própria assinatura desde a Fase 3 (reforço estrutural, não checagem incidental) — verificado nas 11 fases anteriores, cada uma com seu próprio teste de isolamento pontual (`aUserCannotAccessAnotherUsers...`, presente em todo controller de recurso).
- **Não há bug conhecido a corrigir.** O que esta fase entrega para RF-047 é um `MultiTenantIsolationHardeningTest` consolidado — uma suíte única que, para cada área de dado do produto (contas, transações, categorias, orçamentos, metas, notificações, preferências de notificação, consentimento, dashboard, relatórios), cria dois usuários reais via HTTP e verifica exaustivamente que um nunca alcança dados do outro. Isto não substitui os testes pontuais já existentes (mantidos) — é uma rede de segurança explícita e auditável, unificada em um único arquivo, que pegaria uma regressão futura que toque múltiplos recursos de uma vez.

### 2. RF-048 — audit log escopado a acesso administrativo/backoffice, não a autoacesso do usuário

- Novo `AuditLogEntry(id, operatorUserId, targetUserId, action, details, createdAt)` — trilha append-only (mesmo padrão de `ConsentRecord`/`Notification`), nunca atualizada ou apagada.
- **RF-048 fala especificamente em acesso "administrativo/backoffice"** — não em autoacesso do usuário aos próprios dados (já coberto por RF-047/multi-tenancy, sem necessidade de log adicional). Toda ação de um operador (`GetUserForSupportUseCase`, `SuspendAccountUseCase`, `ReactivateAccountUseCase`) grava uma entrada. **Consultar o próprio audit log (`GET /backoffice/users/{id}/audit-log`) não gera uma nova entrada** — decisão deliberada para evitar ruído recursivo (auditar o acesso ao log de auditoria indefinidamente); o próprio log já é a trilha, não precisa auditar quem o lê.

### 3. RF-049/RF-050 — RBAC mínimo via campo `Role` em `User`, sem provisionamento self-service de operador

- `User` ganha `role: Role` (`CUSTOMER` | `SUPPORT_OPERATOR`, padrão `CUSTOMER` em `register()`). **Não existe endpoint para promover um usuário a `SUPPORT_OPERATOR`** — a promoção é manual/fora de banda (alteração direta no banco por alguém com acesso operacional), exatamente a "versão mínima manual" que vision.md § 16 autoriza explicitamente. Construir um fluxo de provisionamento de operadores agora seria escopo especulativo além do que RF-049/050 pedem — e introduziria uma superfície de risco nova (quem pode criar operadores?) sem uma decisão de produto correspondente.
- Toda ação de backoffice verifica o papel do usuário autenticado (`UserRepository.findById(operatorUserId)`, `role == SUPPORT_OPERATOR`) dentro do próprio caso de uso — `ForbiddenException` (HTTP 403) se não for operador. Nenhuma mudança em `TokenService`/`AuthenticationInterceptor` (JWT continua sem claim de papel) — a checagem de papel é uma responsabilidade da camada de aplicação, não da autenticação.
- `GetUserForSupportUseCase` (RF-049) reaproveita `ExportUserDataUseCase` (Fase 11) — segunda ocorrência do padrão "caso de uso compõe caso de uso" já validado em ADR-0022 (`CheckNotificationsUseCase`). Um operador vê exatamente os mesmos dados que o próprio usuário veria em `GET /privacy/export`, o suficiente para investigar uma inconsistência reportada (UC-009).
- `SuspendAccountUseCase`/`ReactivateAccountUseCase` (RF-050): `User` ganha `suspendedAt: Instant?`, `suspend()`/`reactivate()` — **reversível**, ao contrário da anonimização de RF-045 (que é deliberadamente irreversível/conservadora). Suspensão não altera `email`/`name`/`passwordHash`; é um bloqueio de acesso puro.
- **`AccountSuspendedException` (HTTP 403), distinta de `InvalidCredentialsException` (HTTP 401)**: ao contrário de uma conta excluída (ADR-0023, que reaproveita deliberadamente o erro genérico de credenciais para não confirmar a existência de uma conta encerrada), uma conta suspensa continua existindo e pertence a um usuário ativo que pode legitimamente precisar de um retorno acionável ("sua conta foi suspensa"), não um erro genérico que pareça uma senha errada. É uma assimetria deliberada entre os dois estados, não uma inconsistência — exclusão e suspensão têm posturas de UX/segurança diferentes por design.

### 4. Isolamento multi-tenant dentro do próprio backoffice

Mesmo padrão de todo o projeto: toda ação de backoffice é escopada pelo `targetUserId` da rota — nenhum operador pode ver ou agir sobre dados sem especificar exatamente qual usuário, e toda ação é atribuída a um `operatorUserId` real (o autenticado), nunca implícito.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Construir um painel de backoffice completo (múltiplos papéis, permissões granulares, fluxo de convite de operadores) | vision.md § 16 explicitamente autoriza uma versão mínima manual para RF-049/050 — construir mais que isso agora seria escopo especulativo sem requisito correspondente. |
| Codificar o papel (`role`) como claim no JWT, verificado em `AuthenticationInterceptor` | Exigiria reabrir `TokenService`/ADR-0005 (Fase 1, já aprovado e estável) para um ganho pequeno — a checagem de papel via `UserRepository.findById` dentro do caso de uso é suficiente para o volume de chamadas de backoffice esperado, e mantém a autenticação (quem é você) e a autorização (o que você pode fazer) como preocupações separadas. |
| Reaproveitar `InvalidCredentialsException` também para conta suspensa (mesma escolha de conta excluída) | Uma conta suspensa continua existindo e pertence a um usuário legítimo — negar-lhe um retorno acionável ("sua conta foi suspensa") em favor de uma mensagem genérica de "credenciais inválidas" seria pior UX sem ganho de segurança correspondente (o usuário já sabe que sua própria senha está correta). |
| Auditar também o acesso ao próprio audit log | Ruído recursivo sem valor de produto correspondente — o log em si já é a trilha de auditoria; RF-048 não exige meta-auditoria. |
| Construir um endpoint de auto-promoção ou convite para `SUPPORT_OPERATOR` | Introduziria uma superfície de risco nova (escalação de privilégio) sem uma decisão de produto sobre quem pode conceder esse papel — fora do escopo de RF-049/050 conforme escritos, e vision.md já sanciona a alternativa manual. |

## Consequências

- `roadmap.md` registra RF-049/RF-050 como "versão mínima manual", não como um backoffice completo — consistente com a própria linguagem de vision.md § 16.
- Promover um usuário a `SUPPORT_OPERATOR` é uma operação manual/fora de banda nesta fase — documentado como limitação conhecida, não uma lacuna silenciosa. Um fluxo de provisionamento adequado (com sua própria trilha de auditoria de "quem promoveu quem") fica para quando houver decisão de produto sobre governança de operadores.
- `AuthenticateUserUseCase` agora verifica três condições de bloqueio (`isDeleted()`, `isSuspended()`, senha) — mantém a mesma disciplina anti-enumeração para credenciais desconhecidas/incorretas, com uma exceção deliberada e documentada para o caso de suspensão.
