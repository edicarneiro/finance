# QA — Revisão da Fase 13.1 (Frontend): Fundação Técnica + Autenticação

| Campo | Valor |
|---|---|
| Fase | 13.1 (Frontend) — fundação técnica + registro/login/logout, decorrente de ADR-0025 |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). A aderência arquitetural (stack de frontend, decomposição da Fase 13, decisões de CORS/backend) é atribuição do CTO — este documento cobre exclusivamente qualidade de código, testes, segurança e aderência a `rules.md` (incluindo a cláusula de teste de frontend adicionada nesta fase).

## Checklist de Qualidade

- [x] A implementação atende ao escopo de 13.1 conforme ADR-0025: registro, login, logout local, rotas protegidas, shell autenticado — nada além disso (nenhuma tela de MFA/recuperação de senha/perfil, corretamente ausente por não existir backend correspondente).
- [x] Nenhuma regra de negócio duplicada no cliente — validação Zod cobre apenas forma de entrada; toda mensagem de erro exibida ao usuário vem do backend (`ErrorResponse.error`), nunca reescrita silenciosamente no cliente.
- [x] Testes cobrem caminho principal, casos de erro e a regra de sessão (14 testes de frontend nesta fase, incluindo um teste de regressão para o Achado 1 abaixo).
- [x] Aderente à cláusula de frontend recém-adicionada a `rules.md` §3: `LoginPage.test.tsx`/`RegisterPage.test.tsx` renderizam o `App` real (rotas reais, `AuthProvider` real) e interceptam somente na rede via MSW — nenhum teste mocka `useAuth` ou o cliente HTTP diretamente para simular um fluxo de tela completo.
- [x] Sem degradação de performance evidente (bundle de produção: 350 KB / 108 KB gzip, dentro do esperado para a stack escolhida).
- [x] Código segue Clean Code e SOLID (ver detalhamento abaixo).
- [x] CORS adicionado a `backend-java` (pré-requisito da fase, não uma funcionalidade de produto) verificado quanto a não introduzir superfície de risco: sem `allowCredentials`, origem configurável (não `*`), métodos e headers explicitamente listados.
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada com stack, arquitetura, como rodar/testar e limitações conhecidas.
- [x] Não há introdução de funcionalidade fora do escopo — nenhuma tela para RF-004/005/006 foi construída (backend não as expõe).

## Verificação de Execução

```
npm test   → 14 testes, 100% passando (Vitest, jsdom, MSW), após a correção do Achado 1
npm run build → tsc -b && vite build, sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos, após a correção do Achado 2 (fast-refresh)
mvn test (backend-java) → 444 testes, 100% passando, incluindo os 5 novos testes de CORS/preflight
```

Verificação manual adicional contra o backend real (não apenas MSW): registro, login e handshake de CORS (preflight `OPTIONS` para `/auth/login` e para uma rota protegida, `/accounts`) executados com `backend-java` rodando de verdade, com origem `http://localhost:5173` — confirmando que os cabeçalhos `Access-Control-Allow-*` e o token emitido funcionam ponta a ponta fora do ambiente de teste.

## Achados Durante a Revisão

**1. `sessionExpired` nunca era limpo em um novo login bem-sucedido — o banner de sessão expirada reaparecia em uma sessão nova e válida (severidade: média — bug de UX real, corrigido nesta revisão)**

Em `AuthContext.tsx`, o estado `sessionExpired` era definido como `true` pelo listener de `onSessionExpired` (disparado por qualquer `401` de uma chamada autenticada) e só era limpo manualmente pelo botão "Fechar" do banner (`dismissSessionExpired`). Nenhum caminho de sucesso o limpava. Cenário real: sessão expira → usuário é redirecionado para `/login` (o `AppShell`, onde o banner vive, desmonta) → usuário faz login novamente com sucesso → é redirecionado de volta para `/` → `AppShell` remonta → como `sessionExpired` continuava `true` desde a expiração anterior, **o banner "sua sessão expirou" reaparecia imediatamente sobre uma sessão nova e completamente válida**, uma mensagem enganosa logo após o usuário ter acabado de se autenticar corretamente. **Resolução**: `login()` e `register()` agora chamam `setSessionExpired(false)` após obter um token com sucesso. Teste de regressão adicionado (`AuthContext.test.tsx` — "não reexibe o aviso de sessão expirada depois de um novo login bem-sucedido"), que expira a sessão, reloga via a função real de `useAuth`, e verifica que nenhum elemento com `role="alert"` permanece.

