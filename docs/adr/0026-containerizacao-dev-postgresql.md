# ADR-0026: Containerização do Ambiente de Desenvolvimento (Docker Compose) e Migração para PostgreSQL

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-08-02 |
| Autor | CTO / Principal Software Architect (decisão solicitada explicitamente pelo stakeholder) |
| Fase | Transversal — infraestrutura de desenvolvimento local, não uma fase numerada do roadmap |

## Contexto

O ambiente de desenvolvimento local, até esta decisão, exigia instalação manual de JDK 25, Maven (ou o wrapper) e Node.js 24 na máquina do desenvolvedor, com o backend rodando sobre H2 em arquivo local (ver README.md, seção "Como Rodar o Projeto Localmente", e SETUP.md — hoje incorporado ao README). Essa decisão foi deliberada: em resposta a uma solicitação anterior de containerizar o ambiente com Docker/PostgreSQL/Redis, o CTO apontou que (a) `vision.md` § 12 não define stack de infraestrutura, e (b) nenhuma dessas tecnologias existia de fato no projeto — introduzi-las sem decisão formal misturaria aspiração com realidade na documentação.

O stakeholder agora solicita explicitamente, por escrito, a transição para um ambiente containerizado com Docker Compose e PostgreSQL, com a justificativa formal de: isolamento de ambiente, eliminação da instalação manual de SDKs na máquina host, e paridade entre Dev/QA/Produção. Isso resolve a ambiguidade anterior — a tecnologia deixa de ser hipotética e passa a ser uma decisão real, com esta ADR como registro formal.

## Decisão

### 1. PostgreSQL substitui H2 como banco de desenvolvimento — H2 permanece exclusivo dos testes automatizados

- `docker-compose.dev.yml` sobe um serviço `postgres` (`postgres:16-alpine`), com dados persistidos em volume nomeado (`postgres_data`).
- `application-dev.yml` (novo, substitui `application-dev.properties`) aponta para `jdbc:postgresql://postgres:5432/${POSTGRES_DB}`, com driver `org.postgresql.Driver`. Dependência `org.postgresql:postgresql` adicionada ao `pom.xml` (`runtime` scope).
- **Testes automatizados continuam em H2 em memória** (`src/test/resources/application.properties`, inalterado) — decisão deliberada, não uma inconsistência: a suíte de testes precisa ser rápida, determinística e não pode depender de um contêiner Docker estar de pé para `mvn test` funcionar (isso quebraria a experiência de qualquer IDE/CI que rode testes sem orquestrar containers). O contrato SQL relevante (JPA/Hibernate, sem SQL nativo específico de dialeto nas 446 suítes existentes) é portável entre H2 e PostgreSQL o suficiente para este projeto — verificado ao rodar a suíte completa sem alterações após a migração.

### 2. Schema continua via Hibernate `ddl-auto=update` — Flyway/Liquibase explicitamente adiado

- A instrução original permitia `update` OU uma ferramenta de migração dedicada. Optei por **manter `ddl-auto=update`**: introduzir Flyway/Liquibase agora exigiria reconstruir retroativamente o histórico de migração de 12 fases já implementadas e testadas (dezenas de tabelas/colunas), com risco real de divergência silenciosa entre o schema gerado por migração manual e o schema já validado pelos 446 testes existentes — trabalho de escopo comparável a uma fase inteira do roadmap, não uma tarefa de infraestrutura pontual.
- **Registrado como dívida técnica explícita, não como lacuna silenciosa** (ver Consequências): quando o projeto precisar de controle fino sobre migração (ex.: alterações de schema com dados em produção, rollback controlado), Flyway/Liquibase deve ser adotado via uma ADR própria, com o trabalho de retroconstrução do histórico tratado como seu próprio item de escopo.

### 3. Seed de dados de desenvolvimento — reaproveita `DevDataSeeder` já existente, sem alteração

- `DevDataSeeder` (`@Profile("dev")`, idempotente) já reaproveita `RegisterUserUseCase`/`ListCategoriesUseCase` reais via JPA — nenhuma linha SQL específica de H2. Funciona sem alteração contra PostgreSQL, verificado na validação desta ADR.

