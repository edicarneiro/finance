# CTO — Aprovação Formal da Fase 13.7 (Frontend): Notificações e Preferências

| Campo | Valor |
|---|---|
| Fase | 13.7 (Frontend) — Notificações e preferências |
| Decisão arquitetural nova? | Não — aplica integralmente ADR-0025 (React+TS+Vite+Router+TanStack Query+RHF+Zod+CSS Modules+Vitest+RTL+MSW) e ADR-0026 (Docker Compose dev) |
| Revisão de QA | [fase-13-7-frontend-review.md](../qa/fase-13-7-frontend-review.md) — ✅ Aprovado |
| Decisão | ✅ **Aprovado** |
| Data | 2026-08-03 |

## Escopo entregue

- `notificationsApi.ts`: cliente tipado para `GET/PUT /notification-preferences`, `POST /notifications/check`, `GET /notifications`, `PUT /notifications/{id}/read`, com tipos e enums (`AlertType`, `NotificationChannel`) verificados linha a linha contra o backend real (`AlertType.java`, `NotificationChannel.java`, e os DTOs do `NotificationController`).
- `NotificationsPage`: três seções independentes — matriz de preferências (3 tipos de alerta × 2 canais) com salvar, verificação de notificações sob demanda com estado vazio, e lista de notificações com filtro de não lidas e ação de marcar como lida.
- Navegação: link "Notificações" adicionado ao `AppShell`, rota registrada em `App.tsx`.
- 6 novos testes de fluxo completo (App real + MSW na fronteira de rede), incluindo um teste de regressão para o achado de QA descrito abaixo. Suíte total: 55 testes, 12 arquivos, 100% passando.
- Verificação manual contra o backend real via Docker Compose, incluindo um cenário de ponta a ponta de geração de notificação por orçamento estourado (duas notificações reais, mensagens geradas pelo servidor, sem nenhuma construção de texto no cliente).

## Avaliação arquitetural

Nenhuma decisão nova. A fase reforça um padrão já estabelecido pelo ADR-0025 (tela como composição de seções independentes, cada uma com sua própria query/mutação) e não introduz nenhuma dependência, biblioteca ou padrão de comunicação novo.

## Achado de QA e resolução

O QA identificou um bug real de produto: o estado local dos toggles de preferência era resincronizado a cada refetch de fundo do TanStack Query (ex.: o navegador reganhando foco), descartando silenciosamente alterações não salvas do usuário. Este é exatamente o tipo de achado que valida o processo de QA desta squad — um defeito que só se manifesta sob uma condição de corrida entre comportamento padrão de uma biblioteca (`refetchOnWindowFocus: true`) e estado local de formulário, invisível em uma inspeção superficial do código ou em testes que não exercitam refetches. A correção (guarda de sincronização única via `useRef`) é mínima, local, e documentada em `frontend/README.md` como um padrão geral a aplicar em qualquer tela futura que sincronize estado local editável a partir de uma `useQuery`. Endosso a correção e a generalização do aprendizado.

## Parecer do CTO

**Aprovado.** Escopo fiel ao contrato real do backend, sem invenção de funcionalidade, sem decisão arquitetural pendente de registro, e com um achado de QA genuíno corrigido com disciplina (teste de regressão determinístico, não dependente de simulação frágil de foco do navegador). Autorizo a atualização do `roadmap.md` marcando a Fase 13.7 como concluída e o início da Fase 13.8 (Privacidade/LGPD).
