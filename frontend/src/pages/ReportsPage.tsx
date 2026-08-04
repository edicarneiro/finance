import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import * as reportsApi from '../api/reportsApi'
import { ApiError } from '../api/httpClient'
import styles from './ReportsPage.module.css'

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount)
}

function isoDate(date: Date): string {
  return date.toISOString().slice(0, 10)
}

function daysAgo(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() - days)
  return isoDate(date)
}

const TODAY = isoDate(new Date())

export function ReportsPage() {
  return (
    <div>
      <h1>Relatórios</h1>
      <SpendingByCategorySection />
      <PeriodComparisonSection />
      <TransactionsExportSection />
    </div>
  )
}

function SpendingByCategorySection() {
  const [startDate, setStartDate] = useState(daysAgo(30))
  const [endDate, setEndDate] = useState(TODAY)
  const [submittedRange, setSubmittedRange] = useState<{ startDate: string; endDate: string } | null>(null)
  const [exportError, setExportError] = useState<string | null>(null)

  const reportQuery = useQuery({
    queryKey: ['reports', 'spending-by-category', submittedRange],
    queryFn: () => reportsApi.getSpendingByCategory(submittedRange!.startDate, submittedRange!.endDate),
    enabled: submittedRange !== null,
  })

  const exportMutation = useMutation({
    mutationFn: () => reportsApi.exportSpendingByCategory(startDate, endDate),
    onError: (error) => setExportError(error instanceof ApiError ? error.message : 'Não foi possível exportar o relatório.'),
  })

  return (
    <section aria-label="Gastos por categoria" className={styles.section}>
      <h2>Gastos por categoria</h2>
      <form
        className={styles.form}
        onSubmit={(event) => {
          event.preventDefault()
          setSubmittedRange({ startDate, endDate })
        }}
      >
        <label htmlFor="spendingStartDate">Data inicial</label>
        <input id="spendingStartDate" type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
        <label htmlFor="spendingEndDate">Data final</label>
        <input id="spendingEndDate" type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
        <div className={styles.actions}>
          <button type="submit">Gerar relatório</button>
          <button
            type="button"
            onClick={() => {
              setExportError(null)
              exportMutation.mutate()
            }}
          >
            Exportar CSV
          </button>
        </div>
      </form>

      {exportError && <p role="alert">{exportError}</p>}
      {reportQuery.isLoading && <p>Carregando…</p>}
      {reportQuery.isError && (
        <p role="alert">{reportQuery.error instanceof ApiError ? reportQuery.error.message : 'Não foi possível gerar o relatório.'}</p>
      )}
      {reportQuery.data && (
        <div>
          <p>
            Total de despesas de {reportQuery.data.startDate} a {reportQuery.data.endDate}:{' '}
            <strong>{formatCurrency(reportQuery.data.totalExpense)}</strong>
          </p>
          {reportQuery.data.categories.length === 0 ? (
            <p>Nenhum gasto no período.</p>
          ) : (
            <ul className={styles.list}>
              {reportQuery.data.categories.map((category) => (
                <li key={category.categoryId}>
                  {category.categoryName}: {formatCurrency(category.amount)} ({category.percentage.toFixed(0)}%)
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  )
}

function PeriodComparisonSection() {
  const [periodAStart, setPeriodAStart] = useState(daysAgo(60))
  const [periodAEnd, setPeriodAEnd] = useState(daysAgo(30))
  const [periodBStart, setPeriodBStart] = useState(daysAgo(30))
  const [periodBEnd, setPeriodBEnd] = useState(TODAY)
  const [submitted, setSubmitted] = useState<{ periodAStart: string; periodAEnd: string; periodBStart: string; periodBEnd: string } | null>(
    null,
  )

  const comparisonQuery = useQuery({
    queryKey: ['reports', 'period-comparison', submitted],
    queryFn: () => reportsApi.getPeriodComparison(submitted!.periodAStart, submitted!.periodAEnd, submitted!.periodBStart, submitted!.periodBEnd),
    enabled: submitted !== null,
  })

  return (
    <section aria-label="Comparação de períodos" className={styles.section}>
      <h2>Comparação de períodos</h2>
      <form
        className={styles.form}
        onSubmit={(event) => {
          event.preventDefault()
          setSubmitted({ periodAStart, periodAEnd, periodBStart, periodBEnd })
        }}
      >
        <fieldset>
          <legend>Período A</legend>
          <label htmlFor="periodAStart">Data inicial</label>
          <input id="periodAStart" type="date" value={periodAStart} onChange={(event) => setPeriodAStart(event.target.value)} />
          <label htmlFor="periodAEnd">Data final</label>
          <input id="periodAEnd" type="date" value={periodAEnd} onChange={(event) => setPeriodAEnd(event.target.value)} />
        </fieldset>
        <fieldset>
          <legend>Período B</legend>
          <label htmlFor="periodBStart">Data inicial</label>
          <input id="periodBStart" type="date" value={periodBStart} onChange={(event) => setPeriodBStart(event.target.value)} />
          <label htmlFor="periodBEnd">Data final</label>
          <input id="periodBEnd" type="date" value={periodBEnd} onChange={(event) => setPeriodBEnd(event.target.value)} />
        </fieldset>
        <button type="submit">Comparar</button>
      </form>

      {comparisonQuery.isLoading && <p>Carregando…</p>}
      {comparisonQuery.isError && (
        <p role="alert">
          {comparisonQuery.error instanceof ApiError ? comparisonQuery.error.message : 'Não foi possível comparar os períodos.'}
        </p>
      )}
      {comparisonQuery.data && (
        <div>
          <ul className={styles.list}>
            <li>
              Período A ({comparisonQuery.data.periodA.startDate} a {comparisonQuery.data.periodA.endDate}): saldo{' '}
              {formatCurrency(comparisonQuery.data.periodA.net)}
            </li>
            <li>
              Período B ({comparisonQuery.data.periodB.startDate} a {comparisonQuery.data.periodB.endDate}): saldo{' '}
              {formatCurrency(comparisonQuery.data.periodB.net)}
            </li>
          </ul>
          {comparisonQuery.data.categoryComparisons.length === 0 ? (
            <p>Nenhuma categoria com movimentação em nenhum dos períodos.</p>
          ) : (
            <ul className={styles.list}>
              {comparisonQuery.data.categoryComparisons.map((comparison) => (
                <li key={comparison.categoryId}>
                  {comparison.categoryName}: {formatCurrency(comparison.amountPeriodA)} → {formatCurrency(comparison.amountPeriodB)} (
                  {comparison.percentageChange === null ? 'variação indefinida' : `${comparison.percentageChange.toFixed(0)}%`})
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  )
}

function TransactionsExportSection() {
  const [startDate, setStartDate] = useState(daysAgo(30))
  const [endDate, setEndDate] = useState(TODAY)
  const [exportError, setExportError] = useState<string | null>(null)

  const exportMutation = useMutation({
    mutationFn: () => reportsApi.exportTransactions(startDate, endDate),
    onError: (error) => setExportError(error instanceof ApiError ? error.message : 'Não foi possível exportar as transações.'),
  })

  return (
    <section aria-label="Exportar transações" className={styles.section}>
      <h2>Exportar transações</h2>
      <form
        className={styles.form}
        onSubmit={(event) => {
          event.preventDefault()
          setExportError(null)
          exportMutation.mutate()
        }}
      >
        <label htmlFor="transactionsStartDate">Data inicial</label>
        <input id="transactionsStartDate" type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
        <label htmlFor="transactionsEndDate">Data final</label>
        <input id="transactionsEndDate" type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
        <button type="submit">Exportar CSV</button>
      </form>
      {exportError && <p role="alert">{exportError}</p>}
    </section>
  )
}
