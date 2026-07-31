# ADR-0004: Sequenciamento de entrega — backend antes de frontend

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | Aplica-se ao roadmap.md como um todo (Parte 1 — MVP) |

## Contexto

O vision.md descreve funcionalidades do ponto de vista do usuário final (ex.: RF-001 "o sistema deve permitir que um usuário se cadastre"), sem prescrever se a interface web deve ser entregue simultaneamente a cada funcionalidade de backend. É necessário decidir a ordem de entrega entre API (backend) e interface web (frontend) ao longo das fases do MVP.

## Decisão

O backend (API) de todas as funcionalidades do MVP é implementado primeiro (roadmap.md, Fases 1 a 12), com o frontend web consumindo essa API já estabilizada ao final (roadmap.md, Fase 13).

## Justificativa

- Os contratos de API tendem a mudar mais durante as primeiras iterações de cada funcionalidade; construir UI em paralelo geraria retrabalho a cada ajuste de contrato.
- Cada fase de backend já é validada de ponta a ponta via testes automatizados (TDD) e revisão de QA, o que garante valor verificável mesmo sem interface gráfica.
- Isso é uma decisão de **sequenciamento de entrega**, não de escopo: nenhuma funcionalidade do vision.md deixa de ser construída; a Fase 13 entrega a interface web para todas as funcionalidades já implementadas nas Fases 1–12.

## Consequências

- Até a conclusão da Fase 13, o FinancePulse Engine é acessível apenas via API (validável por testes automatizados e chamadas HTTP diretas), não via navegador.
- Este ADR deve ser revisitado caso o stakeholder priorize validação visual incremental (ex.: demonstrações de produto) antes da Fase 13 — nesse caso, uma fase de UI mínima poderia ser inserida mais cedo no roadmap, mediante aprovação explícita do stakeholder.
