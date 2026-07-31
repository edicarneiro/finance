# ADR-0007: Estratégia de refresh token

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 2.1 |

## Contexto

ADR-0005 (Fase 1) definiu token de acesso JWT stateless de 15 minutos e registrou a renovação segura (RF-008) como pendência. É necessário decidir o desenho do refresh token.

## Decisão

- **Refresh token é um valor opaco de alta entropia** (`crypto.randomBytes(32)`, 256 bits), não um JWT — ao contrário do token de acesso, precisa ser revogável a qualquer momento, o que exige estado no servidor.
- **Armazenamento com hash**: o valor bruto nunca é persistido; apenas seu hash SHA-256 é gravado (`adapters/out/persistence`), para que um vazamento do banco não exponha tokens utilizáveis diretamente. SHA-256 (determinístico) foi escolhido em vez de bcrypt porque a busca precisa ser por igualdade exata (`WHERE token_hash = ?`); bcrypt não permite essa busca (hash não determinístico) e seu custo computacional é desnecessário para um segredo de alta entropia gerado aleatoriamente (diferente de senha, que é escolhida pelo usuário e de baixa entropia).
- **Rotação em cada uso**: ao renovar, o refresh token apresentado é revogado e um novo par (access + refresh) é emitido. O cliente deve sempre usar o refresh token mais recente.
- **Detecção de reuso**: se um refresh token já revogado for apresentado novamente, todas as sessões daquele usuário são revogadas imediatamente — sinal de possível token roubado sendo reutilizado após a rotação legítima.
- **TTL do refresh token**: 7 dias, sensivelmente mais curto que o padrão de mercado (ex.: 30 dias), dado o caráter sensível de dados financeiros do produto (postura de segurança conservadora, `rules.md` § 4).
- **Logout explícito**: revoga o refresh token apresentado, idempotente (chamar logout duas vezes com o mesmo token não é erro).
- Novo bounded context de domínio: `domain/session/`, com a entidade `RefreshToken` (estado: emitido, revogado, expirado) — distinto de `domain/user/`, pois representa o ciclo de vida de uma sessão, não do usuário em si.
- Geração do valor do refresh token usa uma porta dedicada (`RefreshTokenGenerator`), separada de `IdGenerator`: os dois têm requisitos de entropia e propósito diferentes (identificador interno vs. segredo portador de acesso), e mantê-los separados evita que uma mudança de requisito em um force mudança no outro (ISP).
- Um caso de uso comum a login e renovação (emitir um novo par de tokens para um `userId`) foi extraído para `application/services/SessionIssuer.ts`, evitando duplicação entre `AuthenticateUserUseCase` e o novo `RefreshAccessTokenUseCase`.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Refresh token também como JWT stateless | Não permite revogação antes da expiração natural — inaceitável para logout e para resposta a suspeita de comprometimento (exigência implícita de segurança de `rules.md` § 4). |
| Reaproveitar `IdGenerator` (UUID v4) para o valor do refresh token | Entropia menor (~122 bits) que o padrão adotado (256 bits) para um segredo de portador de sessão; o custo de uma porta dedicada é baixo e a postura de segurança do produto justifica a escolha mais conservadora. |
| Sem detecção de reuso (apenas rotação) | Rotação sozinha não diferencia um refresh legítimo de um token roubado sendo usado em paralelo; a revogação em massa ao detectar reuso é uma mitigação padrão de baixo custo de implementação. |

## Consequências

- Toda subfase futura que precise encerrar sessões de um usuário (ex.: Fase 2.4 — exclusão de conta) reutiliza `RefreshTokenRepository.revokeAllForUser`.
- `AuthenticateUserUseCase` (Fase 1) muda de assinatura de saída (`{ token }` → `{ token, refreshToken }`) — evolução esperada e coberta por atualização dos testes existentes, não uma quebra silenciosa de contrato (o QA valida isso na revisão desta subfase).
