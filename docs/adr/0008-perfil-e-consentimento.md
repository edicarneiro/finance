# ADR-0008: Edição de perfil e registro de consentimento (Fase 2.2)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 2.2 |

## Contexto

RF-006 exige que o usuário edite "nome, e-mail, preferências". RF-046 exige manter registro de consentimento do usuário para tratamento de dados. Nenhum dos dois é detalhado o suficiente no vision.md para implementação direta sem decisões de design.

## Decisões

### RF-006 — escopo desta subfase: nome e e-mail apenas

O vision.md não define o que constitui "preferências" do usuário (não há RF, RN ou glossário que especifique). Implementar um campo especulativo agora violaria a diretriz de não inventar requisitos. **"Preferências" fica fora do escopo desta subfase**, registrado como dúvida em aberto (ver relatório de encerramento da Fase 2.2) — será implementado quando um requisito concreto de preferência existir (ex.: preferências de notificação na Fase 10).

- `User` ganha um campo `name: string | null` (ausente no cadastro da Fase 1, que exige apenas e-mail/senha — RF-001).
- Edição de perfil é uma substituição completa de `{ name, email }` (não PATCH parcial), por simplicidade.
- Alteração de e-mail reaplica a validação de unicidade (RF-002) — mesma regra do cadastro.
- **Sem verificação de propriedade do novo e-mail** (sem envio de link de confirmação): o sistema inteiro ainda não possui mecanismo de verificação de e-mail (nem no cadastro da Fase 1). Consistente com o escopo atual, não uma lacuna nova desta subfase — registrado como limitação conhecida.
- Alteração de senha autenticada **não está incluída** aqui: RF-006 não a menciona explicitamente (senha é tratada por RF-005, recuperação, na Fase 2.3).

### RF-046 — registro de consentimento como trilha auditável (append-only)

- Novo conceito de domínio `ConsentRecord` (dentro do bounded context `domain/user/`, não um novo contexto — é um dado sobre o usuário, não um ciclo de vida próprio como sessão).
- Cada aceite de consentimento é um **registro imutável e append-only** (nunca atualizado ou apagado), com uma `version` (identificador da versão dos termos/política aceitos, fornecido pelo cliente) e `acceptedAt`. Isso segue o princípio de auditabilidade de `rules.md` § 6 — o histórico completo de consentimentos é preservado, não apenas o estado atual.
- O conteúdo/texto legal da política de privacidade **não é modelado nem armazenado** pelo sistema — está fora do escopo de engenharia (é conteúdo jurídico/produto); o sistema apenas registra que uma versão identificada foi aceita, quando.

### Consolidação de rota "quem sou eu"

A Fase 1 introduziu `GET /auth/me` como rota protegida mínima apenas para provar que a emissão/validação de token funcionava (RF-008), sem retornar dados reais de perfil. Com `GET /users/me` agora entregando o perfil real, `GET /auth/me` torna-se redundante — **removida** nesta subfase (Clean Code: sem código morto/duplicado). Os testes que validavam o fluxo de autenticação via `/auth/me` foram migrados para `/users/me`, sem perda de cobertura.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Modelar "preferências" como um campo JSON genérico livre | Complexidade especulativa sem caso de uso concreto (violaria "não desenhar para requisitos hipotéticos"); adiado até existir necessidade real. |
| `ConsentRecord` como um campo mutável único em `User` (`consentedAt`) | Perde o histórico em caso de múltiplas versões de política ao longo do tempo — não atende ao espírito auditável de RF-046 nem a `rules.md` § 6. |
| Manter `/auth/me` e `/users/me` em paralelo | Duplicação de responsabilidade sem justificativa — `/auth/me` não tinha propósito funcional além de smoke test, já coberto por `/users/me`. |

## Consequências

- `UserRepository` ganha `findById` e `update` (além de `findByEmail`/`save` da Fase 1) — evolução de contrato, não quebra (mesmo padrão do ADR-0007 para `AuthenticateUserUseCase`).
- Novo `ConsentRepository` (`save`, `findAllForUser`).
- Tabela `users` ganha coluna `name` (nullable); nova tabela `consent_records`. Sem migração automatizada ainda (mesma dívida técnica já registrada em ADR-0003) — aceitável enquanto não há dados de produção.
