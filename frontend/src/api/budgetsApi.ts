import { apiRequest } from './httpClient'

export const BUDGET_PERIOD_TYPES = ['MONTHLY', 'WEEKLY', 'CUSTOM'] as const
export type BudgetPeriodType = (typeof BUDGET_PERIOD_TYPES)[number]

export type Budget = {
  id: string
  categoryId: string
  limitAmount: number
  periodType: BudgetPeriodType
  alertThresholds: number[]
  periodStart: string
  periodEnd: string
  consumedAmount: number
  consumedPercentage: number
  thresholdsCrossed: number[]
}

export type CreateBudgetInput = {
  categoryId: string
  limitAmount: number
  periodType: BudgetPeriodType
  customPeriodStart: string | null
  customPeriodEnd: string | null
  alertThresholds: number[]
}

export type BudgetPeriodPerformance = {
  periodStart: string
  periodEnd: string
  consumedAmount: number
  consumedPercentage: number
}

export function listBudgets(): Promise<Budget[]> {
  return apiRequest<Budget[]>('/budgets')
}

export function createBudget(input: CreateBudgetInput): Promise<{ budgetId: string }> {
  return apiRequest('/budgets', { method: 'POST', body: input })
}

export function updateBudget(id: string, limitAmount: number, alertThresholds: number[]): Promise<void> {
  return apiRequest(`/budgets/${id}`, { method: 'PUT', body: { limitAmount, alertThresholds } })
}

export function deleteBudget(id: string): Promise<void> {
  return apiRequest(`/budgets/${id}`, { method: 'DELETE' })
}

export function getBudgetHistory(id: string, periods = 6): Promise<BudgetPeriodPerformance[]> {
  return apiRequest(`/budgets/${id}/history?periods=${periods}`)
}
