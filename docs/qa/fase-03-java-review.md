# QA — Revisão da Fase 3 (Java): Contas e Carteiras Financeiras

| Campo | Valor |
|---|---|
| Fase | 3 (Java) — RF-009 a RF-013, primeira fase construída diretamente em Java |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0014) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-009, RF-010, RF-011, RF-012 e RF-013 do vision.md, no escopo delimitado por ADR-0014.
- [x] Não há violação de regra de negócio ou restrição do vision.md — RN-001 (saldo derivado, não editável diretamente) aplicado literalmente.
- [x] **Isolamento multi-tenant (RF-047) verificado e estruturalmente reforçado**: `AccountRepository.findByIdAndUserId`/`findAllByUserId` escopam por `userId` na própria assinatura, não em checagem de aplicação. Testado explicitamente (`aUserCannotAccessAnotherUsersAccount` em `AccountControllerTest`, testes de "outro usuário" em `UpdateAccountUseCaseTest`/`ArchiveAccountUseCaseTest`/`JpaAccountRepositoryAdapterTest`).
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009) — apenas CRUD de registro contábil, sem integração de pagamento/transferência.
- [x] **Primeira rota protegida real do backend Java** — `AuthenticationInterceptor` validado com token ausente, token inválido e token válido, tanto isoladamente (`AuthenticationInterceptorTest`) quanto via HTTP real (`AccountControllerTest`).
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (86 testes no total; 50 novos nesta fase).
- [x] Sem degradação de performance evidente (operações simples de I/O local, sem N+1 nem consultas não indexadas — `findAllByUserId`/`findByIdAndUserId` seriam candidatos a índice em `user_id` numa fase de hardening de produção, não bloqueante em H2/dev).
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`AccountControllerTest` como smoke test da raiz de composição real, mesmo padrão do `AuthControllerTest`).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo da Fase 3 (confirmado: nenhuma rota de transação, categoria, orçamento etc.).

## Verificação de Execução

```
mvn test → 21 classes de teste, 86 testes, 100% passando (verificado após `mvn clean test` com JDK 25 / Spring Boot 3.5.4)
```

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`CreateAccountUseCase.execute`, `AccountPolicy.assertValidName`, `AccountNotFoundException`); `Account.archive()` documentado quanto à idempotência (decisão não óbvia); `GetConsolidatedBalanceUseCase` documenta a decisão de não agrupar por moeda — todos comentários "por quê", não "o quê", conforme `rules.md` § 2.

**SOLID**: cada caso de uso de conta tem responsabilidade única; `AccountRepository` é uma porta específica ao consumidor (não compartilhada com `UserRepository`); nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/account/` ou `application/usecases/account/` — confirmado por inspeção direta dos pacotes.

**Testes**: pirâmide respeitada — domínio e casos de uso testados com `InMemoryAccountRepository` (dublê, sem I/O); `JpaAccountRepositoryAdapterTest` valida contra H2 real (`@DataJpaTest`); `AccountControllerTest` cobre o fluxo HTTP completo (registro → login → criar conta → listar → saldo consolidado → editar → arquivar → tentativa de acesso por outro usuário) contra a raiz de composição real. Nenhum teste foi pulado ou comentado.

**Segurança**:
- `userId` nunca é aceito do corpo da requisição em nenhum endpoint de `/accounts` — sempre derivado do token autenticado via `AuthenticationInterceptor` (RF-047).
- Acessar/editar/arquivar conta de outro usuário retorna o mesmo erro "não encontrada" (HTTP 404) de uma conta inexistente — postura anti-enumeração validada por teste dedicado.
- `AuthenticationInterceptor` responde 401 diretamente (sem repassar ao controller) tanto para header ausente quanto para token inválido, sem vazar qual dos dois casos ocorreu — mesma granularidade de mensagem em ambos.

## Achados Durante a Revisão

Nenhum achado bloqueante (crítico ou alto) foi identificado nesta revisão.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Corrida de atualização concorrente (lost update) em `UpdateAccountUseCase`/`ArchiveAccountUseCase`**: ambos seguem o padrão "buscar → modificar → salvar" sem controle de concorrência otimista (`AccountJpaEntity` não tem coluna `@Version`). Duas requisições concorrentes sobre a mesma conta (ex.: um rename e um archive simultâneos) podem resultar em uma sobrescrever a outra silenciosamente. Risco baixo no estágio atual (sem carga concorrente real), mas deve ser avaliado numa futura fase de hardening — possivelmente junto com o item de corrida de cadastro já registrado no `docs/qa/fase-m1-review.md`.
2. **`AccountType` inválido ou mal formatado no JSON (`"type":"invalid"` ou tipo numérico) resultaria em HTTP 500, não 400** — o erro de desserialização do Jackson (`InvalidFormatException`) não tem handler dedicado em `GlobalExceptionHandler`, caindo no catch-all genérico. Mesma classe de limitação já registrada para JSON sintaticamente malformado no `docs/qa/fase-m1-review.md` (item 1) — não uma regressão nova, mas agora com uma segunda superfície (o campo enum `type`).
3. **Sem validação de casas decimais em `initialBalance`**: um valor com mais de 4 casas decimais (escala da coluna `balance`, `NUMERIC(19,4)`) seria silenciosamente arredondado pelo H2 ao persistir, sem erro nem aviso ao cliente. Risco baixo (a UI do frontend, ainda não construída, controlaria a entrada), mas vale considerar validação explícita de escala em fase futura envolvendo valores monetários com mais rigor (ex.: RN-001 em conjunto com a Fase 4).
4. **Ausência de índice explícito em `accounts.user_id`**: toda consulta de conta é filtrada por esse campo (`findAllByUserId`, `findByIdAndUserId`); aceitável em H2/dev, mas deve ser considerado na migração para o motor de banco de produção (mesma dívida já registrada para `users.email` no backend TypeScript/Java).
5. **Sem rate limiting** em nenhuma rota de `/accounts` — mesma limitação já aceita nas fases anteriores.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Nenhum apontamento crítico ou de alta severidade foi identificado. RF-047 (isolamento multi-tenant) — critério de aprovação obrigatório a partir desta fase por ser a primeira a manipular dado financeiro de usuário — foi verificado e estruturalmente reforçado, não apenas testado. Os itens não bloqueantes acima são majoritariamente extensões de dívidas técnicas já conhecidas (concorrência, validação de entrada) ou observações de baixo risco para o estágio atual (dev/MVP sem carga real), registradas para avaliação em fases futuras.
