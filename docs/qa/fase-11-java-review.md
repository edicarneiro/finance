# QA — Revisão da Fase 11 (Java): Privacidade e Conformidade (LGPD)

| Campo | Valor |
|---|---|
| Fase | 11 (Java) — RF-044, RF-045, RF-046 completos (ver ADR-0023) |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0023) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-044 (exportação completa de dados pessoais/financeiros), RF-045 (exclusão de conta por anonimização, respeitando a pendência jurídica de retenção já registrada em RN-008) e RF-046 (registro de consentimento append-only) do vision.md.
- [x] Não há violação de regra de negócio ou restrição do vision.md — a decisão de reter dados financeiros na exclusão de conta está explicitamente registrada como posição provisória (ADR-0023), não como uma interpretação silenciosa da pendência de RN-008.
- [x] Isolamento multi-tenant (RF-047) verificado — exportação, exclusão e consentimento operam exclusivamente sobre o `userId` autenticado, nunca um valor do corpo da requisição. Testado explicitamente (`PrivacyControllerTest.aUserNeverSeesAnotherUsersConsentHistoryOrExportedData`).
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (407 testes; 37 novos nesta fase, incluindo um teste de regressão para o Achado 1 abaixo).
- [x] Sem degradação de performance evidente — `ExportUserDataUseCase` faz uma consulta por área de dado (mesmo padrão já aceito em `GetDashboardUseCase`).
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`UserControllerTest`/`PrivacyControllerTest` como smoke tests contra a raiz de composição real, incluindo um fluxo real de exclusão de conta seguido de tentativa de login).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo — nenhuma exportação em CSV foi adicionada (mesmo raciocínio de RF-039); nenhum campo de "preferências" especulativo foi adicionado a `User`; RF-006 (edição de perfil) e demais itens de "gestão de conta" continuam fora do escopo desta fase.

## Verificação de Execução

```
mvn test → 407 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4), após a correção do Achado 1
```

## Achados Durante a Revisão

**1. Exportação de dados (RF-044) descartava o detalhamento por fator do Pulse Score, mesmo ele existindo no domínio (severidade: média — RF-044 promete "todos" os dados, corrigido nesta revisão)**

`ExportUserDataUseCase.toPulseScore` inicialmente mapeava apenas `scoreDate`, `overallScore` e `formulaVersion` de cada `PulseScoreSnapshot`, descartando silenciosamente `budgetConsistencyScore`, `savingsRateScore`, `spendingDiversificationScore` e `balanceTrendScore` — campos que existem no agregado de domínio (persistidos desde a Fase 8) e que compõem a explicabilidade do índice (RF-036). Para um requisito cujo texto literal é "exportar **todos** os seus dados pessoais e financeiros", omitir um detalhamento já armazenado é uma lacuna real de completude, não uma decisão de escopo. **Resolução**: `PulseScoreExport` (e o DTO `UserDataExportResponse.PulseScoreItem` correspondente) agora inclui os quatro campos de fator, com os três opcionais (`budgetConsistencyScore`, `savingsRateScore`, `spendingDiversificationScore`) podendo ser `null` quando o fator não foi calculável no momento do snapshot (mesma semântica já usada em `GET /dashboard`). Teste de regressão adicionado em `ExportUserDataUseCaseTest.aggregatesDataFromEveryAreaOfTheProduct`, asserindo os valores de cada fator explicitamente. Corrigido e verificado nesta revisão.

**2. Corrida de exclusão de conta concorrente não introduz um estado inconsistente, apenas trabalho duplicado (severidade: baixa — nota de robustez, não bloqueante)**

