import { apiRequest, downloadFile } from './httpClient'

export type CategoryAmount = {
  categoryId: string
  categoryName: string
  amount: number
  percentage: number
}

export type SpendingByCategoryReport = {
  startDate: string
  endDate: string
  totalExpense: number
  categories: CategoryAmount[]
}

export type PeriodSummary = {
  startDate: string
  endDate: string
  totalIncome: number
  totalExpense: number
  net: number
}

export type CategoryComparison = {
  categoryId: string
  categoryName: string
  amountPeriodA: number
  amountPeriodB: number
  delta: number
  /** null quando o valor no período A é zero — variação percentual indefinida (base zero). */
  percentageChange: number | null
}

export type PeriodComparisonReport = {
  periodA: PeriodSummary
  periodB: PeriodSummary
  categoryComparisons: CategoryComparison[]
}

function toQuery(params: Record<string, string>): string {
  return new URLSearchParams(params).toString()
}

export function getSpendingByCategory(startDate: string, endDate: string): Promise<SpendingByCategoryReport> {
  return apiRequest(`/reports/spending-by-category?${toQuery({ startDate, endDate })}`)
}

export function exportSpendingByCategory(startDate: string, endDate: string): Promise<void> {
  return downloadFile(`/reports/spending-by-category/export?${toQuery({ startDate, endDate })}`)
}

export function getPeriodComparison(
  periodAStart: string,
  periodAEnd: string,
  periodBStart: string,
  periodBEnd: string,
): Promise<PeriodComparisonReport> {
  return apiRequest(`/reports/period-comparison?${toQuery({ periodAStart, periodAEnd, periodBStart, periodBEnd })}`)
}

export function exportTransactions(startDate: string, endDate: string): Promise<void> {
  return downloadFile(`/reports/transactions/export?${toQuery({ startDate, endDate })}`)
}
