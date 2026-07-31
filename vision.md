# FinancePulse Engine — Documento de Visão do Produto

| Campo | Valor |
|---|---|
| Documento | vision.md |
| Versão | 1.0 |
| Status | Aprovado para uso como fonte de requisitos |
| Autor | CTO / Principal Software Architect |
| Data | 2026-07-30 |
| Audiência | Agentes Full Stack, Agente QA, Product, Stakeholders |

> Este documento é a **fonte oficial de requisitos** do FinancePulse Engine. Qualquer funcionalidade, decisão técnica ou critério de aceite que não esteja aqui — ou em um adendo formalmente aprovado — deve ser tratado como **fora de escopo** até validação explícita com o Product Owner/CTO.

---

## 0. Definições de Escopo Confirmadas com Stakeholder

Antes da elaboração deste documento, os seguintes pontos críticos — que não podiam ser inferidos ou inventados — foram validados diretamente com o stakeholder do produto:

1. **Domínio**: Gestão financeira pessoal (personal finance management).
2. **Público-alvo primário**: Consumidores finais (B2C).
3. **Escopo transacional**: O sistema **não movimenta dinheiro real**. É um sistema de rastreamento, cálculo e visualização — não uma plataforma de pagamentos.
4. **Modelo de negócio**: SaaS multi-tenant, servindo múltiplos clientes finais isolados entre si sobre infraestrutura compartilhada.

Todas as demais decisões de escopo derivadas (integração bancária, moeda, plataformas-alvo, monetização) são tratadas como **premissas explícitas** na Seção 11 e sinalizadas como pontos em aberto na Seção 17, por não terem sido fornecidas e não poderem ser inventadas como requisitos críticos.

---

## 1. Visão Geral

### 1.1 Objetivo do Sistema

O FinancePulse Engine é uma plataforma SaaS de gestão financeira pessoal que permite a indivíduos centralizar, entender e controlar sua vida financeira — contas, receitas, despesas, orçamentos e metas — através de uma experiência unificada, segura e orientada a dados ("o pulso financeiro" do usuário).

### 1.2 Problema que Resolve

Pessoas físicas hoje têm sua vida financeira fragmentada entre múltiplas contas bancárias, cartões, carteiras digitais e investimentos, sem uma visão consolidada. Isso gera:

- Falta de visibilidade sobre para onde o dinheiro está indo.
- Dificuldade em manter orçamentos e hábitos financeiros saudáveis.
- Decisões financeiras reativas em vez de planejadas.
- Ansiedade financeira por falta de controle e previsibilidade.
- Ferramentas existentes (planilhas manuais, apps bancários isolados) que exigem esforço manual alto e não geram insight acionável.

O FinancePulse Engine resolve isso oferecendo um **motor central de agregação, categorização e análise financeira pessoal**, com uma métrica de saúde financeira consolidada (o "Pulse Score") como âncora de produto.

### 1.3 Público-Alvo

- Consumidores finais (pessoas físicas) que desejam controlar suas finanças pessoais.
- Perfil inicial: profissionais com renda ativa, alfabetizados digitalmente, usuários de internet banking/apps financeiros, com necessidade de consolidar múltiplas fontes financeiras.
- Mercado geográfico inicial: a definir (ver Seção 17) — assumido como Brasil por padrão de idioma e conformidade regulatória (LGPD), sujeito a confirmação.

### 1.4 Benefícios

- **Visibilidade unificada**: visão consolidada de todas as contas e movimentações financeiras em um único painel.
- **Controle proativo**: orçamentos, metas e alertas que promovem decisões financeiras antes que problemas ocorram.
- **Redução de esforço manual**: categorização automática e importação de dados reduzem trabalho manual de organização financeira.
- **Confiança e segurança**: dados financeiros sensíveis tratados com padrões de segurança e privacidade de nível bancário, sem risco operacional de movimentação de fundos (o sistema é estritamente analítico).
- **Tomada de decisão orientada por dados**: relatórios e indicadores acionáveis substituem intuição por evidência.

---

## 2. Objetivos do Projeto

1. Entregar uma plataforma que consolide dados financeiros pessoais de forma confiável e segura, servindo como fonte única de verdade para a vida financeira do usuário.
2. Reduzir o atrito de organização financeira pessoal, tornando a categorização e o acompanhamento de gastos majoritariamente automáticos.
3. Estabelecer uma métrica de saúde financeira proprietária (Pulse Score) como diferencial competitivo e âncora de engajamento.
4. Construir a plataforma sobre uma arquitetura multi-tenant escalável, capaz de suportar crescimento de base de usuários sem retrabalho arquitetural.
5. Garantir conformidade regulatória de proteção de dados (LGPD ou equivalente) desde a concepção (privacy by design), dado o caráter sensível dos dados tratados.
6. Criar uma base extensível que permita, em fases futuras, evoluir de "rastreamento" para "insight preditivo" (ex.: recomendações, projeções, IA) sem comprometer a integridade do núcleo transacional-analítico.

