# QA — Revisão da Fase 13.8 (Frontend): Privacidade/LGPD

| Campo | Valor |
|---|---|
| Fase | 13.8 (Frontend) — Exportação de dados, consentimento e exclusão de conta, consumindo `PrivacyController` e `UserController` |
| Revisor | QA / Code Reviewer |
| Data | 2026-08-03 |
| Resultado | ✅ Aprovado sob a ótica de qualidade |

> O QA não revisa arquitetura ([agents/qa.md](../../agents/qa.md)). Esta fase não introduziu nenhuma decisão arquitetural nova (aplica integralmente ADR-0025) — sem novo ADR do CTO, mesmo padrão de 13.2–13.7.

## Checklist de Qualidade

- [x] A implementação atende ao escopo real do backend: exportação de dados (`GET /privacy/export`), registro e histórico de consentimento (`POST`/`GET /privacy/consents`), e exclusão de conta com reautenticação por senha (`DELETE /users/me`).
- [x] Nenhuma regra de negócio duplicada no cliente — o aviso de que dados financeiros não são apagados nem anonimizados na exclusão reflete literalmente a decisão já registrada no ADR-0023, não uma posição nova do frontend.
- [x] Testes cobrem exportação com resumo de contagens, registro/listagem de consentimento, exclusão bem-sucedida com retorno à tela de login, exclusão com senha incorreta mostrando o erro real do backend, e cancelamento da confirmação de exclusão sem chamar o backend (6 testes novos em `PrivacyPage.test.tsx`, mais 1 teste de regressão em `httpClient.test.ts`). Suíte total: 62 testes, 13 arquivos, 100% passando.
- [x] Aderente à cláusula de frontend de `rules.md` §3: `PrivacyPage.test.tsx` renderiza o `App` real com MSW na fronteira de rede.
- [x] Sem degradação de performance evidente (bundle: 414,60 KB / 122,67 KB gzip).
- [x] Código segue Clean Code e SOLID (ver Achado abaixo — corrigido durante a revisão).
- [x] Documentação técnica entregue (`frontend/README.md`) atualizada, incluindo o achado generalizado como padrão para o módulo de rede compartilhado.
- [x] Não há introdução de funcionalidade fora do escopo — a exportação é oferecida como download de JSON gerado no cliente (o endpoint real não envia `Content-Disposition` de anexo, ao contrário das exportações CSV da Fase 13.6), não uma capacidade inventada.

## Verificação de Execução

```
npm test    → 62 testes, 100% passando (Vitest, jsdom, MSW), 2 execuções consecutivas estáveis
npm run build → sem erros de tipo, bundle gerado com sucesso
npm run lint → oxlint, sem avisos
```

Verificação manual adicional contra o backend real, via `docker compose -f docker-compose.dev.yml up`: exportação real de dados do usuário de dev (`dev@financepulse.local`) confirmando a forma exata do JSON contra `UserDataExportResponse.java`, registro e listagem real de um consentimento, e confirmação do `401`/`"E-mail ou senha inválidos."` real ao chamar `DELETE /users/me` com senha errada. A exclusão com senha *correta* foi deliberadamente **não** exercitada contra o backend real — anonimizaria o usuário de dev seedado, do qual a Fase 13.9 (ainda pendente) também depende; esse caminho permanece coberto pelo teste automatizado com MSW.

## Achados Durante a Revisão

**1. Um 401 de reautenticação por senha (não de sessão expirada) deslogava o usuário silenciosamente no meio do fluxo de exclusão de conta (severidade: alta — bug real de UX/confiabilidade, corrigido nesta revisão)**

