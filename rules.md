# FinancePulse Engine — Regras Globais do Projeto (rules.md)

| Campo | Valor |
|---|---|
| Documento | rules.md |
| Versão | 1.0 |
| Status | Vigente |
| Autor | CTO / Principal Software Architect |
| Data | 2026-07-30 |

> Este documento consolida as regras de engenharia **transversais a todas as fases do roadmap**. Ele não define requisitos de produto (isso é papel do [vision.md](vision.md)) nem prioridades de entrega (isso é papel do [roadmap.md](roadmap.md)) — define **como** o código é construído, testado e revisado em qualquer fase, por qualquer agente.
>
> Todas as regras abaixo já estavam implícitas nos documentos de agentes ([agents/cto.md](agents/cto.md), [agents/fullstack.md](agents/fullstack.md), [agents/qa.md](agents/qa.md)); este documento as consolida em um único local, formalizando-as como critério objetivo de aderência exigido pelo QA em toda revisão.

---

## 1. Arquitetura

- **Arquitetura Hexagonal (Ports & Adapters) é obrigatória** em todo serviço/módulo do backend.
  - O **domínio** (entidades, regras de negócio, value objects) não pode importar frameworks, bibliotecas de infraestrutura, drivers de banco de dados ou bibliotecas HTTP.
  - Toda comunicação do domínio/aplicação com o mundo externo (banco de dados, APIs externas, filas, tempo, geração de IDs, etc.) ocorre através de **portas** (interfaces) definidas pela camada de aplicação.
  - **Adaptadores** implementam as portas e vivem isolados em uma camada própria, substituíveis sem alterar domínio ou aplicação.
  - A direção de dependência é sempre de fora para dentro: `adapters → application → domain`. Nunca o inverso.
- Nenhum novo padrão arquitetural (ex.: event sourcing, CQRS, novo estilo de comunicação entre serviços) pode ser introduzido sem ADR aprovado pelo CTO.

## 2. Desenvolvimento

- **TDD é obrigatório** para toda funcionalidade nova: o teste é escrito antes da implementação (red), a implementação mínima é escrita para o teste passar (green), o código é então refatorado mantendo os testes verdes (refactor).
- **SOLID** é aplicado em toda a camada de domínio e aplicação:
  - **S**: cada classe/módulo tem uma única razão para mudar.
  - **O**: extensão de comportamento sem modificar código existente sempre que razoável.
  - **L**: implementações de uma porta são substituíveis entre si sem quebrar o contrato.
  - **I**: portas são específicas ao consumidor, não interfaces genéricas "faz-tudo".
  - **D**: a aplicação depende de abstrações (portas), nunca de implementações concretas de adaptadores.
- **Clean Code** é obrigatório:
  - Nomes de funções, classes e variáveis revelam intenção sem necessidade de comentário explicativo.
  - Funções pequenas, com um único nível de abstração e uma única responsabilidade.
  - Sem código morto, sem código comentado, sem duplicação evitável.
  - Comentários só são usados para explicar o "porquê" de uma decisão não óbvia — nunca o "o quê" (o código já diz o quê).
- Código de produção é escrito em **inglês** (identificadores, nomes de arquivo, mensagens de log técnicas); documentação de projeto (ADRs, roadmap, comentários de racional de negócio) é escrita em **português**, para manter consistência com o vision.md e os agentes.

## 3. Testes

- Testes de **domínio e aplicação (use cases)** são testes unitários, sem I/O real — dependências externas são substituídas por dublês de teste (fakes/mocks) que implementam as portas da aplicação.
- Testes de **adaptadores** (persistência, HTTP, segurança) são testes de integração, validando o adaptador contra uma instância real (ou equivalente local) da tecnologia que ele encapsula.
- **O composition root real (`composition/container.ts`) deve ser exercitado ponta a ponta por ao menos um teste de fumaça** (`composition/container.integration.test.ts`), cobrindo os fluxos críticos através dos adaptadores de produção reais (não dublês/em memória). Toda fase que **adicionar um método a um repositório de produção já existente** (ex.: um novo `UPDATE`/coluna em um repositório Sqlite já implantado) deve estender esse smoke test para cobrir o novo caminho. Regra adicionada após um bug real (Fase 2.3): `SqliteUserRepository.update()` deixou de persistir uma coluna nova sem que nenhum teste de integração — todos baseados em repositórios em memória — detectasse, pois a variante em memória substitui o objeto inteiro e mascara esse tipo de regressão.
- **Equivalente de frontend à regra acima** (adicionado na Fase 13, ver ADR-0025): o frontend não tem um composition root de banco de dados para exercitar — sua única fronteira externa real é a rede. Todo fluxo completo de tela (não cada unidade isolada) deve ter ao menos um teste de integração que renderiza os componentes React reais e usa os hooks reais, interceptando a chamada apenas na camada de rede (MSW), nunca mockando o hook de dados ou o cliente HTTP diretamente — preservando a garantia de que o caminho de código real é exercitado ponta a ponta.
- Cobertura é orientada a **cenário de risco real** (caminho principal, regras de negócio do vision.md, casos de erro esperados) — não a uma meta percentual arbitrária.
- Testes são determinísticos: sem dependência de tempo de execução real, rede externa ou ordem de execução entre testes.
- Nenhum teste é ignorado (`skip`) ou comentado para "fazer a suíte passar" sem registro explícito do motivo e da pendência associada.

