# CTO — Aprovação da Fase 5 (Java): Categorização (CRUD completo, subcategorias)

| Campo | Valor |
|---|---|
| Fase | 5 (Java) — RF-023, RF-024 (já satisfeito); RF-022 adiado para a Fase 4.4 |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-07-31 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

RF-023 (criação, edição e exclusão de categorias e subcategorias customizadas), conforme delimitado em [ADR-0017](../adr/0017-fase-5-categorizacao.md) e [roadmap.md](../../roadmap.md) — Fase 5. RF-024 confirmado já satisfeito desde a Fase 4.1. RF-022 formalmente remanejado para a Fase 4.4.

## Insumos considerados

- [docs/qa/fase-05-java-review.md](../qa/fase-05-java-review.md) — parecer de qualidade do QA: **aprovado**, sem apontamento crítico ou de alta severidade.
- [ADR-0017](../adr/0017-fase-5-categorizacao.md) — decisão de hierarquia de categorias, regras de exclusão e adiamento de RF-022.
- Código-fonte em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] `Category` estendida de forma aditiva (novo campo `parentCategoryId`) sem quebrar o contrato já usado pela Fase 4.1 — `Category.reconstitute` e os pontos de consumo em `Transaction`/`TransactionRepository` não precisaram de alteração estrutural, apenas de assinatura.
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/category/` ou nos novos use cases importa Spring, JPA ou Jackson.
- [x] **Hierarquia de 2 níveis verificada em código, não apenas em documentação**: `CreateCategoryUseCase` consulta o pai antes de criar a subcategoria e rejeita explicitamente `parent.isSubcategory()` — não há caminho de código que permita 3 níveis.
- [x] **RN-002 estruturalmente preservada**: a exclusão de categoria é bloqueada tanto por subcategorias quanto por transações associadas, verificado via consultas dedicadas (`existsByParentCategoryIdAndUserId`, `existsByCategoryIdAndUserId`) — não apenas confiado a uma constraint de banco.
- [x] `rules.md` § 3 atendido: `CategoryControllerTest` estende o smoke test já existente com o fluxo completo de CRUD (criar, subcategoria, bloqueio de exclusão, isolamento entre usuários) contra a raiz de composição real.
- [x] Confirmo a decisão de adiar RF-022 para a Fase 4.4: revisei o texto do vision.md e concordo que "categorizar automaticamente transações **importadas**" não tem nenhum consumidor real até a importação (RF-019–021) existir — construir o motor de regras agora seria especulativo, contrário à disciplina já aplicada em todo o projeto (ex.: ADR-0016 não antecipou RF-023 na Fase 4.1 pelo mesmo motivo).
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009).
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Os itens não bloqueantes do parecer de QA (mapeamento HTTP 400 em vez de 409 para conflitos de exclusão, corrida de exclusão concorrente, ausência de restrição de nome duplicado, lacuna de cobertura para recategorização em subcategoria) são aceitos como dívida técnica consciente, consistentes com decisões e limitações já aceitas em fases anteriores.
- A decisão de manter `parentCategoryId` imutável (sem re-parentar) é reafirmada como correta: nenhum requisito do vision.md pede essa operação, e a ambiguidade sobre o efeito em orçamentos futuros (RN-004, Fase 6) não justificaria a complexidade agora.
- RF-022 permanece uma pendência formalmente rastreada (não uma lacuna esquecida) — vinculada à Fase 4.4 tanto em `roadmap.md` quanto em `docs/adr/0016-decomposicao-fase-4-e-dependencia-categoria.md` (nota atualizada) e neste ADR-0017.

## Decisão

**A Fase 5 (Java) está aprovada.** A implementação entrega o CRUD completo de categorias com subcategorias de forma consistente com o modelo de domínio já estabelecido, preserva RN-002 estruturalmente (não apenas por convenção), mantém a regra de dependência da Arquitetura Hexagonal, e o parecer de qualidade do QA foi favorável sem apontamento crítico. O adiamento de RF-022 para a Fase 4.4 é uma decisão de escopo deliberada e documentada, não uma omissão. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
