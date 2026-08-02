# ADR-0019: Fase 7 (Java) — Metas Financeiras, associação conta/categoria e adiamento da entrega de alertas (RF-032)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | 7 (Java) — RF-030 (CRUD, além do mínimo explícito), RF-031 completo; RF-032 parcial (sinal, entrega adiada) |

## Contexto

Fase 7 cobre RF-030 (criação de metas com valor-alvo, prazo e conta/categoria associada), RF-031 (cálculo e exibição de progresso ao longo do tempo) e RF-032 (notificar ao atingir ou se aproximar de uma meta).

**Mesmo padrão de dependência já resolvido em ADR-0017 (RF-022) e ADR-0018 (RF-028) se repete aqui**: RF-032 exige "notificar o usuário", mas a infraestrutura de notificação (Fase 10, RF-040–043) ainda não existe. Terceira ocorrência do mesmo padrão — resolvida de forma idêntica, por consistência.

## Decisão

### 1. Modelo de domínio

- `Goal`: `id`, `userId`, `name`, `targetAmount` (`BigDecimal`, positivo), `deadline` (`LocalDate`, deve ser futura na criação), `accountId` **ou** `categoryId` (exatamente um dos dois — nunca ambos, nunca nenhum), `progressAlertThresholds` (lista de percentuais, mesmo mecanismo de `Budget.alertThresholds`, padrão `[80, 100]`), `createdAt`.
- **`name` não está listado explicitamente em RF-030** ("valor-alvo, prazo e conta/categoria associada") — incluído porque uma meta sem identificação legível não é utilizável em nenhuma interface real; o próprio vision.md se refere a metas por nome na UC-005 ("meta de reserva de emergência"). Mesma categoria de decisão já sinalizada para a senha mínima de 8 caracteres na Fase 1 — um campo necessário não listado explicitamente, adicionado e declarado, não inventado silenciosamente.
- **`accountId`/`categoryId` são mutuamente exclusivos e imutáveis após a criação** (`InvalidGoalAssociationException` se ambos ou nenhum forem informados) — mesma imutabilidade estrutural já aplicada a `Budget.categoryId`/`periodType` (ADR-0018): trocar o que a meta rastreia tornaria o histórico de progresso ambíguo.
- **RF-030 lista apenas "criação" explicitamente**; esta fase entrega CRUD completo (editar nome/valor-alvo/prazo/limiares; excluir), por consistência com todas as demais entidades do projeto (`Account`, `Category`, `Budget`) e porque a ausência de edição/exclusão seria uma limitação de usabilidade sem justificativa no requisito.

### 2. Cálculo de progresso (RF-031) — dois modos, sem estado persistido

- **Meta associada a uma conta** (`accountId`): progresso = saldo atual da conta (reaproveita `AccountBalanceCalculator`, Fase 3/4.1) em relação ao `targetAmount`. Faz sentido para o caso de uso central do vision.md (UC-005, reserva de emergência = saldo de uma conta específica crescendo até um alvo).
- **Meta associada a uma categoria** (`categoryId`): progresso = soma líquida (receitas − despesas) das transações daquela categoria **desde a criação da meta** até hoje, em relação ao `targetAmount`. Não é uma soma histórica irrestrita — transações anteriores à criação da meta não contam, evitando que uma meta nova "nasça" parcialmente concluída por histórico pré-existente na categoria.
- Ambos os modos reaproveitam a porta `Clock` (introduzida em ADR-0018) e `TransactionRepository.findAllByCategoryIdAndUserId` (já existente).
- Nenhum valor de progresso é persistido — sempre recalculado sob demanda, mesmo racional já aplicado a `Budget` (ADR-0018): a fonte de verdade (transações/saldo) nunca diverge de um snapshot.

### 3. RF-032 — sinal calculado agora, entrega adiada para a Fase 10

Cada meta listada inclui `thresholdsCrossed` (quais `progressAlertThresholds` já foram alcançados) e `achieved` (`currentAmount >= targetAmount`) — o sinal completo de "atingiu ou se aproximou". A entrega (e-mail/push/in-app) fica para a Fase 10, mesma resolução de RF-022 (ADR-0017) e RF-028 (ADR-0018).

### 4. Isolamento multi-tenant (RF-047)

Mesmo padrão já estabelecido: toda leitura/escrita de `Goal` é escopada por `userId` na própria assinatura do repositório. Criar uma meta exige que a conta ou categoria informada pertença ao usuário autenticado.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Permitir meta sem `name` | Inutilizável em qualquer interface real; UC-005 já demonstra que metas são referidas por nome. |
| Permitir meta associada a conta **e** categoria simultaneamente | RF-030 usa "conta/categoria" (uma opção, não uma combinação); os dois modos têm semânticas de cálculo de progresso incompatíveis (saldo vs. soma líquida) sem uma regra de combinação especificada. |
| Progresso de meta por categoria somar todo o histórico da categoria (não só desde a criação da meta) | Uma meta criada sobre uma categoria com histórico extenso "nasceria" parcialmente concluída, o que não corresponde à intenção de acompanhar progresso "ao longo do tempo" (RF-031) a partir da definição da meta. |
| Reaproveitar `BudgetPolicy.assertValidThresholds` diretamente em vez de duplicar em `GoalPolicy` | Acoplaria os agregados `domain.budget` e `domain.goal` sem necessidade — a duplicação da validação (idêntica em espírito à decisão de "regra de três" já registrada para `RefreshToken`/`PasswordResetToken`/`MfaChallenge` no backend TypeScript) é preferível a uma dependência cruzada prematura entre agregados de domínio. |
| Implementar RF-032 com um canal de notificação simplificado já nesta fase | Terceira repetição do mesmo raciocínio de ADR-0017/0018 — sem RF-040–043, seria especulativo. |

## Consequências

- `roadmap.md` mantém RF-032 vinculado à Fase 7 (sinal) e à Fase 10 (entrega), mesmo padrão de RF-022 e RF-028.
- `Goal` segue o mesmo padrão de imutabilidade estrutural + campos editáveis já usado em `Account`, `Category` e `Budget` — consistência arquitetural deliberada entre os quatro agregados financeiros do projeto.
- A Fase 10 (Notificações) agora tem três sinais prontos para consumir quando a infraestrutura de entrega existir: categorização automática (RF-022/Fase 4.4), limiares de orçamento (RF-028) e limiares/conclusão de meta (RF-032).
