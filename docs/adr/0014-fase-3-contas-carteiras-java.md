# ADR-0014: Fase 3 (Java) — Contas e Carteiras Financeiras + aplicação de autenticação a rotas protegidas

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | 3 — RF-009 a RF-013 (primeira fase construída diretamente em Java, sem equivalente TypeScript anterior) |

## Contexto

Por decisão do stakeholder, a Fase 3 (Contas e Carteiras Financeiras) é implementada agora, diretamente em `backend-java/`, **antes** da conclusão das fases M2.1–M2.5.2 da trilha de migração (refresh token, logout, perfil/consentimento, recuperação de senha, exclusão de conta, MFA). Esta é uma mudança deliberada de sequenciamento, não um desvio silencioso — registrada formalmente aqui e em `rules.md` § 7 (atualização de processo de 2026-07-31: a aprovação do stakeholder deixou de ser um bloqueio obrigatório entre fases).

**Consequência aceita**: o backend Java segue, a partir desta fase, com uma superfície de autenticação/conta menor que o backend TypeScript equivalente (sem refresh token — sessões expiram em 15 minutos sem renovação; sem logout; sem edição de perfil; sem recuperação de senha; sem exclusão de conta; sem MFA). Essas lacunas ficam registradas como dívida técnica explícita, não como omissão não identificada, e permanecem no roadmap como M2.1–M2.5.2 para quando o stakeholder decidir retomá-las.

Esta é também a primeira fase do backend Java a introduzir **dado financeiro de um usuário** — RF-047 (isolamento multi-tenant) passa a ser aplicável e inegociável a partir de agora (`rules.md` § 4), e é também a primeira fase que exige uma **rota HTTP protegida** de fato — até aqui, a validação de RF-008 (`AuthenticatedUserResolver`) existia mas não estava conectada a nenhum endpoint (decisão de escopo do ADR-0013, M1).

## Decisões

### Modelo de domínio

- **`Account`**: entidade com `id`, `userId` (nunca aceito do cliente — sempre derivado do token autenticado, RF-047), `type` (`AccountType`: `CHECKING`, `SAVINGS`, `CREDIT_CARD`, `CASH`, `DIGITAL_WALLET` — tradução direta dos cinco tipos do RF-009), `name`, `currency` (código ISO 4217, três letras maiúsculas), `balance` (`BigDecimal`, nunca `double`/`float`, para evitar erro de arredondamento em valor financeiro), `archived` (boolean) e `createdAt`.
- **RN-001** ("o saldo de uma conta é sempre derivado da soma de suas transações... exceto saldo inicial no momento da criação") é aplicado literalmente: `balance` é gravável apenas na criação (`initialBalance` do RF-009); não existe nenhuma operação de edição direta de saldo. Como a Fase 4 (Transações, RF-014 em diante) ainda não existe, o saldo atual de uma conta (RF-011) **é** o saldo inicial nesta fase — não uma simplificação, é a aplicação correta de RN-001 ao estado atual do sistema (soma de zero transações). RF-011 será estendido, não corrigido, quando a Fase 4 introduzir transações.
- **Edição (RF-010)** é restrita ao campo `name`. `type` e `currency` não são editáveis após a criação — ambos têm implicação financeira/estrutural (trocar o tipo de uma conta ou sua moeda depois de criada teria efeito ambíguo sobre RF-011/RF-012 sem uma regra de conversão, que não é requisito do vision.md). Se o usuário errar o tipo/moeda, a via é arquivar e recriar — decisão registrada aqui, não uma limitação não identificada.
- **Arquivamento (RF-010, RF-013)**: `archive()` é a única forma de remoção — não existe endpoint de exclusão definitiva nesta fase. RF-013 fala em impedir exclusão definitiva de contas **com histórico de transações**, mas como a Fase 4 ainda não existe, toda conta nesta fase tem, por definição, zero transações — ou seja, a distinção "com histórico vs. sem histórico" ainda não é observável no sistema. Implementar exclusão física condicional agora seria construir para um cenário (conta sem nenhuma transação, decidir se permite hard-delete) que se tornará obsoleto assim que a Fase 4 chegar. Decisão: **somente arquivamento nesta fase**; exclusão física condicional a "sem histórico" fica para quando "histórico" passar a existir (Fase 4). `archive()` é idempotente (arquivar uma conta já arquivada não é erro) — mesmo padrão já usado em `LogoutUseCase` no backend TypeScript.
- **Moeda (RF-009)**: campo obrigatório, validado como código ISO 4217 de três letras maiúsculas, mas **sem suporte real a multi-moeda** — vision.md (Seção 11, premissa 2) assume BRL como moeda padrão do mercado brasileiro, e multi-moeda está explicitamente no Pós-MVP (Seção 15). O saldo consolidado (RF-012) portanto **soma os saldos de todas as contas ativas em um único total**, sob a premissa MVP de operação em moeda única — não agrupado por moeda. Se um usuário criar contas em moedas diferentes, o total consolidado seria uma soma tecnicamente ingênua entre moedas; aceito como simplificação herdada da premissa do vision.md, não uma lacuna introduzida por esta fase. Revisar quando/se RF pós-MVP de multi-moeda for priorizado.

