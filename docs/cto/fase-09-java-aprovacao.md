# CTO — Aprovação da Fase 9 (Java): Relatórios

| Campo | Valor |
|---|---|
| Fase | 9 (Java) — RF-037, RF-038 completos; RF-039 completo para CSV, PDF fora do escopo (ver ADR-0021) |
| Aprovador | CTO / Principal Software Architect |
| Data | 2026-08-01 |
| Resultado | ✅ Aprovado |

> Conforme [README.md § Fluxo de Colaboração](../../README.md#fluxo-de-colaboração) e [agents/cto.md § Critérios de aprovação](../../agents/cto.md#critérios-de-aprovação), a aprovação final de toda fase é atribuição exclusiva do CTO, emitida por escrito após a aprovação de qualidade do QA. Este documento é esse parecer formal, conforme `rules.md` § 7.

## Escopo revisado

RF-037 (gastos por categoria em período explícito), RF-038 (comparativo entre dois períodos quaisquer) e RF-039 (exportação CSV de relatório e de dados de transações — PDF explicitamente fora do escopo), conforme delimitado em [ADR-0021](../adr/0021-fase-9-relatorios.md) e [roadmap.md](../../roadmap.md) — Fase 9.

## Insumos considerados

- [docs/qa/fase-09-java-review.md](../qa/fase-09-java-review.md) — parecer de qualidade do QA: **aprovado**, com uma vulnerabilidade real (CSV/Formula Injection) identificada e corrigida durante a própria implementação, e uma nota de usabilidade não bloqueante (BOM UTF-8).
- [ADR-0021](../adr/0021-fase-9-relatorios.md) — decisão de escopo CSV-only para RF-039, período explícito (sem presets internos) para RF-037/038, e a extração de `SpendingByCategoryCalculator`.
- vision.md § 4.9, RF-037 a RF-039 — texto literal citado como base das decisões de escopo desta fase.
- Código-fonte em `backend-java/src/`.

## Verificação de aderência arquitetural

- [x] Estrutura de pastas segue o padrão já estabelecido (`domain/ → application/ → adapters/`), estendida com `domain/report/errors/` (sem entidade — corretamente, já que não há estado persistido nesta fase), `application/usecases/report/`, `application/services/{PeriodComparisonCalculator,SpendingByCategoryCalculator,ReportPeriod}`.
- [x] Regra de dependência respeitada: nenhum arquivo em `domain/report/` ou nos use cases/serviços de relatório importa Spring, JPA ou Jackson. Verifiquei especificamente que `CsvWriter` (que sim depende de formatação de apresentação) vive exclusivamente em `adapters/in/web/`, nunca referenciado por `application/`.
- [x] **Extração de `SpendingByCategoryCalculator` corretamente justificada**: era lógica inline em `GetDashboardUseCase` (Fase 8); esta fase precisava do mesmo cálculo para um período explícito. Concordo com a decisão de extrair em vez de duplicar pela segunda vez — verifiquei que o refactor do `GetDashboardUseCase` é comportamentalmente idêntico (suíte da Fase 8, `GetDashboardUseCaseTest`/`DashboardControllerTest`, permanece verde sem nenhuma alteração de asserção).
- [x] **RF-039 escopado para CSV, com PDF explicitamente adiado**: concordo com o raciocínio de ADR-0021 — nenhuma biblioteca de PDF existe no projeto, adicionar uma introduziria uma dependência nova e decisões de layout não especificadas em requisito algum; vision.md já cita "CSV/PDF" como exemplo, não como par obrigatório. A separação entre dados estruturados (casos de uso) e serialização CSV (adaptador) preserva a opção de adicionar PDF depois sem redesenhar `application/`.
- [x] **Validação de período correta**: `ReportPeriod` (record com construtor compacto) rejeita `startDate` após `endDate` de forma centralizada, reaproveitada pelos três casos de uso — evita repetir a mesma checagem três vezes e garante consistência de comportamento (sempre HTTP 400 via `InvalidReportPeriodException`, nunca reordenação silenciosa).
- [x] **Endosso à correção do Achado 1 do QA (CSV/Formula Injection)**: concordo que esta era uma vulnerabilidade real, não uma hipótese — qualquer campo de texto livre já existente no domínio (`Transaction.description`, `Category.name`, `Account.name`, tags) alimenta a exportação, e a correção (prefixo de apóstrofo para valores iniciados por caracteres de fórmula) é o mitigador padrão recomendado pela OWASP para esta classe de vulnerabilidade, aplicado de forma genérica em `CsvWriter.escape` — protege automaticamente qualquer campo futuro que passe a ser exportado, não apenas os campos atuais.
- [x] `rules.md` § 3 atendido: `ReportControllerTest` inclui exportação CSV real via HTTP (não apenas o endpoint JSON), verificando o conteúdo do arquivo gerado, exercitando `Transaction`, `Account`, `Category` e a serialização CSV juntos contra a raiz de composição real.
- [x] Nenhuma funcionalidade de movimentação financeira real foi introduzida (RN-009) — toda a Fase 9 é leitura/agregação/exportação.
- [x] Nenhum desvio arquitetural adicional registrado pelo QA.

## Avaliação de riscos e dívidas técnicas herdadas ou introduzidas

- Ausência de exportação em PDF (RF-039) é uma decisão de escopo consciente, não uma dívida técnica velada — documentada em ADR-0021 e no README. Revisitar se/quando houver demanda concreta de produto.
- Ausência de BOM UTF-8 no CSV (nota do QA) é aceita como dívida técnica de baixa prioridade — trivial de corrigir quando houver relato real de usuário.
- Ausência de exportação CSV para o comparativo de períodos (RF-038) é uma decisão de escopo consciente — os dois exports já entregues cobrem a motivação central de RF-039; um terceiro pode ser adicionado com a mesma forma dos existentes, sem redesenho.
- A correção da vulnerabilidade de CSV injection reforça um padrão que deve ser lembrado em qualquer exportação futura para formatos abertos por ferramentas de terceiros (PDF incluso, se/quando implementado): todo texto livre controlado pelo usuário que alimenta um arquivo de exportação precisa do mesmo tipo de tratamento defensivo.

## Decisão

**A Fase 9 (Java) está aprovada.** A implementação entrega gastos por categoria (RF-037) e comparativo entre períodos (RF-038) com período explícito e validado, exportação CSV de relatório e de dados de transações (RF-039) com hardening correto contra uma classe real de vulnerabilidade de injeção, e reaproveita corretamente lógica já existente (`SpendingByCategoryCalculator`) em vez de duplicá-la. O parecer de qualidade do QA foi favorável, destacando que a vulnerabilidade de segurança identificada foi corrigida durante a própria implementação, com cobertura de teste dedicada. Não há ajuste adicional exigido pelo CTO.

Conforme `rules.md` § 7 (atualizado em 2026-07-31), esta aprovação do CTO encerra o ciclo interno dos agentes para esta fase; por decisão do stakeholder, o início da próxima fase não depende de uma aprovação explícita adicional em separado.
