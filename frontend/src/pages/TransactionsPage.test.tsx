import { screen, waitFor, within } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedAccount, seedCategory, seedTransaction } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

function transactionsList() {
  return within(screen.getByRole('region', { name: 'Transações da conta' }))
}

// A lista de contas (e, depois de selecionada, o formulário de transação e suas categorias) carrega
// de forma assíncrona (TanStack Query) — esperar cada estágio existir evita corridas com a interação.
async function selectAccount(user: UserEvent, accountName: string) {
  const combo = screen.getByLabelText('Conta')
  await within(combo).findByRole('option', { name: accountName })
  await user.selectOptions(combo, accountName)
  await screen.findByLabelText('Categoria')
}

describe('TransactionsPage (fluxo completo via App real)', () => {
  it('lists transactions for the selected account', async () => {
    const account = seedAccount({ name: 'Carteira' })
    const category = seedCategory({ name: 'Alimentação' })
    seedTransaction({ accountId: account.id, categoryId: category.id, type: 'EXPENSE', amount: 42.5, date: '2026-08-01' })
    const user = userEvent.setup()

    renderApp('/transactions')
    await selectAccount(user, 'Carteira')

    expect(await transactionsList().findByText(/Alimentação/)).toBeInTheDocument()
    expect(transactionsList().getByText(/R\$\s?42,50/)).toBeInTheDocument()
  })

  it('creates a new transaction and shows it in the list', async () => {
    seedAccount({ name: 'Carteira' })
    seedCategory({ name: 'Transporte' })
    const user = userEvent.setup()

    renderApp('/transactions')
    await selectAccount(user, 'Carteira')
    await transactionsList().findByText('Nenhuma transação lançada nesta conta ainda.')

    await user.selectOptions(screen.getByLabelText('Tipo'), 'EXPENSE')
    await user.selectOptions(screen.getByLabelText('Categoria'), 'Transporte')
    await user.clear(screen.getByLabelText('Valor'))
    await user.type(screen.getByLabelText('Valor'), '25.90')
    await user.type(screen.getByLabelText('Descrição (opcional)'), 'Ônibus')
    await user.click(screen.getByRole('button', { name: 'Lançar transação' }))

    expect(await transactionsList().findByText(/Ônibus/)).toBeInTheDocument()
  })

  it('rejects a transaction with a zero amount via client-side validation, without calling the backend', async () => {
    seedAccount({ name: 'Carteira' })
    seedCategory({ name: 'Transporte' })
    const user = userEvent.setup()

    renderApp('/transactions')
    await selectAccount(user, 'Carteira')
    await user.selectOptions(screen.getByLabelText('Categoria'), 'Transporte')
    await user.clear(screen.getByLabelText('Valor'))
    await user.type(screen.getByLabelText('Valor'), '0')
    await user.click(screen.getByRole('button', { name: 'Lançar transação' }))

    expect(await screen.findByText('O valor deve ser maior que zero.')).toBeInTheDocument()
  })

  it('shows the backend error when launching a transaction into an archived account', async () => {
    seedAccount({ name: 'Cartão antigo', archived: true })
    seedCategory({ name: 'Transporte' })
    const user = userEvent.setup()

    renderApp('/transactions')
    await selectAccount(user, 'Cartão antigo')
    await user.selectOptions(screen.getByLabelText('Categoria'), 'Transporte')
    await user.clear(screen.getByLabelText('Valor'))
    await user.type(screen.getByLabelText('Valor'), '10')
    await user.click(screen.getByRole('button', { name: 'Lançar transação' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não é possível lançar uma transação em uma conta arquivada.')
  })

  it('deletes a transaction', async () => {
    const account = seedAccount({ name: 'Carteira' })
    const category = seedCategory({ name: 'Alimentação' })
    seedTransaction({ accountId: account.id, categoryId: category.id, description: 'Mercado' })
    const user = userEvent.setup()

    renderApp('/transactions')
    await selectAccount(user, 'Carteira')

    const row = (await transactionsList().findByText(/Mercado/)).closest('li')!
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(transactionsList().queryByText(/Mercado/)).not.toBeInTheDocument())
  })
})
