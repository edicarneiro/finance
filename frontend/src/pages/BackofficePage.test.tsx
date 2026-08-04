import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedBackofficeAuthorized, seedBackofficeUser } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

describe('BackofficePage (fluxo completo via App real)', () => {
  it('shows the real backend error when the logged-in user is not a support operator', async () => {
    seedBackofficeAuthorized(false)
    const user = userEvent.setup()
    renderApp('/backoffice')

    await user.type(screen.getByLabelText('ID do usuário'), 'user-99')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    const section = screen.getByRole('region', { name: 'Dados do usuário' })
    expect(await within(section).findByText('Acesso negado: esta ação exige permissão de operador de suporte.')).toBeInTheDocument()
  })

  it('shows the real backend error when the target user does not exist', async () => {
    seedBackofficeAuthorized(true)
    const user = userEvent.setup()
    renderApp('/backoffice')

    await user.type(screen.getByLabelText('ID do usuário'), 'user-does-not-exist')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    const section = screen.getByRole('region', { name: 'Dados do usuário' })
    expect(await within(section).findByText('Usuário não encontrado.')).toBeInTheDocument()
  })

  it('looks up an existing user and shows a data summary', async () => {
    seedBackofficeAuthorized(true)
    seedBackofficeUser('user-42', { email: 'cliente42@financepulse.local', accountsCount: 3, transactionsCount: 12 })
    const user = userEvent.setup()
    renderApp('/backoffice')

    await user.type(screen.getByLabelText('ID do usuário'), 'user-42')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    const section = screen.getByRole('region', { name: 'Dados do usuário' })
    expect(await within(section).findByText('E-mail: cliente42@financepulse.local')).toBeInTheDocument()
    expect(within(section).getByText('Contas: 3')).toBeInTheDocument()
    expect(within(section).getByText('Transações: 12')).toBeInTheDocument()
  })

  it('suspends an account and reflects the action in the audit log', async () => {
    seedBackofficeAuthorized(true)
    seedBackofficeUser('user-42')
    const user = userEvent.setup()
    renderApp('/backoffice')

    await user.type(screen.getByLabelText('ID do usuário'), 'user-42')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    const section = await screen.findByRole('region', { name: 'Dados do usuário' })
    await within(section).findByText(/E-mail:/)

    await user.type(within(section).getByLabelText('Motivo (opcional)'), 'Atividade suspeita reportada pelo cliente')
    await user.click(within(section).getByRole('button', { name: 'Suspender conta' }))

    expect(await within(section).findByText('Conta suspensa.')).toBeInTheDocument()
    expect(await within(section).findByText(/Conta suspensa por operator-1/)).toBeInTheDocument()
    expect(within(section).getByText(/Atividade suspeita reportada pelo cliente/)).toBeInTheDocument()
  })

  it('reactivates an account', async () => {
    seedBackofficeAuthorized(true)
    seedBackofficeUser('user-42')
    const user = userEvent.setup()
    renderApp('/backoffice')

    await user.type(screen.getByLabelText('ID do usuário'), 'user-42')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    const section = await screen.findByRole('region', { name: 'Dados do usuário' })
    await within(section).findByText(/E-mail:/)

    await user.click(within(section).getByRole('button', { name: 'Reativar conta' }))

    expect(await within(section).findByText('Conta reativada.')).toBeInTheDocument()
    expect(await within(section).findByText(/Conta reativada por operator-1/)).toBeInTheDocument()
  })

  // Achado de QA (Fase 13.9): o backend real registra uma entrada VIEWED_USER_DATA no log de
  // auditoria em toda consulta de suporte (GetUserForSupportUseCase, RF-048) — o log nunca está
  // vazio depois de uma busca bem-sucedida, mesmo sem nenhuma ação de suspensão/reativação ainda.
  it('logs the lookup itself in the audit trail, even before any suspend/reactivate action', async () => {
    seedBackofficeAuthorized(true)
    seedBackofficeUser('user-42')
    const user = userEvent.setup()
    renderApp('/backoffice')

    await user.type(screen.getByLabelText('ID do usuário'), 'user-42')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    const section = await screen.findByRole('region', { name: 'Dados do usuário' })

    await waitFor(() => expect(within(section).getByText(/Dados do usuário visualizados por operator-1/)).toBeInTheDocument())
  })
})
