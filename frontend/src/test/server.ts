import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

const API_BASE_URL = 'http://localhost:8080'

/**
 * Simula o formato real de resposta de `backend-java` (ver AuthController,
 * AccountController, CategoryController, GlobalExceptionHandler), não um
 * dublê da camada de dados do frontend. Intercepta só na fronteira de rede,
 * conforme ADR-0025/rules.md §3.
 */
const knownUsers = new Map<string, string>()

type Account = {
  id: string
  type: string
  name: string
  currency: string
  balance: number
  archived: boolean
  createdAt: string
}

type Category = {
  id: string
  name: string
  parentCategoryId: string | null
}

type Transaction = {
  id: string
  accountId: string
  categoryId: string
  type: string
  amount: number
  date: string
  description: string | null
  tags: string[]
  createdAt: string
}

type Budget = {
  id: string
  categoryId: string
  limitAmount: number
  periodType: string
  alertThresholds: number[]
  periodStart: string
  periodEnd: string
  consumedAmount: number
  consumedPercentage: number
  thresholdsCrossed: number[]
}

type Goal = {
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

type Dashboard = {
  consolidatedBalance: number
  cashFlow: { windowDays: number; totalIncome: number; totalExpense: number; net: number }
  spendingByCategory: { categoryId: string; categoryName: string; amount: number; percentage: number }[]
  pulseScore: { overallScore: number; formulaVersion: string; factors: { name: string; score: number; weight: number }[] }
}

type PulseScoreHistoryEntry = { date: string; score: number; formulaVersion: string }

type SpendingByCategoryReport = {
  startDate: string
  endDate: string
  totalExpense: number
  categories: { categoryId: string; categoryName: string; amount: number; percentage: number }[]
}

type PeriodComparisonReport = {
  periodA: { startDate: string; endDate: string; totalIncome: number; totalExpense: number; net: number }
  periodB: { startDate: string; endDate: string; totalIncome: number; totalExpense: number; net: number }
  categoryComparisons: {
    categoryId: string
    categoryName: string
    amountPeriodA: number
    amountPeriodB: number
    delta: number
    percentageChange: number | null
  }[]
}

function defaultDashboard(): Dashboard {
  return {
    consolidatedBalance: 0,
    cashFlow: { windowDays: 30, totalIncome: 0, totalExpense: 0, net: 0 },
    spendingByCategory: [],
    pulseScore: { overallScore: 0, formulaVersion: 'pulse-v0-provisional', factors: [] },
  }
}

function defaultSpendingByCategoryReport(): SpendingByCategoryReport {
  return { startDate: '2026-01-01', endDate: '2026-01-31', totalExpense: 0, categories: [] }
}

function defaultPeriodComparisonReport(): PeriodComparisonReport {
  return {
    periodA: { startDate: '2026-01-01', endDate: '2026-01-31', totalIncome: 0, totalExpense: 0, net: 0 },
    periodB: { startDate: '2026-02-01', endDate: '2026-02-28', totalIncome: 0, totalExpense: 0, net: 0 },
    categoryComparisons: [],
  }
}

const ALERT_TYPES = ['BUDGET_THRESHOLD', 'GOAL_THRESHOLD', 'ATYPICAL_SPENDING'] as const
const NOTIFICATION_CHANNELS = ['IN_APP', 'EMAIL'] as const

type NotificationPreference = { alertType: string; channel: string; enabled: boolean }
type CheckedNotification = { id: string; alertType: string; message: string }
type Notification = { id: string; alertType: string; message: string; read: boolean; createdAt: string }

type Consent = { id: string; version: string; acceptedAt: string }
type Profile = { id: string; email: string; name: string; createdAt: string; deletedAt: string | null }

function defaultProfile(): Profile {
  return { id: 'user-1', email: 'dev@financepulse.local', name: 'Usuário de teste', createdAt: '2026-01-01T00:00:00Z', deletedAt: null }
}

type BackofficeTargetUser = { profile: Profile; accountsCount: number; transactionsCount: number }
type BackofficeAuditLogEntry = { operatorUserId: string; action: string; details: string | null; createdAt: string }

function defaultNotificationPreferences(): NotificationPreference[] {
  return ALERT_TYPES.flatMap((alertType) => NOTIFICATION_CHANNELS.map((channel) => ({ alertType, channel, enabled: false })))
}

let accounts: Account[] = []
let categories: Category[] = []
let transactions: Transaction[] = []
let budgets: Budget[] = []
let goals: Goal[] = []
// Dashboard e relatórios são agregados calculados inteiramente pelo backend a partir de outros
// recursos — o mock não recalcula a partir de accounts/transactions/etc., apenas expõe um estado
// configurável via as funções seed*, como a resposta real da API já viria pronta.
let dashboard: Dashboard = defaultDashboard()
let pulseScoreHistory: PulseScoreHistoryEntry[] = []
let spendingByCategoryReport: SpendingByCategoryReport = defaultSpendingByCategoryReport()
let periodComparisonReport: PeriodComparisonReport = defaultPeriodComparisonReport()
let notificationPreferences: NotificationPreference[] = defaultNotificationPreferences()
let checkedNotifications: CheckedNotification[] = []
let notifications: Notification[] = []
let consents: Consent[] = []
let profile: Profile = defaultProfile()
let currentUserPassword = 'CorrectPassword1'
let backofficeAuthorized = false
const backofficeUsers = new Map<string, BackofficeTargetUser>()
const backofficeAuditLogs = new Map<string, BackofficeAuditLogEntry[]>()
let nextId = 1

function generateId(prefix: string): string {
  return `${prefix}-${nextId++}`
}

export const handlers = [
  http.post(`${API_BASE_URL}/auth/register`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string }

    if (knownUsers.has(body.email)) {
      return HttpResponse.json({ error: 'E-mail já cadastrado.' }, { status: 400 })
    }
    if (body.password.length < 8) {
      return HttpResponse.json({ error: 'Senha muito fraca.' }, { status: 400 })
    }

    knownUsers.set(body.email, body.password)
    return HttpResponse.json({ userId: 'user-1' }, { status: 201 })
  }),

  http.post(`${API_BASE_URL}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string }

    if (knownUsers.get(body.email) !== body.password) {
      return HttpResponse.json({ error: 'Credenciais inválidas.' }, { status: 401 })
    }

    return HttpResponse.json({ token: 'fake-jwt-token' }, { status: 200 })
  }),

  http.get(`${API_BASE_URL}/accounts`, () => HttpResponse.json(accounts)),

  http.get(`${API_BASE_URL}/accounts/balance/consolidated`, () => {
    const consolidatedBalance = accounts.filter((account) => !account.archived).reduce((sum, a) => sum + a.balance, 0)
    return HttpResponse.json({ consolidatedBalance })
  }),

  http.post(`${API_BASE_URL}/accounts`, async ({ request }) => {
    const body = (await request.json()) as { type: string; name: string; currency: string; initialBalance: number }
    const account: Account = {
      id: generateId('account'),
      type: body.type,
      name: body.name,
      currency: body.currency,
      balance: body.initialBalance,
      archived: false,
      createdAt: new Date().toISOString(),
    }
    accounts.push(account)
    return HttpResponse.json({ accountId: account.id }, { status: 201 })
  }),

  http.put(`${API_BASE_URL}/accounts/:id`, async ({ params, request }) => {
    const body = (await request.json()) as { name: string }
    const account = accounts.find((a) => a.id === params.id)
    if (!account) {
      return HttpResponse.json({ error: 'Conta não encontrada.' }, { status: 404 })
    }
    account.name = body.name
    return new HttpResponse(null, { status: 200 })
  }),

  http.post(`${API_BASE_URL}/accounts/:id/archive`, ({ params }) => {
    const account = accounts.find((a) => a.id === params.id)
    if (!account) {
      return HttpResponse.json({ error: 'Conta não encontrada.' }, { status: 404 })
    }
    account.archived = true
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${API_BASE_URL}/categories`, () => HttpResponse.json(categories)),

  http.post(`${API_BASE_URL}/categories`, async ({ request }) => {
    const body = (await request.json()) as { name: string; parentCategoryId: string | null }
    if (body.parentCategoryId) {
      const parent = categories.find((c) => c.id === body.parentCategoryId)
      if (parent?.parentCategoryId) {
        return HttpResponse.json(
          { error: 'Uma subcategoria não pode ter subcategorias — a hierarquia é limitada a 2 níveis.' },
          { status: 400 },
        )
      }
    }
    const category: Category = { id: generateId('category'), name: body.name, parentCategoryId: body.parentCategoryId ?? null }
    categories.push(category)
    return HttpResponse.json({ categoryId: category.id }, { status: 201 })
  }),

  http.put(`${API_BASE_URL}/categories/:id`, async ({ params, request }) => {
    const body = (await request.json()) as { name: string }
    const category = categories.find((c) => c.id === params.id)
    if (!category) {
      return HttpResponse.json({ error: 'Categoria não encontrada.' }, { status: 404 })
    }
    category.name = body.name
    return new HttpResponse(null, { status: 200 })
  }),

  http.delete(`${API_BASE_URL}/categories/:id`, ({ params }) => {
    const category = categories.find((c) => c.id === params.id)
    if (!category) {
      return HttpResponse.json({ error: 'Categoria não encontrada.' }, { status: 404 })
    }
    if (categories.some((c) => c.parentCategoryId === category.id)) {
      return HttpResponse.json({ error: 'Não é possível excluir uma categoria que possui subcategorias.' }, { status: 400 })
    }
    categories = categories.filter((c) => c.id !== category.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${API_BASE_URL}/transactions`, ({ request }) => {
    const accountId = new URL(request.url).searchParams.get('accountId')
    return HttpResponse.json(transactions.filter((t) => t.accountId === accountId))
  }),

  http.post(`${API_BASE_URL}/transactions`, async ({ request }) => {
    const body = (await request.json()) as Omit<Transaction, 'id' | 'createdAt'>
    const account = accounts.find((a) => a.id === body.accountId)
    if (account?.archived) {
      return HttpResponse.json({ error: 'Não é possível lançar uma transação em uma conta arquivada.' }, { status: 400 })
    }
    if (body.amount <= 0) {
      return HttpResponse.json({ error: 'O valor da transação deve ser maior que zero.' }, { status: 400 })
    }
    const transaction: Transaction = { id: generateId('transaction'), createdAt: new Date().toISOString(), ...body }
    transactions.push(transaction)
    return HttpResponse.json({ transactionId: transaction.id }, { status: 201 })
  }),

  http.put(`${API_BASE_URL}/transactions/:id`, async ({ params, request }) => {
    const body = (await request.json()) as Omit<Transaction, 'id' | 'createdAt'>
    const transaction = transactions.find((t) => t.id === params.id)
    if (!transaction) {
      return HttpResponse.json({ error: 'Transação não encontrada.' }, { status: 404 })
    }
    Object.assign(transaction, body)
    return new HttpResponse(null, { status: 200 })
  }),

  http.delete(`${API_BASE_URL}/transactions/:id`, ({ params }) => {
    const transaction = transactions.find((t) => t.id === params.id)
    if (!transaction) {
      return HttpResponse.json({ error: 'Transação não encontrada.' }, { status: 404 })
    }
    transactions = transactions.filter((t) => t.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${API_BASE_URL}/budgets`, () => HttpResponse.json(budgets)),

  http.post(`${API_BASE_URL}/budgets`, async ({ request }) => {
    const body = (await request.json()) as {
      categoryId: string
      limitAmount: number
      periodType: string
      customPeriodStart: string | null
      customPeriodEnd: string | null
      alertThresholds: number[]
    }
    if (body.limitAmount <= 0) {
      return HttpResponse.json({ error: 'O limite do orçamento deve ser maior que zero.' }, { status: 400 })
    }
    const budget: Budget = {
      id: generateId('budget'),
      categoryId: body.categoryId,
      limitAmount: body.limitAmount,
      periodType: body.periodType,
      alertThresholds: body.alertThresholds,
      periodStart: body.customPeriodStart ?? '2026-08-01',
      periodEnd: body.customPeriodEnd ?? '2026-08-31',
      consumedAmount: 0,
      consumedPercentage: 0,
      thresholdsCrossed: [],
    }
    budgets.push(budget)
    return HttpResponse.json({ budgetId: budget.id }, { status: 201 })
  }),

  http.put(`${API_BASE_URL}/budgets/:id`, async ({ params, request }) => {
    const body = (await request.json()) as { limitAmount: number; alertThresholds: number[] }
    const budget = budgets.find((b) => b.id === params.id)
    if (!budget) {
      return HttpResponse.json({ error: 'Orçamento não encontrado.' }, { status: 404 })
    }
    budget.limitAmount = body.limitAmount
    budget.alertThresholds = body.alertThresholds
    return new HttpResponse(null, { status: 200 })
  }),

  http.delete(`${API_BASE_URL}/budgets/:id`, ({ params }) => {
    const budget = budgets.find((b) => b.id === params.id)
    if (!budget) {
      return HttpResponse.json({ error: 'Orçamento não encontrado.' }, { status: 404 })
    }
    budgets = budgets.filter((b) => b.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${API_BASE_URL}/budgets/:id/history`, ({ params }) => {
    const budget = budgets.find((b) => b.id === params.id)
    if (!budget) {
      return HttpResponse.json({ error: 'Orçamento não encontrado.' }, { status: 404 })
    }
    return HttpResponse.json([
      { periodStart: '2026-07-01', periodEnd: '2026-07-31', consumedAmount: 120, consumedPercentage: 60 },
    ])
  }),

  http.get(`${API_BASE_URL}/goals`, () => HttpResponse.json(goals)),

  http.post(`${API_BASE_URL}/goals`, async ({ request }) => {
    const body = (await request.json()) as {
      name: string
      targetAmount: number
      deadline: string
      accountId: string | null
      categoryId: string | null
      progressAlertThresholds: number[]
    }
    if (Boolean(body.accountId) === Boolean(body.categoryId)) {
      return HttpResponse.json(
        { error: 'Informe exatamente uma associação para a meta: conta ou categoria, nunca ambas nem nenhuma.' },
        { status: 400 },
      )
    }
    if (body.targetAmount <= 0) {
      return HttpResponse.json({ error: 'O valor-alvo da meta deve ser maior que zero.' }, { status: 400 })
    }
    const goal: Goal = {
      id: generateId('goal'),
      name: body.name,
      targetAmount: body.targetAmount,
      deadline: body.deadline,
      accountId: body.accountId,
      categoryId: body.categoryId,
      progressAlertThresholds: body.progressAlertThresholds,
      currentAmount: 0,
      progressPercentage: 0,
      thresholdsCrossed: [],
      achieved: false,
      overdue: false,
    }
    goals.push(goal)
    return HttpResponse.json({ goalId: goal.id }, { status: 201 })
  }),

  http.put(`${API_BASE_URL}/goals/:id`, async ({ params, request }) => {
    const body = (await request.json()) as { name: string; targetAmount: number; deadline: string; progressAlertThresholds: number[] }
    const goal = goals.find((g) => g.id === params.id)
    if (!goal) {
      return HttpResponse.json({ error: 'Meta não encontrada.' }, { status: 404 })
    }
    goal.name = body.name
    goal.targetAmount = body.targetAmount
    goal.deadline = body.deadline
    goal.progressAlertThresholds = body.progressAlertThresholds
    return new HttpResponse(null, { status: 200 })
  }),

  http.delete(`${API_BASE_URL}/goals/:id`, ({ params }) => {
    const goal = goals.find((g) => g.id === params.id)
    if (!goal) {
      return HttpResponse.json({ error: 'Meta não encontrada.' }, { status: 404 })
    }
    goals = goals.filter((g) => g.id !== params.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${API_BASE_URL}/dashboard`, () => HttpResponse.json(dashboard)),

  http.get(`${API_BASE_URL}/dashboard/pulse-score/history`, () => HttpResponse.json(pulseScoreHistory)),

  http.get(`${API_BASE_URL}/reports/spending-by-category`, () => HttpResponse.json(spendingByCategoryReport)),

  http.get(`${API_BASE_URL}/reports/spending-by-category/export`, () => {
    return new HttpResponse('categoryId,categoryName,amount,percentage\ncategory-1,Alimentação,400,100', {
      status: 200,
      headers: {
        'Content-Type': 'text/csv; charset=UTF-8',
        'Content-Disposition': 'attachment; filename="gastos-por-categoria_2026-07-01_2026-07-31.csv"',
      },
    })
  }),

  http.get(`${API_BASE_URL}/reports/period-comparison`, () => HttpResponse.json(periodComparisonReport)),

  http.get(`${API_BASE_URL}/reports/transactions/export`, () => {
    return new HttpResponse('date,accountName,categoryName,type,amount,description,tags\n2026-07-01,Conta,Alimentação,EXPENSE,50,,', {
      status: 200,
      headers: {
        'Content-Type': 'text/csv; charset=UTF-8',
        'Content-Disposition': 'attachment; filename="transacoes_2026-07-01_2026-07-31.csv"',
      },
    })
  }),

  http.get(`${API_BASE_URL}/notification-preferences`, () => HttpResponse.json(notificationPreferences)),

  http.put(`${API_BASE_URL}/notification-preferences`, async ({ request }) => {
    const body = (await request.json()) as NotificationPreference[]
    notificationPreferences = body
    return new HttpResponse(null, { status: 200 })
  }),

  http.post(`${API_BASE_URL}/notifications/check`, () => HttpResponse.json(checkedNotifications)),

  http.get(`${API_BASE_URL}/notifications`, ({ request }) => {
    const unreadOnly = new URL(request.url).searchParams.get('unreadOnly') === 'true'
    return HttpResponse.json(unreadOnly ? notifications.filter((n) => !n.read) : notifications)
  }),

  http.put(`${API_BASE_URL}/notifications/:id/read`, ({ params }) => {
    const notification = notifications.find((n) => n.id === params.id)
    if (!notification) {
      return HttpResponse.json({ error: 'Notificação não encontrada.' }, { status: 404 })
    }
    notification.read = true
    return new HttpResponse(null, { status: 200 })
  }),

  http.get(`${API_BASE_URL}/privacy/export`, () =>
    HttpResponse.json({
      profile,
      accounts,
      transactions,
      categories,
      budgets,
      goals,
      pulseScoreHistory,
      notifications,
      notificationPreferences,
      consentHistory: consents,
    }),
  ),

  http.post(`${API_BASE_URL}/privacy/consents`, async ({ request }) => {
    const body = (await request.json()) as { version: string }
    const consent: Consent = { id: generateId('consent'), version: body.version, acceptedAt: new Date().toISOString() }
    consents.push(consent)
    return HttpResponse.json(consent, { status: 201 })
  }),

  http.get(`${API_BASE_URL}/privacy/consents`, () => HttpResponse.json(consents)),

  http.delete(`${API_BASE_URL}/users/me`, async ({ request }) => {
    const body = (await request.json()) as { password: string }
    if (body.password !== currentUserPassword) {
      return HttpResponse.json({ error: 'E-mail ou senha inválidos.' }, { status: 401 })
    }
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${API_BASE_URL}/backoffice/users/:userId`, ({ params }) => {
    if (!backofficeAuthorized) {
      return HttpResponse.json({ error: 'Acesso negado: esta ação exige permissão de operador de suporte.' }, { status: 403 })
    }
    const userId = params.userId as string
    const target = backofficeUsers.get(userId)
    if (!target) {
      return HttpResponse.json({ error: 'Usuário não encontrado.' }, { status: 404 })
    }
    // Espelha GetUserForSupportUseCase real: toda consulta de suporte é registrada no log de
    // auditoria (RF-048), diferente de GetAuditLogUseCase, que não gera uma entrada nele mesmo.
    const entries = backofficeAuditLogs.get(userId) ?? []
    entries.unshift({ operatorUserId: 'operator-1', action: 'VIEWED_USER_DATA', details: null, createdAt: new Date().toISOString() })
    backofficeAuditLogs.set(userId, entries)
    return HttpResponse.json({
      profile: target.profile,
      accounts: Array.from({ length: target.accountsCount }, () => ({})),
      transactions: Array.from({ length: target.transactionsCount }, () => ({})),
      categories: [],
      budgets: [],
      goals: [],
      pulseScoreHistory: [],
      notifications: [],
      notificationPreferences: [],
      consentHistory: [],
    })
  }),

  http.post(`${API_BASE_URL}/backoffice/users/:userId/suspend`, async ({ params, request }) => {
    if (!backofficeAuthorized) {
      return HttpResponse.json({ error: 'Acesso negado: esta ação exige permissão de operador de suporte.' }, { status: 403 })
    }
    const userId = params.userId as string
    if (!backofficeUsers.has(userId)) {
      return HttpResponse.json({ error: 'Usuário não encontrado.' }, { status: 404 })
    }
    const body = (await request.json()) as { reason: string | null } | null
    const entries = backofficeAuditLogs.get(userId) ?? []
    entries.unshift({ operatorUserId: 'operator-1', action: 'SUSPENDED_ACCOUNT', details: body?.reason ?? null, createdAt: new Date().toISOString() })
    backofficeAuditLogs.set(userId, entries)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post(`${API_BASE_URL}/backoffice/users/:userId/reactivate`, async ({ params, request }) => {
    if (!backofficeAuthorized) {
      return HttpResponse.json({ error: 'Acesso negado: esta ação exige permissão de operador de suporte.' }, { status: 403 })
    }
    const userId = params.userId as string
    if (!backofficeUsers.has(userId)) {
      return HttpResponse.json({ error: 'Usuário não encontrado.' }, { status: 404 })
    }
    const body = (await request.json()) as { reason: string | null } | null
    const entries = backofficeAuditLogs.get(userId) ?? []
    entries.unshift({ operatorUserId: 'operator-1', action: 'REACTIVATED_ACCOUNT', details: body?.reason ?? null, createdAt: new Date().toISOString() })
    backofficeAuditLogs.set(userId, entries)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${API_BASE_URL}/backoffice/users/:userId/audit-log`, ({ params }) => {
    if (!backofficeAuthorized) {
      return HttpResponse.json({ error: 'Acesso negado: esta ação exige permissão de operador de suporte.' }, { status: 403 })
    }
    return HttpResponse.json(backofficeAuditLogs.get(params.userId as string) ?? [])
  }),
]

export const server = setupServer(...handlers)

export function seedKnownUser(email: string, password: string): void {
  knownUsers.set(email, password)
}

export function seedAccount(overrides: Partial<Account> = {}): Account {
  const account: Account = {
    id: generateId('account'),
    type: 'CHECKING',
    name: 'Conta existente',
    currency: 'BRL',
    balance: 100,
    archived: false,
    createdAt: new Date().toISOString(),
    ...overrides,
  }
  accounts.push(account)
  return account
}

export function seedCategory(overrides: Partial<Category> = {}): Category {
  const category: Category = { id: generateId('category'), name: 'Categoria existente', parentCategoryId: null, ...overrides }
  categories.push(category)
  return category
}

export function seedTransaction(overrides: Partial<Transaction> & { accountId: string; categoryId: string }): Transaction {
  const transaction: Transaction = {
    id: generateId('transaction'),
    type: 'EXPENSE',
    amount: 50,
    date: '2026-08-01',
    description: null,
    tags: [],
    createdAt: new Date().toISOString(),
    ...overrides,
  }
  transactions.push(transaction)
  return transaction
}

export function seedBudget(overrides: Partial<Budget> & { categoryId: string }): Budget {
  const budget: Budget = {
    id: generateId('budget'),
    limitAmount: 500,
    periodType: 'MONTHLY',
    alertThresholds: [80, 100],
    periodStart: '2026-08-01',
    periodEnd: '2026-08-31',
    consumedAmount: 0,
    consumedPercentage: 0,
    thresholdsCrossed: [],
    ...overrides,
  }
  budgets.push(budget)
  return budget
}

export function seedGoal(overrides: Partial<Goal> = {}): Goal {
  const goal: Goal = {
    id: generateId('goal'),
    name: 'Meta existente',
    targetAmount: 1000,
    deadline: '2026-12-31',
    accountId: null,
    categoryId: null,
    progressAlertThresholds: [50, 100],
    currentAmount: 0,
    progressPercentage: 0,
    thresholdsCrossed: [],
    achieved: false,
    overdue: false,
    ...overrides,
  }
  goals.push(goal)
  return goal
}

export function seedDashboard(overrides: Partial<Dashboard>): void {
  dashboard = { ...defaultDashboard(), ...overrides }
}

export function seedPulseScoreHistory(entries: PulseScoreHistoryEntry[]): void {
  pulseScoreHistory = entries
}

export function seedSpendingByCategoryReport(overrides: Partial<SpendingByCategoryReport>): void {
  spendingByCategoryReport = { ...defaultSpendingByCategoryReport(), ...overrides }
}

export function seedPeriodComparisonReport(overrides: Partial<PeriodComparisonReport>): void {
  periodComparisonReport = { ...defaultPeriodComparisonReport(), ...overrides }
}

export function seedNotificationPreferences(preferences: NotificationPreference[]): void {
  notificationPreferences = preferences
}

export function seedCheckedNotifications(entries: CheckedNotification[]): void {
  checkedNotifications = entries
}

export function seedNotification(overrides: Partial<Notification> = {}): Notification {
  const notification: Notification = {
    id: generateId('notification'),
    alertType: 'BUDGET_THRESHOLD',
    message: 'Notificação existente',
    read: false,
    createdAt: new Date().toISOString(),
    ...overrides,
  }
  notifications.push(notification)
  return notification
}

export function seedConsent(overrides: Partial<Consent> = {}): Consent {
  const consent: Consent = { id: generateId('consent'), version: '1.0', acceptedAt: new Date().toISOString(), ...overrides }
  consents.push(consent)
  return consent
}

export function seedProfile(overrides: Partial<Profile>): void {
  profile = { ...defaultProfile(), ...overrides }
}

export function seedCurrentUserPassword(password: string): void {
  currentUserPassword = password
}

export function seedBackofficeAuthorized(authorized: boolean): void {
  backofficeAuthorized = authorized
}

export function seedBackofficeUser(userId: string, overrides: Partial<BackofficeTargetUser['profile']> & { accountsCount?: number; transactionsCount?: number } = {}): void {
  const { accountsCount = 0, transactionsCount = 0, ...profileOverrides } = overrides
  backofficeUsers.set(userId, {
    profile: { id: userId, email: 'cliente@financepulse.local', name: 'Cliente existente', createdAt: '2026-01-01T00:00:00Z', deletedAt: null, ...profileOverrides },
    accountsCount,
    transactionsCount,
  })
}

export function resetTestState(): void {
  knownUsers.clear()
  accounts = []
  categories = []
  transactions = []
  budgets = []
  goals = []
  dashboard = defaultDashboard()
  pulseScoreHistory = []
  spendingByCategoryReport = defaultSpendingByCategoryReport()
  periodComparisonReport = defaultPeriodComparisonReport()
  notificationPreferences = defaultNotificationPreferences()
  checkedNotifications = []
  notifications = []
  consents = []
  profile = defaultProfile()
  currentUserPassword = 'CorrectPassword1'
  backofficeAuthorized = false
  backofficeUsers.clear()
  backofficeAuditLogs.clear()
}
