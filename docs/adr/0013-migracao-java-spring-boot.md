# ADR-0013: Migração da stack tecnológica do backend para Java + Spring Boot

| Campo | Valor |
|---|---|
| Status | Aceito — supersede [ADR-0001](0001-stack-tecnologica-backend.md); revisa parcialmente [ADR-0002](0002-arquitetura-hexagonal-backend.md) (mecanismo de composição) e [ADR-0003](0003-persistencia-fase-1.md) (motor de persistência) |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | Migração — Fase 1 (equivalente à Fase 1 original) |

## Contexto

O backend do FinancePulse Engine foi construído em TypeScript/Node.js ao longo de 7 fases (Fase 1 completa + Fase 2 inteira, 2.1–2.5.2), com 263 testes automatizados, Arquitetura Hexagonal, e 12 ADRs registrando decisões técnicas. Por decisão do stakeholder, o backend será migrado para **Java + Spring Boot**. Esta não é uma correção de rumo por problema técnico na stack anterior — é uma mudança de direção explícita e deliberada.

**Custo explicitamente reconhecido**: esta decisão implica reimplementar em Java tudo o que já foi construído, testado e aprovado em TypeScript. Não é uma alteração incremental.

## Decisões

### Linguagem, build e framework

- **Java 17** (LTS), compatível com o JDK já disponível no ambiente. Kotlin não foi escolhido por não ter sido solicitado — Java é a associação mais direta e convencional com "Spring Boot" em contexto corporativo.
- **Maven** como ferramenta de build — convenção mais comum em projetos Spring Boot corporativos, configuração declarativa (XML) com superfície de surpresa menor que um script de build imperativo.
- **Spring Boot 3.x**, que exige Java 17+.

### Persistência: H2 embarcado, não SQLite (revisão do ADR-0003)

O ADR-0003 escolheu SQLite pela ausência de infraestrutura de servidor e baixo atrito de setup. O mesmo racional se aplica em Java, mas o motor concreto muda para **H2 embarcado** (via Spring Data JPA/Hibernate) — suporte de primeira classe no ecossistema Spring, evitando um dialeto Hibernate menos maduro para SQLite. Como na decisão original, a persistência fica isolada atrás de portas (interfaces Java), portanto a troca futura para um banco de produção real (provavelmente PostgreSQL) permanece de baixo custo — a mesma dívida técnica já registrada no ADR-0003 apenas migra de motor, não de natureza.

### Composição de dependências: contêiner do Spring, não composition root manual (revisão do ADR-0002)

O ADR-0002 definiu um composition root manual (`container.ts`) deliberadamente, evitando um framework de DI. Em Spring Boot, lutar contra o contêiner de inversão de controle nativo do framework seria não idiomático e sem benefício real — o contêiner do Spring passa a ser o mecanismo de composição (injeção via construtor, estereótipos `@Component`/`@Service`/`@Repository`). **A regra de dependência da Arquitetura Hexagonal permanece idêntica e inegociável**: `domain` sem qualquer anotação ou dependência de framework; `application` define portas como interfaces Java puras; `adapters` implementam as portas e são o único lugar onde anotações Spring aparecem.

### Segurança e utilitários

- **Hash de senha**: `spring-security-crypto` (`BCryptPasswordEncoder`), como dependência isolada — **não** o starter completo `spring-boot-starter-security`, que ativaria autenticação automática (formulário de login, bloqueio de todas as rotas) incompatível com o mecanismo de autenticação via JWT já desenhado. Equivalente direto ao `bcryptjs` do ADR-0001.
- **JWT**: biblioteca `jjwt` (`io.jsonwebtoken`), equivalente ao `jsonwebtoken` usado em TypeScript.
- **TOTP/MFA** (fase futura de migração): biblioteca `dev.samstevens.totp`, equivalente ao `otplib`.

### Testes: JUnit 5 + dublês escritos à mão, não Mockito

