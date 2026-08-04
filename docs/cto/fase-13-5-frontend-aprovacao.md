# CTO — Aprovação da Fase 13.5 (Frontend): Dashboard e Pulse Score

| Campo | Valor |
|---|---|
| Fase | 13.5 (Frontend) — Dashboard e Pulse Score, consumindo `DashboardController` |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado |

> Conforme `rules.md` § 7, este é o parecer formal de encerramento de fase. **Nenhum novo ADR foi produzido** — ADR-0025 já cobre a arquitetura de toda a Fase 13, e 13.5 não introduziu nenhuma decisão arquiteturalmente significativa, mesmo raciocínio já registrado nas aprovações de 13.2–13.4.

## Escopo revisado

`DashboardPage` — saldo consolidado, fluxo de caixa por janela de dias, gastos por categoria, Pulse Score com fatores explicáveis e sua ressalva de fórmula provisória (RN-006), e histórico do Pulse Score — consumindo `DashboardController` real de `backend-java`. Substitui o placeholder `HomePage` na rota `/`. Conforme [roadmap.md](../../roadmap.md) — Fase 13.5.

## Insumos considerados

- [docs/qa/fase-13-5-frontend-review.md](../qa/fase-13-5-frontend-review.md) — parecer de qualidade do QA: **aprovado**, com dois achados reais identificados e corrigidos (impacto na suíte de testes de fases anteriores; escopo compartilhado de um controle de UI sem indicação visual).
- [ADR-0025](../adr/0025-decomposicao-fase-13-frontend.md) — arquitetura e stack já definidas, integralmente reaplicadas.
- Contrato real de `DashboardController`/`GetDashboardUseCase` (`backend-java`), incluindo a confirmação de que `spendingByCategory` e `cashFlow` compartilham a mesma janela de dias — verificado diretamente no código-fonte do backend antes de aceitar a correção do Achado 2 do QA.
- Código-fonte em `frontend/src/`.

## Verificação de aderência arquitetural

- [x] Nenhum padrão arquitetural novo introduzido — `dashboardApi.ts` segue o formato já estabelecido; `DashboardPage` é somente leitura, sem introduzir um padrão de mutação desnecessário.
- [x] **Fórmula provisória do Pulse Score corretamente exibida com ressalva, não escondida nem apresentada como definitiva** — verificado que o texto exibido (`formulaVersion` + nota sobre RN-006) reflete exatamente a mesma pendência já documentada no backend, sem inventar uma explicação adicional.
- [x] **Endosso à correção do Achado 1 do QA (rota `/` mudando de dono quebrando testes de outras fases)**: concordo que era um risco real e não óbvio — qualquer mudança de rota raiz em uma SPA autenticada tem esse potencial de efeito cascata sobre testes de fluxos que passam por ali incidentalmente; a correção (handlers MSW padrão sempre ativos, não escopados aos testes da própria fase) é o padrão correto para este tipo de rota compartilhada.
- [x] **Endosso à correção do Achado 2 do QA (escopo compartilhado do seletor de período)**: a verificação contra o código-fonte real do backend (`GetDashboardUseCase`) antes de aceitar a correção evita uma correção especulativa — o QA confirmou o comportamento real, não presumiu.
- [x] **Decisão de não introduzir biblioteca de gráficos**: endosso — não há uma decisão de charting registrada em nenhuma ADR; introduzir uma dependência nova para isso exigiria decisão própria do CTO, corretamente não tomada de forma unilateral pelo Full Stack nesta fase.
- [x] Nenhuma regra de negócio duplicada no cliente.
- [x] Equivalente de frontend a `rules.md` §3 atendido.
- [x] Nenhuma funcionalidade de movimentação financeira real introduzida (RN-009) — dashboard é inteiramente de leitura/visualização.
- [x] Nenhum desvio arquitetural registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Sem biblioteca de gráficos — histórico e gastos por categoria como lista, não visualização gráfica. Não é uma dívida técnica: é uma decisão de escopo consciente, documentada, revisitável em uma fase própria se o produto priorizar isso.
- `formatCurrency` duplicada entre páginas de fases diferentes — dívida de estilo menor já registrada em 13.4, não agravada nesta fase.
- Esta fase encerra a superfície de leitura mais crítica do MVP (Dashboard) — as subfases restantes (13.6–13.9: Relatórios, Notificações, Privacidade, Backoffice) são todas de escopo mais contido e não bloqueiam o uso funcional do produto por um usuário final típico.

## Decisão

**A Fase 13.5 (Frontend) está aprovada.** O Dashboard substitui corretamente o placeholder da Fase 13.1, com o Pulse Score exibido de forma transparente quanto à sua natureza provisória (RN-006). O parecer de qualidade do QA foi favorável, destacando dois achados reais — um de risco à suíte de testes de fases anteriores (mitigado com handlers MSW padrão sempre ativos) e um de UX genuína (escopo de controle não indicado visualmente, verificado contra o backend real antes da correção). Toda alegação de comportamento foi verificada com execução real contra `backend-java` via Docker Compose, incluindo dados reais lançados refletindo corretamente no saldo, fluxo de caixa e Pulse Score. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7, esta aprovação encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de aprovação explícita adicional em separado.
