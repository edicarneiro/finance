# CTO — Aprovação da Fase 12 (Java): Multi-tenancy Hardening e Backoffice

| Campo | Valor |
|---|---|
| Fase | 12 (Java) — RF-047, RF-048, RF-049, RF-050 completos (RF-049/050 como versão mínima manual, ver ADR-0024) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

RF-047 (reforço do isolamento multi-tenant já estrutural desde a Fase 3, via suíte de testes consolidada), RF-048 (registro de auditoria append-only para acesso administrativo/backoffice), RF-049 (visualização de dados de um usuário por um operador de suporte, para fins de atendimento) e RF-050 (suspensão e reativação reversível de conta). Conforme delimitado em [ADR-0024](../adr/0024-fase-12-multitenancy-hardening-backoffice.md) e [roadmap.md](../../roadmap.md) — Fase 12. Esta é a última fase de "Parte 1" (MVP do backend) do roadmap.

## Insumos considerados

- [docs/qa/fase-12-java-review.md](../qa/fase-12-java-review.md) — parecer de qualidade do QA: **aprovado**, com um achado real de autorização (`OperatorAuthorization` não verificava suspensão/exclusão do próprio operador) identificado e corrigido durante a própria revisão, com teste de regressão dedicado.
- [ADR-0024](../adr/0024-fase-12-multitenancy-hardening-backoffice.md) — decisão de tratar RF-047 como hardening via suíte de teste (não reescrita de repositórios já corretamente escopados por `userId` desde a Fase 3); decisão de escopar RF-048 apenas a acesso administrativo, não a autoacesso já coberto por RF-047; decisão de RF-049/050 como RBAC mínimo manual, sem painel administrativo nem autopromoção.
- vision.md RF-047 a RF-050, § 16 (texto que sanciona explicitamente "backoffice administrativo avançado... podem ser versão mínima manual no MVP") — base literal das decisões de escopo desta fase.
- ADR-0022 — precedente do padrão "caso de uso compõe caso de uso" (`CheckNotificationsUseCase`), reaplicado aqui em `GetUserForSupportUseCase` sobre `ExportUserDataUseCase`.
- Código-fonte em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue o padrão já estabelecido — `domain/backoffice/` como bounded context próprio (separado de `domain/user/`), coerente com a distinção conceitual entre "quem é o usuário" e "quem pode agir administrativamente sobre ele". `application/usecases/backoffice/` segue a convenção de subpacote por área já usada desde a Fase 6.
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/backoffice/`, `domain/user/` (campos novos incluídos) ou `application/usecases/backoffice/` importa Spring, JPA ou Jackson.
- [x] **`OperatorAuthorization` corretamente modelada como classe package-private, não uma porta pública**: concordo com a leitura do QA — é um detalhe de implementação compartilhado apenas pelos quatro casos de uso do mesmo pacote, e elevá-la a porta/serviço público seria abstração prematura sem consumidor fora deste contexto.
- [x] **`GetUserForSupportUseCase` reaproveitando `ExportUserDataUseCase` em vez de duplicar a agregação**: segunda aplicação do padrão de ADR-0022, justificada pelo mesmo critério — a composição é estritamente somente-leitura (nenhum efeito colateral adicional além do registro de auditoria, que é responsabilidade do caso de uso composto, não do composto internamente).
- [x] **Suspensão corretamente modelada como reversível e distinta de anonimização**: verifiquei que `User.suspend`/`reactivate` não tocam `email`/`name`/`passwordHash`, ao contrário de `anonymize` — a distinção semântica (conta que continua existindo vs. conta apagada) é preservada estruturalmente no domínio, não apenas em comentário.
- [x] **Assimetria `AccountSuspendedException` (403) vs. `InvalidCredentialsException` (401) endossada**: concordo com o raciocínio de ADR-0024 — replicar o padrão anti-enumeração de conta excluída para suspensão seria incorreto, pois uma conta suspensa "existe" de forma legítima aos olhos do próprio titular, que se beneficia de retorno acionável; a checagem ocorre estritamente após a verificação de senha, preservando a garantia anti-enumeração para quem não possui a senha correta.
- [x] **Endosso à correção do Achado 1 do QA** (`OperatorAuthorization` não verificava suspensão/exclusão do próprio operador): concordo que esta era uma falha de autorização real, não apenas teórica — um operador revogado mantinha, na prática, capacidade de agir sobre contas de terceiros durante a janela de validade do token. A correção (`|| operator.isSuspended() || operator.isDeleted()`) fecha a lacuna no nível do estado persistido, e a limitação residual de token stateless remanescente é a mesma classe já aceita em todo o projeto (ADR-0010/0023) — não exijo trabalho adicional nesta fase para resolvê-la.
- [x] **RF-047 corretamente entregue como hardening, não retrabalho**: `MultiTenantIsolationHardeningTest` é o artefato certo para este requisito — consolida em um único teste auditável a verificação de isolamento que já era estrutural, sem reescrever repositórios que já estavam corretos.
- [x] `rules.md` § 3 atendido: `BackofficeControllerTest` e `MultiTenantIsolationHardeningTest` exercitam a raiz de composição real via `@SpringBootTest` + `MockMvc`, incluindo o fluxo ponta a ponta de suspensão bloqueando login real e reativação restaurando-o.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009).
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Ausência de endpoint de promoção a `SUPPORT_OPERATOR` é uma decisão de escopo deliberada (sancionada por vision.md § 16), não uma dívida — promoção continua manual/fora de banda até que um painel administrativo avançado seja priorizado, se algum dia for.
- Janela de até 15 minutos entre a revogação do acesso de um operador (suspensão/exclusão) e a rejeição efetiva de um JWT já emitido — mesma classe de limitação estrutural de token stateless já aceita em todo o projeto (ADR-0010/0023), aqui com raio de ação maior por afetar contas de terceiros; mitigá-la exigiria infraestrutura de revogação de token, fora do escopo desta fase.
- Nenhum requisito impede um operador de suspender a própria conta — comportamento aceito como inofensivo (a própria checagem corrigida nesta fase garante que o operador perde acesso imediatamente após a suspensão ser persistida).
- Consultar o audit log não gera, propositalmente, uma nova entrada de auditoria — decisão registrada em ADR-0024 para evitar ruído recursivo, não uma lacuna de rastreabilidade (a ação de "visualizar dados para suporte" já é auditada; visualizar o próprio log de auditoria não é uma ação sobre dados do usuário-alvo).

## Decisão

**A Fase 12 (Java) está aprovada.** A implementação entrega reforço auditável do isolamento multi-tenant (RF-047), registro de auditoria para acesso administrativo (RF-048), e um mecanismo mínimo e deliberadamente manual de suporte/backoffice (RF-049, RF-050), fiel ao texto do vision.md que sanciona essa versão reduzida para o MVP. O parecer de qualidade do QA foi favorável, destacando que uma falha de autorização real — um operador com acesso revogado mantendo capacidade de agir sobre contas de terceiros — foi identificada e corrigida durante a própria revisão, com teste de regressão dedicado. Não há ajuste adicional exigido pelo CTO.

Esta aprovação encerra **"Parte 1" (MVP) do backend Java** conforme a estrutura do roadmap — RF-047 a RF-050 eram os últimos requisitos funcionais pendentes desse escopo; a Fase 13 (Frontend) inicia uma parte distinta do roadmap.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