**2. `AuthContext.tsx` exportava o Context junto com o componente `AuthProvider` (severidade: baixa — encontrado pelo lint, corrigido nesta revisão)**

`oxlint` (regra `react/only-export-components`) sinalizou que misturar a exportação de um objeto `Context` com a de um componente no mesmo arquivo quebra o Fast Refresh do React (o arquivo inteiro é remontado em vez de apenas o componente alterado durante o desenvolvimento). **Resolução**: o `Context` e seu tipo foram movidos para `authContextInstance.ts`, um módulo dedicado sem componentes; `AuthContext.tsx` agora exporta apenas `AuthProvider`, e `useAuth.ts` importa o `Context` do novo módulo.

## Avaliação por Critério

**Clean Code**: nomes revelam intenção (`onSessionExpired`, `dismissSessionExpired`, `ProtectedRoute`); o comentário sobre `sessionStorage` vs. `localStorage` em `AuthContext.tsx` explica o "porquê" (janela de exposição para dados financeiros), não o "o quê". `httpClient.ts` centraliza toda a lógica de rede (injeção de `Authorization`, parsing de erro, disparo de sessão expirada) em um único módulo, evitando duplicação em `authApi.ts` e nas páginas.

**SOLID/estrutura**: `AuthContext`/`useAuth`/`httpClient` têm responsabilidades únicas e bem separadas (estado de autenticação vs. mecânica de rede vs. consumo do contexto). `ProtectedRoute` é um componente puro de roteamento, sem lógica de negócio. Nenhuma regra de validação de domínio (ex.: política de senha) foi duplicada no cliente — os schemas Zod validam apenas forma.

**Testes**: a suíte cobre os três fluxos de tela completos (login com sucesso, login com credenciais erradas, registro com sucesso, registro com senha divergente, registro com e-mail duplicado, redirecionamento de rota protegida) sempre via `App` real + MSW na fronteira de rede, conforme a nova cláusula de `rules.md` §3. `httpClient.test.ts` cobre o módulo de rede isoladamente (injeção de header, parsing de erro, disparo/não disparo do listener de sessão expirada conforme `authenticated: true/false`) — uma escolha correta, já que não há tela alguma que dependa desse comportamento diretamente nesta fase.

**Segurança**: token nunca logado; `sessionStorage` (não `localStorage`) escolhido deliberadamente e documentado; CORS no backend não usa `allowCredentials` (autenticação via header, não cookie) e não libera `*` como origem. Nenhuma tentativa de contornar a ausência de refresh token com armazenamento client-side de credenciais ou lógica especulativa de renovação.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. **Cobertura de "sessão expira durante o uso" ainda não passa por um clique de tela real** — a Home é um placeholder sem chamada de API nesta fase; o teste de regressão do Achado 1 exercita `AuthProvider`/`httpClient` reais mas não uma tela real fazendo a chamada. Documentado no README como limitação conhecida, com expectativa de fechar naturalmente na Fase 13.2 (primeira tela autenticada com chamada real).
2. **O comportamento de retornar à página originalmente solicitada após login (`location.state.from`) está implementado mas pouco exercitável** — só existe uma rota protegida (`/`) nesta fase, então o redirecionamento "de volta para onde eu estava" sempre aponta para o mesmo lugar. Passa a ter valor prático real a partir da Fase 13.2.
3. **Vulnerabilidade `npm audit` em `react-router` (RSC Mode CSRF Bypass)** — afeta apenas o modo experimental de React Server Components do React Router, que este projeto não usa (SPA puro, sem SSR, ver ADR-0025). Aceito como risco não aplicável; deve ser revisto quando o React Router publicar um patch.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real de UX/estado (banner de sessão expirada reaparecendo após um login válido) foi identificado e corrigido durante esta revisão, com teste de regressão dedicado. A fundação técnica estabelece corretamente os padrões que todas as subfases seguintes (13.2–13.9) vão reutilizar: cliente HTTP único, contexto de autenticação, rotas protegidas e a disciplina de teste via MSW na fronteira de rede. Nenhum apontamento crítico adicional foi identificado.
