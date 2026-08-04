import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { seedKnownUser } from '../test/server'
import { renderApp } from '../test/renderApp'

describe('LoginPage (fluxo completo via App real)', () => {
  it('logs the user in and redirects to the authenticated home', async () => {
    seedKnownUser('user@example.com', 'StrongPass1')
    const user = userEvent.setup()
    renderApp('/login')

    await user.type(screen.getByLabelText('E-mail'), 'user@example.com')
    await user.type(screen.getByLabelText('Senha'), 'StrongPass1')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument())
    expect(sessionStorage.getItem('financepulse.token')).toBe('fake-jwt-token')
  })

  it('shows the backend error message for wrong credentials without redirecting', async () => {
    seedKnownUser('user@example.com', 'StrongPass1')
    const user = userEvent.setup()
    renderApp('/login')

    await user.type(screen.getByLabelText('E-mail'), 'user@example.com')
    await user.type(screen.getByLabelText('Senha'), 'WrongPassword')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciais inválidas.')
    expect(sessionStorage.getItem('financepulse.token')).toBeNull()
  })

  it('redirects an unauthenticated visitor from the protected home to the login page', async () => {
    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })
})