> **Nota**: Metas quantitativas de negócio (ex.: número de usuários, receita, retenção-alvo) não foram fornecidas pelo stakeholder e não são inventadas aqui. Devem ser definidas pelo Product Owner e anexadas como adendo a este documento.

---

## 3. Escopo

### 3.1 O que o sistema FAZ

- Permite que usuários criem contas/carteiras financeiras (bancárias, cartão, dinheiro, poupança) dentro da plataforma.
- Permite o registro manual de transações (receitas e despesas).
- Permite importação de extratos via arquivo (CSV/OFX) para popular transações em lote.
- Categoriza transações automaticamente com base em regras e heurísticas, com opção de categorização/correção manual pelo usuário.
- Calcula e exibe saldo consolidado e por conta.
- Permite criação de orçamentos (budgets) por categoria/período, com acompanhamento de consumo do orçamento.
- Permite definição de metas financeiras (ex.: reserva de emergência, meta de economia).
- Gera relatórios e visualizações (gastos por categoria, evolução mensal, comparativos período a período).
- Calcula um indicador consolidado de saúde financeira ("Pulse Score").
- Envia alertas e notificações relevantes (ex.: orçamento estourado, gasto incomum, meta atingida).
- Garante isolamento de dados entre usuários/tenants (multi-tenancy a nível de conta de usuário).
- Fornece funcionalidades de conformidade com LGPD: exportação e exclusão de dados pessoais mediante solicitação do titular.

### 3.2 O que o sistema NÃO FAZ

- **Não movimenta dinheiro real**: não realiza pagamentos, transferências, PIX, boletos ou qualquer transação financeira efetiva. É estritamente um sistema de registro e análise.
- Não atua como instituição financeira, não é gestora de recursos, não emite cartões, não concede crédito.
- Não fornece aconselhamento financeiro/de investimento regulado (não é uma consultoria de investimentos licenciada) — insights são informativos, não recomendações fiduciárias.
- Não realiza conciliação contábil-fiscal formal nem geração de documentos fiscais/declaração de imposto de renda (fora do escopo desta visão; pode ser considerado em roadmap futuro).
- Não oferece, na versão inicial, gestão financeira para pessoas jurídicas (fluxo de caixa empresarial, contas a pagar/receber corporativas) — esse é um domínio distinto (B2B), fora do escopo B2C definido.
- Não inclui, no MVP, conectividade automática via Open Banking com instituições financeiras (ver Seção 11 — Premissas e Seção 14 — MVP). Entrada de dados no MVP é manual ou via importação de arquivo.

---

## 4. Funcionalidades Principais

### 4.1 Onboarding e Autenticação
Cadastro de usuário, autenticação segura (com suporte a MFA), configuração inicial guiada (criação da primeira conta/carteira, categorias padrão).

### 4.2 Gestão de Contas e Carteiras
Criação, edição e arquivamento de contas financeiras (conta corrente, poupança, cartão de crédito, dinheiro em espécie, carteira digital). Cada conta possui saldo, moeda e histórico de transações.

### 4.3 Lançamentos Financeiros (Transações)
Registro manual de receitas e despesas, com valor, data, categoria, conta associada, descrição e tags opcionais. Suporte a transações recorrentes (ex.: assinatura mensal, salário).

### 4.4 Importação de Dados
Upload de arquivos CSV/OFX para importação em lote de extratos bancários, com mapeamento de colunas e detecção de duplicidade.

### 4.5 Categorização
Motor de categorização automática baseado em regras (palavras-chave, histórico de categorização do usuário) com fallback para categorização manual. Usuário pode criar categorias e subcategorias customizadas.

### 4.6 Orçamentos (Budgets)
Definição de limites de gasto por categoria e período (mensal, semanal). Acompanhamento visual do consumo do orçamento e alertas ao se aproximar/ultrapassar o limite.

### 4.7 Metas Financeiras (Goals)
Definição de metas de economia ou redução de gastos, com acompanhamento de progresso ao longo do tempo.

### 4.8 Dashboard e Pulse Score
Painel central consolidando saldo total, fluxo de caixa recente, distribuição de gastos e o "Pulse Score" — um indicador proprietário de saúde financeira calculado a partir de sinais como consistência orçamentária, taxa de poupança, diversificação de gastos e tendência de saldo.

### 4.9 Relatórios e Analytics
Relatórios de gastos por categoria, evolução temporal, comparativos mês a mês/ano a ano, exportáveis.

### 4.10 Notificações e Alertas
Alertas configuráveis: estouro de orçamento, gasto atípico, lembrete de conta a vencer (baseado em transação recorrente prevista), meta atingida.

### 4.11 Privacidade e Conformidade (LGPD)
Central de privacidade do usuário: visualização de dados armazenados, exportação de dados pessoais, solicitação de exclusão de conta e dados associados.

### 4.12 Administração e Suporte (Backoffice)
Ferramentas internas (não expostas ao usuário final) para suporte ao cliente, auditoria de contas, monitoramento de saúde da plataforma e investigação de incidentes, respeitando controles de acesso e trilha de auditoria.