### Isolamento multi-tenant (RF-047)

Todo método de `AccountRepository` que lê ou escreve uma conta específica é escopado por `userId` na própria assinatura (`findByIdAndUserId`, `findAllByUserId`) — nunca um `findById` genérico seguido de checagem em memória. Reforça estruturalmente o isolamento no nível do repositório, conforme exigido por `rules.md` § 4, em vez de depender de disciplina do código de aplicação. Tentar acessar/editar/arquivar uma conta de outro usuário retorna o mesmo erro "não encontrada" (`AccountNotFoundException`, HTTP 404) usado para uma conta inexistente — mesma postura anti-enumeração já aplicada a login (ADR consistente com o padrão do backend TypeScript).

### Autenticação aplicada a rotas protegidas (primeira vez no backend Java)

- Implementado `AuthenticationInterceptor` (`adapters/in/web`), um `HandlerInterceptor` do Spring MVC registrado via `WebMvcConfigurer` apenas para `/accounts/**`. Reaproveita o `AuthenticatedUserResolver` já existente (M1) — apenas passa a ser efetivamente conectado a rotas HTTP agora.
- Mantém a decisão do ADR-0013 de **não** adotar `spring-boot-starter-security`: um interceptor comum do Spring MVC é suficiente para o requisito atual (validar um Bearer JWT e expor o `userId` autenticado ao controller via atributo da requisição), sem o acoplamento a autoconfiguração de login/lockdown que o starter completo traria.
- Em caso de token ausente ou inválido, o interceptor escreve a resposta 401 diretamente (mesmo formato `{ "error": "..." }` do `GlobalExceptionHandler`) e interrompe a cadeia — equivalente direto ao `requireAuth.ts` do backend TypeScript.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Implementar exclusão física condicional a "sem histórico de transações" já nesta fase | Toda conta nesta fase tem zero transações por definição (Fase 4 não existe ainda) — a condição de RF-013 não é observável; implementar agora seria construir para um estado do sistema que ainda não existe. |
| Saldo consolidado agrupado por moeda | Multi-moeda é explicitamente Pós-MVP no vision.md (Seção 15); agrupar por moeda agora seria escopo não solicitado. Revisitar se/quando multi-moeda entrar em escopo. |
| Adotar `spring-boot-starter-security` para a rota protegida | Mesma razão do ADR-0013: ativaria autoconfiguração de login/lockdown incompatível com o mecanismo de JWT já desenhado, para um ganho (a esta altura) inexistente sobre um interceptor simples. |
| Aguardar M2.1–M2.5.2 antes de iniciar a Fase 3 | Rejeitada por decisão explícita do stakeholder (2026-07-31) — registrada como mudança de sequenciamento, não desvio arquitetural. |

## Consequências

- Primeira fase do backend Java a manipular dado financeiro de usuário — RF-047 passa a ser critério de aprovação obrigatório do QA a partir de agora (já era regra em `rules.md` § 4, mas não era aplicável antes por ausência de dado financeiro).
- Primeira fase do backend Java com rota HTTP protegida — `AuthenticationInterceptor` fica disponível para reuso por toda fase futura que precise de autenticação (Fase 4 em diante).
- Dívida técnica explícita: sessões no backend Java não podem ser renovadas (sem refresh token) nem revogadas (sem logout/exclusão de conta) até M2.1/M2.4 serem retomadas — aceito pelo stakeholder como consequência da decisão de sequenciamento.
- RF-011/RF-012 serão estendidos (não corrigidos) quando a Fase 4 introduzir transações reais.
