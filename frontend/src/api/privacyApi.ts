import { apiRequest } from './httpClient'

export type UserDataExport = {
  profile: { id: string; email: string; name: string; createdAt: string; deletedAt: string | null }
  accounts: unknown[]
  transactions: unknown[]
  categories: unknown[]
  budgets: unknown[]
  goals: unknown[]
  pulseScoreHistory: unknown[]
  notifications: unknown[]
  notificationPreferences: unknown[]
  consentHistory: unknown[]
}

export type ConsentRecord = { id: string; version: string; acceptedAt: string }

export function exportUserData(): Promise<UserDataExport> {
  return apiRequest('/privacy/export')
}

export function recordConsent(version: string): Promise<ConsentRecord> {
  return apiRequest('/privacy/consents', { method: 'POST', body: { version } })
}

export function listConsents(): Promise<ConsentRecord[]> {
  return apiRequest('/privacy/consents')
}

export function deleteAccount(password: string): Promise<void> {
  return apiRequest('/users/me', { method: 'DELETE', body: { password }, treatUnauthorizedAsSessionExpired: false })
}