---

## 5. Requisitos Funcionais

### Autenticação e Conta de Usuário
- **RF-001**: O sistema deve permitir que um usuário se cadastre com e-mail e senha.
- **RF-002**: O sistema deve validar unicidade de e-mail no cadastro.
- **RF-003**: O sistema deve permitir autenticação via e-mail e senha.
- **RF-004**: O sistema deve suportar autenticação multifator (MFA) opcional.
- **RF-005**: O sistema deve permitir recuperação de senha via fluxo seguro (e-mail com token de expiração limitada).
- **RF-006**: O sistema deve permitir que o usuário edite dados de perfil (nome, e-mail, preferências).
- **RF-007**: O sistema deve permitir que o usuário exclua sua conta, com confirmação explícita e exclusão/anonimização dos dados pessoais conforme LGPD.
- **RF-008**: O sistema deve emitir e validar tokens de sessão com expiração e renovação segura.

### Contas e Carteiras
- **RF-009**: O sistema deve permitir a criação de contas financeiras com tipo (corrente, poupança, cartão de crédito, dinheiro, carteira digital), nome, moeda e saldo inicial.
- **RF-010**: O sistema deve permitir edição e arquivamento (soft-delete) de contas financeiras.
- **RF-011**: O sistema deve calcular e exibir o saldo atual de cada conta com base nas transações registradas.
- **RF-012**: O sistema deve calcular e exibir o saldo consolidado de todas as contas ativas do usuário.
- **RF-013**: O sistema deve impedir a exclusão definitiva de contas com histórico de transações, permitindo apenas arquivamento.

### Transações
- **RF-014**: O sistema deve permitir o registro manual de uma transação com valor, tipo (receita/despesa), data, conta, categoria e descrição.
- **RF-015**: O sistema deve permitir edição e exclusão de transações registradas manualmente.
- **RF-016**: O sistema deve permitir a criação de transações recorrentes (ex.: mensal, semanal) que geram lançamentos futuros automaticamente.
- **RF-017**: O sistema deve permitir associar tags livres a uma transação.
- **RF-018**: O sistema deve permitir filtrar e buscar transações por conta, categoria, período, valor e tags.
- **RF-019**: O sistema deve permitir a importação de transações via arquivo CSV/OFX.
- **RF-020**: O sistema deve detectar e sinalizar possíveis transações duplicadas durante a importação.
- **RF-021**: O sistema deve permitir ao usuário revisar e confirmar/descartar transações importadas antes da consolidação final.

### Categorização
- **RF-022**: O sistema deve categorizar automaticamente transações importadas com base em regras predefinidas e histórico de categorização do usuário.
- **RF-023**: O sistema deve permitir a criação, edição e exclusão de categorias e subcategorias customizadas.
- **RF-024**: O sistema deve permitir a recategorização manual de qualquer transação.
- **RF-025**: O sistema deve fornecer um conjunto de categorias padrão pré-configuradas no onboarding.

### Orçamentos
- **RF-026**: O sistema deve permitir a criação de orçamentos por categoria e período (mensal/semanal/customizado).
- **RF-027**: O sistema deve calcular o percentual de consumo de um orçamento em tempo real com base nas transações do período.
- **RF-028**: O sistema deve notificar o usuário ao atingir limiares configuráveis de consumo do orçamento (ex.: 80%, 100%).
- **RF-029**: O sistema deve permitir a visualização histórica de desempenho de orçamentos de períodos anteriores.

### Metas Financeiras
- **RF-030**: O sistema deve permitir a criação de metas financeiras com valor-alvo, prazo e conta/categoria associada.
- **RF-031**: O sistema deve calcular e exibir o progresso de uma meta financeira ao longo do tempo.
- **RF-032**: O sistema deve notificar o usuário ao atingir ou se aproximar de uma meta.

### Dashboard e Pulse Score
- **RF-033**: O sistema deve exibir um painel consolidado com saldo total, fluxo de caixa recente e distribuição de gastos por categoria.
- **RF-034**: O sistema deve calcular um Pulse Score (indicador de saúde financeira) com base em regras de negócio definidas (ver Seção 9).
- **RF-035**: O sistema deve exibir a evolução histórica do Pulse Score do usuário.
- **RF-036**: O sistema deve fornecer explicabilidade básica do Pulse Score (quais fatores impactaram positiva/negativamente o índice).

### Relatórios
- **RF-037**: O sistema deve gerar relatórios de gastos por categoria em um período selecionável.
- **RF-038**: O sistema deve gerar relatórios comparativos entre períodos (ex.: mês atual vs. mês anterior).
- **RF-039**: O sistema deve permitir exportação de relatórios e dados de transações (ex.: CSV/PDF).

