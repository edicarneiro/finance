# QA — Revisão: Containerização do Ambiente de Desenvolvimento (Docker Compose + PostgreSQL)

| Campo | Valor |
|---|---|
| Escopo | Infraestrutura transversal — Docker Compose, Dockerfiles, migração do banco de dev de H2 para PostgreSQL (ADR-0026), decorrente de solicitação explícita do stakeholder |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-02 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A decisão de containerizar e migrar para PostgreSQL foi solicitada explicitamente pelo stakeholder e formalizada em [ADR-0026](../adr/0026-containerizacao-dev-postgresql.md) — esta revisão cobre exclusivamente qualidade da implementação, testes e aderência a `rules.md`.

## Checklist de Qualidade

- [x] `docker compose -f docker-compose.dev.yml up --build` sobe os três serviços (`postgres`, `backend`, `frontend`) sem erros — **verificado de verdade**, não apenas revisão de arquivo (ver "Verificação de Execução").
- [x] Backend conecta ao PostgreSQL real, aplica o schema via Hibernate e o `DevDataSeeder` popula usuário + categorias — verificado via login real e consulta a `GET /categories` contra o container.
- [x] Frontend consome a API do backend containerizado com sucesso — verificado (`VITE_API_BASE_URL` corretamente injetada no container, servidor Vite respondendo em `5173`).
- [x] Nenhuma dependência local além de Docker/Docker Compose é exigida no fluxo recomendado — JDK/Maven/Node/Postgres continuam documentados apenas como alternativa (Opção 2), não como pré-requisito.
- [x] `README.md` reflete o novo fluxo, com os dois caminhos (Docker e manual) e o motivo de cada um existir.
- [x] Suíte de testes do backend (446, H2) e do frontend (30, MSW) passam sem alteração de comportamento — a mudança de banco de dev não afetou os testes, por design (ADR-0026, decisão de manter H2 nos testes).
- [x] Idempotência do `DevDataSeeder` contra PostgreSQL real, incluindo após restart de container com dados persistidos — verificada (mesmo `userId`, mesma contagem de categorias antes/depois do restart).

## Verificação de Execução

```
docker compose -f docker-compose.dev.yml up --build -d  → 3 containers healthy/started, sem erros
curl login (dev@financepulse.local) + GET /categories    → 8 categorias, dados reais em PostgreSQL 16.14
docker compose ... restart backend                        → mesmo userId após restart (idempotência confirmada)
curl http://localhost:5173                                 → 200, Vite servindo com HMR
./mvnw test (fora de container)                            → 446 testes, 0 falhas, 0 erros
docker build --target build && docker run ... ./mvnw test → 446 testes, 0 falhas, 0 erros (dentro do container)
docker compose ... exec frontend npm test                  → 30 testes, 0 falhas (dentro do container)
Backend manual (POSTGRES_PORT=5433) + só o postgres do compose → conecta ao mesmo Postgres 16.14, mesmo usuário semeado
```

Todo comando acima foi executado de verdade nesta revisão — nenhuma instrução do README foi documentada sem antes ser validada.

## Achados Durante a Revisão

**1. `DevDataSeederTest` quebrava com a introdução de `application-dev.yml` apontando para PostgreSQL (severidade: alta — suíte de testes completa falhando, corrigido nesta revisão)**

`DevDataSeederTest` usa `@ActiveProfiles("dev")` + `@TestPropertySource` para isolar o teste em H2 em memória, sem depender de PostgreSQL/Docker. Antes desta mudança, `application-dev.properties` já usava H2, então sobrescrever apenas `spring.datasource.url` no teste era suficiente (driver compatível por coincidência). Ao migrar `application-dev.yml` para PostgreSQL (`spring.datasource.driver-class-name: org.postgresql.Driver`), o teste passou a sobrescrever a URL para uma `jdbc:h2:mem:...` **mas herdar o driver do PostgreSQL** — uma combinação inválida (driver Postgres não abre uma URL H2), quebrando os dois testes da classe com erro de inicialização de contexto Spring. **Resolução**: `@TestPropertySource` agora sobrescreve `spring.datasource.driver-class-name`, `username` e `password` junto com a `url`, restaurando o isolamento completo do teste em relação ao perfil `dev`. `mvn clean test` confirmou 446/446 após a correção.

## Avaliação por Critério

**Clean Code**: Dockerfiles e `docker-compose.dev.yml` têm comentários explicando decisões não óbvias (por que a porta do Postgres é remapeada no host, por que o volume do frontend é escopado a `src/`/`index.html`/`vite.config.ts` em vez do diretório inteiro, por que o backend não tem hot-reload) — exatamente o padrão de "comentário explica o porquê" já estabelecido no projeto.

**Segurança**: `.env` (com credenciais reais, ainda que de dev) permanece fora do controle de versão (`.gitignore` já cobria o padrão genérico `.env`); `.env.example` usa valores de exemplo claramente marcados como trocáveis (`change-me-in-dev`, `fp_password`) — mesmo padrão já usado para `FINANCEPULSE_JWT_SECRET`. Nenhum segredo real foi commitado.

**Testes**: a decisão de manter a suíte de testes em H2 (não migrá-la para PostgreSQL/Testcontainers) foi verificada como correta na prática — `mvn test` continua rodando sem Docker de pé, preservando velocidade e determinismo (`rules.md` §3). A suíte não precisou de nenhuma alteração além da correção do Achado 1.

**Infraestrutura**: multi-stage no Dockerfile do backend produz uma imagem final sem Maven/fontes (menor superfície, mais rápida de distribuir) — verifiquei que isso teria impedido rodar testes dentro do container final, por isso o README documenta corretamente o uso do estágio intermediário `build` para esse propósito, não a imagem de runtime.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Sem ferramenta de migração versionada (Flyway/Liquibase)** — decisão explícita e justificada em ADR-0026 (reconstruir o histórico de 13 fases já implementadas é um escopo maior, próprio). Dívida técnica registrada, não uma lacuna silenciosa.
2. **Sem hot-reload do backend em container** — decisão explícita em ADR-0026; documentado no README como trade-off, com o fluxo manual (Opção 2) como alternativa para iteração rápida.
3. **Sem Dockerfile de produção (build estático + Nginx) para o frontend** — fora do escopo desta ADR, que é explicitamente `docker-compose.dev.yml`.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. A decisão arquitetural em si (containerização + PostgreSQL) já está documentada e justificada em ADR-0026, solicitada explicitamente pelo stakeholder. Nenhum registro adicional encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real e severo (suíte de testes inteira falhando por uma combinação inválida de driver/URL introduzida pela migração) foi identificado e corrigido durante esta revisão, confirmado com a suíte completa rodando limpa depois. Toda alegação do README foi verificada com execução real — build das imagens, subida dos três serviços, login contra PostgreSQL real, idempotência do seed através de um restart de container, e testes de ambas as aplicações rodando tanto dentro quanto fora de containers. Nenhum apontamento crítico adicional foi identificado.
