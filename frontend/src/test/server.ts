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

let accounts: Account[] = []
let categories: Category[] = []
let transactions: Transaction[] = []
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

export function resetTestState(): void {
  knownUsers.clear()
  accounts = []
  categories = []
  transactions = []
}
