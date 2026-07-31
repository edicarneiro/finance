# ADR-0010: Exclusão de conta (Fase 2.4)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 2.4 |

## Contexto

RF-007 exige que o usuário exclua sua conta, "com confirmação explícita e exclusão/anonimização dos dados pessoais conforme LGPD". RN-008 do vision.md já alerta: "a exclusão de uma conta de usuário deve respeitar prazos e exceções legais de retenção de dados... a validar com jurídico" — e a Seção 17.1.7 do vision.md registra isso como dúvida formalmente **não resolvida** ("existem obrigações legais que exigem retenção de determinados dados mesmo após solicitação de exclusão?").

## Decisões

### Anonimização, não exclusão física

Como a questão de retenção legal está **explicitamente em aberto** no vision.md (não decidida, não é uma lacuna desta fase), excluir fisicamente a linha do usuário (`DELETE FROM users`) seria uma ação irreversível tomada sob incerteza — se depois se confirmar uma obrigação de retenção (ex.: fiscal), não haverá como reverter. A escolha mais conservadora e alinhada ao próprio texto do RF-007 ("exclusão/**anonimização**") é:

- `User` ganha um campo `deletedAt: Date | null` e um método `anonymize(anonymizedEmail, deletedAt)`.
- Ao "excluir" a conta: `email` é substituído por um valor sintético e único (`deleted-<uuid>@anonymized.financepulse.internal`), `name` é limpo, `passwordHash` é substituído por um hash válido de um valor aleatório inutilizável (nunca combina com nenhuma senha real), e `deletedAt` é registrado.
- A linha permanece no banco (id preservado), mas nenhum dado pessoal identificável resta associado a ela — atende ao espírito de "anonimização" da LGPD sem descartar a possibilidade de retenção mínima por obrigação legal futura, quando essa dúvida for resolvida com jurídico.
- Registros de consentimento (`consent_records`, ADR-0008) **não são apagados** — são uma trilha de auditoria própria, já desenhada como append-only; removê-los destruiria evidência de conformidade passada.

### Confirmação explícita: reautenticação por senha

- `DELETE /users/me` exige `{ password }` no corpo — a senha atual do usuário deve ser reenviada e validada antes de qualquer efeito. Isso satisfaz "confirmação explícita" do RF-007 sem inventar um fluxo adicional (ex.: confirmação por e-mail) não pedido pelo requisito, e segue o mesmo padrão de mercado usado por outros produtos para ações destrutivas de conta.
- Senha incorreta reaproveita `InvalidCredentialsError` (já existente, já mapeado para 401) — evita duplicar um tipo de erro quase idêntico.

### Efeitos colaterais da exclusão

- Todas as sessões ativas do usuário são revogadas (`RefreshTokenRepository.revokeAllForUser`), reaproveitando o mesmo mecanismo já usado em RF-005 (ADR-0009).
- Qualquer token de recuperação de senha pendente é invalidado (`PasswordResetTokenRepository.invalidateAllForUser`) — limpeza defensiva, mesmo sendo redundante com o login já bloqueado.
- `AuthenticateUserUseCase` passa a verificar `user.isDeleted()` explicitamente antes de comparar a senha — defesa em profundidade e clareza de intenção no código, ainda que o hash anonimizado já torne a autenticação impossível por si só.

## Limitação conhecida (não corrigida nesta fase)

Tokens de acesso (JWT) já emitidos antes da exclusão continuam válidos até sua expiração natural (15 minutos, ADR-0005) — são stateless por decisão da Fase 1 e não são verificados contra o estado do banco a cada requisição. Nesse intervalo, `GET /users/me` com um token antigo retornaria o perfil já anonimizado (sem vazar dados de outro usuário), não representando risco de confidencialidade, mas a "exclusão" não é instantaneamente refletida em chamadas já autenticadas. Corrigir isso exigiria uma lista de revogação de access tokens (infraestrutura adicional, ex.: cache compartilhado) — desproporcional ao risco atual do projeto nesta fase. Registrado como dívida técnica.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| `DELETE FROM users` físico | Irreversível diante de uma dúvida legal ainda não resolvida (vision.md Seção 17.1.7); RF-007 já permite anonimização como alternativa textual. |
| Confirmação via e-mail (fluxo similar à recuperação de senha) | Não exigido pelo texto do RF-007 ("confirmação explícita" é satisfeito por reautenticação); adicionaria fricção e complexidade não solicitadas. |
| Apagar `consent_records` do usuário excluído | Destruiria a própria trilha de auditoria de conformidade que RF-046 exige preservar. |

## Consequências

- `SqliteUserRepository` ganha a coluna `deleted_at` e seu `UPDATE` (já existente) precisa incluí-la — conforme a exigência já registrada em `rules.md` § 3, o smoke test do composition root (`container.integration.test.ts`) deve ser estendido para cobrir esse novo caminho.
- Nenhuma nova porta foi necessária — `findById`/`update` (já existentes desde a Fase 2.2) são suficientes.
