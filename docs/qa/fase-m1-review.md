# QA — Revisão da Fase de Migração M1 (Fundação técnica + Cadastro e Login, Java)

| Campo | Valor |
|---|---|
| Fase | M1 — RF-001, RF-002, RF-003, RF-008 (equivalente Java da Fase 1 original) |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0013) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-001, RF-002, RF-003 e RF-008 do vision.md, no escopo restrito definido pelo ADR-0013.
- [x] Não há violação de regra de negócio ou restrição do vision.md.
- [x] N/A nesta fase: isolamento multi-tenant (RF-047) — ainda não há dado financeiro de outro usuário a isolar.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Senha nunca armazenada ou logada em texto plano; hash via `BCryptPasswordEncoder` (`spring-security-crypto`).
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (36 testes).
- [x] Sem degradação de performance perceptível para o escopo (operações simples de I/O local).
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (smoke test contra a raiz de composição real).
- [x] Documentação técnica entregue (`backend-java/README.md`) é clara e suficiente, incluindo Limitações conhecidas.
- [x] Não há introdução de funcionalidade fora do escopo da Fase M1 (confirmado: nenhum endpoint além de `/auth/register` e `/auth/login`).

## Verificação de Execução

```
mvn test → 10 classes de teste, 36 testes, 100% passando (verificado em duas execuções limpas consecutivas,
           incluindo `mvn clean test`, para descartar flakiness de inicialização de contexto Spring)
```

Sem ferramenta de cobertura configurada nesta fase (equivalente a `npm run test:coverage` do backend TypeScript) — `rules.md` § 3 não exige uma meta percentual, apenas cobertura orientada a risco real; os cenários de risco identificados (RF-001, RF-002, RF-003, RF-008) estão cobertos em múltiplas camadas (unitário com dublês, adaptador contra tecnologia real, smoke test de composição real).

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`RegisterUserUseCase.execute`, `PasswordPolicy.assertStrongPassword`, `InvalidCredentialsException`); nenhum comentário explica "o quê" — os comentários existentes (`JwtTokenServiceAdapter`, `AuthenticateUserUseCase`, `AuthenticatedUserResolver`, `UseCaseConfiguration`) documentam decisões não óbvias ("por quê"), conforme `rules.md` § 2.

**SOLID**: `RegisterUserUseCase` e `AuthenticateUserUseCase` têm responsabilidade única; as três portas (`UserRepository`, `PasswordHasher`, `TokenService`) têm implementações substituíveis sem quebra de contrato (verificado nos próprios testes, que trocam a implementação real por dublês em `testsupport/`); nenhuma importação de `org.springframework.*`, BCrypt ou JJWT foi encontrada em `domain/` ou `application/` — confirmado por inspeção direta dos pacotes.

**Testes**: pirâmide respeitada — testes de domínio/aplicação são unitários com dublês (`InMemoryUserRepository`, `FakePasswordHasher`, `FakeTokenService`, sem I/O), testes de adaptador validam a tecnologia real encapsulada (`@DataJpaTest` com H2 real, `BCryptPasswordEncoder` real, `jjwt` real), e `AuthControllerTest` (`@SpringBootTest` + `MockMvc`) cobre o fluxo HTTP completo contra a raiz de composição real — equivalente Java ao `container.integration.test.ts` exigido por `rules.md` § 3 (o mecanismo de composição mudou do `container.ts` manual para o contêiner do Spring, conforme ADR-0013, mas o princípio do smoke test real é preservado). Nenhum teste foi pulado ou comentado.