## 4. Segurança

- Nenhum segredo (chave, senha, token) é commitado no repositório — segredos vivem em variáveis de ambiente, nunca em código-fonte.
- Senhas de usuário nunca são armazenadas ou logadas em texto plano — apenas hash com algoritmo forte.
- Nenhum dado financeiro ou pessoal sensível é exposto em logs, mensagens de erro genéricas para o cliente, ou stack traces retornados via API.
- **Isolamento multi-tenant é inegociável** (RF-047 do vision.md): toda consulta a dados financeiros de usuário deve ser escopada ao usuário autenticado; nenhuma implementação pode depender apenas de disciplina de código para garantir isolamento — deve ser estruturalmente reforçado (ex.: cláusula obrigatória de filtro por usuário no nível do repositório).
- **Restrição inviolável (RN-009 do vision.md)**: nenhuma linha de código, endpoint, integração ou dependência pode conferir ao sistema capacidade técnica de mover dinheiro real (pagamentos, transferências, PIX, boletos). Qualquer código nessa direção é motivo de reprovação automática pelo QA e escalonamento ao CTO.
- **Toda entidade que representa uma forma de acesso ativo ou pendente a uma conta** (sessão/refresh token, token de recuperação de senha, credencial MFA, desafio de login, ou qualquer mecanismo futuro semelhante) **deve ser explicitamente revogada/invalidada por `DeleteAccountUseCase`** (e por qualquer futuro mecanismo de encerramento de conta). Ao introduzir uma nova entidade desse tipo, é responsabilidade do Full Stack atualizar esse use case na mesma fase, e do QA verificar explicitamente essa atualização na revisão — não apenas testar a entidade nova isoladamente. Regra adicionada após um achado real (Fase 2.5.2): a introdução de `MfaChallenge` não atualizou `DeleteAccountUseCase`, permitindo que um desafio de MFA pendente, emitido antes da exclusão, ainda fosse completável depois, reabrindo acesso a uma conta excluída.

## 5. Observabilidade

- Toda operação de escrita relevante (criação/edição/exclusão de dados financeiros do usuário) gera um log estruturado com: ator, ação, entidade afetada, timestamp — sem incluir o valor de dados sensíveis no corpo do log.
- Erros não tratados nunca falham silenciosamente: são logados com contexto suficiente para diagnóstico.

## 6. Aderência e Revisão

- O QA valida a aderência a este documento em **toda** revisão de entrega, como parte do checklist de qualidade obrigatório.
- Qualquer exceção a uma regra deste documento exige justificativa técnica explícita registrada na entrega e, se a exceção for recorrente ou estrutural, deve ser formalizada como atualização deste documento pelo CTO — nunca como desvio silencioso.

## 7. Encerramento de Fase

- **A aprovação final de toda fase do roadmap é sempre emitida pelo CTO, por escrito**, nunca inferida ou resumida em prosa dentro do relatório de encerramento. Nenhuma fase é considerada encerrada do ponto de vista dos agentes sem esse parecer formal.
- Todo encerramento de fase produz dois artefatos versionados, um por agente revisor:
  - `docs/qa/fase-NN-review.md` — parecer de qualidade do QA.
  - `docs/cto/fase-NN-aprovacao.md` — parecer de aprovação arquitetural final do CTO, emitido **após** o parecer do QA, avaliando aderência aos ADRs da fase e a este documento.
- A aprovação do CTO encerra o ciclo interno dos agentes; ela não substitui a aprovação do stakeholder para o início da fase seguinte, quando esta for exigida pelo processo em vigor.
- **Atualização de processo (2026-07-31, decisão do stakeholder)**: a aprovação explícita do stakeholder deixou de ser um bloqueio obrigatório entre toda fase e a próxima — o stakeholder pode indicar diretamente qual a próxima fase a implementar, sem aguardar um pedido formal de aprovação a cada encerramento. Isso **não dispensa** os dois artefatos formais (QA e CTO) exigidos acima ao final de cada fase — apenas remove o gate de espera pela resposta do stakeholder antes de iniciar a fase seguinte.
