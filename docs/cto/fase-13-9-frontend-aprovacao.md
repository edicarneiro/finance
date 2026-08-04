# CTO — Aprovação Formal da Fase 13.9 (Frontend): Backoffice

| Campo | Valor |
|---|---|
| Fase | 13.9 (Frontend) — Investigação de suporte, suspensão/reativação de conta, log de auditoria |
| Decisão arquitetural nova? | Não — RBAC sem claim de papel no JWT já resolvido pelo ADR-0024 existente; frontend aplica ADR-0025/ADR-0026 |
| Revisão de QA | [fase-13-9-frontend-review.md](../qa/fase-13-9-frontend-review.md) — ✅ Aprovado |
| Decisão | ✅ **Aprovado** |
| Data | 2026-08-03 |

## Escopo entregue

- `backofficeApi.ts`: cliente tipado para `GET /backoffice/users/{userId}`, `POST .../suspend`, `POST .../reactivate`, `GET .../audit-log`, verificado contra `BackofficeController.java` e reaproveitando o tipo `UserDataExport` já definido em `privacyApi.ts` (a resposta de busca é literalmente o mesmo formato — `GetUserForSupportUseCase` reaproveita `ExportUserDataUseCase` internamente).
- `BackofficePage`: busca de usuário por ID, com seções de ações (suspender/reativar com motivo opcional) e log de auditoria, condicionadas ao sucesso da busca.
- Navegação: link "Backoffice" adicionado ao `AppShell`, **sempre visível** para qualquer usuário autenticado — decisão explícita descrita abaixo.
- 6 novos testes de fluxo completo em `BackofficePage.test.tsx` (App real + MSW na fronteira de rede), incluindo os caminhos reais de 403 (sem permissão) e 404 (usuário inexistente). Suíte total: 68 testes, 14 arquivos, 100% passando.
- Verificação manual contra o backend real via Docker Compose, incluindo promoção manual do usuário de dev a `SUPPORT_OPERATOR` via SQL direto (sem endpoint de autopromoção, conforme ADR-0024), um ciclo completo de suspensão/reativação com log de auditoria real, e os erros reais de 403/404.

## Decisão de RBAC sem claim de papel no JWT

Esta fase precisava resolver uma questão de design deixada em aberto desde a Fase 13.1: como o frontend deveria decidir se mostra ou esconde o link de navegação para o Backoffice, dado que `backend-java` (por decisão já registrada no ADR-0024) não inclui o papel (`Role`) do usuário no JWT — a checagem de `SUPPORT_OPERATOR` é feita inteiramente no backend, por consulta ao banco, a cada chamada. A implementação resolve isso mantendo o link **sempre visível** para qualquer usuário autenticado, com o controle de acesso real imposto pela API a cada chamada — a página de busca simplesmente exibe a mensagem de erro real do backend (`403`, "Acesso negado: esta ação exige permissão de operador de suporte.") para quem não tiver a permissão. Esta é a decisão correta e consistente com a arquitetura já estabelecida: o frontend nunca é a fonte de verdade de autorização neste projeto (mesmo princípio já aplicado, por exemplo, ao não replicar validações de negócio do backend em nenhuma tela anterior). Não introduz uma nova decisão arquitetural — apenas aplica o ADR-0024 já existente à camada de apresentação.

## Achado de QA e resolução

O QA identificou uma divergência de fidelidade entre o mock MSW e o comportamento real do backend: `GetUserForSupportUseCase` registra uma entrada de auditoria `VIEWED_USER_DATA` em toda busca de suporte, não só nas ações de suspender/reativar — um detalhe que só a verificação manual contra o backend real revelou, já que a suíte automatizada isolada não tinha como detectar essa divergência sozinha. O mock foi corrigido para espelhar exatamente esse comportamento, e o teste que antes verificava um estado de "log vazio" inatingível na prática foi reescrito para verificar o estado real e alcançável. Endosso a correção — é um lembrete válido de que a disciplina de "verificar contra o backend real" desta squad (ADR-0026) continua a pagar dividendos mesmo em fases onde a suíte automatizada já está 100% verde.

## Parecer do CTO

**Aprovado.** Esta fase encerra a decomposição completa da Fase 13 (13.1–13.9, ver ADR-0025) resolvendo a última questão de design pendente (RBAC sem claim de papel no JWT) de forma consistente com os princípios já estabelecidos em todo o projeto. Escopo fiel ao contrato real do backend, sem invenção de funcionalidade, sem decisão arquitetural pendente de registro, e com um achado de fidelidade do mock corrigido com disciplina. Autorizo a atualização do `roadmap.md` marcando a Fase 13.9 — e, por consequência, a Fase 13 como um todo — como concluída.
