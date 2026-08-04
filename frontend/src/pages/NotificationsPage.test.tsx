import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { queryClient } from '../queryClient'
import { seedCheckedNotifications, seedNotification, seedNotificationPreferences } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

describe('NotificationsPage (fluxo completo via App real)', () => {
  it('loads preferences, toggles one on, and saves', async () => {
    seedNotificationPreferences([
      { alertType: 'BUDGET_THRESHOLD', channel: 'IN_APP', enabled: false },
      { alertType: 'BUDGET_THRESHOLD', channel: 'EMAIL', enabled: false },
      { alertType: 'GOAL_THRESHOLD', channel: 'IN_APP', enabled: false },
      { alertType: 'GOAL_THRESHOLD', channel: 'EMAIL', enabled: false },
      { alertType: 'ATYPICAL_SPENDING', channel: 'IN_APP', enabled: false },
      { alertType: 'ATYPICAL_SPENDING', channel: 'EMAIL', enabled: false },
    ])
    const user = userEvent.setup()
    renderApp('/notifications')
    const section = screen.getByRole('region', { name: 'Preferências de notificação' })

    const checkbox = await within(section).findByLabelText('Limiar de orçamento atingido — No aplicativo')
    expect(checkbox).not.toBeChecked()
    await user.click(checkbox)
    await user.click(within(section).getByRole('button', { name: 'Salvar preferências' }))

    expect(await within(section).findByText('Preferências salvas.')).toBeInTheDocument()
  })

  /**
   * Achado de QA (Fase 13.7): o estado local sincronizava com `preferencesQuery.data` a cada
   * mudança, não só na primeira carga — um refetch em segundo plano (TanStack Query refaz a busca
   * sozinho em vários gatilhos, ex.: o navegador reganhando foco) sobrescrevia silenciosamente uma
   * alteração ainda não salva. Corrigido para sincronizar só uma vez.
   */
  it('keeps an unsaved toggle after a background refetch of the preferences query', async () => {
    seedNotificationPreferences([
      { alertType: 'BUDGET_THRESHOLD', channel: 'IN_APP', enabled: false },
      { alertType: 'BUDGET_THRESHOLD', channel: 'EMAIL', enabled: false },
      { alertType: 'GOAL_THRESHOLD', channel: 'IN_APP', enabled: false },
      { alertType: 'GOAL_THRESHOLD', channel: 'EMAIL', enabled: false },
      { alertType: 'ATYPICAL_SPENDING', channel: 'IN_APP', enabled: false },
      { alertType: 'ATYPICAL_SPENDING', channel: 'EMAIL', enabled: false },
    ])
    const user = userEvent.setup()
    renderApp('/notifications')
    const section = screen.getByRole('region', { name: 'Preferências de notificação' })

    const checkbox = await within(section).findByLabelText('Limiar de orçamento atingido — No aplicativo')
    await user.click(checkbox)
    expect(checkbox).toBeChecked()

    // Simula um refetch em segundo plano (ex.: o navegador reganhando foco) enquanto a alteração
    // acima ainda não foi salva.
    await queryClient.refetchQueries({ queryKey: ['notification-preferences'] })

    expect(checkbox).toBeChecked()
  })

  it('checks for new notifications and shows the result', async () => {
    seedCheckedNotifications([{ id: 'notif-1', alertType: 'GOAL_THRESHOLD', message: 'Meta atingiu 80% do progresso.' }])
    const user = userEvent.setup()
    renderApp('/notifications')
    const section = screen.getByRole('region', { name: 'Verificar notificações' })

    await user.click(within(section).getByRole('button', { name: 'Verificar agora' }))

    expect(await within(section).findByText(/Meta atingiu 80% do progresso\./)).toBeInTheDocument()
  })

  it('shows an empty state when checking finds nothing new', async () => {
    const user = userEvent.setup()
    renderApp('/notifications')
    const section = screen.getByRole('region', { name: 'Verificar notificações' })

    await user.click(within(section).getByRole('button', { name: 'Verificar agora' }))

    expect(await within(section).findByText('Nenhuma notificação nova.')).toBeInTheDocument()
  })

  it('lists notifications and filters to unread only', async () => {
    seedNotification({ message: 'Notificação lida', read: true })
    seedNotification({ message: 'Notificação não lida', read: false })
    const user = userEvent.setup()
    renderApp('/notifications')
    const section = screen.getByRole('region', { name: 'Suas notificações' })

    expect(await within(section).findByText(/Notificação lida/)).toBeInTheDocument()
    expect(within(section).getByText(/Notificação não lida/)).toBeInTheDocument()

    await user.click(within(section).getByLabelText('Somente não lidas'))

    await waitFor(() => expect(within(section).queryByText(/Notificação lida/)).not.toBeInTheDocument())
    expect(within(section).getByText(/Notificação não lida/)).toBeInTheDocument()
  })

  it('marks a notification as read', async () => {
    seedNotification({ message: 'Para marcar como lida', read: false })
    const user = userEvent.setup()
    renderApp('/notifications')
    const section = screen.getByRole('region', { name: 'Suas notificações' })

    const row = (await within(section).findByText(/Para marcar como lida/)).closest('li')!
    await user.click(within(row).getByRole('button', { name: 'Marcar como lida' }))

    await waitFor(() => expect(within(row).queryByRole('button', { name: 'Marcar como lida' })).not.toBeInTheDocument())
  })
})
