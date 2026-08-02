import { apiRequest } from './httpClient'

export const TRANSACTION_TYPES = ['INCOME', 'EXPENSE'] as const
export type TransactionType = (typeof TRANSACTION_TYPES)[number]

export type Transaction = {
  id: string
  accountId: string
  categoryId: string
  type: TransactionType
  amount: number
  date: string
  description: string | null
  tags: string[]
  createdAt: string
}

export type TransactionInput = {
  accountId: string
  categoryId: string
  type: TransactionType
  amount: number
  date: string
  description: string | null
  tags: string[]
}

export function listTransactions(accountId: string): Promise<Transaction[]> {
  return apiRequest<Transaction[]>(`/transactions?accountId=${encodeURIComponent(accountId)}`)
}

export function createTransaction(input: TransactionInput): Promise<{ transactionId: string }> {
  return apiRequest('/transactions', { method: 'POST', body: input })
}

export function updateTransaction(id: string, input: TransactionInput): Promise<void> {
  return apiRequest(`/transactions/${id}`, { method: 'PUT', body: input })
}

export function deleteTransaction(id: string): Promise<void> {
  return apiRequest(`/transactions/${id}`, { method: 'DELETE' })
}
