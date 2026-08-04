# CTO — Aprovação Formal da Fase 13.8 (Frontend): Privacidade/LGPD

| Campo | Valor |
|---|---|
| Fase | 13.8 (Frontend) — Exportação de dados, consentimento e exclusão de conta |
| Decisão arquitetural nova? | Não — aplica integralmente ADR-0025 (React+TS+Vite+Router+TanStack Query+RHF+Zod+CSS Modules+Vitest+RTL+MSW) e ADR-0026 (Docker Compose dev) |
| Revisão de QA | [fase-13-8-frontend-review.md](../qa/fase-13-8-frontend-review.md) — ✅ Aprovado |
| Decisão | ✅ **Aprovado** |
| Data | 2026-08-03 |

## Escopo entregue

- `privacyApi.ts`: cliente tipado para `GET /privacy/export`, `POST/GET /privacy/consents`, `DELETE /users/me`, verificado linha a linha contra `PrivacyController.java`, `UserController.java` e seus DTOs reais.
- `PrivacyPage`: três seções independentes — exportação de dados (resumo por contagem + download do JSON completo gerado no cliente), consentimento (registro + histórico), e exclusão de conta com confirmação em duas etapas e reautenticação por senha, seguida de logout e retorno à tela de login.
- Navegação: link "Privacidade" adicionado ao `AppShell`, rota registrada em `App.tsx`.
- 6 novos testes de fluxo completo em `PrivacyPage.test.tsx` (App real + MSW na fronteira de rede) mais 1 teste de regressão em `httpClient.test.ts`. Suíte total: 62 testes, 13 arquivos, 100% passando.
- Verificação manual contra o backend real via Docker Compose: exportação real, registro/listagem real de consentimento, e confirmação do erro real de senha incorreta na exclusão — com a exclusão bem-sucedida deliberadamente não exercitada contra o backend real para preservar o usuário de dev compartilhado, permanecendo coberta pela suíte automatizada.

## Avaliação arquitetural

Nenhuma decisão nova. A exportação usa download client-side (Blob a partir do JSON já recebido) em vez de `httpClient.downloadFile` — não é uma capacidade de rede nova, apenas uma composição diferente do mesmo primitivo (`URL.createObjectURL` + `<a>` temporário), já que o endpoint real de exportação responde um corpo JSON comum, sem cabeçalho de anexo, ao contrário dos endpoints de exportação CSV da Fase 13.6.

## Achado de QA e resolução

O QA identificou e corrigiu um bug real de alta severidade: qualquer 401 vindo de uma chamada autenticada era tratado como sessão expirada pelo módulo de rede compartilhado (`httpClient.apiRequest`), inclusive quando o 401 vinha de uma senha de reautenticação incorreta em `DELETE /users/me` — um erro de validação de negócio genuíno (`InvalidCredentialsException`), não uma prova de token inválido. O efeito era deslogar silenciosamente um usuário que só errou a senha de confirmação ao tentar excluir a conta, sem nunca mostrar a mensagem real do backend. A correção introduz uma opção nova e mínima (`treatUnauthorizedAsSessionExpired`) no módulo de rede compartilhado, preservando o comportamento padrão para todas as chamadas existentes e resolvendo o caso específico sem enfraquecer a detecção real de sessão expirada em nenhum outro fluxo. Endosso a correção — é exatamente o tipo de achado que só aparece quando o caminho de erro de uma tela é genuinamente exercitado, reforçando (mais uma vez nesta fase 13) o valor da disciplina de TDD já estabelecida neste processo.

## Parecer do CTO

**Aprovado.** Escopo fiel ao contrato real do backend, sem invenção de funcionalidade, sem decisão arquitetural pendente de registro, e com um achado de QA de alta severidade corrigido de forma cirúrgica no módulo compartilhado, com teste de regressão em dois níveis (unidade e fluxo completo). A decisão de não exercitar a exclusão bem-sucedida contra o backend real foi criteriosa — evita destruir um recurso compartilhado (o usuário de dev seedado) do qual a próxima subfase ainda depende, sem abrir mão de cobertura automatizada real desse caminho. Autorizo a atualização do `roadmap.md` marcando a Fase 13.8 como concluída e o início da Fase 13.9 (Backoffice).
