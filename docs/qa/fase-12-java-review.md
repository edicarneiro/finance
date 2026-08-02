# QA — Revisão da Fase 12 (Java): Multi-tenancy Hardening e Backoffice

| Campo | Valor |
|---|---|
| Fase | 12 (Java) — RF-047, RF-048, RF-049, RF-050 completos (RF-049/050 como versão mínima manual, ver ADR-0024) |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0024) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-047 (isolamento multi-tenant reforçado, via suíte consolidada), RF-048 (audit log de acesso administrativo), RF-049 (investigação de suporte com RBAC) e RF-050 (suspensão/reativação de conta) do vision.md — RF-049/050 corretamente escopados como "versão mínima manual", conforme vision.md § 16 já autoriza explicitamente.
- [x] Não há violação de regra de negócio ou restrição do vision.md.
- [x] Isolamento multi-tenant (RF-047) verificado com particular atenção nesta fase — `MultiTenantIsolationHardeningTest` cobre exaustivamente todas as áreas de dado em um único teste, além dos testes pontuais já existentes em cada controller.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (439 testes; 32 novos nesta fase, incluindo um teste de regressão para o Achado 1 abaixo).
- [x] Sem degradação de performance evidente.
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`BackofficeControllerTest` e `MultiTenantIsolationHardeningTest` como smoke tests contra a raiz de composição real).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo — nenhum painel administrativo visual foi construído; nenhum endpoint de auto-promoção a operador foi criado (decisão explícita, não uma lacuna).

## Verificação de Execução

```
mvn test → 439 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4), após a correção do Achado 1
```

## Achados Durante a Revisão

**1. `OperatorAuthorization` verificava o papel do operador, mas não se o próprio operador estava suspenso/excluído (severidade: média — falha de autorização real, corrigida nesta revisão)**

`OperatorAuthorization.requireSupportOperator` originalmente verificava apenas `operator.isSupportOperator()`. Um operador cujo próprio acesso tivesse sido revogado (suspenso por outro operador, ou com a conta excluída) — mas que ainda possuísse um JWT válido emitido antes da revogação — continuaria autorizado a executar ações de backoffice sobre **contas de terceiros** (visualizar dados, suspender, reativar) durante a janela de validade do token (15 minutos). Isso é estruturalmente diferente da limitação já aceita de "token stateless sobrevive à própria exclusão/suspensão" (ADR-0010/0023, que afeta apenas a conta do próprio titular do token) — aqui, o raio de ação de um token obsoleto se estende a outras contas, um risco maior. **Resolução**: `OperatorAuthorization.requireSupportOperator` agora verifica também `!operator.isSuspended() && !operator.isDeleted()`, fechando a janela para o estado já persistido no banco (o operador perde acesso de backoffice assim que sua própria suspensão/exclusão é gravada, independentemente da validade residual do JWT para outras finalidades). A limitação de token stateless em si (0 a 15 minutos de atraso entre a revogação e a rejeição) permanece — é a mesma classe já aceita em todo o projeto, e resolvê-la exigiria infraestrutura de revogação de token fora do escopo desta fase. Teste de regressão adicionado (`rejectsAnOperatorWhoseOwnAccountHasBeenSuspended`).

**2. Um operador pode suspender/consultar a própria conta (severidade: baixa — nota de design, não bloqueante)**

Nenhum dos casos de uso de backoffice impede que `operatorUserId == targetUserId`. Um operador pode, por exemplo, suspender a própria conta. Não há requisito que proíba isso, e o efeito é razoável (a checagem do Achado 1 garante que, uma vez suspenso, o operador perde imediatamente a capacidade de continuar agindo). Registrado como nota, não como defeito.

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`OperatorAuthorization.requireSupportOperator`, `AuditAction.VIEWED_USER_DATA`); a decisão de não auditar a própria consulta ao audit log está documentada com um comentário explicando o "porquê" (ruído recursivo), não o "o quê".

**SOLID**: `OperatorAuthorization` é uma classe package-private, não uma porta pública — corretamente escopada como detalhe de implementação compartilhado apenas pelos casos de uso de `usecases/backoffice/`, não vazada para fora do pacote. `GetUserForSupportUseCase` reaproveita `ExportUserDataUseCase` em vez de duplicar a agregação de nove repositórios pela segunda vez — segunda composição de caso de uso deste projeto, mesmo padrão validado em ADR-0022. Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/backoffice/`, `domain/user/` (além do já existente) ou `application/usecases/backoffice/`.

**Testes**: `MultiTenantIsolationHardeningTest` é o teste mais valioso desta fase — cria dois usuários reais via HTTP e verifica, em um único arquivo auditável, que nenhuma das dez áreas de dado do produto vaza entre eles, incluindo um caso de tentativa ativa de escrita (criar uma transação referenciando conta/categoria de outro usuário, HTTP 404). `BackofficeControllerTest.anOperatorCanSuspendAndReactivateAnAccountBlockingAndRestoringLogin` verifica o fluxo de ponta a ponta mais importante para RF-050: suspensão real bloqueia login real, reativação real restaura o login. O teste de regressão do Achado 1 isola especificamente a nova checagem (operador promovido, depois suspenso, tentativa de ação rejeitada).

**Segurança**: `userId`/`operatorUserId` nunca aceitos do corpo da requisição além do que a rota/token já determinam; toda ação de backoffice é escopada pelo `targetUserId` da própria rota. A correção do Achado 1 fecha uma lacuna real de autorização, não apenas teórica — validado com teste de regressão específico.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Operador pode suspender/consultar a própria conta (Achado 2 acima) — nota de design, sem requisito que restrinja.
2. Janela de até 15 minutos entre a revogação do acesso de um operador e a rejeição de um JWT já emitido — mesma classe de limitação estrutural já aceita em todo o projeto (ADR-0010/0023), agora também documentada explicitamente para o caso de operadores.
3. Sem endpoint de promoção a `SUPPORT_OPERATOR` — decisão de escopo explícita (ADR-0024, sancionada por vision.md § 16), não uma lacuna.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real de autorização (operador revogado mantendo acesso de backoffice via token obsoleto) foi identificado e corrigido durante esta revisão, com teste de regressão dedicado — particularmente relevante por afetar contas de terceiros, não apenas a própria conta do token. A suíte `MultiTenantIsolationHardeningTest` entrega exatamente o que RF-047 pede: uma verificação consolidada e auditável de que o isolamento já estrutural do projeto se mantém correto em toda área de dado. Nenhum apontamento crítico adicional foi identificado.
