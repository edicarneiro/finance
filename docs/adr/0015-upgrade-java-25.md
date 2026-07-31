# ADR-0015: Atualização do backend Java para Java 25 e Spring Boot 3.5.4

| Campo | Valor |
|---|---|
| Status | Aceito — revisa a escolha de versão do Java em [ADR-0013](0013-migracao-java-spring-boot.md) (que permanece válido em tudo o mais: Maven, H2, `spring-security-crypto` isolado, `jjwt`, JUnit 5 + dublês) |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | Trilha de Migração / Fase 3 (Java) — decisão de infraestrutura, não vinculada a um requisito funcional específico |

## Contexto

Por decisão do stakeholder, o backend Java passa a usar **Java 25** em vez do Java 17 (LTS) originalmente escolhido em ADR-0013. JDK 25 (LTS) já está disponível no ambiente de desenvolvimento (`C:\Users\edime\.jdks\jdk-25.0.2`).

Esta decisão foi tomada em meio a um incidente: uma ferramenta externa de modernização de código ("App Modernization for Java", sessão `20260731135726` registrada em `backend-java/.github/modernize/java-upgrade/`) havia iniciado, sem coordenação com este processo, uma tentativa própria de subir o projeto para Java 25, e no processo interferiu no estado do repositório (auto-stash que removeu temporariamente o código-fonte e artefatos de fase da árvore de trabalho — recuperados integralmente a partir do próprio stash, sem perda). Essa ferramenta não chegou a concluir a mudança (sua própria etapa de upgrade falhou, pois o projeto Maven ficou temporariamente irreconhecível para ela). A atualização abaixo foi então executada por este processo (CTO → Full Stack), não pela ferramenta externa.

## Decisão

- **`java.version` em `backend-java/pom.xml`: `17` → `25`.**
- **Versão do Spring Boot (`spring-boot-starter-parent`): `3.3.4` → `3.5.4`.** Necessário porque o ASM empacotado no Spring Framework 3.3.x não reconhece o major version 69 do bytecode do Java 25 (`ClassFormatException: Unsupported class file major version 69`) — falha observada em `@SpringBootTest` ao escanear classes de teste já compiladas para Java 25 em busca da classe de configuração. A versão 3.5.4 resolve corretamente as dependências e passa em toda a suíte sem esse erro.
- Toda a suíte de testes (36 testes) foi reexecutada com JDK 25 + Spring Boot 3.5.4 e passou integralmente, sem nenhuma alteração de código de produção ou de teste — confirma que a mudança é puramente de toolchain/runtime, sem impacto funcional.
- As demais decisões de ADR-0013 permanecem válidas: Maven como build tool, H2 embarcado, `spring-security-crypto` isolado (não o starter completo de segurança), `jjwt`, JUnit 5 + AssertJ com dublês escritos à mão.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Permanecer em Java 17 | Rejeitada por decisão explícita do stakeholder. |
| Deixar a ferramenta externa de modernização concluir a mudança | A tentativa dela já havia falhado (projeto Maven não reconhecido após seu próprio auto-stash) e seu escopo (`.github/modernize/java-upgrade/`) não segue o processo de governança deste projeto (ADRs, ciclo CTO → Full Stack → QA → CTO). A atualização foi refeita dentro do processo estabelecido. |
| Manter Spring Boot 3.3.4 e apenas trocar o JDK | Falha comprovada empiricamente (`Unsupported class file major version 69` no scanner ASM de `@SpringBootTest`) — não é uma opção viável. |

## Consequências

- `backend-java/README.md` atualizado para refletir Java 25 e Spring Boot 3.5.4.
- Recomenda-se ao stakeholder decidir o destino da pasta `backend-java/.github/modernize/java-upgrade/`, deixada pela ferramenta externa (não gerada por este processo) — não removida unilateralmente por não ser um artefato deste processo de governança.
- Nenhuma alteração de código de domínio, aplicação ou adaptadores foi necessária — confirma o isolamento da Arquitetura Hexagonal em relação a detalhes de toolchain.