### Notificações
- **RF-040**: O sistema deve permitir ao usuário configurar preferências de notificação por tipo de alerta e canal (in-app, e-mail).
- **RF-041**: O sistema deve enviar notificação de estouro de orçamento.
- **RF-042**: O sistema deve enviar notificação de gasto atípico (com base em desvio estatístico do padrão histórico do usuário).
- **RF-043**: O sistema deve enviar lembrete de transação recorrente prevista e ainda não confirmada.

### Privacidade e Conformidade
- **RF-044**: O sistema deve permitir ao usuário exportar todos os seus dados pessoais e financeiros em formato legível (ex.: JSON/CSV).
- **RF-045**: O sistema deve processar solicitações de exclusão de conta em conformidade com prazos legais aplicáveis (LGPD), removendo ou anonimizando dados pessoais.
- **RF-046**: O sistema deve manter registro de consentimento do usuário para tratamento de dados.

### Multi-tenancy e Isolamento
- **RF-047**: O sistema deve garantir que um usuário não possa, sob nenhuma circunstância, acessar dados financeiros de outro usuário.
- **RF-048**: O sistema deve registrar (audit log) todo acesso administrativo/backoffice a dados de um usuário.

### Administração/Backoffice
- **RF-049**: O sistema deve fornecer uma interface interna para suporte investigar (com trilha de auditoria) problemas reportados por usuários, mediante controle de acesso baseado em papel (RBAC).
- **RF-050**: O sistema deve permitir a operadores autorizados suspender uma conta de usuário em caso de suspeita de fraude ou abuso da plataforma.

---

## 6. Requisitos Não Funcionais

### 6.1 Escalabilidade
- A arquitetura deve suportar crescimento horizontal de usuários e volume de transações sem redesenho estrutural.
- Serviços core (transações, categorização, cálculo de Pulse Score) devem ser escaláveis independentemente entre si.
- O modelo de dados deve suportar particionamento por tenant/usuário para crescimento sustentável.

### 6.2 Segurança
- Dados financeiros sensíveis devem ser criptografados em trânsito (TLS) e em repouso.
- Senhas devem ser armazenadas com hashing forte (ex.: bcrypt/argon2), nunca em texto plano.
- Autenticação deve seguir boas práticas (proteção contra força bruta, rate limiting, MFA opcional).
- Acesso administrativo/backoffice deve seguir princípio de menor privilégio (RBAC) e ser integralmente auditado.
- O sistema não deve armazenar credenciais bancárias do usuário (login/senha de bancos) diretamente — qualquer integração futura com instituições financeiras deve ocorrer via provedor certificado de Open Banking (ver Restrições, Seção 12).

### 6.3 Performance
- Operações de leitura do dashboard principal devem responder dentro de limites aceitáveis de experiência de usuário (referência inicial: p95 < 2s), sujeito a validação com equipe de produto/engenharia durante a fase de design técnico.
- Importação de arquivos deve processar extratos de tamanho típico (centenas a poucos milhares de transações) sem bloquear a interface do usuário (processamento assíncrono).

### 6.4 Disponibilidade
- A plataforma deve ser projetada para alta disponibilidade, adequada a um produto SaaS comercial (referência inicial: 99,9% de uptime mensal), com meta formal de SLA a ser definida pelo negócio.

### 6.5 Resiliência
- Falhas em componentes não críticos (ex.: serviço de notificação, geração de relatório) não devem impactar a disponibilidade de funcionalidades core (registro de transações, consulta de saldo).
- O sistema deve adotar padrões de tolerância a falha (retry com backoff, circuit breaker) em integrações externas e comunicação entre serviços.

### 6.6 Observabilidade
- Todos os serviços devem emitir logs estruturados, métricas operacionais e tracing distribuído suficientes para diagnóstico de incidentes.
- Eventos de negócio críticos (criação de transação, cálculo de Pulse Score, falha de importação) devem ser observáveis e alertáveis.

### 6.7 Portabilidade
- A arquitetura conceitual não deve criar acoplamento rígido a um único provedor de nuvem, permitindo, em princípio, portabilidade entre ambientes (decisão de implementação a ser detalhada na fase técnica).

### 6.8 Auditabilidade
- Toda alteração em dados financeiros do usuário (criação/edição/exclusão de transação, conta, orçamento) deve ser rastreável (quem, quando, o quê).
- Acessos administrativos a dados de usuários devem gerar trilha de auditoria imutável.

---

## 7. Personas

### 7.1 Marina — Profissional Autônoma Organizando as Finanças
Freelancer, 29 anos, renda variável. Possui múltiplas contas e recebe pagamentos de fontes distintas. Quer entender quanto realmente sobra por mês e criar uma reserva de emergência. Usa o smartphone como principal dispositivo de acesso.

### 7.2 Carlos — Buscando Sair do Vermelho
34 anos, assalariado, endividado em cartão de crédito. Não tem hábito de acompanhar gastos e é surpreendido negativamente todo mês. Precisa de alertas simples e visão clara de orçamento para recuperar controle.

