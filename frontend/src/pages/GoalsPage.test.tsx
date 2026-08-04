import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedAccount, seedCategory, seedGoal } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

// Inputs nativos type="date" não respondem de forma confiável a userEvent.type no jsdom
// (edição segmentada do seletor de data) — fireEvent.change é a forma recomendada pela
// Testing Library para esse caso.
function setDate(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

function goalsList() {
  return within(screen.getByRole('region', { name: 'Suas metas' }))
}

describe('GoalsPage (fluxo completo via App real)', () => {
  it('lists existing goals with progress', async () => {
    const account = seedAccount({ name: 'Poupança' })
    seedGoal({ name: 'Reserva de emergência', accountId: account.id, targetAmount: 1000, currentAmount: 400, progressPercentage: 40 })

    renderApp('/goals')

    expect(await goalsList().findByText(/Reserva de emergência/)).toBeInTheDocument()
    expect(goalsList().getByText(/Poupança/)).toBeInTheDocument()
  })

  it('creates a new goal associated to an account', async () => {
    seedAccount({ name: 'Carteira' })
    const user = userEvent.setup()
    renderApp('/goals')
    await goalsList().findByText('Nenhuma meta cadastrada ainda.')

    await user.type(screen.getByLabelText('Nome'), 'Viagem')
    await user.clear(screen.getByLabelText('Valor-alvo'))
    await user.type(screen.getByLabelText('Valor-alvo'), '5000')
    setDate('Prazo', '2027-01-01')
    await user.selectOptions(screen.getByLabelText('Conta'), 'Carteira')
    await user.click(screen.getByRole('button', { name: 'Criar meta' }))

    expect(await goalsList().findByText(/Viagem/)).toBeInTheDocument()
  })

  it('creates a new goal associated to a category, switching the association selector', async () => {
    seedCategory({ name: 'Alimentação' })
    const user = userEvent.setup()
    renderApp('/goals')
    await goalsList().findByText('Nenhuma meta cadastrada ainda.')

    await user.type(screen.getByLabelText('Nome'), 'Reduzir gasto com comida')
    await user.clear(screen.getByLabelText('Valor-alvo'))
    await user.type(screen.getByLabelText('Valor-alvo'), '300')
    setDate('Prazo', '2027-01-01')
    await user.selectOptions(screen.getByLabelText('Vincular a'), 'category')
    await user.selectOptions(screen.getByLabelText('Categoria'), 'Alimentação')
    await user.click(screen.getByRole('button', { name: 'Criar meta' }))

    expect(await goalsList().findByText(/Reduzir gasto com comida/)).toBeInTheDocument()
  })

  it('deletes a goal', async () => {
    const account = seedAccount({ name: 'Conta X' })
    seedGoal({ name: 'Meta a remover', accountId: account.id })
    const user = userEvent.setup()
    renderApp('/goals')

    const row = (await goalsList().findByText(/Meta a remover/)).closest('li')!
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(goalsList().queryByText(/Meta a remover/)).not.toBeInTheDocument())
  })
})
