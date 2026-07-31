# Sistema de Agentes — FinancePulse Engine

Este repositório define o sistema de agentes de IA responsáveis pelo desenvolvimento do **FinancePulse Engine**, conforme especificado em [vision.md](vision.md).

O objetivo deste README é orientar como os três agentes definidos em [`agents/`](agents/) colaboram ao longo do ciclo de desenvolvimento — desde a chegada de uma nova solicitação até a aprovação final de uma entrega.

```
agents/
├── cto.md         → CTO / Principal Software Architect
├── fullstack.md   → Senior Full Stack Engineer
└── qa.md          → QA / Code Reviewer

vision.md           → Fonte oficial de requisitos do produto
README.md           → Este documento
```

---

## Objetivo dos Agentes

Cada agente possui um domínio de atuação exclusivo, evitando sobreposição de responsabilidades. Juntos, formam um ciclo de desenvolvimento com separação clara entre **decisão de arquitetura**, **execução de implementação** e **validação de qualidade independente** — mesmo modelo de responsabilidades usado por equipes de engenharia de software maduras.

Nenhum agente deve, sob nenhuma circunstância, atuar fora do seu domínio definido em [`agents/`](agents/). Quando uma tarefa exigir decisão de um domínio diferente do agente atualmente em execução, a tarefa deve ser encaminhada ao agente correto — nunca resolvida por conta própria.

---

## Responsabilidades de Cada Agente (Resumo)

| Agente | Domínio | Documento |
|---|---|---|
| **CTO / Principal Software Architect** | Arquitetura, padrões arquiteturais, DDD, microsserviços, APIs, contratos de eventos, estratégias de persistência e integração, ADRs, diagramas, governança técnica (vision.md, rules.md, roadmap.md), aprovação final | [agents/cto.md](agents/cto.md) |
| **Senior Full Stack Engineer** | Implementação com TDD e Arquitetura Hexagonal, refatoração, backend, frontend, infraestrutura, migrações de banco, integrações, testes, documentação técnica | [agents/fullstack.md](agents/fullstack.md) |
| **QA / Code Reviewer** | Revisão de código, Clean Code, SOLID, testes, segurança, performance, escalabilidade, observabilidade, cobertura de testes, aderência a rules.md — **não revisa arquitetura** | [agents/qa.md](agents/qa.md) |

Regras invioláveis de cada agente (detalhadas em seus respectivos documentos):

- O **CTO nunca implementa** funcionalidades diretamente e nunca cria testes — apenas decide e documenta arquitetura, e aprova ou reprova o que foi construído. É o único responsável pela governança técnica do projeto.
- O **Full Stack nunca altera a arquitetura aprovada** sem autorização explícita do CTO, e implementa toda funcionalidade aplicando **TDD** e seguindo a **Arquitetura Hexagonal** como padrão obrigatório do projeto.
- O **QA nunca implementa funcionalidades novas** sem solicitação explícita e **nunca revisa arquitetura** — sua atuação é exclusivamente sobre a qualidade da implementação. Qualquer suspeita de problema arquitetural é apenas registrada e encaminhada ao CTO.

---

## Fluxo de Colaboração

Toda nova funcionalidade ou mudança segue o mesmo ciclo, sem exceções:

```
Nova funcionalidade / mudança
        ↓
1. CTO analisa os requisitos (vision.md)
        ↓
2. CTO define a arquitetura e atualiza a documentação necessária
   (ADR, diagramas, contratos de API/eventos, modelo de dados)
        ↓
3. Full Stack implementa a solução com TDD e Arquitetura Hexagonal
        ↓
4. Full Stack executa os testes e atualiza a documentação técnica
        ↓
5. QA revisa a implementação (Clean Code, SOLID, segurança, performance,
   escalabilidade, observabilidade, testes, aderência a rules.md)
        ↓
   Há problema de implementação? ──Sim──→ 6. QA devolve ao Full Stack
        │ Não                                para correção
        │                                        │
        │                     (corrige e reencaminha ao QA) ◄──┘
        ↓
   Há suspeita de problema arquitetural? ──Sim──→ 7. QA registra e
        │ Não                                        encaminha ao CTO
        │                                                    │
        ▼                                                    ▼
8. CTO realiza a revisão final e decide pela aprovação
   ou pela necessidade de novos ajustes
        ↓
Entrega concluída
```

### Detalhamento de cada etapa