### 7.3 Ana — Iniciante em Planejamento Financeiro
26 anos, começou a trabalhar há pouco tempo, quer criar bons hábitos financeiros desde cedo. Valoriza metas visuais e um indicador simples (Pulse Score) que diga se está "indo bem".

### 7.4 Operador de Suporte Interno (Persona Interna)
Colaborador da equipe de atendimento ao cliente do FinancePulse. Precisa investigar problemas reportados por usuários (ex.: transação não aparece, saldo incorreto) com acesso controlado e auditado, sem poder alterar dados financeiros do usuário livremente.

---

## 8. Casos de Uso

- **UC-001 — Cadastro e Onboarding**: Marina se cadastra, cria sua primeira conta financeira e categorias padrão são sugeridas. (RF-001, RF-009, RF-025)
- **UC-002 — Registro Manual de Transação**: Carlos registra uma despesa de supermercado manualmente, categorizando-a. (RF-014, RF-024)
- **UC-003 — Importação de Extrato**: Marina importa um extrato CSV do banco, revisa transações sugeridas e confirma a importação. (RF-019, RF-020, RF-021)
- **UC-004 — Criação de Orçamento e Acompanhamento**: Carlos cria um orçamento mensal para "Alimentação" e recebe alerta ao atingir 80% do limite. (RF-026, RF-028)
- **UC-005 — Definição de Meta Financeira**: Ana define uma meta de reserva de emergência e acompanha o progresso mensalmente. (RF-030, RF-031)
- **UC-006 — Consulta do Pulse Score**: Ana acessa o dashboard e visualiza seu Pulse Score, entendendo quais fatores o influenciaram. (RF-034, RF-036)
- **UC-007 — Exportação de Dados Pessoais (LGPD)**: Marina solicita exportação completa de seus dados pessoais e financeiros. (RF-044)
- **UC-008 — Exclusão de Conta**: Carlos solicita o encerramento de sua conta e exclusão de seus dados. (RF-007, RF-045)
- **UC-009 — Investigação de Suporte**: Um operador de suporte investiga, com acesso auditado, uma inconsistência de saldo reportada por um usuário. (RF-049, RF-048)
- **UC-010 — Suspensão de Conta por Suspeita de Abuso**: Um operador autorizado suspende uma conta suspeita de uso fraudulento da plataforma (ex.: automação abusiva, não movimentação financeira real). (RF-050)

---

## 9. Regras de Negócio

- **RN-001**: O saldo de uma conta é sempre derivado da soma de suas transações — nunca um valor editável diretamente pelo usuário após a criação da conta (exceto saldo inicial no momento da criação).
- **RN-002**: Uma transação sempre pertence a exatamente uma conta e a exatamente uma categoria.
- **RN-003**: Transações não podem ser compartilhadas ou visíveis entre usuários distintos, mesmo que estes tenham vínculo familiar/relacional dentro da plataforma (sem funcionalidade de conta compartilhada no escopo atual).
- **RN-004**: Um orçamento é sempre associado a uma categoria e a um período recorrente; o consumo do orçamento é recalculado a cada nova transação relevante.
- **RN-005**: O Pulse Score é recalculado periodicamente (ex.: diariamente) e após eventos financeiros relevantes (ex.: fechamento de mês), e não pode ser editado manualmente pelo usuário.
- **RN-006**: A fórmula/composição exata do Pulse Score é uma decisão de produto/ciência de dados a ser detalhada em documento técnico complementar; este documento define apenas seu papel funcional (Seção 17 — pendência formal).
- **RN-007**: Transações importadas identificadas como prováveis duplicatas não são consolidadas automaticamente — exigem confirmação explícita do usuário.
- **RN-008**: A exclusão de uma conta de usuário (RF-007/RF-045) deve respeitar prazos e exceções legais de retenção de dados (ex.: obrigações fiscais/regulatórias aplicáveis), a validar com jurídico.
- **RN-009**: Nenhuma funcionalidade do sistema pode iniciar, autorizar ou executar movimentação financeira real (pagamento, transferência) — essa é uma restrição de produto inviolável (ver Seção 12).

---

## 10. Arquitetura Conceitual

*(Descrição conceitual, sem detalhamento de código, linguagens ou frameworks — decisões de implementação pertencem à fase técnica seguinte.)*

### 10.1 Camadas Lógicas

1. **Camada de Cliente**: aplicações que consomem a plataforma (web responsiva no MVP; mobile nativo em fase futura — ver Seção 15). Toda a lógica de negócio reside no backend; o cliente é uma camada de apresentação e interação.

2. **Camada de API/Gateway**: ponto único de entrada para os clientes, responsável por autenticação, roteamento para os serviços internos, rate limiting e validação de contrato.

