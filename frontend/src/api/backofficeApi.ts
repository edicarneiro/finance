import { apiRequest } from './httpClient'
import type { UserDataExport } from './privacyApi'

export const AUDIT_ACTIONS = ['VIEWED_USER_DATA', 'SUSPENDED_ACCOUNT', 'REACTIVATED_ACCOUNT'] as const
export type AuditAction = (typeof AUDIT_ACTIONS)[number]

export type AuditLogEntry = { operatorUserId: string; action: AuditAction; details: string | null; createdAt: string }

export function getUserForSupport(userId: string): Promise<UserDataExport> {
  return apiRequest(`/backoffice/users/${userId}`)
}

export function suspendAccount(userId: string, reason: string): Promise<void> {
  return apiRequest(`/backoffice/users/${userId}/suspend`, { method: 'POST', body: { reason: reason || null } })
}

export function reactivateAccount(userId: string, reason: string): Promise<void> {
  return apiRequest(`/backoffice/users/${userId}/reactivate`, { method: 'POST', body: { reason: reason || null } })
}

export function getAuditLog(userId: string): Promise<AuditLogEntry[]> {
  return apiRequest(`/backoffice/users/${userId}/audit-log`)
}
