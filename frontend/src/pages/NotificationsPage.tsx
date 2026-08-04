import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import * as notificationsApi from '../api/notificationsApi'
import { ALERT_TYPES, NOTIFICATION_CHANNELS } from '../api/notificationsApi'
import type { NotificationPreference } from '../api/notificationsApi'
import { ApiError } from '../api/httpClient'
import styles from './NotificationsPage.module.css'

const ALERT_TYPE_LABELS: Record<(typeof ALERT_TYPES)[number], string> = {
  BUDGET_THRESHOLD: 'Limiar de orçamento atingido',
  GOAL_THRESHOLD: 'Progresso de meta atingido',
  ATYPICAL_SPENDING: 'Gasto atípico detectado',
}

const CHANNEL_LABELS: Record<(typeof NOTIFICATION_CHANNELS)[number], string> = {
  IN_APP: 'No aplicativo',
  EMAIL: 'E-mail',
}

function preferenceKey(alertType: string, channel: string): string {
  return `${alertType}-${channel}`
}

export function NotificationsPage() {
  return (
    <div>
      <h1>Notificações</h1>
      <PreferencesSection />
      <CheckNotificationsSection />
      <NotificationsListSection />
    </div>
  )
}

function PreferencesSection() {
  const queryClient = useQueryClient()
  const preferencesQuery = useQuery({ queryKey: ['notification-preferences'], queryFn: notificationsApi.getPreferences })
  const [enabledByKey, setEnabledByKey] = useState<Record<string, boolean>>({})
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  // Sincroniza o estado local só na primeira carga — TanStack Query refaz a busca sozinho em vários
  // gatilhos (ex.: o navegador reganhando foco), e sem essa guarda um refetch em segundo plano
  // sobrescreveria silenciosamente alterações ainda não salvas pelo usuário.
  const hasInitializedRef = useRef(false)

  useEffect(() => {
    if (!preferencesQuery.data || hasInitializedRef.current) return
    hasInitializedRef.current = true
    const next: Record<string, boolean> = {}
    for (const preference of preferencesQuery.data) {
      next[preferenceKey(preference.alertType, preference.channel)] = preference.enabled
    }
    setEnabledByKey(next)
  }, [preferencesQuery.data])

  const saveMutation = useMutation({
    mutationFn: (preferences: NotificationPreference[]) => notificationsApi.updatePreferences(preferences),
    onSuccess: () => {
      setSaved(true)
      queryClient.invalidateQueries({ queryKey: ['notification-preferences'] })
    },
    onError: (error) => setSaveError(error instanceof ApiError ? error.message : 'Não foi possível salvar as preferências.'),
  })

  const toggle = (alertType: string, channel: string) => {
    setSaved(false)
    setEnabledByKey((prev) => ({ ...prev, [preferenceKey(alertType, channel)]: !prev[preferenceKey(alertType, channel)] }))
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    setSaveError(null)
    const preferences: NotificationPreference[] = ALERT_TYPES.flatMap((alertType) =>
      NOTIFICATION_CHANNELS.map((channel) => ({
        alertType,
        channel,
        enabled: enabledByKey[preferenceKey(alertType, channel)] ?? false,
      })),
    )
    saveMutation.mutate(preferences)
  }

  return (
    <section aria-label="Preferências de notificação" className={styles.section}>
      <h2>Preferências</h2>
      {preferencesQuery.isLoading && <p>Carregando…</p>}
      {preferencesQuery.data && (
        <form onSubmit={onSubmit}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Tipo de alerta</th>
                {NOTIFICATION_CHANNELS.map((channel) => (
                  <th key={channel}>{CHANNEL_LABELS[channel]}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {ALERT_TYPES.map((alertType) => (
                <tr key={alertType}>
                  <td>{ALERT_TYPE_LABELS[alertType]}</td>
                  {NOTIFICATION_CHANNELS.map((channel) => (
                    <td key={channel}>
                      <input
                        type="checkbox"
                        aria-label={`${ALERT_TYPE_LABELS[alertType]} — ${CHANNEL_LABELS[channel]}`}
                        checked={enabledByKey[preferenceKey(alertType, channel)] ?? false}
                        onChange={() => toggle(alertType, channel)}
                      />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
          {saveError && <p role="alert">{saveError}</p>}
          {saved && <p>Preferências salvas.</p>}
          <button type="submit" disabled={saveMutation.isPending}>
            {saveMutation.isPending ? 'Salvando…' : 'Salvar preferências'}
          </button>
        </form>
      )}
    </section>
  )
}

function CheckNotificationsSection() {
  const queryClient = useQueryClient()
  const [checkError, setCheckError] = useState<string | null>(null)

  const checkMutation = useMutation({
    mutationFn: notificationsApi.checkNotifications,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
    onError: (error) => setCheckError(error instanceof ApiError ? error.message : 'Não foi possível verificar notificações.'),
  })

  return (
    <section aria-label="Verificar notificações" className={styles.section}>
      <h2>Verificar notificações</h2>
      <p>Verifica se algum orçamento, meta ou padrão de gasto atípico cruzou um limiar desde a última verificação.</p>
      <button
        type="button"
        onClick={() => {
          setCheckError(null)
          checkMutation.mutate()
        }}
        disabled={checkMutation.isPending}
      >
        {checkMutation.isPending ? 'Verificando…' : 'Verificar agora'}
      </button>
      {checkError && <p role="alert">{checkError}</p>}
      {checkMutation.isSuccess && (
        <div>
          {checkMutation.data.length === 0 ? (
            <p>Nenhuma notificação nova.</p>
          ) : (
            <ul className={styles.list}>
              {checkMutation.data.map((notification) => (
                <li key={notification.id}>
                  {ALERT_TYPE_LABELS[notification.alertType]}: {notification.message}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  )
}

function NotificationsListSection() {
  const queryClient = useQueryClient()
  const [unreadOnly, setUnreadOnly] = useState(false)
  const notificationsQuery = useQuery({
    queryKey: ['notifications', unreadOnly],
    queryFn: () => notificationsApi.listNotifications(unreadOnly),
  })

  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationsApi.markNotificationRead(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  return (
    <section aria-label="Suas notificações" className={styles.section}>
      <h2>Suas notificações</h2>
      <label htmlFor="unreadOnly">
        <input id="unreadOnly" type="checkbox" checked={unreadOnly} onChange={(event) => setUnreadOnly(event.target.checked)} />
        Somente não lidas
      </label>

      {notificationsQuery.isLoading && <p>Carregando…</p>}
      {notificationsQuery.isSuccess && notificationsQuery.data.length === 0 && <p>Nenhuma notificação encontrada.</p>}
      <ul className={styles.list}>
        {notificationsQuery.data?.map((notification) => (
          <li key={notification.id} className={styles.notificationRow}>
            <span>
              {!notification.read && <strong>[Não lida] </strong>}
              {ALERT_TYPE_LABELS[notification.alertType]}: {notification.message}
            </span>
            {!notification.read && (
              <button type="button" onClick={() => markReadMutation.mutate(notification.id)}>
                Marcar como lida
              </button>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
