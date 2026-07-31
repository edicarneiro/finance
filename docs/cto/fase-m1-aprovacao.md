# CTO — Aprovação da Fase de Migração M1 (Fundação técnica + Cadastro e Login, Java)

| Campo | Valor |
|---|---|
| Fase | M1 — RF-001, RF-002, RF-003, RF-008 (equivalente Java da Fase 1 original) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal.

## Escopo revisado

RF-001 (cadastro), RF-002 (unicidade de e-mail), RF-003 (login), RF-008 (emissão/validação de token de sessão), reimplementados em Java + Spring Boot conforme delimitado em [roadmap.md](../../roadmap.md) — Fase M1, e conforme a decisão de escopo registrada em [ADR-0013](../adr/0013-migracao-java-spring-boot.md) (a rota histórica `GET /auth/me` da Fase 1 original não foi replicada, por já estar obsoleta desde a Fase 2.2 do backend TypeScript).

## Insumos considerados

- [docs/qa/fase-m1-review.md](../qa/fase-m1-review.md) — parecer de qualidade do QA: **aprovado**, sem apontamento crítico ou de alta severidade pendente.
- [ADR-0013](../adr/0013-migracao-java-spring-boot.md) — decisão de migração, stack, e escopo desta primeira fase.
- Código-fonte entregue em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue o definido em ADR-0013 (`domain/ → application/ → adapters/`), preservando a mesma regra de dependência do backend TypeScript (ADR-0002).
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/user/` ou `application/` importa `org.springframework.*`, `spring-security-crypto` ou `jjwt` — confirmado por inspeção dos imports de cada arquivo dessas camadas.
- [x] Toda porta definida em `application/ports/` tem exatamente uma implementação em `adapters/`, substituível sem alteração de use case (`UserRepository` → `JpaUserRepositoryAdapter`; `PasswordHasher` → `BCryptPasswordHasherAdapter`; `TokenService` → `JwtTokenServiceAdapter`; `IdGenerator` → `UuidIdGeneratorAdapter`) — mesma estrutura de substituibilidade já usada em TypeScript, agora com o contêiner do Spring resolvendo a injeção.
- [x] Casos de uso (`RegisterUserUseCase`, `AuthenticateUserUseCase`) permanecem classes Java puras, sem anotação de framework — a composição é feita explicitamente em `composition/UseCaseConfiguration.java` via `@Bean`, preservando a regra de dependência da Arquitetura Hexagonal mesmo com o contêiner do Spring como mecanismo de injeção (conforme decidido em ADR-0013, revisão do ADR-0002).
- [x] Stack tecnológica corresponde exatamente ao decidido em ADR-0013: Java 17, Maven, Spring Boot 3.3.4, H2 embarcado, `spring-security-crypto` isolado (não o starter completo de segurança), `jjwt`, JUnit 5 + AssertJ com dublês escritos à mão (sem Mockito).
- [x] `rules.md` § 3 (smoke test contra a raiz de composição real) atendido: `AuthControllerTest` exercita `@SpringBootTest` completo (H2 real, BCrypt real, JWT real) — equivalente Java ao `container.integration.test.ts`, adaptado ao novo mecanismo de composição.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009) — não aplicável a este escopo, mas verificado.
- [x] Backend TypeScript (`backend/`) permanece intacto e não foi alterado por este trabalho — confirmado.
- [x] Nenhum desvio arquitetural foi registrado pelo QA nesta fase.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Os itens não bloqueantes registrados pelo QA (JSON malformado retornando 500, corrida de cadastro concorrente, ausência de ferramenta de cobertura, ausência de rate limiting) são aceitos conscientemente como escopo adiado. Os dois primeiros são paridade com limitações já existentes e aceitas no backend TypeScript, não regressões introduzidas pela migração.
- A corrida de cadastro concorrente (item 3 do parecer de QA) é uma observação nova, de baixo risco prático nesta fase (sem carga concorrente real em desenvolvimento) — fica registrada como candidata a avaliação conjunta em ambas as stacks numa futura fase de hardening, não bloqueia esta aprovação.
- A dependência de setup manual do Maven neste ambiente Windows (documentada em `backend-java/README.md` § Limitações conhecidas) é um risco operacional para novos ambientes de desenvolvimento, não um risco de produto — aceito como pendência de tooling, não de arquitetura.

## Decisão

**A Fase de Migração M1 está aprovada.** A implementação é fiel à arquitetura definida em ADR-0013, preserva integralmente a regra de dependência da Arquitetura Hexagonal com o contêiner do Spring como novo mecanismo de composição, os testes automatizados comprovam corretude do comportamento especificado (paridade funcional com a Fase 1 original em TypeScript, dentro do escopo deliberadamente reduzido), e o parecer de qualidade do QA foi favorável, sem apontamento crítico pendente. Não há ajuste adicional exigido pelo CTO.

Conforme o processo definido pelo stakeholder — "somente após [sua] aprovação iniciar a próxima fase" — esta aprovação do CTO **encerra o ciclo interno dos agentes**, mas não substitui a aprovação explícita do stakeholder para o início da Fase M2.1 (Refresh token e logout, em Java). Esse gate permanece com o stakeholder.
