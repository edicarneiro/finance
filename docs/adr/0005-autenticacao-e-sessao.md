# ADR-0005: Estratégia de autenticação e sessão — Fase 1

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 1 |

## Contexto

RF-003 exige autenticação via e-mail e senha; RF-008 exige emissão e validação de tokens de sessão com expiração e renovação segura. `rules.md` § 4 exige que senhas nunca sejam armazenadas em texto plano.

## Decisão

- Autenticação é **stateless**, via **JWT** assinado com segredo simétrico (`HS256`), lido de variável de ambiente (`JWT_SECRET`) — nunca hardcoded no código-fonte (`rules.md` § 4).
- O token carrega apenas o identificador do usuário (`sub`) e timestamps padrão de emissão/expiração — nenhum dado pessoal ou financeiro no payload, já que JWT não é criptografado, apenas assinado.
- Expiração do token de acesso: 15 minutos. Renovação segura (refresh token) **não está no escopo da Fase 1** — é registrada como pendência explícita (ver relatório de encerramento da Fase 1), pois a decisão de estratégia de refresh (rotação de refresh token, revogação, armazenamento) tem superfície própria e não deve ser resolvida às pressas dentro do escopo mínimo de "cadastro e login".
- Senhas são hasheadas com `bcryptjs` (custo de trabalho 10) antes de qualquer persistência, nunca logadas.
- A porta `application/ports/PasswordHasher.ts` abstrai o algoritmo de hashing; a porta `application/ports/TokenService.ts` abstrai a geração/validação de token — ambas substituíveis sem alterar use cases, conforme `rules.md` § 1 (Dependency Inversion).

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Sessão baseada em cookie + armazenamento server-side (ex.: Redis) | Adiciona uma dependência de infraestrutura (armazenamento de sessão) não justificada para o escopo mínimo da Fase 1; JWT stateless é suficiente para RF-008 sem essa dependência adicional. Pode ser revisitado se um requisito futuro exigir revogação imediata de sessão. |
| Expiração de token mais longa (ex.: 24h) sem refresh | Aumentaria a janela de exposição em caso de vazamento de token, contrariando `rules.md` § 4 (postura de segurança). Preferiu-se expiração curta, com o mecanismo de renovação explicitamente registrado como pendência a ser desenhado, em vez de comprometer segurança por padrão. |

## Consequências

- **Pendência formal para a Fase 2** (Gestão de Conta de Usuário): desenhar e implementar o mecanismo de refresh token, já que RF-008 menciona "renovação segura" e a Fase 1 cobre apenas emissão/validação inicial.
- Enquanto não houver refresh token, o usuário precisa re-autenticar a cada 15 minutos — aceitável para validação do MVP, mas deve ser comunicado como limitação conhecida.
