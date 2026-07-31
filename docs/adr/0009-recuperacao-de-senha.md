# ADR-0009: Recuperação de senha (Fase 2.3)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 2.3 |

## Contexto

RF-005 exige "recuperação de senha via fluxo seguro (e-mail com token de expiração limitada)". É a primeira funcionalidade do projeto que exige enviar uma notificação externa (e-mail) a um usuário — nenhuma decisão de provedor de e-mail foi tomada até agora (deliberadamente, por não ser necessária antes).

## Decisões

### Modelo do token de recuperação

- Novo `PasswordResetToken` em `domain/user/`: opaco, hash SHA-256 em repouso, TTL de **1 hora** (mais curto que o refresh token de 7 dias — RF-005 não especifica o valor, mas a natureza sensível de uma troca de senha justifica uma janela curta), uso único (`markUsed`).
- **Estruturalmente semelhante a `RefreshToken`** (ADR-0007), mas modelado como entidade própria, não generalizado em uma abstração comum. Extrair uma abstração compartilhada agora, com apenas dois casos de uso estruturalmente parecidos, seria abstração prematura (regra de três não satisfeita) — os dois têm semânticas de consumo diferentes (rotação contínua vs. uso terminal único) que poderiam divergir ainda mais no futuro.
- Ao solicitar uma nova recuperação, qualquer token de recuperação anterior ainda válido do mesmo usuário é invalidado (`invalidateAllForUser`) — evita múltiplos tokens de recuperação válidos simultâneos.
- Ao concluir a troca de senha com sucesso, **todas as sessões ativas do usuário são revogadas** (reaproveitando `RefreshTokenRepository.revokeAllForUser`, já existente desde a Fase 2.1) — prática de segurança padrão: uma troca de senha deve encerrar sessões que possam estar comprometidas.

### Prevenção de enumeração de contas

- `POST /auth/password-reset/request` **sempre responde de forma idêntica** (202 Accepted, corpo vazio) independentemente de o e-mail existir ou não. Só o formato do e-mail é validado (400 se malformado) — a existência da conta nunca é revelada, ao contrário do cadastro (RF-002 exige revelar e-mail duplicado; recuperação de senha não tem esse requisito, então aplicamos a postura mais segura por padrão, como já feito em `/auth/login` na Fase 1).
- Não são adicionadas contramedidas de canal de tempo (*timing attack*) nesta fase — diferença de tempo entre e-mail existente/inexistente ainda é teoricamente observável. Registrado como limitação conhecida, não corrigida agora por não ser um requisito explícito e para não introduzir complexidade (padding artificial de tempo) sem necessidade comprovada.

### Envio de notificação — porta desacoplada de infraestrutura real

- Nova porta `PasswordResetNotifier` (nível de aplicação, não "EmailSender" genérico — expressa a intenção "avisar sobre recuperação de senha", não o mecanismo).
- Adaptador desta fase: `ConsolePasswordResetNotifier`, que apenas loga o link/token (nenhum provedor de e-mail real integrado ainda). Mesma lógica do ADR-0003 (SQLite para a Fase 1): a porta isola a decisão de infraestrutura real (SendGrid, SES, etc.) para quando a fase de deployment/produção for desenhada, sem custo de retrabalho no domínio/aplicação.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Reaproveitar a porta `RefreshTokenGenerator` (Fase 2.1) para gerar o token de recuperação | Mesmo raciocínio de ISP do ADR-0007: seria reaproveitar um nome de porta com semântica de outro conceito. Optou-se por duplicar uma porta pequena (`PasswordResetTokenGenerator`) e aceitar a duplicação mínima, em vez de reabrir e renomear uma decisão já aprovada e implantada da Fase 2.1 sem necessidade comprovada. |
| Integrar um provedor de e-mail real agora (ex.: SMTP) | Decisão de infraestrutura de produção fora do escopo desta fase; a porta já isola essa troca futura a custo baixo. |
| Responder de forma diferente conforme o e-mail exista | Violaria a postura anti-enumeração já estabelecida no projeto (Fase 1, `/auth/login`). |

## Consequências

- Nova tabela `password_reset_tokens`, seguindo o mesmo padrão de porta/adaptador (Sqlite/InMemory) das fases anteriores.
- `ResetPasswordUseCase` depende de `RefreshTokenRepository` (para revogar sessões) além de `PasswordResetTokenRepository` — acoplamento intencional e documentado, não um vazamento de camada (ambos são portas da camada de aplicação).
- Antes de uma fase de deployment real, `ConsolePasswordResetNotifier` deve ser substituído por um adaptador de e-mail real — registrado como dívida técnica no relatório de encerramento desta fase.