3. **Camada de Serviços de Domínio (Core Services)**, logicamente separados por responsabilidade:
   - **Identidade e Acesso**: cadastro, autenticação, MFA, gestão de sessão.
   - **Contas e Carteiras**: gestão de contas financeiras do usuário.
   - **Transações**: registro, edição, importação e consulta de lançamentos.
   - **Categorização**: motor de regras/heurísticas de categorização automática.
   - **Orçamentos e Metas**: gestão e cálculo de progresso de budgets e goals.
   - **Analytics / Pulse Engine**: cálculo do Pulse Score e geração de relatórios/insights.
   - **Notificações**: orquestração e envio de alertas por canal.
   - **Privacidade/Conformidade**: exportação e exclusão de dados pessoais (LGPD).
   - **Backoffice/Administração**: ferramentas internas com RBAC e auditoria.

4. **Camada de Dados**: armazenamento transacional (dados financeiros do usuário, com isolamento lógico por tenant) separado logicamente de um armazenamento analítico (para relatórios e cálculo de Pulse Score em escala), evitando que cargas analíticas impactem performance transacional.

5. **Camada de Integração Assíncrona**: barramento de eventos para comunicação entre serviços (ex.: "transação criada" disparando recálculo de orçamento, Pulse Score e possíveis notificações), desacoplando processamento pesado do caminho crítico de resposta ao usuário.

6. **Camada de Observabilidade**: logging estruturado, métricas e tracing centralizados, transversal a todos os serviços.

### 10.2 Princípios Arquiteturais

- **Isolamento multi-tenant por padrão**: toda consulta a dados financeiros deve ser inerentemente escopada ao usuário autenticado; isolamento não pode depender apenas de disciplina de código no nível de aplicação — deve ser reforçado estruturalmente (ex.: nível de dados).
- **Separação entre caminho crítico e processamento assíncrono**: ações que não precisam de resposta imediata ao usuário (recálculo de Pulse Score, envio de notificação, categorização em lote) não devem bloquear a experiência interativa.
- **Nenhum componente do sistema deve ter capacidade técnica de iniciar movimentação financeira real**, reforçando a Restrição RN-009 também no nível de arquitetura (nenhuma integração com rails de pagamento faz parte do desenho do sistema).
- **Extensibilidade para integrações futuras** (ex.: Open Banking) deve ser prevista como um módulo de integração plugável, sem exigir redesenho do núcleo de domínio.

---

## 11. Premissas

As premissas abaixo foram assumidas pela ausência de definição explícita do stakeholder e **devem ser validadas** antes do início da implementação pelos demais agentes:

1. **Entrada de dados no MVP é manual/importação de arquivo**, sem integração automática com bancos (Open Banking) — assumido por ausência de definição; integração automática é tratada como evolução de roadmap (Seção 15).
2. **Mercado geográfico inicial**: Brasil, com moeda BRL como padrão e conformidade LGPD como referência regulatória — assumido pelo idioma da solicitação; caso o mercado-alvo seja outro (ex.: EUA/GDPR, mercado multi-país), requisitos regulatórios e de moeda mudam substancialmente.
3. **Plataforma de acesso no MVP**: aplicação web responsiva; aplicativo mobile nativo é tratado como fase futura, não MVP.
4. **Modelo de monetização** (freemium, assinatura, etc.) não foi definido e não impacta este documento de visão funcional, mas deve ser definido antes do desenho de funcionalidades de billing/planos.
5. **"Multi-tenant"** é interpretado, no contexto B2C, como isolamento lógico entre contas de usuários individuais sobre infraestrutura compartilhada — não como suporte a organizações/empresas com múltiplos usuários sob um mesmo tenant corporativo.
6. **Idioma da plataforma**: português (Brasil) como idioma primário, dado o contexto da solicitação; internacionalização não é requisito confirmado do MVP.

---

## 12. Restrições

- **Restrição de produto inviolável**: o sistema nunca deve executar, autorizar ou intermediar movimentação financeira real (pagamentos, transferências, PIX, boletos). Qualquer proposta de funcionalidade futura nesse sentido exige nova análise de escopo, compliance e regulação — não pode ser adicionada incrementalmente sem revisão formal deste documento.
- O sistema deve estar em conformidade com a LGPD (Lei Geral de Proteção de Dados) desde o MVP, dado que trata dados financeiros pessoais sensíveis.
- O sistema não deve armazenar credenciais de acesso a contas bancárias reais dos usuários (login/senha de internet banking).
- Este documento não define stack tecnológica, linguagens, frameworks ou provedores de infraestrutura — essas decisões pertencem à fase de arquitetura técnica detalhada, conduzida pelos agentes Full Stack, respeitando os princípios arquiteturais da Seção 10.
- Funcionalidades não listadas na Seção 5 (Requisitos Funcionais) não devem ser implementadas sem atualização formal deste documento.

---

## 13. Riscos