Duas chamadas concorrentes a `DELETE /users/me` com a senha correta poderiam, em teoria, ambas ler o `passwordHash` original antes que a primeira gravasse a anonimização (sem `@Version`/controle de concorrência otimista). O pior caso é a segunda chamada também "passar" na reautenticação e sobrescrever a anonimização da primeira com um segundo e-mail/hash sintéticos — o estado final ainda é "conta anonimizada e inacessível", não uma inconsistência real (não há como a segunda chamada reverter a exclusão nem vazar dados). Mesma classe de risco TOCTOU já aceita e documentada em todo o projeto desde a Fase 3 (`docs/qa/fase-03-java-review.md`), não uma regressão nova desta fase.

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`anonymize`, `ANONYMIZED_EMAIL_DOMAIN`, `unusablePasswordHash`); a decisão de reter dados financeiros e adiar a resolução jurídica de RN-008 está documentada tanto em Javadoc (`User.anonymize`) quanto em ADR-0023, não escondida atrás de código silencioso.

**SOLID**: `ConsentRecord` e `User.anonymize` são transformações de domínio puras, sem I/O; `ExportUserDataUseCase` depende de nove portas de repositório mas cada uma é usada exatamente uma vez, sem lógica condicional entrelaçada — fácil de auditar que nenhuma área de dado foi esquecida (o Achado 1 foi encontrado justamente comparando a lista de portas injetadas com os campos de domínio de cada uma, não por acaso). `DeleteAccountUseCase` reaproveita `PasswordHasher`/`InvalidCredentialsException` já existentes em vez de introduzir um mecanismo de confirmação paralelo. Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/user/` (além do já existente) ou `application/usecases/user/`.

**Testes**: `UserTest.anonymizePreservesIdAndCreatedAt` e `anonymizeReplacesEmailPasswordHashAndNameAndRecordsDeletedAt` cobrem exatamente os campos que a LGPD exige que mudem (e os que não devem). `DeleteAccountUseCaseTest.aDeletedUserCanNoLongerAuthenticate` e `AuthenticateUserUseCaseTest.rejectsAuthenticationForADeletedUserEvenIfThePasswordHashStillMatched` verificam, em conjunto, tanto o caminho natural (hash nunca mais confere) quanto a defesa em profundidade (`isDeleted()` isolada, com um hash artificialmente construído para bater) — evita que os dois mecanismos de proteção mascarem uma regressão um no outro. `UserControllerTest.deletingTheAccountRevokesFutureLoginWithTheOriginalCredentials` é o teste de ponta a ponta mais importante desta fase: exclusão real via HTTP seguida de tentativa real de login.

**Segurança**: `userId` nunca aceito do corpo/parâmetros da requisição; `passwordHash` nunca aparece no JSON de exportação (`PrivacyControllerTest.neverExposesThePasswordHashInTheExportedJson` verifica isso no corpo real da resposta HTTP, não apenas na estrutura do DTO). Exclusão de conta exige reautenticação por senha; erro de senha incorreta e de conta já excluída são indistinguíveis (mesma postura anti-enumeração do login).

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Corrida de exclusão concorrente (Achado 2 acima) — mesma classe de risco TOCTOU já aceita em todas as fases anteriores.
2. Dados financeiros retidos na exclusão de conta é uma posição provisória, explicitamente pendente de validação jurídica (RN-008) — não uma lacuna de implementação desta fase.
3. Campos de texto livre (`Transaction.description`, `Goal.name`) não são anonimizados na exclusão — apenas os campos estruturais de `User`. Nenhum requisito exige mais que isso hoje.
4. Tokens JWT emitidos antes da exclusão continuam válidos até expirar (15 minutos) — mesma limitação já registrada em ADR-0010 para o backend TypeScript, aqui sem sequer a camada de sessão para mitigar.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real de completude (exportação de dados omitindo o detalhamento por fator do Pulse Score) foi identificado e corrigido durante esta revisão, com teste de regressão dedicado — particularmente relevante para uma funcionalidade cujo requisito literal é "exportar todos os dados". A decisão de anonimizar apenas `User` e reter dados financeiros é conservadora, reversível e corretamente rotulada como provisória diante de uma pendência jurídica já formalmente reconhecida pelo próprio vision.md. Nenhum apontamento crítico de segurança ou de isolamento multi-tenant foi identificado.
