# QA — Revisão da Fase 13.9 (Frontend): Backoffice

| Campo | Valor |
|---|---|
| Fase | 13.9 (Frontend) — Investigação de suporte, suspensão/reativação de conta e log de auditoria, consumindo `BackofficeController` |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-03 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A decisão de RBAC sem claim de papel no JWT (nav link sempre visível, controle de acesso imposto pelo backend a cada chamada) já está coberta pelo ADR-0024 existente — nenhum ADR novo foi produzido, mesmo padrão de 13.2–13.8.

## Checklist de Qualidade

- [x] A implementação atende ao escopo real do backend: busca de usuário por ID (`GET /backoffice/users/{userId}`), suspensão e reativação de conta com motivo opcional (`POST .../suspend`, `POST .../reactivate`), e log de auditoria da conta consultada (`GET .../audit-log`).
- [x] Nenhuma regra de negócio duplicada no cliente — o frontend não tenta adivinhar ou cachear localmente se o usuário logado é um operador de suporte; deixa o backend impor a permissão a cada chamada e exibe a mensagem real de erro.
- [x] Testes cobrem: erro real de 403 (usuário sem permissão de operador), erro real de 404 (usuário-alvo inexistente), busca bem-sucedida com resumo de dados, suspensão refletida no log de auditoria, reativação, e o achado de fidelidade do mock descrito abaixo (6 testes em `BackofficePage.test.tsx`). Suíte total: 68 testes, 14 arquivos, 100% passando.
- [x] Aderente à cláusula de frontend de `rules.md` §3: `BackofficePage.test.tsx` renderiza o `App` real com MSW na fronteira de rede.
- [x] Sem degradação de performance evidente (bundle: 418,86 KB / 123,52 KB gzip).
- [x] Código segue Clean Code e SOLID (ver Achado abaixo — corrigido durante a revisão).
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada, incluindo a decisão de RBAC e o achado de fidelidade do mock.
- [x] Não há introdução de funcionalidade fora do escopo — nenhuma tentativa de esconder o link de navegação "Backoffice" com base em um papel que o frontend não tem como conhecer de antemão (decisão deliberada, ver README).

## Verificação de Execução

```
npm test    → 68 testes, 100% passando (Vitest, jsdom, MSW), 2 execuções consecutivas estáveis
npm run build → sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos
```

Verificação manual adicional contra o backend real, via `docker compose -f docker-compose.dev.yml up`: como `backend-java` não expõe endpoint de autopromoção (ADR-0024), o usuário de dev foi promovido a `SUPPORT_OPERATOR` diretamente via `docker exec ... psql ... UPDATE users SET role = 'SUPPORT_OPERATOR' ...`, um segundo usuário foi registrado como alvo, e o fluxo completo foi exercitado: busca real do usuário-alvo (confirmando a forma exata do JSON, idêntica à de `GET /privacy/export`), suspensão real com motivo, log de auditoria real (confirmando ordem decrescente por data e a entrada `VIEWED_USER_DATA` automática da própria busca), reativação real, e o `403`/`404` reais de um usuário sem permissão e de um ID inexistente. O papel do usuário de dev foi restaurado para `CUSTOMER` ao final.

## Achados Durante a Revisão

**1. O mock MSW de busca de usuário não registrava a entrada `VIEWED_USER_DATA` que o backend real sempre gera, tornando o teste do estado "log vazio" irrealista (severidade: baixa/média — fidelidade do mock, não um bug do código de produção; corrigido nesta revisão)**

A verificação manual contra o backend real revelou que `GetUserForSupportUseCase` registra uma entrada de auditoria `VIEWED_USER_DATA` em **toda** chamada de busca de suporte (RF-048), não só nas ações de suspender/reativar — consultar os dados de um usuário por si só já é uma ação auditável. O mock inicial de `GET /backoffice/users/:userId` em `test/server.ts` não fazia isso, o que permitiu escrever (e passar) um teste que verificava um estado de "log vazio" logo após uma busca bem-sucedida — um estado que, no sistema real, é inalcançável por esse caminho: a própria busca que revela a seção do log já gerou uma entrada nele momentos antes. Isso não é um bug no código do frontend (que apenas exibe o que o backend retorna), mas um mock que divergia do comportamento real documentado, mascarando como a tela realmente se comporta em produção. **Descoberto** durante a verificação manual contra o backend real (não pela suíte automatizada isoladamente) — reforça, mais uma vez nesta fase 13, o valor de rodar contra `backend-java` de verdade, não só contra o MSW. **Resolução**: o mock foi corrigido para espelhar exatamente o comportamento real (a própria busca já insere a entrada `VIEWED_USER_DATA`), e o teste foi reescrito para verificar essa entrada em vez de um estado vazio inatingível na prática.

## Avaliação por Critério

**Clean Code**: `BackofficePage` reaproveita o tipo `UserDataExport` já definido em `privacyApi.ts` para a resposta de `GET /backoffice/users/{userId}` — mesma forma exata de dado (o backend literalmente reaproveita `ExportUserDataUseCase` internamente), evitando duplicar a definição de tipo.

**SOLID/estrutura**: mesma decomposição em seções independentes já estabelecida em `NotificationsPage`/`PrivacyPage` (`UserLookupSection`, `ActionsSection`, `AuditLogSection`), cada uma isolando sua própria responsabilidade.

**Testes**: a cobertura do caminho de erro (403 real de falta de permissão, 404 real de usuário inexistente) é particularmente valiosa nesta fase — é exatamente o tipo de tela onde um controle de acesso mal implementado teria maior impacto, e ambos os caminhos foram verificados tanto com MSW quanto contra o backend real.

**Segurança**: a decisão de manter o link "Backoffice" sempre visível no `AppShell`, com o controle de acesso real imposto pelo backend a cada chamada (não replicado ou antecipado no cliente), é a escolha correta dado que `backend-java` não expõe o papel do usuário no JWT (ADR-0024) — qualquer tentativa de esconder o link no cliente com base em uma suposição local seria, na melhor das hipóteses, cosmética (um usuário mal-intencionado ainda chamaria a API diretamente) e, na pior, uma falsa sensação de segurança. A mensagem de erro exibida é literalmente a resposta real do backend, sem reformulação que pudesse vazar ou esconder informação de forma inconsistente com a política real do sistema.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. Sem paginação na lista de log de auditoria — não é uma capacidade que o backend expõe (`GetAuditLogUseCase` retorna a lista completa), então não seria uma correção do frontend; um volume grande de entradas é uma consideração de escala futura, não desta fase.
2. Promoção a `SUPPORT_OPERATOR` continua manual/fora de banda (sem tela de administração de papéis) — decisão de escopo já registrada no ADR-0024 para o MVP, não uma lacuna desta fase.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** A decisão de RBAC sem claim de papel no JWT foi resolvida de forma consistente com a disciplina já estabelecida no projeto (o backend é a única fonte de verdade de autorização). Um achado real de fidelidade do mock — encontrado apenas ao verificar contra o backend real, não pela suíte automatizada isoladamente — foi identificado e corrigido, com o teste correspondente reescrito para refletir um estado genuinamente alcançável em produção, em vez de um estado artificial que o mock antigo permitia. Nenhum apontamento crítico adicional foi identificado.
