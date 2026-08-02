import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedAccount } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

describe('AccountsPage (fluxo completo via App real)', () => {
  it('lists existing accounts with their balance and the consolidated total', async () => {
    seedAccount({ name: 'Carteira', balance: 150.5, currency: 'BRL' })
    seedAccount({ name: 'Poupança', balance: 300, currency: 'BRL', type: 'SAVINGS' })

    renderApp('/accounts')

    expect(await screen.findByText(/Carteira/)).toBeInTheDocument()
    expect(screen.getByText(/Poupança/)).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText(/Saldo consolidado/)).toHaveTextContent('R$ 450,50'))
  })

  it('creates a new account and shows it in the list', async () => {
    const user = userEvent.setup()
    renderApp('/accounts')
    await screen.findByText('Nenhuma conta cadastrada ainda.')

    await user.selectOptions(screen.getByLabelText('Tipo'), 'SAVINGS')
    await user.type(screen.getByLabelText('Nome'), 'Reserva de emergência')
    await user.clear(screen.getByLabelText('Moeda'))
    await user.type(screen.getByLabelText('Moeda'), 'BRL')
    await user.clear(screen.getByLabelText('Saldo inicial'))
    await user.type(screen.getByLabelText('Saldo inicial'), '500')
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByText(/Reserva de emergência/)).toBeInTheDocument()
  })

  it('archives an account after confirmation, hiding further archive actions for it', async () => {
    seedAccount({ name: 'Cartão antigo' })
    const user = userEvent.setup()
    renderApp('/accounts')

    const row = (await screen.findByText(/Cartão antigo/)).closest('li')!
    await user.click(within(row).getByRole('button', { name: 'Arquivar' }))
    await user.click(within(row).getByRole('button', { name: 'Confirmar arquivamento' }))

    await waitFor(() => expect(within(row).getByText(/\(arquivada\)/)).toBeInTheDocument())
    expect(within(row).queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument()
  })

  it('edits an account name inline', async () => {
    seedAccount({ name: 'Nome antigo' })
    const user = userEvent.setup()
    renderApp('/accounts')

    const row = (await screen.findByText(/Nome antigo/)).closest('li')!
    await user.click(within(row).getByRole('button', { name: 'Editar' }))
    const nameInput = within(row).getByLabelText('Nome')
    await user.clear(nameInput)
    await user.type(nameInput, 'Nome novo')
    await user.click(within(row).getByRole('button', { name: 'Salvar' }))

    expect(await screen.findByText(/Nome novo/)).toBeInTheDocument()
  })
})
