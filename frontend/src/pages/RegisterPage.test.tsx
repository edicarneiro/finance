import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { seedKnownUser } from '../test/server'
import { renderApp } from '../test/renderApp'

describe('RegisterPage (fluxo completo via App real)', () => {
  it('registers, logs in automatically and redirects to the authenticated home', async () => {
    const user = userEvent.setup()
    renderApp('/register')

    await user.type(screen.getByLabelText('E-mail'), 'new-user@example.com')
    await user.type(screen.getByLabelText('Senha'), 'StrongPass1')
    await user.type(screen.getByLabelText('Confirmar senha'), 'StrongPass1')
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    await waitFor(() => expect(screen.getByText('Bem-vindo ao FinancePulse')).toBeInTheDocument())
  })

  it('shows a client-side validation error when passwords do not match, without calling the backend', async () => {
    const user = userEvent.setup()
    renderApp('/register')

    await user.type(screen.getByLabelText('E-mail'), 'new-user@example.com')
    await user.type(screen.getByLabelText('Senha'), 'StrongPass1')
    await user.type(screen.getByLabelText('Confirmar senha'), 'DifferentPass1')
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('As senhas não coincidem.')
  })

  it('shows the backend error message when the e-mail is already registered', async () => {
    seedKnownUser('taken@example.com', 'StrongPass1')
    const user = userEvent.setup()
    renderApp('/register')

    await user.type(screen.getByLabelText('E-mail'), 'taken@example.com')
    await user.type(screen.getByLabelText('Senha'), 'StrongPass1')
    await user.type(screen.getByLabelText('Confirmar senha'), 'StrongPass1')
    await user.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail já cadastrado.')
  })
})
