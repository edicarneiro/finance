# ADR-0011: Autenticação multifator via TOTP (Fase 2.5)

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-31 |
| Autor | CTO / Principal Software Architect |
| Fase | 2.5.1 (cadastro/gestão) e 2.5.2 (integração com login) |

## Contexto

RF-004 exige suporte a autenticação multifator opcional. O vision.md não especifica o mecanismo. É necessário escolher um segundo fator, desenhar seu ciclo de vida (cadastro, confirmação, desativação) e sua integração com o login.

## Decisões

### Mecanismo: TOTP (RFC 6238), não SMS

TOTP (Time-based One-Time Password, compatível com Google Authenticator, Authy, 1Password etc.) foi escolhido em vez de SMS: não introduz uma nova dependência de infraestrutura externa (provedor de SMS, com custo por mensagem e superfície de falha adicional — mesmo raciocínio que levou à Fase 2.3 a isolar o envio de e-mail atrás de uma porta em vez de integrá-lo diretamente), funciona offline no dispositivo do usuário, e é o padrão de mercado para MFA baseado em aplicativo.

A implementação do algoritmo (geração de segredo, cálculo/verificação de código, formato `otpauth://` para QR code) usa a biblioteca `otplib`, isolada atrás da porta `TotpService` — implementar HMAC-TOTP manualmente introduziria risco desproporcional (é exatamente o tipo de código criptográfico que não deve ser reescrito à mão).

### Decomposição em subfases

Dado o tamanho do escopo (cadastro, confirmação, desativação, e mudança do fluxo de login), a Fase 2.5 é dividida em:

- **2.5.1** — cadastro e gestão de MFA (`enroll`, `confirm`, `disable`, `status`), sem alterar o fluxo de login existente.
- **2.5.2** — login exige o segundo fator quando MFA está ativo para a conta.

Mesmo raciocínio do ADR-0006: preservar "cada fase totalmente concluível" antes de avançar.

### Segredo TOTP: criptografado em repouso, não apenas hash

Ao contrário de refresh tokens e tokens de recuperação de senha (Fases 2.1/2.3), que só precisam ser **comparados** (hash SHA-256 de mão única é suficiente), o segredo TOTP precisa ser **recuperado em texto plano** a cada login para calcular/validar o código — hashing não é aplicável aqui. A alternativa seria armazená-lo em texto plano, o que tornaria um vazamento do banco equivalente a comprometer o segundo fator de **todos** os usuários com MFA ativo, anulando o benefício de segurança do próprio recurso.

Decisão: o segredo é **criptografado em repouso** (AES-256-GCM, via `node:crypto`, sem nova dependência) por uma porta `SecretCipher`, com a chave de criptografia vinda de uma nova variável de ambiente (`MFA_ENCRYPTION_KEY`, 32 bytes em hex) — seguindo o mesmo padrão de `JWT_SECRET` (obrigatória, nunca hardcoded, `rules.md` § 4). A criptografia/decriptografia ocorre inteiramente no adaptador de persistência (`SqliteMfaCredentialRepository`); domínio e aplicação sempre trabalham com o segredo em texto plano, sem conhecer o mecanismo de proteção em repouso — mesmo princípio de isolamento já usado para o hash de refresh/reset tokens.

### Cadastro exige reautenticação por senha

Cadastrar ou desativar MFA exige `{ password }` como confirmação explícita — mesmo padrão já estabelecido em RF-007 (ADR-0010). Justificativa: sem essa exigência, um token de acesso já emitido e ainda válido (janela de 15 minutos, limitação conhecida desde a Fase 1) poderia ser usado por um atacante para cadastrar **seu próprio** segredo MFA na conta da vítima, estabelecendo persistência de acesso além da expiração natural do token roubado. Exigir a senha atual fecha esse vetor.

### Re-cadastro substitui o segredo anterior

Chamar `enroll` novamente substitui qualquer credencial anterior (confirmada ou pendente) — há no máximo uma credencial MFA por usuário (`UNIQUE` em `user_id`). Não há necessidade de desativar explicitamente antes de trocar de dispositivo/aplicativo autenticador.

### Duplicação estrutural com `PasswordResetToken` — decisão de não generalizar agora

A Fase 2.5.2 introduzirá `MfaChallenge`, um token opaco de uso único e curta duração — estruturalmente quase idêntico a `PasswordResetToken` (ADR-0009), que já havia sido comparado a `RefreshToken` (ADR-0007) com a mesma conclusão de não generalizar. Com `MfaChallenge`, a "regra de três" mencionada no ADR-0009 passa a ser satisfeita. **Decisão**: mesmo assim, não generalizar nesta fase — extrair uma abstração compartilhada agora misturaria uma mudança estrutural de três fases já aprovadas com a entrega de uma funcionalidade nova, aumentando o raio de impacto da revisão sem necessidade funcional. Registrado como candidato explícito a uma fase futura dedicada de consolidação técnica, não como pendência esquecida.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| SMS como segundo fator | Nova dependência de infraestrutura externa (provedor de SMS) e custo operacional, sem necessidade comprovada; TOTP atende RF-004 sem essa dependência. |
| Armazenar o segredo TOTP em texto plano | Um vazamento de banco comprometeria o segundo fator de todos os usuários — inaceitável para o objetivo de segurança do próprio recurso. |
| Implementar HMAC-TOTP manualmente (sem biblioteca) | Código criptográfico feito à mão é uma fonte desproporcional de risco frente ao baixo custo de usar uma biblioteca madura e amplamente auditada (`otplib`). |
| Permitir múltiplas credenciais MFA simultâneas por usuário | Sem caso de uso concreto exigido por RF-004; adiciona complexidade de UX (qual credencial usar) sem benefício claro nesta fase. |

## Consequências

- Nova variável de ambiente obrigatória: `MFA_ENCRYPTION_KEY` (32 bytes em hex) — o processo falha ao iniciar se ausente, mesmo padrão de `JWT_SECRET`.
- Nova dependência: `otplib`.
- `AuthenticateUserUseCase` (Fase 1) terá sua saída estendida na Fase 2.5.2 para acomodar o desafio de MFA — evolução análoga à já realizada na Fase 2.1 (adição de `refreshToken` à saída de login).
