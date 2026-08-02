# ADR-0016: Decomposição da Fase 4, resolução da dependência de Categoria (RN-002) e extensão do cálculo de saldo (RN-001)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | 4 (Java) |

## Contexto

`roadmap.md` define a Fase 4 como "Transações (manuais e importação)", cobrindo RF-014 a RF-021 — registro manual, edição/exclusão, recorrência, tags, filtro/busca, importação CSV/OFX, detecção de duplicatas e revisão de importação. Implementar todo esse escopo como uma única entrega violaria o mesmo critério já usado para decompor a Fase 2 (ADR-0006): fases pequenas o suficiente para serem integralmente concluídas antes da próxima começar.

**Um segundo problema, mais crítico, foi identificado nesta análise**: RN-002 do vision.md exige que "uma transação sempre pertence a exatamente uma conta e a exatamente uma categoria". RF-014 (registro manual) lista "categoria" como campo obrigatório. No entanto, categorias (criação, edição, exclusão) são RF-023, formalmente escopo da **Fase 5** — que no roadmap depende da Fase 4. Isso cria uma dependência circular de fato: a Fase 4 não pode satisfazer RN-002 sem que algum conceito de categoria já exista, mas categorias "pertencem" à fase seguinte.

## Decisão

### 1. Decomposição da Fase 4

| Subfase | Escopo | Requisitos |
|---|---|---|
| 4.1 | Transações manuais (criar/editar/excluir), tags, fundação mínima de categoria | RF-014, RF-015, RF-017, RF-025 (parcial — ver abaixo) |
| 4.2 | Transações recorrentes | RF-016 |
| 4.3 | Filtro e busca de transações | RF-018 |
| 4.4 | Importação CSV/OFX, detecção de duplicatas, revisão de importação | RF-019, RF-020, RF-021 |

Justificativa: 4.1 é o pré-requisito estrutural de todas as demais (entidade `Transaction` e persistência precisam existir antes de filtrar, recorrer ou importar). 4.2–4.3 são extensões independentes uma da outra sobre a mesma base. 4.4 é a mais complexa (parsing de arquivo, heurística de duplicata, fluxo de revisão em duas etapas) — deixada por último, mesmo padrão do ADR-0006 (MFA por último dentro da Fase 2).

### 2. Resolução da dependência de Categoria

A Fase 4.1 introduz um conceito **mínimo** de Categoria — apenas o suficiente para RN-002 e RF-014 serem satisfeitos — sem antecipar o escopo completo de RF-023 (CRUD customizado de categorias/subcategorias, que permanece na Fase 5):

- `Category`: `id`, `userId`, `name`, `createdAt`. Sem subcategorias, sem edição, sem exclusão nesta fase.
- **RF-025 (categorias padrão) é resolvido via seed automático e preguiçoso ("lazy")**: a primeira vez que as categorias de um usuário são consultadas (`GET /categories`, ou implicitamente ao criar a primeira transação), um conjunto fixo de categorias padrão é criado automaticamente para aquele usuário, caso ele ainda não tenha nenhuma. Isso satisfaz o requisito funcional (categorias padrão disponíveis, sem esforço do usuário) sem exigir alterar o fluxo de cadastro já aprovado (Fase 1/M1) nem antecipar infraestrutura de eventos assíncronos (vision.md Seção 12, item 5 — fora de escopo do MVP).
- **Fora de escopo desta subfase (permanece RF-023, Fase 5)**: criar, editar, excluir e organizar categorias/subcategorias customizadas. Nesta fase, `GET /categories` é a única rota exposta — suficiente para o cliente popular um seletor de categoria ao registrar uma transação.
- **RF-024 (recategorização manual) é satisfeito incidentalmente por RF-015** (edição geral de uma transação já permite trocar `categoryId`) — não requer um endpoint dedicado; registrado aqui para não ser reaberto como pendência na Fase 5.
- **RF-022 (categorização automática por regras)** permanece integralmente na Fase 5 — não há heurística nem regra automática nesta subfase; toda transação manual exige que o usuário informe a categoria explicitamente.

### 3. Extensão do cálculo de saldo (cumprindo o compromisso do ADR-0014)

O ADR-0014 (Fase 3) registrou que "RF-011/RF-012 serão estendidos (não corrigidos) quando a Fase 4 introduzir transações reais" — este é esse momento:

- `Account.balance` (armazenado) passa a representar exclusivamente o **saldo inicial** (RN-001: "...exceto saldo inicial no momento da criação").
- O **saldo atual** de uma conta (RF-011) passa a ser calculado como `saldoInicial + Σ(transações de receita) − Σ(transações de despesa)` daquela conta, computado sob demanda por `ListAccountsUseCase` e `GetConsolidatedBalanceUseCase` (que passam a depender também de `TransactionRepository`) — nunca armazenado como um campo mutável, preservando RN-001 literalmente ("nunca um valor editável diretamente pelo usuário").
- Uma transação só pode ser criada contra uma conta **não arquivada** do próprio usuário — decisão nova desta fase: permitir lançamentos em uma conta arquivada contradiz o propósito de arquivamento (RF-010/RF-013) como "fechamento" de uma conta.

### 4. Isolamento multi-tenant (RF-047)

Mesmo padrão já estabelecido para `Account` (ADR-0014): toda leitura/escrita de `Transaction`/`Category` é escopada por `userId` na própria assinatura do repositório, não em checagem de aplicação. Uma transação só pode referenciar uma conta e uma categoria do **mesmo** usuário — validado explicitamente no caso de uso de criação (não apenas confiado ao isolamento de leitura).

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Adiantar toda a Fase 5 (RF-022–025) para antes da Fase 4 | Escopo desnecessário — Fase 4.1 só precisa que uma categoria exista, não do CRUD completo nem de categorização automática. |
| Tornar `categoryId` opcional em `Transaction` nesta fase, adicionando a obrigatoriedade só na Fase 5 | Violaria RN-002 diretamente, e re-exigiria migração de dados históricos quando a obrigatoriedade fosse adicionada depois. |
| Semear categorias padrão no `RegisterUserUseCase` (Fase 1/M1) | Reabriria uma fase já aprovada e encerrada sem necessidade real — o seed preguiçoso (on-demand) alcança o mesmo resultado funcional sem esse custo. |
| Implementar a fase inteira (RF-014–021) de uma vez | Repete o erro que a decomposição da Fase 2 (ADR-0006) já corrigiu — quebra o critério de "fase pequena o suficiente para ser integralmente concluída". |

## Consequências

- `roadmap.md` é atualizado para refletir as subfases 4.1–4.4 no lugar de uma única linha "Fase 4", mesmo padrão do ADR-0006.
- `Category` ganha um segundo conjunto de operações (CRUD completo, subcategorias) na Fase 5 — a entidade mínima desta fase deve ser extensível sem *breaking change* de schema (campos adicionados, não removidos/renomeados).
- `AccountController`/`ListAccountsUseCase`/`GetConsolidatedBalanceUseCase` (Fase 3) são estendidos, não reescritos — o contrato de resposta HTTP não muda, apenas o valor de `balance` passa a refletir transações reais.
