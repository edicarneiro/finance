# ADR-0012: Integração do MFA com o fluxo de login (Fase 2.5.2)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | 2.5.2 |

## Contexto

A Fase 2.5.1 entregou cadastro/gestão de MFA sem alterar o login. Esta fase fecha RF-004: quando a conta tem MFA ativo, `POST /auth/login` não deve mais emitir tokens de sessão diretamente a partir de e-mail/senha.

## Decisões

### Login em duas etapas via "desafio" de curta duração

- `POST /auth/login` continua validando e-mail/senha. Se a conta **não** tem MFA ativo, o comportamento é idêntico ao de antes (`{ mfaRequired: false, token, refreshToken }`) — nenhuma mudança de contrato para contas sem MFA.
- Se a conta **tem** MFA ativo, em vez de tokens de sessão, a resposta é `{ mfaRequired: true, challengeToken }` (`200 OK`) — um token opaco de curta duração (**5 minutos**) e uso único, provando que e-mail/senha já foram validados, sem repetir a senha na segunda etapa.
- Novo `POST /auth/login/mfa` com `{ challengeToken, code }` valida o desafio e o código TOTP, então emite os tokens de sessão normalmente.

### `MfaChallenge`: nova entidade, mesma decisão de não generalizar do ADR-0011

`MfaChallenge` é estruturalmente quase idêntica a `PasswordResetToken` (opaco, hash em repouso, uso único, curta duração). O ADR-0011 já registrava que a "regra de três" seria satisfeita nesta fase — **a decisão de não generalizar permanece a mesma, pelos mesmos motivos**: evitar misturar uma refatoração estrutural de três fases já aprovadas com a entrega desta funcionalidade. Fica novamente registrado como candidato explícito a uma fase futura de consolidação técnica.

### `MfaChallengeIssuer`: novo colaborador, não inflar `AuthenticateUserUseCase`

Em vez de injetar `MfaChallengeRepository`, `MfaChallengeGenerator`, `IdGenerator`, `Clock` e o TTL diretamente em `AuthenticateUserUseCase` (que já tem `SessionIssuer` como colaborador desde a Fase 2.1), a emissão do desafio foi extraída para `MfaChallengeIssuer`, mesmo padrão do `SessionIssuer` (ADR-0007): um colaborador de aplicação com uma única responsabilidade ("emitir um desafio de MFA para um usuário"), mantendo o construtor de `AuthenticateUserUseCase` com apenas dois parâmetros novos (`mfaCredentialRepository`, `mfaChallengeIssuer`) em vez de cinco.

### `AuthenticateUserUseCase` — mudança de contrato de saída

A saída passa a ser um tipo condicional:

```
{ mfaRequired: false; token: string; refreshToken: string }
| { mfaRequired: true; challengeToken: string }
```

Evolução análoga à já realizada na Fase 2.1 (adição de `refreshToken` à saída de login) — mudança de contrato consciente e documentada, não um desvio silencioso. Todos os pontos de instanciação (testes, composition root) foram atualizados.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Exigir a senha novamente em `POST /auth/login/mfa` | Redundante — a senha já foi validada na primeira chamada; o token de desafio já prova isso. Pior UX sem ganho de segurança. |
| JWT stateless como token de desafio (em vez de opaco + hash em repouso) | Inconsistente com o padrão já estabelecido para segredos de uso único de curta duração (`PasswordResetToken`, ADR-0009) — um JWT não pode ser invalidado antes da expiração natural, e aqui precisamos marcar uso único explicitamente. |
| Injetar as dependências de desafio diretamente em `AuthenticateUserUseCase` | Infla o construtor de um use case que já orquestra várias portas; extrair `MfaChallengeIssuer` mantém a mesma disciplina de composição já usada para `SessionIssuer`. |

## Consequências

- Nova tabela `mfa_challenges`, seguindo o mesmo padrão de porta/adaptador (Sqlite/InMemory) das demais fases.
- `AuthenticateUserUseCase.test.ts` e todo teste que instancia esse use case diretamente (`ResetPasswordUseCase.test.ts`, `DeleteAccountUseCase.test.ts`, `server.test.ts`) precisaram ser atualizados para o novo construtor — mudança mecânica, sem alteração de comportamento nesses testes.
- Com esta fase, RF-004 está **completo de ponta a ponta**: cadastro, confirmação, desativação e exigência efetiva do segundo fator no login.
