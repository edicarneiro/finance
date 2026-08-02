# ADR-0023: Fase 11 (Java) — Privacidade e Conformidade (LGPD): exportação, consentimento e exclusão de conta

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-08-01 |
| Autor | CTO / Principal Software Architect |
| Fase | 11 (Java) — RF-044, RF-045, RF-046 completos |

## Contexto

Fase 11 cobre RF-044 (exportação de todos os dados pessoais/financeiros), RF-045 (exclusão de conta respeitando prazos legais de retenção) e RF-046 (registro de consentimento).

**Duas questões de dependência/escopo precisam de decisão explícita:**

1. **RF-045 (exclusão de conta) é, na prática, o mesmo mecanismo de RF-007** ("usuário exclui sua conta, com confirmação explícita e exclusão/anonimização de dados conforme LGPD", Seção 4.2), já implementado no backend TypeScript na Fase 2.4 ([ADR-0010](0010-exclusao-de-conta.md)) — mas **não migrado para o backend Java** (`rules.md`/README já registram que perfil, recuperação de senha, exclusão de conta e MFA permanecem fora do backend Java, ver ADR-0014). RF-045 não pode ser satisfeito sem o mecanismo de exclusão existir — ao contrário de RF-043 (Fase 10) ou RF-022 (Fase 5), aqui não há como "adiar a parte que falta": a própria natureza de RF-045 é a exclusão em si. Diferente dos adiamentos anteriores, portanto, esta fase **constrói o mecanismo mínimo de exclusão de conta** (equivalente ao RF-007), sem reabrir o restante do escopo de "gestão de conta" ainda adiado (edição de perfil RF-006, recuperação de senha RF-005, MFA RF-004, refresh token RF-008) — esses continuam fora do escopo, sem relação de dependência com RF-045.
2. **RN-008 e vision.md § 17.2 (dúvida #7) já declaram formalmente que a política de retenção pós-exclusão é uma pendência jurídica não resolvida** ("existem obrigações legais que exigem retenção de determinados dados mesmo após solicitação de exclusão? Requer validação jurídica antes de finalizar RF-045/RN-008"). Mesma classe de pendência formal já tratada para o Pulse Score (RN-006, ADR-0020): implementar uma posição provisória, conservadora e documentada agora, sem fingir que a pendência jurídica foi resolvida.

## Decisão

### 1. Exclusão de conta — anonimização, não exclusão física (replica ADR-0010)

- `User` ganha `deletedAt: Instant?` e o método `anonymize(anonymizedEmail, unusablePasswordHash, deletedAt)`. Ao excluir: `email` é substituído por um valor sintético único (`deleted-{id}@anonymized.financepulse.internal`), `name` é limpo, `passwordHash` é substituído por um hash de um valor aleatório inutilizável, `deletedAt` é registrado. **A linha permanece no banco** (id preservado) — mesma decisão e mesmo raciocínio de ADR-0010: excluir fisicamente seria uma ação irreversível tomada sob uma incerteza jurídica já formalmente registrada como não resolvida.
- `DELETE /users/me` exige `{ password }` no corpo — reautenticação como confirmação explícita (RF-007), reaproveitando `PasswordHasher` e `InvalidCredentialsException` já existentes (mesmo padrão anti-enumeração: senha incorreta e usuário já anonimizado retornam o mesmo erro, já que a senha nunca mais confere após a anonimização — idempotência "de graça", sem checagem explícita de "já excluído").
- `AuthenticateUserUseCase` passa a verificar `user.isDeleted()` — defesa em profundidade, ainda que o hash anonimizado já torne a autenticação impossível por si só (mesmo raciocínio de ADR-0010).

### 2. Dados financeiros NÃO são apagados nem anonimizados — escopo provisório, pendente de validação jurídica

- **Apenas o registro `User` é anonimizado.** `Account`, `Transaction`, `Category`, `Budget`, `Goal`, `Notification`, `NotificationPreference`, `PulseScoreSnapshot` e `ConsentRecord` permanecem intactos, ainda vinculados ao mesmo `userId` (agora anonimizado) — RF-045 fala em "dados **pessoais**"; nenhum desses registros carrega identificação pessoal própria (nome/e-mail) além da referência de `userId`, que deixa de apontar para uma identidade reconhecível assim que `User` é anonimizado.
- **Esta é uma posição deliberadamente conservadora e provisória**, não uma decisão jurídica final — RN-008/§17.2 já declaram a pendência. Excluir fisicamente o histórico financeiro agora seria irreversível diante da mesma incerteza (podem existir obrigações de retenção fiscal sobre esse histórico); mantê-lo intacto, vinculado a um `userId` já sem identificação pessoal, preserva a opção de decidir depois (anonimizar mais, purgar, ou reter) sem ter destruído dado algum no caminho.
- Registros de consentimento (`ConsentRecord`) **nunca são apagados nem alterados** — trilha de auditoria própria, mesmo raciocínio de ADR-0010.

### 3. Consentimento — trilha auditável append-only (replica ADR-0008)

- `ConsentRecord(id, userId, version, acceptedAt)` — imutável, nunca atualizado ou apagado. `version` é fornecida pelo cliente (identificador da versão dos termos aceitos); o conteúdo jurídico da política **não é modelado nem armazenado** pelo sistema, mesma decisão de ADR-0008.
- `POST /privacy/consents` registra um novo aceite; `GET /privacy/consents` lista o histórico completo do usuário — a leitura não existia explicitamente em ADR-0008 (TS), mas vision.md § 4.11 ("central de privacidade... visualização de dados armazenados") justifica adicioná-la agora.

### 4. Exportação de dados (RF-044) — JSON, agregando todas as áreas já existentes no backend Java

- `GET /privacy/export` retorna um único documento JSON com perfil, contas, transações, categorias, orçamentos, metas, histórico de Pulse Score, notificações, preferências de notificação e histórico de consentimento — tudo que o backend Java já armazena para o usuário autenticado.
- **Apenas JSON, não CSV** — vision.md cita "ex.: JSON/CSV" (exemplo, não par obrigatório), mesmo raciocínio já aplicado a RF-039 (ADR-0021). Um export com múltiplas áreas de dados heterogêneas mapeia naturalmente para um documento JSON aninhado; um CSV exigiria ou um arquivo por área (complexidade de empacotamento, ex. .zip) ou achatar tudo em uma tabela sem sentido — sem ganho claro sobre JSON para o caso de uso central (LGPD, leitura por humano ou por outra ferramenta).
- `passwordHash` nunca é incluído no export — nunca houve exigência de exportar segredos, e fazê-lo seria uma falha de segurança, não uma feature.

### 5. Isolamento multi-tenant (RF-047)

Mesmo padrão já estabelecido: toda leitura usada pela exportação é escopada por `userId`; a exclusão de conta só afeta o próprio usuário autenticado (nunca um `userId` do corpo da requisição).

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Adiar RF-045 inteiramente até M2.4 (perfil/exclusão) ser migrado para Java | Ao contrário de RF-022/RF-043, RF-045 **é** o mecanismo de exclusão — não há uma "parte que falta" para adiar sem deixar RF-045 completamente não implementado. O restante de "gestão de conta" (RF-004–006, RF-008) não tem relação de dependência com RF-045 e continua adiado normalmente. |
| Excluir fisicamente `User` e cascatear a exclusão de todos os dados financeiros associados | Irreversível diante de uma pendência jurídica já formalmente registrada como não resolvida (RN-008/§17.2); reproduziria o mesmo erro que ADR-0010 já evitou para o registro `User` isolado. |
| Anonimizar também `Transaction.description` e `Goal.name`/`Budget` (campos de texto livre que o usuário pode ter preenchido com informação pessoal) | Nenhum requisito exige isso especificamente, e fazer isso obscureceria dados que podem ser relevantes para uma eventual obrigação de retenção fiscal — mesma cautela do item anterior. Registrado como limitação conhecida, não uma lacuna silenciosa. |
| Exportação em CSV (múltiplos arquivos) além de JSON | Vision.md trata "JSON/CSV" como exemplo, não par obrigatório; um export multi-área mapeia mais naturalmente para JSON aninhado. Mesmo raciocínio de ADR-0021 para RF-039. |
| Modelar `ConsentRecord` com um campo de "tipo" (termos de uso vs. política de privacidade vs. marketing) | RF-046 não especifica múltiplos tipos de consentimento; ADR-0008 já havia optado por um único registro genérico por aceite. Introduzir tipos agora seria escopo especulativo. |

## Consequências

- `UserRepository` ganha `update` (além de `findByEmail`/`findById`/`save` já existentes) — mesma evolução de contrato não disruptiva já usada em fases anteriores.
- Tokens de acesso (JWT) já emitidos antes da exclusão continuam válidos até a expiração natural (15 minutos) — mesma limitação conhecida já registrada em ADR-0010, ainda mais direta no backend Java (que não tem sequer a camada de sessão/refresh token da Fase 2.1 para mitigar). Registrado como dívida técnica.
- A posição "dados financeiros retidos, apenas `User` anonimizado" precisa ser revisitada assim que a validação jurídica de RN-008/§17.2 for concluída — pode exigir anonimização ou purga adicional dependendo do resultado.
- `roadmap.md` registra RF-045 como completo **sob a política provisória acima**, não como a palavra final sobre retenção de dados.