| ID | Risco | Impacto | Mitigação Proposta |
|---|---|---|---|
| R-01 | Vazamento de dados financeiros sensíveis | Crítico — perda de confiança, exposição legal (LGPD) | Criptografia em trânsito/repouso, isolamento multi-tenant reforçado, auditoria de acesso, princípio de menor privilégio |
| R-02 | Cálculo incorreto de saldo/Pulse Score | Alto — erosão de confiança no produto | Regras de negócio determinísticas e testáveis (RN-001, RN-005), cobertura de testes rigorosa pelo agente QA |
| R-03 | Baixa retenção (típico de apps de finanças pessoais) | Alto — inviabilidade do modelo SaaS | Priorizar valor percebido rápido no onboarding (Pulse Score, insights imediatos) |
| R-04 | Categorização automática imprecisa gera frustração | Médio — usuário perde confiança na automação | Fallback manual sempre disponível (RF-024), aprendizado a partir de correções do usuário |
| R-05 | Ambiguidade regulatória não resolvida (mercado-alvo, LGPD vs. outras leis) | Alto — retrabalho de compliance | Validar mercado-alvo formalmente antes da fase de implementação (ver Seção 17) |
| R-06 | Escopo pressionado a incluir movimentação financeira real futuramente | Crítico — mudança radical de superfície de risco regulatório/segurança | Tratar como restrição inviolável (Seção 12), exigindo nova visão de produto caso ocorra |
| R-07 | Vazamento de dados entre tenants por falha de isolamento lógico | Crítico | Isolamento reforçado a nível de dados, testes de segurança dedicados (RF-047) |
| R-08 | Dependência futura de provedores de Open Banking (fora do MVP) | Médio (futuro) | Desenho de módulo de integração plugável e isolado do núcleo (Seção 10.2) |

---

## 14. MVP

O MVP do FinancePulse Engine deve entregar o ciclo completo de valor "registrar → entender → agir", sem funcionalidades que dependam de integrações externas não validadas.

**Incluso no MVP:**
- Cadastro, autenticação e onboarding (RF-001 a RF-008).
- Gestão de contas/carteiras (RF-009 a RF-013).
- Registro manual de transações e importação via CSV (RF-014 a RF-021).
- Categorização automática básica por regras + categorização manual (RF-022 a RF-025).
- Orçamentos por categoria com alertas (RF-026 a RF-029).
- Metas financeiras básicas (RF-030 a RF-032).
- Dashboard consolidado com Pulse Score inicial (RF-033 a RF-036).
- Relatórios básicos de gastos por categoria e período (RF-037 a RF-039).
- Notificações essenciais (estouro de orçamento, gasto atípico) (RF-040 a RF-043).
- Conformidade LGPD básica: exportação e exclusão de dados (RF-044 a RF-046).
- Isolamento multi-tenant e trilha de auditoria mínima (RF-047, RF-048).

**Explicitamente fora do MVP:**
- Integração automática com bancos via Open Banking.
- Aplicativo mobile nativo (web responsiva cobre o MVP).
- Multi-moeda.
- Rastreamento de investimentos/portfólio.
- Insights preditivos/recomendações via IA/ML.
- Contas compartilhadas/família.
- Backoffice administrativo avançado (RF-049, RF-050 podem ser versão mínima manual no MVP).

---

## 15. Roadmap do Produto

*(Fases sequenciais, sem datas fixas — sujeitas a priorização de negócio.)*

**Fase 1 — MVP**: conforme Seção 14. Foco em validar o ciclo core de valor e a confiabilidade do cálculo financeiro.

**Fase 2 — Automação e Engajamento**:
- Integração com Open Banking (leitura) para importação automática de extratos.
- Notificações expandidas (push mobile, mais gatilhos inteligentes).
- Melhoria do motor de categorização com aprendizado baseado em correções do usuário.
- Metas financeiras avançadas (metas compartilhadas com parceiro/família — sujeito a nova análise de escopo).

**Fase 3 — Inteligência Financeira**:
- Insights preditivos (projeção de saldo futuro, alertas preventivos).
- Recomendações personalizadas de economia (não-fiduciárias, apenas informativas).
- Suporte a multi-moeda.
- Rastreamento básico de investimentos (somente leitura/consolidação, sem execução de ordens).

**Fase 4 — Expansão de Plataforma**:
- Aplicativo mobile nativo (iOS/Android).
- Internacionalização (i18n) para novos mercados geográficos.
- Possível abertura de API pública para integrações de terceiros (parcerias fintech).

> Cada fase deve ser formalmente priorizada e aprovada pelo Product Owner antes do início de implementação pelos agentes Full Stack.

---

## 16. Glossário

