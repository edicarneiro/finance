# ADR-0002: Estrutura de pastas para Arquitetura Hexagonal

| Campo | Valor |
|---|---|
| Status | Aceito |
| Data | 2026-07-30 |
| Autor | CTO / Principal Software Architect |
| Fase | 1 (estrutura válida para todas as fases seguintes) |

## Contexto

`rules.md` § 1 exige Arquitetura Hexagonal (Ports & Adapters) obrigatória em todo o backend, com a regra de dependência `adapters → application → domain`. É necessário definir a estrutura de pastas de referência que todos os módulos do backend seguirão, para que a regra seja verificável objetivamente pelo QA em cada revisão.

## Decisão

Estrutura de pastas do backend (`backend/src/`):

```
backend/src/
  domain/
    <contexto>/            ex.: user/
      <Entidade>.ts         entidades e value objects — zero dependência externa
      errors/                erros de domínio (ex.: DuplicateEmailError)
  application/
    ports/
      <Porta>.ts             interfaces que a aplicação define e os adaptadores implementam
    use-cases/
      <CasoDeUso>.ts          orquestra domínio + portas, sem conhecer detalhes de infraestrutura
  adapters/
    in/
      http/                   adaptador de entrada (Express): rotas, controllers, DTOs de request/response
    out/
      persistence/            adaptadores de saída que implementam portas de repositório
      security/                adaptadores de saída que implementam portas de hashing/token
  composition/
    container.ts               composition root: único lugar do sistema que conhece domínio, aplicação E adaptadores concretos, e faz a ligação (injeção de dependência manual)
```

Regras associadas:

- `domain/` não importa nada de `application/` ou `adapters/`, nem bibliotecas externas de infraestrutura (Express, drivers de banco, JWT, bcrypt). Pode usar apenas TypeScript puro e, no máximo, bibliotecas de domínio puras (ex.: validação de formato sem I/O).
- `application/ports/` define interfaces; `application/use-cases/` depende apenas dessas interfaces e do `domain/`, nunca de uma implementação concreta de adaptador.
- `adapters/*` implementam as portas definidas em `application/ports/` e podem depender livremente de bibliotecas de infraestrutura.
- `composition/container.ts` é o único arquivo autorizado a importar tanto `use-cases` quanto implementações concretas de `adapters/out/*`, instanciando e injetando as dependências.
- Testes de domínio e de use-cases vivem ao lado do arquivo testado (`Arquivo.ts` + `Arquivo.test.ts`), usando dublês de teste para as portas. Testes de adaptadores validam a implementação real contra a tecnologia que encapsulam.

## Alternativas Consideradas

| Alternativa | Motivo de não escolha |
|---|---|
| Estrutura em camadas técnicas (`controllers/`, `services/`, `models/`) | Não expressa explicitamente a fronteira porta/adaptador exigida pela Arquitetura Hexagonal; facilita o vazamento de detalhes de infraestrutura para a lógica de negócio, violando `rules.md` § 1. |
| Um módulo por feature sem separação hexagonal interna | Mais rápido para prototipagem, mas não atende ao requisito explícito de Arquitetura Hexagonal mandatado para o projeto. |

## Consequências

- Toda nova fase do roadmap segue esta mesma estrutura, adicionando novos subdiretórios em `domain/`, `application/` e `adapters/` conforme o contexto (ex.: `domain/account/`, `domain/transaction/` em fases futuras).
- O QA usa esta estrutura como critério objetivo de verificação de aderência arquitetural na revisão de qualidade (embora o mérito arquitetural em si seja atribuição do CTO — QA apenas registra desvios observáveis).
