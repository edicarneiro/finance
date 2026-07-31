# ADR-0001: Stack tecnológica do backend

| Campo | Valor |
|---|---|
| Status | **Superado por [ADR-0013](0013-migracao-java-spring-boot.md)** — mantido para registro histórico da decisão original (TypeScript/Node.js) |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 1 |

## Contexto

O vision.md não define stack tecnológica (isso é deliberadamente deixado para a fase de arquitetura técnica — ver vision.md Seção 12, Restrições). É necessário escolher uma stack para iniciar a Fase 1 (Fundação técnica + Cadastro e Login), que sirva de base para todo o backend do MVP.

Requisitos que influenciam a decisão:
- `rules.md` exige Arquitetura Hexagonal, TDD, SOLID e Clean Code como padrão obrigatório de todo o backend.
- vision.md (Seção 6) exige portabilidade (sem acoplamento rígido a um provedor específico) e observabilidade.
- O ambiente de desenvolvimento disponível já possui Node.js (v24) instalado, reduzindo fricção de setup.
- O produto é um SaaS B2C com necessidade de iteração rápida (MVP) mantendo qualidade (RN-002, RN-005 exigem cálculos financeiros corretos e confiáveis).

## Decisão

- **Linguagem/runtime**: TypeScript sobre Node.js.
  - Tipagem estática facilita a definição explícita de portas (interfaces) exigida pela Arquitetura Hexagonal, reduzindo erros de contrato entre camadas.
  - Ecossistema maduro de testes (Vitest) com suporte nativo a TDD.
- **Framework de testes**: Vitest.
  - Execução rápida, configuração mínima para TypeScript, API compatível com o padrão Jest (baixa curva de aprendizado), adequado para TDD (feedback rápido no ciclo red-green-refactor).
- **Framework HTTP (adaptador de entrada)**: Express.
  - Minimalista, maduro, não impõe estrutura ao domínio — adequado como adaptador fino em Arquitetura Hexagonal (o framework HTTP fica isolado na camada de adaptadores, nunca vaza para domínio/aplicação).
- **Hashing de senha**: bcryptjs.
  - Implementação pura em JavaScript (sem dependência de compilação nativa), reduzindo fricção de setup multiplataforma, mantendo o algoritmo bcrypt recomendado por `rules.md` § 4.
- **Token de sessão**: JWT (biblioteca `jsonwebtoken`), conforme RF-008.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Java/Spring Boot | Maior peso de boilerplate para o ritmo de iteração exigido pelo MVP; ecossistema de testes mais verboso para o ciclo TDD estrito adotado. |
| Python/FastAPI | Viável tecnicamente, mas o ambiente de desenvolvimento já está pronto para Node.js/TypeScript, e a tipagem estática de TypeScript favorece a explicitação de portas na Arquitetura Hexagonal com menos esforço de configuração (mypy adicionaria fricção extra). |
| argon2 para hashing | Requer compilação nativa (node-gyp), introduzindo fricção de setup no ambiente Windows atual sem ferramentas de build C++ garantidas. bcryptjs foi preferido por ser puro JS; a decisão pode ser revisitada em ADR futuro se um requisito de segurança mais rígido justificar a migração. |

## Consequências

- Todo o backend do FinancePulse Engine seguirá esta stack, salvo decisão explícita em contrário registrada em novo ADR.
- A escolha de bcryptjs em vez de argon2 é registrada como *trade-off aceito*, não como lacuna — revisar se requisitos de segurança mais rígidos surgirem (ex.: auditoria externa, Seção 17.2 do vision.md).
