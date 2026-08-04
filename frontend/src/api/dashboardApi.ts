import { apiRequest } from './httpClient'

export type CashFlow = {
  windowDays: number
  totalIncome: number
  totalExpense: number
  net: number
}

export type CategorySpending = {
  categoryId: string
  categoryName: string
  amount: number
  percentage: number
}

export type PulseScoreFactor = {
  name: string
  score: number
  weight: number
}

export type PulseScore = {
  overallScore: number
  formulaVersion: string
  factors: PulseScoreFactor[]
}

export type Dashboard = {
  consolidatedBalance: number
  cashFlow: CashFlow
  spendingByCategory: CategorySpending[]
  pulseScore: PulseScore
}

export type PulseScoreHistoryEntry = {
  date: string
  score: number
  formulaVersion: string
}

export function getDashboard(days = 30): Promise<Dashboard> {
  return apiRequest(`/dashboard?days=${days}`)
}

export function getPulseScoreHistory(days = 90): Promise<PulseScoreHistoryEntry[]> {
  return apiRequest(`/dashboard/pulse-score/history?days=${days}`)
}
