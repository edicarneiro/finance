import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedDashboard, seedPulseScoreHistory } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

describe('DashboardPage (fluxo completo via App real)', () => {
  it('shows the consolidated balance, cash flow, pulse score and spending by category', async () => {
    seedDashboard({
      consolidatedBalance: 1500.5,
      cashFlow: { windowDays: 30, totalIncome: 3000, totalExpense: 1200, net: 1800 },
      spendingByCategory: [{ categoryId: 'category-1', categoryName: 'Alimentação', amount: 400, percentage: 33 }],
      pulseScore: {
        overallScore: 72,
        formulaVersion: 'pulse-v0-provisional',
        factors: [{ name: 'savingsRate', score: 80, weight: 0.25 }],
      },
    })

    renderApp('/')

    expect(await screen.findByText(/1\.500,50/)).toBeInTheDocument()
    expect(screen.getByText(/3\.000,00/)).toBeInTheDocument()
    expect(screen.getByText('72 / 100')).toBeInTheDocument()
    expect(screen.getByText(/pulse-v0-provisional/)).toBeInTheDocument()
    expect(screen.getByText(/Taxa de poupança: 80\/100/)).toBeInTheDocument()
    expect(screen.getByText(/Alimentação: R\$\s?400,00 \(33%\)/)).toBeInTheDocument()
  })

  it('shows the Pulse Score history for the selected window', async () => {
    seedPulseScoreHistory([
      { date: '2026-08-01', score: 70, formulaVersion: 'pulse-v0-provisional' },
      { date: '2026-07-01', score: 65, formulaVersion: 'pulse-v0-provisional' },
    ])

    renderApp('/')

    const historySection = within(await screen.findByRole('region', { name: 'Histórico do Pulse Score' }))
    expect(await historySection.findByText(/2026-08-01: 70\/100/)).toBeInTheDocument()
    expect(historySection.getByText(/2026-07-01: 65\/100/)).toBeInTheDocument()
  })

  it('refetches cash flow and spending by category with the newly selected summary window', async () => {
    seedDashboard({ cashFlow: { windowDays: 30, totalIncome: 100, totalExpense: 50, net: 50 } })
    const user = userEvent.setup()
    renderApp('/')

    await screen.findByText(/Receitas: R\$\s?100,00/)

    seedDashboard({ cashFlow: { windowDays: 7, totalIncome: 20, totalExpense: 5, net: 15 } })
    await user.selectOptions(screen.getByLabelText('Período do resumo (fluxo de caixa e gastos por categoria)'), '7')

    await waitFor(() => expect(screen.getByText(/Receitas: R\$\s?20,00/)).toBeInTheDocument())
  })

  it('shows an empty state when there is no spending in the period', async () => {
    seedDashboard({ spendingByCategory: [] })

    renderApp('/')

    expect(await screen.findByText('Nenhum gasto no período.')).toBeInTheDocument()
  })
})
