import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedPeriodComparisonReport, seedSpendingByCategoryReport, server } from '../test/server'
import { renderApp } from '../test/renderApp'

const API_BASE_URL = 'http://localhost:8080'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

// "Data inicial"/"Data final" se repetem em mais de uma seção da página — as buscas por rótulo
// precisam ser escopadas à seção (within), senão são ambíguas.
function setDate(container: HTMLElement, label: string, value: string) {
  fireEvent.change(within(container).getByLabelText(label), { target: { value } })
}

describe('ReportsPage (fluxo completo via App real)', () => {
  it('generates the spending-by-category report for the selected range', async () => {
    seedSpendingByCategoryReport({
      startDate: '2026-07-01',
      endDate: '2026-07-31',
      totalExpense: 400,
      categories: [{ categoryId: 'category-1', categoryName: 'Alimentação', amount: 400, percentage: 100 }],
    })
    const user = userEvent.setup()
    renderApp('/reports')
    const section = screen.getByRole('region', { name: 'Gastos por categoria' })

    setDate(section, 'Data inicial', '2026-07-01')
    setDate(section, 'Data final', '2026-07-31')
    await user.click(within(section).getByRole('button', { name: 'Gerar relatório' }))

    expect(await within(section).findByText(/Total de despesas/)).toBeInTheDocument()
    expect(within(section).getByText(/Alimentação: R\$\s?400,00 \(100%\)/)).toBeInTheDocument()
  })

  it('compares two periods and shows category deltas, including undefined percentage change on a zero base', async () => {
    seedPeriodComparisonReport({
      periodA: { startDate: '2026-06-01', endDate: '2026-06-30', totalIncome: 1000, totalExpense: 400, net: 600 },
      periodB: { startDate: '2026-07-01', endDate: '2026-07-31', totalIncome: 1000, totalExpense: 500, net: 500 },
      categoryComparisons: [
        { categoryId: 'category-1', categoryName: 'Alimentação', amountPeriodA: 400, amountPeriodB: 500, delta: 100, percentageChange: 25 },
        { categoryId: 'category-2', categoryName: 'Lazer', amountPeriodA: 0, amountPeriodB: 50, delta: 50, percentageChange: null },
      ],
    })
    const user = userEvent.setup()
    renderApp('/reports')
    const section = screen.getByRole('region', { name: 'Comparação de períodos' })

    await user.click(within(section).getByRole('button', { name: 'Comparar' }))

    expect(await within(section).findByText(/Alimentação.*25%/)).toBeInTheDocument()
    expect(within(section).getByText(/Lazer.*variação indefinida/)).toBeInTheDocument()
  })

  /**
   * Achado de QA (Fase 13.6): os dois relatórios de leitura (useQuery) mostravam uma mensagem
   * genérica em qualquer erro, em vez da mensagem real do backend — inconsistente com o padrão já
   * estabelecido em todas as outras páginas do projeto. Corrigido para checar `instanceof ApiError`.
   */
  it('shows the real backend error message when the spending-by-category report fails, not a generic one', async () => {
    server.use(
      http.get(`${API_BASE_URL}/reports/spending-by-category`, () =>
        HttpResponse.json({ error: 'A data final deve ser posterior à data inicial.' }, { status: 400 }),
      ),
    )
    const user = userEvent.setup()
    renderApp('/reports')
    const section = screen.getByRole('region', { name: 'Gastos por categoria' })

    await user.click(within(section).getByRole('button', { name: 'Gerar relatório' }))

    expect(await within(section).findByRole('alert')).toHaveTextContent('A data final deve ser posterior à data inicial.')
  })

  it('shows the real backend error message when the period comparison fails, not a generic one', async () => {
    server.use(
      http.get(`${API_BASE_URL}/reports/period-comparison`, () =>
        HttpResponse.json({ error: 'A data final deve ser posterior à data inicial.' }, { status: 400 }),
      ),
    )
    const user = userEvent.setup()
    renderApp('/reports')
    const section = screen.getByRole('region', { name: 'Comparação de períodos' })

    await user.click(within(section).getByRole('button', { name: 'Comparar' }))

    expect(await within(section).findByRole('alert')).toHaveTextContent('A data final deve ser posterior à data inicial.')
  })

  it('downloads the spending-by-category CSV export', async () => {
    const user = userEvent.setup()
    renderApp('/reports')
    const section = screen.getByRole('region', { name: 'Gastos por categoria' })

    await user.click(within(section).getByRole('button', { name: 'Exportar CSV' }))

    await waitFor(() => expect(within(section).queryByRole('alert')).not.toBeInTheDocument())
  })

  it('downloads the transactions CSV export', async () => {
    const user = userEvent.setup()
    renderApp('/reports')
    const section = screen.getByRole('region', { name: 'Exportar transações' })

    await user.click(within(section).getByRole('button', { name: 'Exportar CSV' }))

    await waitFor(() => expect(within(section).queryByRole('alert')).not.toBeInTheDocument())
  })
})
