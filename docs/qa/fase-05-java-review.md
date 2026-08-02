# QA — Revisão da Fase 5 (Java): Categorização (CRUD completo, subcategorias)

| Campo | Valor |
|---|---|
| Fase | 5 (Java) — RF-023 (CRUD completo + subcategorias); RF-024 confirmado já satisfeito; RF-022 adiado |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0017) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-023 (criação, edição, exclusão de categorias e subcategorias) do vision.md.
- [x] Não há violação de regra de negócio ou restrição do vision.md — **RN-002 verificada com atenção**: exclusão de categoria bloqueada quando há subcategorias ou transações associadas, preservando a garantia de que toda transação sempre tem uma categoria válida.
- [x] Isolamento multi-tenant (RF-047) verificado para as novas operações — criação de subcategoria valida que o pai pertence ao usuário autenticado; edição/exclusão seguem o mesmo padrão anti-enumeração já usado em `Account`/`Transaction`.
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009).
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (167 testes; 26 novos nesta fase).
- [x] Sem degradação de performance evidente para o estágio atual.
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (`CategoryControllerTest` estendido com smoke tests de CRUD completo contra a raiz de composição real).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo da Fase 5 — **verificado com atenção**: RF-022 (categorização automática) foi conscientemente adiado para a Fase 4.4 (ver ADR-0017), não implementado nem parcialmente esboçado nesta fase.

## Verificação de Execução

```
mvn test → 40 classes de teste, 167 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4)
```

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`CategoryHasSubcategoriesException`, `CategoryHasTransactionsException`, `isSubcategory()`); o Javadoc de `Category` documenta a limitação de hierarquia a 2 níveis, uma decisão não óbvia sem o requisito explicitar profundidade — comentário "por quê", conforme `rules.md` § 2.

**SOLID**: `CreateCategoryUseCase`, `UpdateCategoryUseCase` e `DeleteCategoryUseCase` têm responsabilidade única e não se sobrepõem; `CategoryRepository`/`TransactionRepository` ganharam apenas os métodos estritamente necessários (`existsByParentCategoryIdAndUserId`, `existsByCategoryIdAndUserId`), sem inflar a porta com operações não usadas. Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/category/` ou `application/usecases/category/`.

**Testes**: pirâmide respeitada — domínio e casos de uso com dublês em memória (incluindo `InMemoryCategoryRepository.existsByParentCategoryIdAndUserId`, implementado corretamente com `Optional::map` para tratar o campo opcional), adaptador contra H2 real (`@DataJpaTest`, incluindo teste dedicado para hierarquia pai/filho), e `CategoryControllerTest` estendido cobrindo criação, subcategoria, bloqueio de exclusão com filhos, e isolamento entre usuários via HTTP real. Nenhum teste foi pulado ou comentado.

**Segurança**: `userId` nunca aceito do corpo da requisição; criação de subcategoria valida que a categoria-pai pertence ao usuário autenticado (não apenas que existe) — testado explicitamente (`rejectsAParentBelongingToAnotherUser`). Edição/exclusão de categoria de outro usuário retornam o mesmo erro "não encontrada" de uma categoria inexistente (HTTP 404) — mesma postura anti-enumeração do restante do projeto, testada via HTTP (`aUserCannotAccessAnotherUsersCategory`).

## Achados Durante a Revisão

Nenhum achado bloqueante (crítico ou alto) foi identificado nesta revisão.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **`CategoryHasSubcategoriesException`/`CategoryHasTransactionsException` mapeadas para HTTP 400**, não 409 Conflict — tecnicamente um 409 seria mais preciso semanticamente (o recurso existe, mas seu estado atual impede a operação), mas mantém consistência com a convenção já estabelecida para `ArchivedAccountException` (Fase 4.1) — decisão deliberada de uniformidade, não um erro de mapeamento.
2. **Corrida de exclusão concorrente (TOCTOU)** em `DeleteCategoryUseCase`: a verificação de subcategorias/transações associadas e a exclusão em si não são atômicas. Mesma classe de risco já registrada nas Fases M1, 3 e 4.1 — baixo risco no estágio atual, candidato a solução conjunta numa futura fase de hardening de concorrência.
3. **Sem restrição de nome duplicado** entre categorias do mesmo usuário — duas categorias (ou uma categoria e uma subcategoria) podem ter o mesmo nome. Não é um requisito do vision.md; mesma ausência de restrição já aceita para nomes de conta (`Account.name`).
4. **Cobertura de teste**: não há um teste explícito de `PUT /transactions/{id}` recategorizando para uma **subcategoria** (apenas para categorias de nível superior nos testes existentes) — o código trata ambas uniformemente (nenhuma distinção entre categoria e subcategoria em `UpdateTransactionUseCase`), então o risco é baixo, mas um teste dedicado fecharia a lacuna com mais confiança para RF-024.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Nenhum apontamento crítico ou de alta severidade foi identificado. RN-002 foi verificada com atenção especial (exclusão de categoria em uso corretamente bloqueada) e o escopo de RF-022 foi corretamente mantido fora desta fase, conforme decidido em ADR-0017. Os itens não bloqueantes são extensões de dívidas técnicas já conhecidas ou lacunas de cobertura de baixo risco.