| Termo | Definição |
|---|---|
| **Pulse Score** | Indicador proprietário e consolidado de saúde financeira do usuário, calculado a partir de sinais como consistência orçamentária, taxa de poupança e tendência de saldo. |
| **Tenant** | No contexto B2C deste produto, equivale à conta isolada de um usuário individual sobre a infraestrutura compartilhada. |
| **Lançamento / Transação** | Registro individual de uma movimentação financeira (receita ou despesa) associada a uma conta e categoria. |
| **Categorização automática** | Processo pelo qual o sistema atribui uma categoria a uma transação com base em regras/heurísticas, sem intervenção manual do usuário. |
| **Orçamento (Budget)** | Limite de gasto definido pelo usuário para uma categoria em um período recorrente. |
| **Meta (Goal)** | Objetivo financeiro definido pelo usuário (ex.: valor de economia) com prazo e acompanhamento de progresso. |
| **Open Banking** | Padrão regulatório/tecnológico que permite, mediante consentimento do usuário, o compartilhamento de dados financeiros entre instituições de forma segura e padronizada. |
| **LGPD** | Lei Geral de Proteção de Dados (Brasil) — legislação de proteção de dados pessoais aplicável a este produto por tratar dados financeiros sensíveis. |
| **RF** | Requisito Funcional. |
| **RN** | Regra de Negócio. |
| **UC** | Caso de Uso (Use Case). |
| **MVP** | Minimum Viable Product — menor conjunto de funcionalidades que entrega valor completo e testável ao usuário. |
| **RBAC** | Role-Based Access Control — controle de acesso baseado em papéis, usado no backoffice administrativo. |

---

## 17. Encerramento — Análise Crítica

### 17.1 Dúvidas Encontradas (Requerem Definição do Stakeholder)

1. **Mercado geográfico e regulatório**: o produto é destinado exclusivamente ao Brasil, ou há intenção de expansão internacional desde o início? Isso impacta diretamente moeda, idioma e framework regulatório (LGPD vs. GDPR vs. outros).
2. **Integração com Open Banking**: deve estar no MVP ou é aceitável um MVP com entrada manual/importação de arquivo, conforme assumido neste documento (Premissa 1, Seção 11)?
3. **Modelo de monetização**: freemium, assinatura única, tiers de funcionalidades? Isso afeta diretamente o desenho de requisitos de billing e limites por plano, que não foram incluídos neste documento por ausência de definição.
4. **Plataformas-alvo**: web responsiva é suficiente para o MVP, ou existe expectativa de aplicativo mobile nativo desde o lançamento?
5. **Fórmula do Pulse Score**: este documento define seu papel funcional (RN-005, RN-006), mas a composição exata do cálculo é uma decisão de produto/ciência de dados que precisa ser detalhada separadamente antes da implementação do RF-034.
6. **Contas compartilhadas/familiares**: existe demanda para múltiplos usuários compartilharem visibilidade sobre as mesmas contas (ex.: casais)? Atualmente fora de escopo (RN-003).
7. **Retenção de dados pós-exclusão de conta**: existem obrigações legais (ex.: fiscais) que exigem retenção de determinados dados mesmo após solicitação de exclusão pelo titular? Requer validação jurídica antes de finalizar RF-045/RN-008.

### 17.2 Sugestões de Melhoria

- Definir metas quantitativas de negócio (retenção, ativação, NPS) em documento complementar de Product Requirements, para orientar priorização entre as fases do roadmap.
- Conduzir pesquisa de usuário (discovery) com o público-alvo antes de finalizar detalhes de UX do onboarding, dado que a adesão inicial é crítica em produtos de finanças pessoais (Risco R-03).
- Avaliar parceria com provedor certificado de Open Banking com antecedência, mesmo que a integração só ocorra na Fase 2, para reduzir tempo de implementação futura.
- Considerar programa de auditoria de segurança externa (pentest) antes do lançamento comercial, dado o caráter sensível dos dados tratados.

### 17.3 Funcionalidades Futuras (Candidatas a Roadmap, Não Confirmadas)

- Insights preditivos e recomendações personalizadas via IA/ML.
- Rastreamento de investimentos e portfólio.
- Suporte a múltiplas moedas.
- Contas compartilhadas (família/casal).
- Aplicativo mobile nativo.
- API pública para parceiros/integrações de terceiros.
- Módulo de apoio à declaração de imposto de renda (sujeito a nova análise regulatória).

### 17.4 Riscos Técnicos

- Falha no isolamento multi-tenant a nível de dados pode gerar vazamento de dados financeiros entre usuários — risco crítico que deve ser tratado com testes de segurança dedicados desde o primeiro incremento de implementação (não apenas ao final do projeto).
- Cálculo financeiro incorreto (saldo, orçamento, Pulse Score) é um risco de confiança de produto tão crítico quanto um risco de segurança, e deve receber cobertura de teste equivalente à de um sistema financeiro real, mesmo sem movimentação de dinheiro efetiva.
- Dependência futura de provedores externos de Open Banking (Fase 2) introduz risco de disponibilidade e de mudança de contrato/API fora do controle do time — deve ser isolada arquiteturalmente (Seção 10.2) para não contaminar o núcleo do sistema.
- Ausência de definição de mercado/regulação (Dúvida 17.1.1) é um risco técnico indireto: decisões de modelagem de dados (moeda, formatos, retenção) tomadas precocemente sob a premissa "Brasil/LGPD" podem exigir retrabalho estrutural caso o mercado-alvo mude.

---

*Fim do documento. Este vision.md deve ser tratado como a fonte oficial e vinculante de requisitos para os agentes Full Stack e QA do FinancePulse Engine, até que uma nova versão seja formalmente aprovada.*
