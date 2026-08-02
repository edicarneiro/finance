# ADR-0017: Fase 5 (Java) — Categorização (CRUD completo, subcategorias) e adiamento de RF-022

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | 5 (Java) — RF-023 (CRUD completo de categorias/subcategorias); RF-024 confirmado já satisfeito; RF-022 adiado |

## Contexto

A Fase 4.1 introduziu uma `Category` deliberadamente mínima (id, userId, name), apenas para satisfazer RN-002 (toda transação exige categoria) sem antecipar o CRUD completo, formalmente RF-023 (ver ADR-0016). Esta fase entrega esse CRUD completo, incluindo subcategorias.

**Um segundo problema de dependência, da mesma natureza do identificado em ADR-0016, foi encontrado nesta análise**: RF-022 ("categorizar automaticamente transações **importadas** com base em regras predefinidas e histórico de categorização do usuário") é, pelo texto literal do vision.md, um recurso do fluxo de **importação** (RF-019–021, Fase 4.4 — ainda não implementada). Como RF-014 já exige categoria explícita em toda transação manual desde a Fase 4.1, não existe hoje nenhuma transação sem categoria que precisasse de categorização automática — construir um motor de regras agora não teria nenhum consumidor real até a Fase 4.4 existir.

## Decisão

### 1. CRUD completo de categorias e subcategorias (RF-023)

- `Category` ganha um campo `parentCategoryId` (nulo = categoria de nível superior; não nulo = subcategoria).
- **Hierarquia limitada a 2 níveis**: uma subcategoria não pode ter subcategorias — criar uma categoria cujo `parentCategoryId` aponta para uma categoria que já é, ela mesma, uma subcategoria, é rejeitado (`InvalidCategoryHierarchyException`). O vision.md não especifica profundidade; 2 níveis é o padrão comum em apps de finanças pessoais e evita a complexidade combinatória de uma árvore arbitrária sem requisito que a justifique.
- **`parentCategoryId` é imutável após a criação** — mesma decisão já tomada para `Account.type`/`Account.currency` (ADR-0014): "re-parentar" uma categoria (promovê-la a nível superior, ou movê-la para outro pai) teria efeito ambíguo sobre transações e orçamentos futuros (RN-004) já associados a ela, sem um requisito que peça essa operação. Apenas o `name` é editável via `PUT`.
- **Exclusão é bloqueada em duas condições**, ambas para preservar RN-002 (toda transação tem exatamente uma categoria — apagar uma categoria em uso deixaria transações com uma referência inválida):
  - A categoria tem **subcategorias** (`CategoryHasSubcategoriesException`) — o usuário precisa excluir/mover as subcategorias primeiro.
  - A categoria (de nível superior ou subcategoria) tem **transações associadas** (`CategoryHasTransactionsException`) — mesmo espírito de RF-013 (contas com histórico não podem ser definitivamente excluídas), aplicado agora a categorias, ainda que RF-023 não mencione essa restrição explicitamente — é uma consequência direta de RN-002, não uma extensão de escopo arbitrária.
- Categorias sem subcategorias e sem transações associadas podem ser **excluídas definitivamente** — ao contrário de contas (RF-013), não há requisito equivalente de "arquivamento apenas" para categorias.

### 2. RF-024 (recategorização manual) — confirmado já satisfeito, sem código novo

Como registrado em ADR-0016, `UpdateTransactionUseCase` (Fase 4.1) já permite substituir o `categoryId` de qualquer transação do usuário via `PUT /transactions/{id}`. Nenhum endpoint dedicado é necessário; este ADR apenas confirma essa análise após a Fase 5 introduzir subcategorias — recategorizar para uma subcategoria funciona sem alteração, pois o caso de uso não distingue categoria de nível superior de subcategoria (ambas são apenas um `categoryId` válido do usuário).

### 3. RF-022 (categorização automática) — adiado para acompanhar a Fase 4.4

Não implementado nesta fase. Construir um motor de regras (`CategoryRule`: padrão de texto → categoria sugerida) sem a importação de transações (Fase 4.4) para consumi-lo seria especulativo — nenhuma transação manual precisa dele, já que RF-014 exige categoria explícita desde a Fase 4.1. RF-022 será implementado **junto com** a Fase 4.4, no momento em que o fluxo de importação (que é o único gatilho descrito no requisito) passar a existir.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Implementar RF-022 (motor de regras) nesta fase, sem consumidor | Violaria a diretriz de não construir para requisito hipotético — nenhuma transação do sistema atual precisa de categorização automática. |
| Hierarquia de categorias sem limite de profundidade | Complexidade desproporcional sem requisito explícito do vision.md; 2 níveis atende RF-023 ("categorias e subcategorias") literalmente. |
| Permitir excluir categoria em uso e desassociar as transações (`categoryId = null`) | Violaria RN-002 diretamente ("toda transação pertence a exatamente uma categoria") — a exclusão precisa ser bloqueada, não a associação relaxada. |
| Permitir re-parentar categorias (mover subcategoria entre pais, promover a nível superior) | Sem requisito explícito; a ambiguidade sobre o efeito em transações/orçamentos já associados (RN-004) não compensa a complexidade adicional. |

## Consequências

- `roadmap.md` marca RF-023 e RF-024 como entregues na Fase 5; RF-022 permanece pendente, agora explicitamente vinculado à Fase 4.4 em vez de aberto sem previsão.
- `CategoryRepository` ganha métodos de exclusão e verificação de uso (subcategorias, transações associadas).
- `TransactionRepository` ganha um método de verificação de uso por categoria, reaproveitável pela futura Fase 6 (Orçamentos, RN-004) se uma restrição semelhante for necessária para categorias associadas a orçamentos.
