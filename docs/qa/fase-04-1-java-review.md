# QA — Revisão da Fase 4.1 (Java): Transações Manuais + Fundação Mínima de Categoria

| Campo | Valor |
|---|---|
| Fase | 4.1 (Java) — RF-014, RF-015, RF-017, RF-025 (parcial) |
| Revisor | QA / Code Reviewer |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (Arquitetura Hexagonal, ADR-0016) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md`.

## Checklist de Qualidade

- [x] A implementação atende RF-014, RF-015, RF-017 e RF-025 (parcial, ver ADR-0016) do vision.md.
- [x] Não há violação de regra de negócio ou restrição do vision.md — **RN-001 e RN-002 verificadas com atenção especial**, por serem o núcleo desta fase.
- [x] Isolamento multi-tenant (RF-047) verificado para os três recursos novos (`Transaction`, `Category`) e reforçado no nível do repositório, mesmo padrão de `Account` (Fase 3).
- [x] Nenhuma capacidade de movimentação financeira real introduzida (RN-009) — transações são registros contábeis internos, sem integração de pagamento.
- [x] Testes cobrem caminho principal, casos de erro e regras de negócio associadas (141 testes; 55 novos nesta fase).
- [x] Sem degradação de performance evidente para o estágio atual (ver item não bloqueante sobre N+1 abaixo).
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] Aderente às regras definidas em `rules.md`, incluindo § 3 (smoke tests `TransactionControllerTest`/`CategoryControllerTest` contra a raiz de composição real).
- [x] Documentação técnica entregue (`backend-java/README.md`) atualizada com os novos endpoints, regras de negócio e limitações.
- [x] Não há introdução de funcionalidade fora do escopo da Fase 4.1 (confirmado: nenhuma rota de recorrência, filtro/busca completo, ou importação — corretamente adiadas para 4.2/4.3/4.4).

## Verificação de Execução

```
mvn test → 33 classes de teste, 141 testes, 100% passando (mvn clean test, JDK 25 / Spring Boot 3.5.4)
```

## Achado Durante a Revisão (encontrado → corrigido nesta mesma revisão)

**Severidade**: Alta (regra de negócio mais restritiva do que o pretendido, sem violação de segurança).

**Descrição**: `UpdateTransactionUseCase` reutilizava a mesma checagem de `CreateTransactionUseCase` — rejeitar qualquer conta arquivada — mas a aplicava a **toda edição**, mesmo quando a transação já pertencia àquela conta antes dela ser arquivada e o usuário só queria corrigir um campo (valor, categoria, descrição). O ADR-0016 registra explicitamente que a restrição de conta arquivada se aplica à **criação** de lançamentos ("uma transação só pode ser **criada** contra uma conta não arquivada"), não à correção de histórico já existente. O comportamento implementado impediria permanentemente qualquer correção em transações de contas arquivadas — nenhuma trilha de auditoria seria perdida, mas o usuário ficaria sem meio de corrigir um erro de digitação, por exemplo.

**Ação**: corrigido para bloquear apenas quando a transação está sendo **movida para** uma conta diferente que esteja arquivada (`accountId` de entrada ≠ `accountId` atual da transação). Editar campos mantendo a mesma conta (mesmo arquivada) permanece permitido. Dois testes de regressão adicionados: `allowsCorrectingATransactionThatAlreadyBelongsToAnAccountArchivedAfterTheFact` e `rejectsMovingATransactionIntoAnArchivedAccount`. Suíte reexecutada: 141/141 passando.

**Status**: ✅ Corrigido e validado nesta revisão.

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`AccountBalanceCalculator.currentBalance`, `ListCategoriesUseCase.seedDefaultCategories`, `movingToADifferentAccount`); o comentário adicionado em `UpdateTransactionUseCase` documenta exatamente a decisão não óbvia acima ("por quê", não "o quê"), conforme `rules.md` § 2.

**SOLID**: `AccountBalanceCalculator` é uma função pura e isolada, sem dependência de infraestrutura — pode ser testada e reutilizada sem dublês; cada caso de uso de transação tem responsabilidade única; `CategoryRepository`/`TransactionRepository` são portas específicas ao consumidor. Nenhuma importação de `org.springframework.*`, JPA ou Jackson foi encontrada em `domain/transaction/`, `domain/category/` ou nos pacotes `application/usecases/{transaction,category}` — confirmado por inspeção direta.

**Testes**: pirâmide respeitada — domínio e casos de uso com dublês em memória (`InMemoryTransactionRepository`, `InMemoryCategoryRepository`), adaptadores contra H2 real (`@DataJpaTest`), e dois smoke tests HTTP completos (`TransactionControllerTest`, `CategoryControllerTest`) contra a raiz de composição real, incluindo um teste que verifica **de ponta a ponta** que criar transações altera o saldo retornado por `GET /accounts` e `GET /accounts/balance/consolidated` — a verificação mais importante desta fase (RN-001 estendida). Nenhum teste foi pulado ou comentado.

**Segurança**: `userId` nunca aceito do corpo da requisição; `accountId`/`categoryId` de uma transação são validados contra o usuário autenticado no próprio caso de uso de criação/edição, não apenas confiados ao isolamento de leitura (`AccountNotFoundException`/`CategoryNotFoundException` — mesmo erro de "não encontrado" para recurso inexistente ou de outro usuário, postura anti-enumeração consistente). Testado explicitamente em `aUserCannotAccessAnotherUsersTransaction`.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Corrida de seed de categorias padrão (TOCTOU)**: duas requisições `GET /categories` simultâneas para um usuário novo poderiam ambas ver a lista vazia e ambas semear o conjunto padrão, resultando em categorias duplicadas (16 em vez de 8). Mesma classe de risco já registrada para cadastro concorrente de e-mail (`docs/qa/fase-m1-review.md`) e atualização concorrente de conta (`docs/qa/fase-03-java-review.md`) — baixo risco prático no estágio atual, candidato a solução conjunta numa futura fase de hardening de concorrência.
2. **N+1 no cálculo de saldo**: `ListAccountsUseCase`/`GetConsolidatedBalanceUseCase` fazem uma consulta de transações por conta (uma consulta adicional para cada conta do usuário). Aceitável para volume de dev/MVP; deve ser revisitado (ex.: uma única consulta agregada) se o volume de contas por usuário crescer.
3. **`AccountType`/`TransactionType` inválido no JSON continua retornando HTTP 500** em vez de 400 (mesma limitação já registrada em `docs/qa/fase-m1-review.md`, agora com uma terceira superfície de enum). Não é uma regressão desta fase.
4. **Sem validação de escala decimal em `amount`** de transação — mesma observação já registrada para `initialBalance` de conta (`docs/qa/fase-03-java-review.md`), agora também aplicável ao valor de transações.
5. **`GET /transactions` sem o parâmetro `accountId`** retorna HTTP 500 (erro de parâmetro obrigatório ausente do Spring não mapeado explicitamente) em vez de 400 — mesma classe de limitação de validação de borda já aceita em fases anteriores.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado de alta severidade foi identificado (regra de conta arquivada aplicada de forma mais ampla do que o pretendido pelo ADR-0016) e corrigido nesta mesma revisão, com testes de regressão comprovando ambos os comportamentos (edição em conta já arquivada permitida; movimentação para conta arquivada bloqueada). Os itens não bloqueantes são, em sua maioria, extensões de dívidas técnicas já conhecidas e aceitas em fases anteriores, não riscos novos introduzidos por esta fase.
