# CTO — Aprovação: Containerização do Ambiente de Desenvolvimento (Docker Compose + PostgreSQL)

| Campo | Valor |
|---|---|
| Escopo | Infraestrutura transversal — Docker Compose, Dockerfiles, migração do banco de dev de H2 para PostgreSQL ([ADR-0026](../adr/0026-containerizacao-dev-postgresql.md)) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado |

> Conforme `rules.md` § 7, este é o parecer formal de encerramento. Diferente das fases numeradas do roadmap, esta é uma mudança de infraestrutura transversal solicitada explicitamente pelo stakeholder por escrito, com contexto e critérios de aceite próprios — a decisão arquitetural já está formalizada em [ADR-0026](../adr/0026-containerizacao-dev-postgresql.md), que esta aprovação referencia e endossa.

## Escopo revisado

Containerização completa do ambiente de desenvolvimento (Docker Compose orquestrando `postgres`, `backend`, `frontend`), migração do banco de desenvolvimento de H2 para PostgreSQL 16, `DevDataSeeder` reaproveitado sem alteração, e documentação consolidada no README raiz com dois caminhos (Docker recomendado, manual como alternativa).

## Insumos considerados

- [docs/qa/adr-0026-containerizacao-review.md](../qa/adr-0026-containerizacao-review.md) — parecer de qualidade do QA: **aprovado**, com um achado real de alta severidade (`DevDataSeederTest` quebrando por combinação inválida de driver/URL) identificado e corrigido, com a suíte completa confirmada limpa depois.
- [ADR-0026](../adr/0026-containerizacao-dev-postgresql.md) — decisão formal, com alternativas consideradas e consequências registradas.
- Solicitação explícita e por escrito do stakeholder (`[TASK-CTO] Padronização de Infraestrutura Local e Contêineres`), com contexto, instruções e critérios de aceite próprios.
- Execução real de todos os quatro critérios de aceite listados na solicitação original (ver abaixo).

## Verificação de aderência arquitetural

- [x] **`docker compose -f docker-compose.dev.yml up` sobe o ecossistema completo sem erros** — verificado com build real das duas imagens e subida dos três serviços, incluindo o `depends_on: condition: service_healthy` do Postgres funcionando corretamente (backend só inicia após o healthcheck do banco passar).
- [x] **Backend conecta ao PostgreSQL, aplica schema e disponibiliza a API** — verificado via login real contra o usuário semeado e consulta autenticada a `GET /categories`, confirmando dados persistidos em PostgreSQL 16.14 (não H2).
- [x] **Frontend consome a API do backend containerizado** — verificado (`VITE_API_BASE_URL` corretamente propagada ao container, servidor respondendo).
- [x] **Nenhuma dependência local além de Docker/Docker Compose é exigida** no fluxo recomendado — JDK/Maven/Node/Postgres documentados apenas como alternativa (Opção 2 do README), nunca como pré-requisito do fluxo principal.
- [x] **README reflete o novo fluxo de inicialização** — dois caminhos documentados (Docker recomendado, manual como alternativa para iteração rápida no backend), com todas as instruções verificadas por execução real, não apenas escritas.
- [x] **Decisão de manter H2 nos testes automatizados, não migrar para PostgreSQL/Testcontainers**: endosso — a suíte precisa continuar rápida e determinística sem depender de Docker rodando (`rules.md` §3); o contrato JPA/Hibernate usado no projeto é suficientemente portável entre os dois bancos, confirmado pela suíte de 446 testes passando sem alteração de comportamento.
- [x] **Decisão de adiar Flyway/Liquibase**: endosso — reconstruir o histórico de migração de 13 fases já implementadas e testadas é um escopo comparável a uma fase inteira do roadmap, desproporcional a uma tarefa de containerização. Registrado como dívida técnica explícita em ADR-0026, não uma lacuna silenciosa.
- [x] **Endosso à correção do Achado 1 do QA**: a combinação driver Postgres + URL H2 no teste era um erro real que quebraria a suíte completa — a correção (sobrescrever driver/usuário/senha junto da URL) é mínima e correta.
- [x] Nenhuma funcionalidade de movimentação financeira real introduzida (RN-009) — mudança é inteiramente de infraestrutura de desenvolvimento.
- [x] `vision.md` § 14 atualizado nomeando Docker explicitamente, com justificativa registrada de por que este item difere do resto do documento (tecnologia decidida de fato, não hipotética) — aderente a § 12.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- **Sem migração versionada (Flyway/Liquibase)** — dívida técnica explícita, a ser revisitada antes de qualquer ambiente com dados reais a preservar entre deploys.
- **Sem hot-reload do backend em container** — trade-off aceito; fluxo manual documentado como mitigação para quem itera rápido no backend.
- **Sem Dockerfile de produção (Nginx/build estático) para o frontend** — fora do escopo desta ADR, que é exclusivamente o ambiente de desenvolvimento; deve ser tratado quando houver decisão real de deploy.
- **Porta 5432 do Postgres remapeada para 5433 no host** — motivado por conflito real observado com outro Postgres local já em uso na máquina de desenvolvimento; documentado no compose e no README, não afeta a comunicação interna entre os serviços.

## Decisão

**A containerização do ambiente de desenvolvimento está aprovada.** Todos os quatro critérios de aceite da solicitação original foram verificados com execução real — build das imagens, subida do ecossistema completo, conexão real ao PostgreSQL com dados persistidos e sobrevivendo a um restart de container, consumo do backend pelo frontend, e ausência de dependência local além de Docker no fluxo recomendado. O parecer de qualidade do QA foi favorável, destacando a correção de uma falha real e severa na suíte de testes, confirmada resolvida com a suíte completa (446 testes) passando limpa. As duas decisões de escopo deliberadamente conservadoras (H2 nos testes, adiar Flyway/Liquibase) estão corretamente justificadas e registradas como dívida técnica explícita, não lacunas silenciosas. Não há ajuste adicional exigido pelo CTO.
