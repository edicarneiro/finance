import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import * as dashboardApi from '../api/dashboardApi'
import styles from './DashboardPage.module.css'

const FACTOR_LABELS: Record<string, string> = {
  budgetConsistency: 'Consistência de orçamento',
  savingsRate: 'Taxa de poupança',
  spendingDiversification: 'Diversificação de gastos',
  balanceTrend: 'Tendência de saldo',
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount)
}

const CASH_FLOW_WINDOW_OPTIONS = [7, 30, 90] as const
const HISTORY_WINDOW_OPTIONS = [30, 90, 180] as const

export function DashboardPage() {
  // Um único período rege fluxo de caixa E gastos por categoria — o backend calcula os dois a partir
  // da mesma janela de dias (GetDashboardUseCase), então o seletor fica fora das duas seções para não
  // sugerir que ele só afeta uma delas.
  const [summaryWindowDays, setSummaryWindowDays] = useState<number>(30)
  const [historyDays, setHistoryDays] = useState<number>(90)

  const dashboardQuery = useQuery({
    queryKey: ['dashboard', summaryWindowDays],
    queryFn: () => dashboardApi.getDashboard(summaryWindowDays),
  })
  const historyQuery = useQuery({
    queryKey: ['dashboard', 'pulse-score', 'history', historyDays],
    queryFn: () => dashboardApi.getPulseScoreHistory(historyDays),
  })

  if (dashboardQuery.isLoading) {
    return (
      <div>
        <h1>Dashboard</h1>
        <p>Carregando…</p>
      </div>
    )
  }

  if (dashboardQuery.isError || !dashboardQuery.data) {
    return (
      <div>
        <h1>Dashboard</h1>
        <p role="alert">Não foi possível carregar o dashboard.</p>
      </div>
    )
  }

  const dashboard = dashboardQuery.data

  return (
    <div>
      <h1>Dashboard</h1>

      <section aria-label="Saldo consolidado" className={styles.balance}>
        <h2>Saldo consolidado</h2>
        <p className={styles.balanceValue}>{formatCurrency(dashboard.consolidatedBalance)}</p>
      </section>

      <div className={styles.summaryWindow}>
        <label htmlFor="summaryWindowDays">Período do resumo (fluxo de caixa e gastos por categoria)</label>
        <select
          id="summaryWindowDays"
          value={summaryWindowDays}
          onChange={(event) => setSummaryWindowDays(Number(event.target.value))}
        >
          {CASH_FLOW_WINDOW_OPTIONS.map((days) => (
            <option key={days} value={days}>
              Últimos {days} dias
            </option>
          ))}
        </select>
      </div>

      <section aria-label="Fluxo de caixa" className={styles.section}>
        <h2>Fluxo de caixa</h2>
        <ul className={styles.cashFlowList}>
          <li>Receitas: {formatCurrency(dashboard.cashFlow.totalIncome)}</li>
          <li>Despesas: {formatCurrency(dashboard.cashFlow.totalExpense)}</li>
          <li>
            <strong>Saldo do período: {formatCurrency(dashboard.cashFlow.net)}</strong>
          </li>
        </ul>
      </section>

      <section aria-label="Pulse Score" className={styles.section}>
        <h2>Pulse Score</h2>
        <p className={styles.pulseScoreValue}>{dashboard.pulseScore.overallScore.toFixed(0)} / 100</p>
        <p className={styles.disclaimer}>
          Fórmula provisória ({dashboard.pulseScore.formulaVersion}) — a composição definitiva do Pulse Score ainda é uma decisão de
          produto pendente (RN-006).
        </p>
        <ul className={styles.factorsList}>
          {dashboard.pulseScore.factors.map((factor) => (
            <li key={factor.name}>
              {FACTOR_LABELS[factor.name] ?? factor.name}: {factor.score.toFixed(0)}/100
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Gastos por categoria" className={styles.section}>
        <h2>Gastos por categoria</h2>
        {dashboard.spendingByCategory.length === 0 && <p>Nenhum gasto no período.</p>}
        <ul className={styles.list}>
          {dashboard.spendingByCategory.map((spending) => (
            <li key={spending.categoryId}>
              {spending.categoryName}: {formatCurrency(spending.amount)} ({spending.percentage.toFixed(0)}%)
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Histórico do Pulse Score" className={styles.section}>
        <h2>Histórico do Pulse Score</h2>
        <label htmlFor="historyDays">Período</label>
        <select id="historyDays" value={historyDays} onChange={(event) => setHistoryDays(Number(event.target.value))}>
          {HISTORY_WINDOW_OPTIONS.map((days) => (
            <option key={days} value={days}>
              Últimos {days} dias
            </option>
          ))}
        </select>
        {historyQuery.isLoading && <p>Carregando…</p>}
        {historyQuery.isSuccess && historyQuery.data.length === 0 && <p>Sem histórico registrado ainda.</p>}
        <ul className={styles.list}>
          {historyQuery.data?.map((entry) => (
            <li key={entry.date}>
              {entry.date}: {entry.score.toFixed(0)}/100
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
