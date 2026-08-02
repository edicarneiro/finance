# CTO — Aprovação da Fase 4.1 (Java): Transações Manuais + Fundação Mínima de Categoria

| Campo | Valor |
|---|---|
| Fase | 4.1 (Java) — RF-014, RF-015, RF-017, RF-025 (parcial) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

RF-014 (registro manual de transação), RF-015 (edição e exclusão), RF-017 (tags livres) e RF-025 parcial (categorias padrão via seed automático), conforme delimitado em [ADR-0016](../adr/0016-decomposicao-fase-4-e-dependencia-categoria.md) e [roadmap.md](../../roadmap.md) — Fase 4.1.

## Insumos considerados

- [docs/qa/fase-04-1-java-review.md](../qa/fase-04-1-java-review.md) — parecer de qualidade do QA: **aprovado**, com um achado de alta severidade corrigido na própria revisão.
- [ADR-0016](../adr/0016-decomposicao-fase-4-e-dependencia-categoria.md) — decomposição da Fase 4, resolução da dependência de Categoria (RN-002) e extensão do cálculo de saldo (RN-001).
- Código-fonte em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue o padrão já estabelecido (`domain/ → application/ → adapters/`), estendida com `domain/transaction/`, `domain/category/`, `application/usecases/{transaction,category}`, `application/services/AccountBalanceCalculator`.
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/transaction/`, `domain/category/` ou nos use cases correspondentes importa Spring, JPA ou Jackson.
- [x] **RN-002 resolvido conforme decidido em ADR-0016**: `Category` é uma fundação mínima (sem CRUD, sem subcategorias), suficiente para satisfazer a obrigatoriedade de categoria em toda transação sem antecipar o escopo de RF-023 (Fase 5). Verifiquei que nenhum endpoint de criação/edição/exclusão de categoria foi exposto além do planejado.
- [x] **RN-001 estendido conforme decidido em ADR-0016**: `Account.balance` passou a representar exclusivamente o saldo inicial; `AccountBalanceCalculator` deriva o saldo atual somando transações, sem introduzir um campo mutável — verifiquei que nenhum caminho de código escreve diretamente em `Account.balance` fora da criação.
- [x] `TransactionRepository`/`CategoryRepository` seguem o mesmo padrão de escopo por `userId` na assinatura já estabelecido para `AccountRepository` (RF-047) — verificado nos adaptadores JPA e nos casos de uso (validação explícita de posse de conta/categoria na criação/edição de transação, não apenas confiada ao isolamento de leitura).
- [x] `rules.md` § 3 (smoke test contra a raiz de composição real) atendido: `TransactionControllerTest` inclui um teste de ponta a ponta verificando que uma transação criada via HTTP altera o saldo retornado por `GET /accounts` — a verificação mais crítica desta fase, exercitando a integração completa entre `Transaction`, `Account` e os dois use cases de saldo.
- [x] Endosso à correção do achado de QA (regra de conta arquivada restrita a movimentação, não a toda edição) — a leitura do QA está correta e alinhada à intenção original registrada em ADR-0016; nenhuma revisão adicional do ADR foi necessária, pois o código é que estava mais restritivo do que o texto já aprovado.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009).
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Os itens não bloqueantes do parecer de QA (corrida de seed de categorias, N+1 no cálculo de saldo, enum inválido retornando 500, validação de escala decimal, parâmetro obrigatório ausente retornando 500) são aceitos como dívida técnica consciente — em sua maioria extensões de itens já registrados nas Fases M1 e 3, não riscos novos de arquitetura.
- A decisão de introduzir `Category` de forma deliberadamente mínima (ADR-0016) é reafirmada como correta nesta revisão: o escopo desta fase não foi contaminado por antecipação de RF-023, e a Fase 5 permanece livre para desenhar o CRUD completo sem restrição herdada de schema incompatível.
- O achado corrigido nesta fase (conta arquivada bloqueando edição, não só criação) reforça um padrão de risco já observado no projeto: regras de negócio replicadas entre casos de uso similares (criar vs. editar) tendem a herdar restrições que fazem sentido em um contexto mas não no outro. Registro para atenção em revisões futuras — não formalizado como nova regra em `rules.md` por ainda não ter ocorrido um segundo caso desta natureza especificamente.

## Decisão

**A Fase 4.1 (Java) está aprovada.** A implementação é fiel ao modelo de domínio e à resolução de dependência definidos em ADR-0016, preserva integralmente a regra de dependência da Arquitetura Hexagonal, estende corretamente RN-001 (saldo derivado de transações) sem comprometer a imutabilidade do saldo inicial, aplica RF-047 de forma estrutural aos novos recursos financeiros, e o parecer de qualidade do QA foi favorável após a correção de um achado de alta severidade. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
