import { apiRequest } from './httpClient'

export const ALERT_TYPES = ['BUDGET_THRESHOLD', 'GOAL_THRESHOLD', 'ATYPICAL_SPENDING'] as const
export type AlertType = (typeof ALERT_TYPES)[number]

export const NOTIFICATION_CHANNELS = ['IN_APP', 'EMAIL'] as const
export type NotificationChannel = (typeof NOTIFICATION_CHANNELS)[number]

export type NotificationPreference = {
  alertType: AlertType
  channel: NotificationChannel
  enabled: boolean
}

export type CheckedNotification = {
  id: string
  alertType: AlertType
  message: string
}

export type Notification = {
  id: string
  alertType: AlertType
  message: string
  read: boolean
  createdAt: string
}

export function getPreferences(): Promise<NotificationPreference[]> {
  return apiRequest('/notification-preferences')
}

export function updatePreferences(preferences: NotificationPreference[]): Promise<void> {
  return apiRequest('/notification-preferences', { method: 'PUT', body: preferences })
}

export function checkNotifications(): Promise<CheckedNotification[]> {
  return apiRequest('/notifications/check', { method: 'POST' })
}

export function listNotifications(unreadOnly: boolean): Promise<Notification[]> {
  return apiRequest(`/notifications?unreadOnly=${unreadOnly}`)
}

export function markNotificationRead(id: string): Promise<void> {
  return apiRequest(`/notifications/${id}/read`, { method: 'PUT' })
}