`httpClient.apiRequest` já tinha uma distinção para `authenticated: false` (usada em `/auth/login`/`/auth/register`, onde nenhum header `Authorization` é enviado e um 401 significa "credenciais inválidas", não "sessão expirada"). Mas essa distinção não cobria o caso de `DELETE /users/me`: essa chamada **é** autenticada (precisa do header `Authorization` para identificar o usuário) e **também** exige uma senha de reautenticação no corpo da requisição como confirmação explícita da exclusão. `DeleteAccountUseCase` lança `InvalidCredentialsException` (mapeada para HTTP 401 pelo `GlobalExceptionHandler`) tanto para usuário inexistente quanto para senha de confirmação incorreta — um erro de validação de negócio, não uma prova de que o token JWT expirou. Como `apiRequest` tratava *qualquer* 401 de uma chamada autenticada como sessão expirada, disparando `onSessionExpired` (que zera o token e força redirecionamento para `/login` via `ProtectedRoute`), um usuário que digitasse a senha de confirmação errada era deslogado imediatamente — o componente com o formulário de exclusão era desmontado no meio do fluxo, e a mensagem de erro real do backend nunca chegava a ser exibida. **Descoberto** ao escrever o teste do caminho de senha incorreta: o teste travava com o botão preso em "Excluindo…", porque a seção capturada pelo teste já não fazia mais parte do documento (substituída pela tela de login) — o mesmo tipo de sintoma silencioso já visto em achados anteriores desta fase 13 (estado não observável até um teste dedicado ao caminho de erro exercitar exatamente esse cenário). **Resolução**: nova opção `treatUnauthorizedAsSessionExpired` em `apiRequest` (`true` por padrão, preservando o comportamento de todas as chamadas existentes; `false` apenas em `privacyApi.deleteAccount`) — o header `Authorization` continua sendo enviado normalmente, só o efeito colateral de "sessão expirada" no 401 é suprimido para esta chamada específica. Teste de regressão adicionado em `httpClient.test.ts` (unidade, no módulo de rede compartilhado) e reforçado pelo teste de fluxo completo em `PrivacyPage.test.tsx`.

## Avaliação por Critério

**Clean Code**: a nova opção `treatUnauthorizedAsSessionExpired` segue o mesmo estilo de `authenticated` já existente em `RequestOptions`, com comentário explicando o "porquê" (a diferença entre um 401 de sessão inválida e um 401 de senha de confirmação incorreta), não o "o quê".

**SOLID/estrutura**: `PrivacyPage` segue a mesma decomposição em seções independentes já estabelecida em `NotificationsPage`/`ReportsPage` (`ExportSection`, `ConsentSection`, `DeleteAccountSection`), cada uma com sua própria query/mutação.

**Testes**: o achado desta fase é particularmente valioso por ser um bug de **produção real que afeta toda a base de código**, não só esta tela — qualquer chamada futura que precise combinar autenticação com reautenticação por senha (o mesmo padrão já existe conceitualmente para MFA/RF-004, ainda não migrado para `backend-java`) se beneficia da correção.

**Segurança**: a correção não enfraquece a detecção real de sessão expirada — o comportamento padrão (`treatUnauthorizedAsSessionExpired: true`) permanece inalterado para todas as chamadas autenticadas existentes; a exclusão de conta continua exigindo o header `Authorization` válido além da senha de confirmação, então um 401 genuíno de token inválido nessa mesma chamada (não coberto pelo `password` incorreto, mas por um `token` inválido) ainda seria uma situação anômala que a mensagem de erro genérica cobre adequadamente sem deslogar incorretamente um usuário com sessão válida que só errou a senha.

## Itens Não Bloqueantes (registrados, não impedem aprovação)

1. A exportação exibe apenas contagens por categoria, não o conteúdo completo de cada item, na tela — o usuário vê o documento completo apenas no arquivo JSON baixado. Decisão de escopo (evitar uma tabela gigante e pouco legível de dados brutos), não uma lacuna de capacidade: todo o conteúdo do endpoint está no arquivo baixado.
2. Sem tela para MFA (RF-004) — o próprio `backend-java` não implementa isso ainda (só existe no backend TypeScript legado, nunca migrado), já documentado como limitação conhecida desde a Fase 13.1.

## Suspeita de Problema Arquitetural

Nenhuma identificada nesta revisão. Nenhum registro encaminhado ao CTO.

## Parecer

**Aprovado.** Um achado real e de alta severidade (deslogamento silencioso durante uma tentativa legítima de correção de senha no fluxo de exclusão de conta) foi identificado e corrigido durante esta revisão, com correção no módulo de rede compartilhado — beneficiando qualquer fluxo futuro do mesmo tipo — e teste de regressão tanto no nível de unidade (`httpClient.test.ts`) quanto no nível de fluxo completo (`PrivacyPage.test.tsx`). Toda alegação de comportamento foi verificada com execução real contra `backend-java` via Docker Compose, com a exceção deliberada e justificada da exclusão bem-sucedida (que anonimizaria o usuário de dev compartilhado). Nenhum apontamento crítico adicional foi identificado.
