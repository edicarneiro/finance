import { apiRequest } from './httpClient'

export const ACCOUNT_TYPES = ['CHECKING', 'SAVINGS', 'CREDIT_CARD', 'CASH', 'DIGITAL_WALLET'] as const
export type AccountType = (typeof ACCOUNT_TYPES)[number]

export type Account = {
  id: string
  type: AccountType
  name: string
  currency: string
  balance: number
  archived: boolean
  createdAt: string
}

export type CreateAccountInput = {
  type: AccountType
  name: string
  currency: string
  initialBalance: number
}

export function listAccounts(): Promise<Account[]> {
  return apiRequest<Account[]>('/accounts')
}

export function getConsolidatedBalance(): Promise<{ consolidatedBalance: number }> {
  return apiRequest('/accounts/balance/consolidated')
}

export function createAccount(input: CreateAccountInput): Promise<{ accountId: string }> {
  return apiRequest('/accounts', { method: 'POST', body: input })
}

export function updateAccount(id: string, name: string): Promise<void> {
  return apiRequest(`/accounts/${id}`, { method: 'PUT', body: { name } })
}

export function archiveAccount(id: string): Promise<void> {
  return apiRequest(`/accounts/${id}/archive`, { method: 'POST' })
}