### 4. Containers de desenvolvimento — hot-reload no frontend, rebuild explícito no backend

- **Frontend**: `Dockerfile` roda `npm run dev -- --host 0.0.0.0` (servidor de desenvolvimento Vite real, não build estático servido por Nginx) — `docker-compose.dev.yml` faz bind-mount do código-fonte, preservando Hot Module Reload. Um `Dockerfile` de produção (build estático + Nginx) fica fora do escopo desta ADR — este é explicitamente `docker-compose.dev.yml`, não um pipeline de deploy.
- **Backend**: `Dockerfile` multi-stage (build via Maven Wrapper + JDK 25, runtime em JRE 25 Alpine) empacota um JAR — sem hot-reload configurado (Spring DevTools + bind mount + recompilação incremental em container é uma escala de complexidade maior, não solicitada explicitamente e não crítica dado que o fluxo de trabalho Java já usa `./mvnw spring-boot:run` local para iteração rápida). Alterações no backend exigem `docker compose up --build`. Registrado como limitação conhecida, não uma omissão silenciosa.

### 5. `vision.md` passa a nomear Docker explicitamente no item de infraestrutura de dev (Seção 14)

Diferente da decisão anterior (manter vision.md neutro quanto a tecnologia, registrada quando a containerização ainda era hipotética), agora que a decisão é real e formalizada nesta ADR, o item de escopo do MVP é atualizado para refletir a tecnologia efetivamente adotada — mantendo o restante do documento neutro quanto a stack, conforme § 12.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Manter H2 e não containerizar (posição anterior) | Superada por decisão explícita e justificada do stakeholder — isolamento de ambiente e paridade Dev/QA/Produção são objetivos legítimos que H2-local não atende. |
| Introduzir Flyway/Liquibase imediatamente | Exigiria reconstruir o histórico de migração de 12 fases já implementadas sem ferramenta de migração — risco de divergência de schema desproporcional ao pedido original (containerização), tratado como item de escopo futuro e próprio. |
| Migrar também os testes automatizados para PostgreSQL (via Testcontainers, por exemplo) | Tornaria `mvn test` dependente de Docker rodando — quebra o fluxo de teste rápido/determinístico já estabelecido (rules.md §3) sem ganho correspondente, já que o contrato JPA/Hibernate usado no projeto é portável entre os dois bancos. Pode ser revisitado se surgir uma necessidade real de validar comportamento específico do PostgreSQL. |
| Dockerfile de frontend com build estático + Nginx (produção) para o compose de dev | `docker-compose.dev.yml` é explicitamente um ambiente de desenvolvimento — hot-reload via `npm run dev` tem mais valor que uma imagem "parecida com produção" nesta fase. Um Dockerfile de produção é um artefato separado, fora deste escopo. |
| Hot-reload completo do backend em container (Spring DevTools + bind mount) | Complexidade adicional não solicitada; o fluxo local via `./mvnw spring-boot:run` já cobre iteração rápida sem Docker. |

## Consequências

- **Nenhuma dependência local além de Docker/Docker Compose é necessária para subir o ambiente completo** — JDK, Maven e Node deixam de ser pré-requisitos obrigatórios (continuam documentados como alternativa para quem prefere rodar sem contêiner).
- **Dívida técnica registrada**: ausência de ferramenta de migração versionada (Flyway/Liquibase) — schema gerenciado por `ddl-auto=update`, aceitável para o estágio atual do projeto (sem produção real, sem dados a preservar em migração controlada), mas a ser revisitado antes de qualquer deploy real.
- **Backend não tem hot-reload em container** — desenvolvedores que iteram frequentemente no backend podem preferir rodar `./mvnw spring-boot:run` localmente (fora do Docker) e usar o compose apenas para o PostgreSQL; ambos os fluxos continuam documentados.
- `README.md` passa a apresentar o fluxo Docker Compose como o caminho recomendado, mantendo o fluxo manual documentado como alternativa.