Mantém a mesma filosofia já estabelecida em `rules.md` § 3: testes de domínio/aplicação usam dublês de teste que implementam as portas diretamente (classes `Fake*`/`Sequential*`, mesmo padrão dos testes TypeScript), não uma biblioteca de mock. Testes de adaptadores validam contra a tecnologia real (H2 real, não mock de banco). JUnit 5 como executor, AssertJ para asserções fluentes.

### Estratégia de migração: incremental, fase por fase, mesmos limites já aprovados

A migração **não é um "big bang"**. Cada fase já concluída em TypeScript (Fase 1; 2.1; 2.2; 2.3; 2.4; 2.5.1; 2.5.2) será reimplementada em Java como uma "Fase de Migração" equivalente, seguindo o mesmo ciclo completo (CTO → Full Stack → QA → CTO) e a mesma aprovação por etapa do stakeholder — nenhuma migração de fase começa antes da anterior estar concluída e aprovada, mesmo princípio já usado em todo o projeto.

Todas as regras de negócio (RN), requisitos funcionais (RF) e restrições do vision.md permanecem **idênticas** — esta migração é de implementação, não de escopo de produto. ADRs que registram decisões de domínio/regra de negócio (ex.: ADR-0007 rotação de refresh token, ADR-0009 anti-enumeração, ADR-0010 anonimização) continuam válidos e serão replicados fielmente; apenas os detalhes de biblioteca/linguagem são adaptados.

**O backend TypeScript (`backend/`) é mantido intacto durante toda a migração** — não é apagado nem descontinuado até que a migração completa em Java seja validada e explicitamente aprovada pelo stakeholder. O novo código vive em `backend-java/`, em paralelo.

### Escopo desta primeira fase de migração

Equivalente à Fase 1 original: RF-001 (cadastro), RF-002 (unicidade de e-mail), RF-003 (login), RF-008 (emissão e validação de token de sessão). A rota `GET /auth/me` da Fase 1 original **não é replicada** — ela foi uma rota de exemplo mínima, já removida na Fase 2.2 em favor de `GET /users/me` (ADR-0008). Replicar um artefato histórico já obsoleto no código atual seria desperdício; a validação do token (RF-008) é coberta por teste direto do filtro de autenticação, sem endpoint de negócio descartável.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Kotlin em vez de Java | Não solicitado; Java é a associação padrão com "Spring Boot" sem indicação em contrário. |
| Gradle em vez de Maven | Maven é mais convencional em ambientes corporativos Java e tem configuração puramente declarativa; sem motivo concreto para preferir Gradle aqui. |
| Manter SQLite via driver JDBC | Suporte de segunda classe no ecossistema Spring/Hibernate comparado a H2; sem benefício real sobre H2 para o estágio atual (dev/MVP). |
| `spring-boot-starter-security` completo | Ativaria autenticação automática incompatível com o mecanismo JWT já desenhado; `spring-security-crypto` isolado atende à necessidade real (hash de senha) sem esse acoplamento. |
| Migração "big bang" (tudo de uma vez) | Contraria a disciplina de fases pequenas e totalmente concluíveis já validada pelo stakeholder ao longo de todo o projeto; aumentaria o raio de impacto de qualquer revisão a um nível impraticável. |
| Apagar o backend TypeScript imediatamente | Perderia a referência funcional validada durante a migração incremental, sem necessidade — a remoção é adiada para depois da aprovação final da migração completa. |

## Consequências

- `docs/adr/0001-stack-tecnologica-backend.md` marcado como superado por este ADR (mantido para registro histórico, não apagado).
- Novo diretório `backend-java/`, com sua própria estrutura Maven, testes e documentação técnica (`backend-java/README.md`).
- `roadmap.md` ganha uma trilha de migração espelhando as fases já concluídas.
- Toda decisão de regra de negócio já validada (ADRs 0004 a 0012) permanece a fonte de verdade — a migração adapta biblioteca/linguagem, não requisito.
