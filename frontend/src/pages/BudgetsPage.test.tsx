import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedBudget, seedCategory } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

function budgetsList() {
  return within(screen.getByRole('region', { name: 'Seus orçamentos' }))
}

describe('BudgetsPage (fluxo completo via App real)', () => {
  it('lists existing budgets with consumed amount and period', async () => {
    const category = seedCategory({ name: 'Alimentação' })
    seedBudget({ categoryId: category.id, limitAmount: 500, consumedAmount: 300, consumedPercentage: 60 })

    renderApp('/budgets')

    expect(await budgetsList().findByText(/Alimentação/)).toBeInTheDocument()
    expect(budgetsList().getByText(/300,00/)).toBeInTheDocument()
  })

  it('creates a new monthly budget', async () => {
    seedCategory({ name: 'Transporte' })
    const user = userEvent.setup()
    renderApp('/budgets')
    await budgetsList().findByText('Nenhum orçamento cadastrado ainda.')

    await user.selectOptions(screen.getByLabelText('Categoria'), 'Transporte')
    await user.clear(screen.getByLabelText('Limite'))
    await user.type(screen.getByLabelText('Limite'), '300')
    await user.click(screen.getByRole('button', { name: 'Criar orçamento' }))

    expect(await budgetsList().findByText(/Transporte/)).toBeInTheDocument()
  })

  it('shows custom period date fields only when período customizado is selected', async () => {
    seedCategory({ name: 'Lazer' })
    const user = userEvent.setup()
    renderApp('/budgets')

    expect(screen.queryByLabelText('Início do período')).not.toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Período'), 'CUSTOM')

    expect(screen.getByLabelText('Início do período')).toBeInTheDocument()
    expect(screen.getByLabelText('Fim do período')).toBeInTheDocument()
  })

  it('deletes a budget', async () => {
    const category = seedCategory({ name: 'Saúde' })
    seedBudget({ categoryId: category.id })
    const user = userEvent.setup()
    renderApp('/budgets')

    const row = (await budgetsList().findByText(/Saúde/)).closest('li')!
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(budgetsList().queryByText(/Saúde/)).not.toBeInTheDocument())
  })

  it('shows budget history when requested', async () => {
    const category = seedCategory({ name: 'Educação' })
    seedBudget({ categoryId: category.id })
    const user = userEvent.setup()
    renderApp('/budgets')

    const row = (await budgetsList().findByText(/Educação/)).closest('li')!
    await user.click(within(row).getByRole('button', { name: 'Ver histórico' }))

    expect(await within(row).findByText(/2026-07-01 a 2026-07-31/)).toBeInTheDocument()
  })
})
