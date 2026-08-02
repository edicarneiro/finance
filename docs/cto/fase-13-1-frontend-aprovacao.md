# CTO — Aprovação da Fase 13.1 (Frontend): Fundação Técnica + Autenticação

| Campo | Valor |
|---|---|
| Fase | 13.1 (Frontend) — fundação técnica + registro/login/logout, decorrente de ADR-0025 |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

Fundação técnica do frontend (stack, roteamento, cliente HTTP) e a primeira área funcional: registro, login, logout local e rotas protegidas, consumindo `AuthController` de `backend-java`. Conforme delimitado em [ADR-0025](../adr/0025-decomposicao-fase-13-frontend.md) e [roadmap.md](../../roadmap.md) — Fase 13.1.

## Insumos considerados

- [docs/qa/fase-13-1-frontend-review.md](../qa/fase-13-1-frontend-review.md) — parecer de qualidade do QA: **aprovado**, com um achado real de estado (banner de sessão expirada reaparecendo após um novo login válido) identificado e corrigido durante a própria revisão, com teste de regressão dedicado.
- [ADR-0025](../adr/0025-decomposicao-fase-13-frontend.md) — decisão de decompor a Fase 13, escolha de stack, tratamento da lacuna RF-004/005/006 e adição de CORS a `backend-java`.
- Auditoria da superfície real de API de `backend-java` (`AuthController`, `WebMvcConfig`, `AuthenticationInterceptor`, `JwtTokenServiceAdapter`) — confirmando a ausência de refresh token, MFA, recuperação de senha e edição de perfil antes de desenhar o frontend contra eles.
- vision.md Seção 10.1 ("Camada de Cliente: web responsiva no MVP... o cliente é uma camada de apresentação e interação") e § 12 (stack de frontend não definida pelo vision.md, decisão pertence à fase técnica) — base literal das decisões de stack desta fase.
- Código-fonte em `frontend/src/` e `backend-java/src/` (mudanças de CORS).

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue a arquitetura definida em ADR-0025 — `api/` (fronteira de rede), `auth/` (estado de autenticação), `routes/` (roteamento), `pages/`/`components/` (apresentação) — sem lógica de negócio de domínio duplicada em nenhuma camada.
- [x] **Nenhuma regra de negócio é reimplementada no cliente**: verifiquei que os schemas Zod (`loginSchema`, `registerSchema`) validam apenas forma de entrada (e-mail, tamanho mínimo de senha refletindo `PasswordPolicy.MIN_PASSWORD_LENGTH` apenas como feedback antecipado, não como fonte de verdade) — toda mensagem de erro de negócio efetivamente exibida vem de `ErrorResponse.error` do backend.
- [x] **Lacuna RF-004/005/006 corretamente tratada como pendência explícita, não lacuna silenciosa**: concordo com a decisão de ADR-0025 de não construir telas para funcionalidade que `backend-java` não expõe — replicar o mesmo erro já evitado para RF-043 (ADR-0022) seria apresentar funcionalidade que não funciona.
- [x] **CORS em `backend-java` (WebMvcConfig.addCorsMappings) verificado como escopo mínimo correto**: sem `allowCredentials` (autenticação via header `Authorization`, nunca cookie — nenhuma superfície de CSRF nova introduzida), origem configurável via propriedade (mesma disciplina de `financepulse.jwt.secret`), não libera `*`. Endosso à correção encontrada durante a implementação (não pelo QA, mas por TDD do próprio Full Stack): `AuthenticationInterceptor` bloqueava com 401 o preflight `OPTIONS` de toda rota protegida antes da correção — um bug que teria inviabilizado o frontend contra qualquer endpoint além de `/auth/**`. A correção (bypass de `OPTIONS` no interceptor) é correta e mínima: o preflight nunca carrega `Authorization` por design do navegador, e a requisição real subsequente continua sendo verificada normalmente.
- [x] **Endosso à correção do Achado 1 do QA** (`sessionExpired` não limpo em novo login): concordo que era um bug de estado real, não cosmético — um usuário via uma mensagem de erro falsa ("sua sessão expirou") imediatamente após se autenticar com sucesso. A correção é local e não introduz novo estado.
- [x] **Ausência de refresh token corretamente tratada como limitação herdada do backend, não contornada**: o frontend detecta `401` e desloga — não implementa nenhuma lógica especulativa de renovação sem endpoint correspondente, consistente com a disciplina de não inventar funcionalidade de backend inexistente já aplicada em todas as fases anteriores.
- [x] Equivalente de frontend a `rules.md` §3 atendido: `LoginPage.test.tsx`/`RegisterPage.test.tsx` renderizam o `App` real com MSW na fronteira de rede, não dublês do hook de autenticação.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009) — fase não toca em dado financeiro algum.
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- MFA, recuperação de senha e edição de perfil permanecem sem interface até que `backend-java` implemente RF-004/005/006 — dívida herdada do backend (trilha M2.x nunca retomada), não desta fase; registrado em `roadmap.md`.
- Sessão expira em 15 minutos sem aviso prévio — limitação herdada do backend (sem refresh token); aceitável para o volume de uso esperado do MVP, revisitar se a UX se mostrar um problema real em uso.
- Vulnerabilidade de `npm audit` em `react-router` (modo RSC, não usado por este projeto) — risco não aplicável à superfície de uso real, mas deve ser monitorado para uma atualização quando disponível.
- Cobertura de "sessão expira durante o uso" ainda não passa por uma tela real com chamada de API (só existe placeholder nesta fase) — fecha naturalmente na Fase 13.2, não uma dívida a resolver isoladamente.

## Decisão

**A Fase 13.1 (Frontend) está aprovada.** A fundação técnica (stack, cliente HTTP, autenticação, rotas protegidas) está corretamente alinhada à arquitetura definida em ADR-0025, sem duplicar regra de negócio do backend e sem apresentar funcionalidade que `backend-java` não suporta. O parecer de qualidade do QA foi favorável, destacando que um bug real de estado (banner de sessão expirada persistindo além de um novo login válido) foi corrigido durante a própria revisão, com teste de regressão dedicado. A correção de CORS no backend (incluindo o bypass de preflight no `AuthenticationInterceptor`) foi verificada e é o escopo mínimo necessário para desbloquear qualquer subfase seguinte. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
