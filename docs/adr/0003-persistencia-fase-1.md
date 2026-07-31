# ADR-0003: Estratégia de persistência — Fase 1

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 1 |

## Contexto

RF-001/RF-002 exigem persistir usuários cadastrados e garantir unicidade de e-mail. É necessário decidir a tecnologia de persistência para a Fase 1, sem antecipar decisões de infraestrutura de produção (ex.: provedor de nuvem, motor de banco definitivo), que não são requisitos desta fase.

## Decisão

- Adaptador de persistência da Fase 1: **SQLite embarcado** (`better-sqlite3`), com arquivo local de banco de dados.
- O acesso ao banco ocorre exclusivamente através do adaptador `adapters/out/persistence/SqliteUserRepository.ts`, que implementa a porta `application/ports/UserRepository.ts`.
- Para os testes automatizados de use case, é usado um `InMemoryUserRepository` (também implementando a mesma porta), evitando I/O real nos testes unitários, conforme `rules.md` § 3.
- A unicidade de e-mail (RF-002) é garantida em duas camadas: constraint `UNIQUE` no schema SQLite (garantia estrutural) e verificação explícita no use case (mensagem de domínio clara via `DuplicateEmailError`).

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| PostgreSQL desde a Fase 1 | Requer infraestrutura de servidor de banco de dados, que ainda não foi definida (decisão de deployment/produção fora do escopo desta fase). Como a Arquitetura Hexagonal isola o adaptador de persistência atrás de uma porta, a migração para PostgreSQL em fase futura não exige alteração de domínio ou aplicação — apenas um novo adaptador. |
| Apenas em memória (sem persistência real) | Não atenderia ao requisito funcional de cadastro persistente entre reinicializações, mesmo em ambiente de desenvolvimento. |

## Consequências

- **Dívida técnica assumida conscientemente**: o adaptador SQLite é adequado para desenvolvimento e validação do MVP, mas a decisão de motor de banco de dados para produção (ex.: PostgreSQL gerenciado) ainda precisa ser tomada em ADR futuro, antes de qualquer fase de deployment. Este ADR não decide isso — apenas garante que a troca futura seja de baixo custo, por estar isolada atrás da porta `UserRepository`.
- Todas as fases futuras que exigirem persistência (contas, transações, etc.) seguem o mesmo padrão: porta definida em `application/ports/`, adaptador SQLite em `adapters/out/persistence/`.
