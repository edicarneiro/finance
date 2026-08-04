import { apiRequest } from './httpClient'

export type Goal = {
  id: string
  name: string
  targetAmount: number
  deadline: string
  accountId: string | null
  categoryId: string | null
  progressAlertThresholds: number[]
  currentAmount: number
  progressPercentage: number
  thresholdsCrossed: number[]
  achieved: boolean
  overdue: boolean
}

export type CreateGoalInput = {
  name: string
  targetAmount: number
  deadline: string
  accountId: string | null
  categoryId: string | null
  progressAlertThresholds: number[]
}

export type UpdateGoalInput = {
  name: string
  targetAmount: number
  deadline: string
  progressAlertThresholds: number[]
}

export function listGoals(): Promise<Goal[]> {
  return apiRequest<Goal[]>('/goals')
}

export function createGoal(input: CreateGoalInput): Promise<{ goalId: string }> {
  return apiRequest('/goals', { method: 'POST', body: input })
}

export function updateGoal(id: string, input: UpdateGoalInput): Promise<void> {
  return apiRequest(`/goals/${id}`, { method: 'PUT', body: input })
}

export function deleteGoal(id: string): Promise<void> {
  return apiRequest(`/goals/${id}`, { method: 'DELETE' })
}