**Segurança**:
- Hash de senha com BCrypt (nunca texto plano); segredo JWT configurável via variável de ambiente (`FINANCEPULSE_JWT_SECRET`), nunca hardcoded no código-fonte (o valor padrão em `application.properties` é apenas um fallback de desenvolvimento, mesmo padrão do `.env.example` do backend TypeScript).
- `GlobalExceptionHandler` não vaza stack trace nem detalhes internos ao cliente em erros inesperados (HTTP 500 genérico; detalhe fica apenas no log de servidor via SLF4J).
- Login retorna o **mesmo** erro para "e-mail inexistente" e "senha incorreta" (`InvalidCredentialsException`, HTTP 401), prevenindo enumeração de contas via login — validado por teste dedicado (`AuthenticateUserUseCaseTest.rejectsAuthenticationWithAWrongPasswordUsingTheSameErrorAsAnUnknownEmail`).
- `JwtTokenServiceAdapter` deriva a chave HMAC via SHA-256 do segredo configurado, em vez de assinar com os bytes crus do segredo — decisão técnica **não presente no backend TypeScript**, adotada para satisfazer o comprimento mínimo de chave que `jjwt` exige por padrão para HS256 (a lib `jsonwebtoken` usada em TS não impõe essa validação). Documentado em comentário no próprio adaptador; efeito é estritamente mais seguro que o equivalente TypeScript, não uma divergência de comportamento observável.

## Achados Durante a Revisão

Nenhum achado bloqueante (crítico ou alto) foi identificado nesta revisão — ao contrário das Fases 1, 2.3, 2.4, 2.5.1 e 2.5.2 do backend TypeScript, que cada uma teve ao menos um defeito real corrigido durante a revisão de QA.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **JSON sintaticamente malformado no corpo da requisição resultaria em HTTP 500, não 400** — `GlobalExceptionHandler` tem um `@ExceptionHandler(Exception.class)` genérico que capturaria `HttpMessageNotReadableException` (erro de parsing do Jackson) sem um handler dedicado. Isso **não é uma regressão introduzida pela migração**: o comportamento equivalente no backend TypeScript (corpo com JSON inválido, não apenas campos ausentes) também cai no branch de erro 500 genérico do `errorHandler.ts`, já que `SyntaxError` do `body-parser` não está em `CLIENT_ERROR_TYPES`. Registrado como paridade de limitação pré-existente, não como defeito novo — candidato a correção em ambas as stacks numa futura fase de hardening.
2. **`AuthenticatedUserResolver` não está conectado a nenhum endpoint HTTP nesta fase** — é uma decisão de escopo explícita e documentada no próprio ADR-0013 (a rota histórica `GET /auth/me` não foi replicada por já estar obsoleta desde a Fase 2.2 do backend TypeScript). A validação de RF-008 é coberta por teste direto (`AuthenticatedUserResolverTest`). Fica pronto para ser aplicado a rotas protegidas a partir de M2.1.
3. **Corrida de cadastro concorrente (TOCTOU) em `RegisterUserUseCase`**: a verificação `findByEmail` seguida de `save` não é atômica; duas requisições de cadastro simultâneas com o mesmo e-mail poderiam ambas passar pela checagem antes de qualquer uma persistir. A constraint `UNIQUE` na coluna `email` (H2) impediria a duplicação real dos dados, mas a segunda requisição receberia uma `DataIntegrityViolationException` não mapeada explicitamente, caindo no handler genérico (HTTP 500 em vez de 400 `DuplicateEmailException`). Mesma limitação estrutural já existe no backend TypeScript (SQLite com constraint `UNIQUE` equivalente) — não é uma regressão, mas fica registrado para avaliação conjunta em ambas as stacks.
4. **Sem ferramenta de cobertura de testes configurada** (JaCoCo ou equivalente) — não bloqueante, já que `rules.md` § 3 não exige uma meta percentual, mas recomenda-se configurar antes da fase M2.1 para manter paridade de visibilidade com o backend TypeScript (`npm run test:coverage`).
5. **Sem rate limiting** em `/auth/register` e `/auth/login` — mesma limitação já aceita e documentada no backend TypeScript.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Nenhum apontamento crítico ou de alta severidade foi identificado. Os itens não bloqueantes acima são, em sua maioria, paridade com limitações já conhecidas e aceitas no backend TypeScript (não regressões da migração), com exceção do item 3 (corrida de cadastro concorrente), que é uma observação nova mas de baixo risco prático neste estágio (sem carga concorrente real em ambiente de desenvolvimento) — registrado como recomendação para avaliação conjunta em fase futura, não como pendência da Fase M1.