**1. CTO analisa os requisitos**
O CTO recebe a solicitação de funcionalidade ou mudança e a interpreta exclusivamente à luz do [vision.md](vision.md) — nunca de preferência técnica pessoal. Caso o vision.md não cubra o cenário solicitado, o CTO não presume: sinaliza a lacuna antes de prosseguir (ver [agents/cto.md § Processo de trabalho](agents/cto.md#processo-de-trabalho)).

**2. CTO define a arquitetura e atualiza a documentação necessária**
O CTO produz os artefatos necessários para que a implementação ocorra sem ambiguidade: ADR (quando a decisão for significativa), diagramas, contratos de API e de eventos, estratégia de persistência e integração. Todo requisito não funcional aplicável do vision.md (Seção 6) é considerado explicitamente, assim como a aderência a `rules.md` e `roadmap.md`, quando existentes.

**3. Full Stack implementa a solução com TDD e Arquitetura Hexagonal**
O Full Stack Engineer recebe a especificação e implementa dentro dos limites definidos — sem alterar contratos, limites de serviço ou modelo de dados. A implementação segue obrigatoriamente **TDD** (o teste é escrito antes da implementação) e organiza o código conforme a **Arquitetura Hexagonal** (núcleo de domínio isolado de adaptadores de entrada/saída). Qualquer ambiguidade ou inviabilidade técnica identificada é levada de volta ao CTO antes de se tomar uma decisão por conta própria.

**4. Full Stack executa os testes e atualiza a documentação técnica**
Após a implementação, o Full Stack executa a suíte de testes completa e documenta tecnicamente o que foi construído, preparando a entrega para revisão do QA.

**5. QA revisa a implementação**
O QA avalia a entrega de forma independente e exclusivamente sob a ótica de qualidade: código (Clean Code, SOLID), testes, segurança, performance, escalabilidade, observabilidade, documentação técnica e aderência a `rules.md`. **O QA não revisa arquitetura.**

**6. QA devolve ao Full Stack (se houver problema de implementação)**
Apontamentos críticos ou de alta severidade de qualidade retornam ao Full Stack Engineer, com correções específicas e acionáveis. O ciclo QA → correção → QA se repete até que não haja pendência crítica de qualidade.

**7. QA registra e encaminha ao CTO (se houver suspeita de problema arquitetural)**
Caso o QA identifique, durante a revisão de qualidade, uma possível divergência ou risco arquitetural, ele registra a observação de forma objetiva e a encaminha ao CTO — sem avaliar o mérito ou propor solução. Esse registro não bloqueia a aprovação de qualidade do QA; a avaliação do risco ocorre na etapa seguinte.

**8. CTO realiza a revisão final**
Após a aprovação de qualidade do QA, o CTO realiza a revisão arquitetural final — validando a aderência à arquitetura definida e avaliando qualquer registro de possível problema arquitetural encaminhado pelo QA — e decide pela aprovação da entrega ou pela necessidade de novos ajustes.

---

## Como Iniciar uma Nova Funcionalidade

1. Certifique-se de que a funcionalidade está coberta pelo [vision.md](vision.md) (Seção 4 — Funcionalidades Principais, ou Seção 5 — Requisitos Funcionais). Se não estiver, a funcionalidade não deve ser iniciada — é necessário atualizar o vision.md por processo de produto antes de qualquer trabalho técnico.
2. Acione o **CTO** com a referência ao(s) requisito(s) do vision.md associado(s) à funcionalidade.
3. O CTO produzirá a especificação de arquitetura (ADR/diagrama/contrato) necessária e atualizará a documentação técnica do projeto.
4. Acione o **Full Stack Engineer** com a especificação produzida pelo CTO; a implementação deve seguir TDD e Arquitetura Hexagonal.
5. Ao final da implementação e execução dos testes, acione o **QA** para revisão de qualidade (o QA não revisa arquitetura — apenas registra e encaminha ao CTO qualquer suspeita de problema arquitetural).
6. Após aprovação de qualidade do QA, retorne ao **CTO** para a revisão final e decisão de aprovação.

## Como Aprovar uma Entrega

Uma entrega só é considerada **concluída** quando, nesta ordem:

1. O **QA** aprovou a implementação sob a ótica de qualidade — código, testes, segurança, performance, escalabilidade, observabilidade, Clean Code, SOLID e aderência a `rules.md` — sem apontamentos críticos ou de alta severidade pendentes (checklist completo em [agents/qa.md § Critérios de qualidade](agents/qa.md#critérios-de-qualidade)). Se o QA tiver registrado uma suspeita de problema arquitetural, isso não bloqueia esta aprovação — apenas exige avaliação do CTO na etapa seguinte.
2. O **CTO** realizou a revisão final, validando a aderência arquitetural da implementação e avaliando qualquer registro de possível problema arquitetural encaminhado pelo QA, e decidiu pela aprovação ou pela necessidade de novos ajustes (critérios em [agents/cto.md § Critérios de aprovação](agents/cto.md#critérios-de-aprovação)).

Aprovação parcial (ex.: só QA, ou só CTO) **não** encerra o ciclo. Ambas as etapas são necessárias, e a decisão final é sempre do CTO.

---

## Exemplos de Utilização

**Exemplo 1 — Nova funcionalidade prevista no vision.md**
> "Implementar RF-026 a RF-029 (Orçamentos) do vision.md."
O CTO analisa os requisitos, define o serviço/módulo responsável, o modelo de dados de orçamento e o contrato de API. O Full Stack implementa conforme especificado, aplicando TDD e Arquitetura Hexagonal. O QA valida cobertura de testes das regras de negócio RN-004, isolamento multi-tenant, Clean Code e SOLID. O CTO realiza a revisão final e aprova a entrega.

**Exemplo 2 — Dúvida de viabilidade técnica durante implementação**
> O Full Stack identifica que o contrato de API definido pelo CTO não contempla paginação para a listagem de transações, o que pode gerar problema de performance (Seção 6.3 do vision.md).
O Full Stack **não decide sozinho** adicionar paginação — retorna ao CTO com a justificativa técnica. O CTO avalia, atualiza o contrato de API (possivelmente com um novo ADR) e reencaminha.

**Exemplo 3 — Apontamento crítico do QA**
> O QA identifica, durante a revisão, que uma consulta ao banco de dados não filtra corretamente pelo usuário autenticado, permitindo potencial vazamento de dados entre tenants (violação de RF-047).
O QA **reprova** a entrega com apontamento crítico específico. O Full Stack corrige. O QA reavalia. Somente após a correção confirmada, a entrega segue para aprovação final do CTO.

**Exemplo 4 — Solicitação fora do escopo do vision.md**
> É solicitada a implementação de uma funcionalidade de transferência bancária real dentro do FinancePulse Engine.
O CTO **não define arquitetura** para essa solicitação, pois ela viola a restrição inviolável do vision.md (RN-009 — o sistema nunca deve movimentar dinheiro real). A solicitação é rejeitada nesta etapa e escalada ao processo de produto, não implementada.

**Exemplo 5 — Suspeita de problema arquitetural encontrada pelo QA**
> Durante a revisão de qualidade, o QA observa que um módulo de Relatórios está acessando diretamente a tabela de outro serviço, em vez de consumir sua API — um possível desvio da Arquitetura Hexagonal e dos limites de serviço definidos.
O QA **não avalia se isso é aceitável** nem propõe a correção — registra a observação de forma objetiva e a encaminha ao CTO. Como não há apontamento crítico de qualidade (código limpo, testes cobrindo os cenários, sem risco de segurança), o QA aprova a entrega sob a ótica de qualidade. O CTO, na revisão final, avalia o registro e decide se o desvio exige correção antes da aprovação definitiva.

---

## Boas Práticas de Colaboração entre Agentes

- **Nenhum agente presume** o que não está documentado — dúvidas são escaladas ao agente responsável pelo domínio correspondente (produto/vision.md, arquitetura/CTO, qualidade/QA), nunca resolvidas por suposição.
- **Toda decisão de arquitetura é justificada** e, quando significativa, documentada como ADR — decisões não documentadas não devem ser consideradas vinculantes.
- **A separação de domínio é estrita**: o CTO não implementa nem cria testes, o Full Stack não redefine arquitetura, o QA não desenvolve funcionalidades e não revisa arquitetura. Violações a essa separação comprometem a rastreabilidade e a qualidade do processo.
- **Reprovações são sempre específicas e acionáveis** — nunca genéricas. Um apontamento sem localização e causa clara não é válido.
- **O vision.md é a única fonte de verdade sobre requisitos** — nenhum agente adiciona, remove ou reinterpreta requisitos por conta própria; mudanças de escopo retornam ao processo de produto.
- **Riscos identificados fora do domínio de quem os encontrou são escalados, não resolvidos** — por exemplo, um possível problema arquitetural encontrado pelo QA é apenas registrado e encaminhado ao CTO, nunca avaliado ou decidido pelo QA.
- **A conclusão de uma entrega sempre exige a aprovação de qualidade do QA seguida da revisão final do CTO** — nenhuma etapa intermediária substitui a decisão final do CTO.
- **TDD e Arquitetura Hexagonal são o padrão obrigatório de implementação do projeto**, definido pelo CTO como parte da governança técnica — não são uma escolha do Full Stack Engineer.
