import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedAccount, seedConsent, seedCurrentUserPassword, seedProfile } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

describe('PrivacyPage (fluxo completo via App real)', () => {
  it('generates a data export and shows a summary of the counts', async () => {
    seedProfile({ id: 'user-42' })
    seedAccount()
    seedAccount()
    const user = userEvent.setup()
    renderApp('/privacy')
    const section = screen.getByRole('region', { name: 'Exportar meus dados' })

    await user.click(within(section).getByRole('button', { name: 'Gerar exportação' }))

    expect(await within(section).findByText('Contas: 2')).toBeInTheDocument()
    expect(within(section).getByRole('button', { name: 'Baixar como JSON' })).toBeInTheDocument()
  })

  it('records a consent and lists it in the history', async () => {
    const user = userEvent.setup()
    renderApp('/privacy')
    const section = screen.getByRole('region', { name: 'Consentimento' })

    expect(await within(section).findByText('Nenhum consentimento registrado.')).toBeInTheDocument()

    await user.type(within(section).getByLabelText('Versão dos termos aceitos'), '2.1')
    await user.click(within(section).getByRole('button', { name: 'Registrar consentimento' }))

    expect(await within(section).findByText(/Versão 2\.1 — aceito em/)).toBeInTheDocument()
  })

  it('lists an existing consent history entry', async () => {
    seedConsent({ version: '1.0' })
    renderApp('/privacy')
    const section = screen.getByRole('region', { name: 'Consentimento' })

    expect(await within(section).findByText(/Versão 1\.0 — aceito em/)).toBeInTheDocument()
  })

  it('deletes the account after confirming with the correct password and returns to login', async () => {
    seedCurrentUserPassword('CorrectPassword1')
    const user = userEvent.setup()
    renderApp('/privacy')
    const section = screen.getByRole('region', { name: 'Excluir minha conta' })

    await user.click(within(section).getByRole('button', { name: 'Excluir minha conta' }))
    await user.type(within(section).getByLabelText('Confirme sua senha'), 'CorrectPassword1')
    await user.click(within(section).getByRole('button', { name: 'Confirmar exclusão' }))

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('shows the real backend error when the confirmation password is wrong', async () => {
    seedCurrentUserPassword('CorrectPassword1')
    const user = userEvent.setup()
    renderApp('/privacy')
    const section = screen.getByRole('region', { name: 'Excluir minha conta' })

    await user.click(within(section).getByRole('button', { name: 'Excluir minha conta' }))
    await user.type(within(section).getByLabelText('Confirme sua senha'), 'WrongPassword1')
    await user.click(within(section).getByRole('button', { name: 'Confirmar exclusão' }))

    expect(await within(section).findByText('E-mail ou senha inválidos.')).toBeInTheDocument()
    await waitFor(() => expect(within(section).getByRole('button', { name: 'Confirmar exclusão' })).toBeInTheDocument())
  })

  it('cancels the deletion confirmation without calling the backend', async () => {
    const user = userEvent.setup()
    renderApp('/privacy')
    const section = screen.getByRole('region', { name: 'Excluir minha conta' })

    await user.click(within(section).getByRole('button', { name: 'Excluir minha conta' }))
    await user.click(within(section).getByRole('button', { name: 'Cancelar' }))

    expect(within(section).getByRole('button', { name: 'Excluir minha conta' })).toBeInTheDocument()
    expect(within(section).queryByLabelText('Confirme sua senha')).not.toBeInTheDocument()
  })
})
