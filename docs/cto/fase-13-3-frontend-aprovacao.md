# CTO — Aprovação da Fase 13.3 (Frontend): Transações

| Campo | Valor |
|---|---|
| Fase | 13.3 (Frontend) — CRUD de transações, consumindo `TransactionController` |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado |

> Conforme `rules.md` § 7, este é o parecer formal de encerramento de fase. **Nenhum novo ADR foi produzido** — ADR-0025 já cobre a arquitetura de toda a Fase 13, e 13.3 não introduziu nenhuma decisão arquiteturalmente significativa, mesmo raciocínio já registrado na aprovação de 13.2.

## Escopo revisado

CRUD de transações (criação, listagem por conta, edição, exclusão), consumindo `TransactionController` real de `backend-java`. Conforme [roadmap.md](../../roadmap.md) — Fase 13.3.

## Insumos considerados

- [docs/qa/fase-13-3-frontend-review.md](../qa/fase-13-3-frontend-review.md) — parecer de qualidade do QA: **aprovado**, com um achado real de infraestrutura de teste (cache do TanStack Query vazando entre testes) identificado e corrigido, com estabilidade confirmada por múltiplas execuções consecutivas.
- [ADR-0025](../adr/0025-decomposicao-fase-13-frontend.md) — arquitetura e stack já definidas, integralmente reaplicadas.
- Contrato real de `TransactionController` (`backend-java`), incluindo a limitação de que a listagem é **sempre por conta** (`GET /transactions?accountId=`) — RF-018 (filtro/busca geral) é a Fase 4.3 do backend, ainda não construída, verificado diretamente no código-fonte antes da implementação.
- Código-fonte em `frontend/src/`.

## Verificação de aderência arquitetural

- [x] Nenhum padrão arquitetural novo introduzido — `transactionsApi.ts` segue o mesmo formato de `accountsApi.ts`/`categoriesApi.ts`.
- [x] **UI corretamente reflete a limitação real da API, sem simular capacidade inexistente**: a página oferece um seletor de conta (usando `?accountId=` como parâmetro de URL) em vez de fingir um filtro/busca geral que RF-018 ainda não entrega no backend — mesma disciplina de "não apresentar funcionalidade que não funciona" já aplicada em fases anteriores (ADR-0022 para RF-043, ADR-0025 para RF-004/005/006).
- [x] **Valor da transação corretamente tratado como sempre positivo, com direção vinda de `type`**: verificado que o frontend nunca envia um valor negativo para simular despesa, espelhando `TransactionPolicy.assertPositiveAmount` do backend sem duplicá-la — a validação Zod (`.positive()`) é só uma antecipação de feedback.
- [x] **Endosso à correção do Achado 1 do QA (cache de query vazando entre testes)**: concordo que, embora não seja um bug de produção, é uma correção estruturalmente correta — extrair `queryClient` para um módulo próprio, limpável entre testes, é a forma padrão de resolver esse problema em aplicações que usam TanStack Query, e a suíte de testes é um artefato de confiança tão crítico quanto o código de produção para o restante do projeto.
- [x] Nenhuma regra de negócio duplicada no cliente.
- [x] Equivalente de frontend a `rules.md` §3 atendido.
- [x] Nenhuma funcionalidade de movimentação financeira real introduzida (RN-009) — transações no MVP são apenas registro/categorização, nunca execução de pagamento.
- [x] Nenhum desvio arquitetural registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Sem filtro/busca de transações (RF-018) e sem transações recorrentes/importação (RF-016, RF-019–022) — pendências do próprio backend (Fases 4.2–4.4, não construídas), não desta fase.
- A correção de cache de teste (Achado 1) reduz risco de falhas intermitentes não diagnosticadas em todas as subfases seguintes (13.4–13.9), que vão continuar adicionando testes ao mesmo conjunto de arquivos.

## Decisão

**A Fase 13.3 (Frontend) está aprovada.** O CRUD de transações reflete fielmente a superfície real do backend, incluindo sua limitação atual de listagem por conta, sem inventar funcionalidade inexistente. O parecer de qualidade do QA foi favorável, destacando a correção de uma fonte real de falhas intermitentes na suíte de testes — confirmada estável após a correção. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7, esta aprovação encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de aprovação explícita adicional em separado.
