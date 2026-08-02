import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AppShell } from '../components/AppShell'
import { seedKnownUser, server } from '../test/server'
import { renderApp } from '../test/renderApp'
import { AuthProvider } from './AuthContext'
import { apiRequest } from '../api/httpClient'
import { useAuth } from './useAuth'

const API_BASE_URL = 'http://localhost:8080'

describe('logout', () => {
  it('clears the token and returns to the login page when "Sair" is clicked', async () => {
    sessionStorage.setItem('financepulse.token', 'pre-existing-token')
    const user = userEvent.setup()
    renderApp('/')

    await user.click(await screen.findByRole('button', { name: 'Sair' }))

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(sessionStorage.getItem('financepulse.token')).toBeNull()
  })
})

/**
 * A Fase 13.1 ainda não tem nenhuma tela autenticada que faça uma chamada
 * real à API (a Home é um placeholder até a Fase 13.5) — este teste exercita
 * a fiação real de AuthProvider + httpClient (não um dublê do hook de
 * autenticação) contra um endpoint autenticado simulado no limite de rede,
 * a cobertura ponta a ponta via clique de tela real virá com a Fase 13.2.
 */
function ProbeButton() {
  return (
    <button
      type="button"
      onClick={() => {
        apiRequest('/probe').catch(() => {})
      }}
    >
      Disparar chamada autenticada
    </button>
  )
}

describe('expiração de sessão', () => {
  it('desloga e exibe o aviso quando uma chamada autenticada retorna 401', async () => {
    server.use(http.get(`${API_BASE_URL}/probe`, () => HttpResponse.json({ error: 'Token expirado.' }, { status: 401 })))
    sessionStorage.setItem('financepulse.token', 'pre-existing-token')
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AuthProvider>
          <AppShell>
            <ProbeButton />
          </AppShell>
        </AuthProvider>
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Disparar chamada autenticada' }))

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Sua sessão expirou'))
    expect(sessionStorage.getItem('financepulse.token')).toBeNull()
  })

  /**
   * Achado de QA (Fase 13.1): sessionExpired nunca era limpo em um novo
   * login bem-sucedido — o banner "sua sessão expirou" reaparecia na tela
   * recém-autenticada mesmo com uma sessão nova e válida. Corrigido em
   * AuthContext.login/register.
   */
  it('não reexibe o aviso de sessão expirada depois de um novo login bem-sucedido', async () => {
    server.use(http.get(`${API_BASE_URL}/probe`, () => HttpResponse.json({ error: 'Token expirado.' }, { status: 401 })))
    seedKnownUser('user@example.com', 'StrongPass1')
    sessionStorage.setItem('financepulse.token', 'pre-existing-token')
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AuthProvider>
          <AppShell>
            <ProbeButton />
            <ReloginButton />
          </AppShell>
        </AuthProvider>
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Disparar chamada autenticada' }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Sua sessão expirou'))

    await user.click(screen.getByRole('button', { name: 'Logar de novo' }))

    await waitFor(() => expect(sessionStorage.getItem('financepulse.token')).toBe('fake-jwt-token'))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

function ReloginButton() {
  const { login } = useAuth()
  return (
    <button type="button" onClick={() => login('user@example.com', 'StrongPass1')}>
      Logar de novo
    </button>
  )
}
