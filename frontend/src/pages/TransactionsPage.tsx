import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import * as accountsApi from '../api/accountsApi'
import * as categoriesApi from '../api/categoriesApi'
import * as transactionsApi from '../api/transactionsApi'
import { TRANSACTION_TYPES, type Transaction, type TransactionInput } from '../api/transactionsApi'
import { ApiError } from '../api/httpClient'
import styles from './TransactionsPage.module.css'

const TRANSACTION_TYPE_LABELS: Record<(typeof TRANSACTION_TYPES)[number], string> = {
  INCOME: 'Receita',
  EXPENSE: 'Despesa',
}

function formatCurrency(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency }).format(amount)
  } catch {
    return `${amount.toFixed(2)} ${currency}`
  }
}

export function TransactionsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const accountId = searchParams.get('accountId')
  const accountsQuery = useQuery({ queryKey: ['accounts'], queryFn: accountsApi.listAccounts })

  return (
    <div>
      <h1>Transações</h1>

      <label htmlFor="accountId">Conta</label>
      <select
        id="accountId"
        value={accountId ?? ''}
        onChange={(event) => setSearchParams(event.target.value ? { accountId: event.target.value } : {})}
      >
        <option value="">Selecione uma conta</option>
        {accountsQuery.data?.map((account) => (
          <option key={account.id} value={account.id}>
            {account.name}
          </option>
        ))}
      </select>

      {accountId ? (
        <TransactionsForAccount key={accountId} accountId={accountId} currency={accountsQuery.data?.find((a) => a.id === accountId)?.currency ?? 'BRL'} />
      ) : (
        <p>Selecione uma conta para ver e lançar transações.</p>
      )}
    </div>
  )
}

const transactionSchema = z.object({
  categoryId: z.string().min(1, 'Selecione uma categoria.'),
  type: z.enum(TRANSACTION_TYPES),
  amount: z.coerce.number({ message: 'Informe um valor numérico.' }).positive('O valor deve ser maior que zero.'),
  date: z.string().min(1, 'Informe a data.'),
  description: z.string(),
  tags: z.string(),
})

type TransactionFormValues = z.input<typeof transactionSchema>

function toTagList(tags: string): string[] {
  return tags
    .split(',')
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0)
}

function TransactionsForAccount({ accountId, currency }: { accountId: string; currency: string }) {
  const queryClient = useQueryClient()
  const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.listCategories })
  const transactionsQuery = useQuery({
    queryKey: ['transactions', accountId],
    queryFn: () => transactionsApi.listTransactions(accountId),
  })
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TransactionFormValues>({
    resolver: zodResolver(transactionSchema),
    defaultValues: { categoryId: '', type: 'EXPENSE', amount: 0, date: new Date().toISOString().slice(0, 10), description: '', tags: '' },
  })

  const invalidateTransactions = () => queryClient.invalidateQueries({ queryKey: ['transactions', accountId] })

  const createMutation = useMutation({
    mutationFn: (input: TransactionInput) => transactionsApi.createTransaction(input),
    onSuccess: () => {
      invalidateTransactions()
      reset()
    },
  })

  const onSubmit = handleSubmit((values) => {
    setFormError(null)
    const parsed = transactionSchema.parse(values)
    const input: TransactionInput = {
      accountId,
      categoryId: parsed.categoryId,
      type: parsed.type,
      amount: parsed.amount,
      date: parsed.date,
      description: parsed.description.trim() === '' ? null : parsed.description.trim(),
      tags: toTagList(parsed.tags),
    }
    createMutation.mutate(input, {
      onError: (error) => setFormError(error instanceof ApiError ? error.message : 'Não foi possível lançar a transação.'),
    })
  })

  const categoryName = (categoryId: string) => categoriesQuery.data?.find((category) => category.id === categoryId)?.name ?? categoryId

  return (
    <div>
      <form onSubmit={onSubmit} className={styles.form} noValidate>
        <h2>Nova transação</h2>

        <label htmlFor="type">Tipo</label>
        <select id="type" {...register('type')}>
          {TRANSACTION_TYPES.map((type) => (
            <option key={type} value={type}>
              {TRANSACTION_TYPE_LABELS[type]}
            </option>
          ))}
        </select>

        <label htmlFor="categoryId">Categoria</label>
        <select id="categoryId" {...register('categoryId')}>
          <option value="">Selecione uma categoria</option>
          {categoriesQuery.data?.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        {errors.categoryId && <p role="alert">{errors.categoryId.message}</p>}

        <label htmlFor="amount">Valor</label>
        <input id="amount" type="number" step="0.01" {...register('amount')} />
        {errors.amount && <p role="alert">{errors.amount.message}</p>}

        <label htmlFor="date">Data</label>
        <input id="date" type="date" {...register('date')} />
        {errors.date && <p role="alert">{errors.date.message}</p>}

        <label htmlFor="description">Descrição (opcional)</label>
        <input id="description" {...register('description')} />

        <label htmlFor="tags">Tags (separadas por vírgula, opcional)</label>
        <input id="tags" {...register('tags')} />

        {formError && <p role="alert">{formError}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Lançando…' : 'Lançar transação'}
        </button>
      </form>

      <section aria-label="Transações da conta">
        <h2>Transações</h2>
        {transactionsQuery.isLoading && <p>Carregando…</p>}
        {transactionsQuery.isSuccess && transactionsQuery.data.length === 0 && <p>Nenhuma transação lançada nesta conta ainda.</p>}
        <ul className={styles.list}>
          {transactionsQuery.data?.map((transaction) => (
            <TransactionRow
              key={transaction.id}
              transaction={transaction}
              currency={currency}
              categoryName={categoryName(transaction.categoryId)}
              categories={categoriesQuery.data ?? []}
              onChanged={invalidateTransactions}
            />
          ))}
        </ul>
      </section>
    </div>
  )
}

function TransactionRow({
  transaction,
  currency,
  categoryName,
  categories,
  onChanged,
}: {
  transaction: Transaction
  currency: string
  categoryName: string
  categories: { id: string; name: string }[]
  onChanged: () => void
}) {
  const [editing, setEditing] = useState(false)
  const [categoryId, setCategoryId] = useState(transaction.categoryId)
  const [amount, setAmount] = useState(String(transaction.amount))
  const [rowError, setRowError] = useState<string | null>(null)

  const updateMutation = useMutation({
    mutationFn: () =>
      transactionsApi.updateTransaction(transaction.id, {
        accountId: transaction.accountId,
        categoryId,
        type: transaction.type,
        amount: Number(amount),
        date: transaction.date,
        description: transaction.description,
        tags: transaction.tags,
      }),
    onSuccess: () => {
      setEditing(false)
      onChanged()
    },
    onError: (error) => setRowError(error instanceof ApiError ? error.message : 'Não foi possível salvar.'),
  })

  const deleteMutation = useMutation({
    mutationFn: () => transactionsApi.deleteTransaction(transaction.id),
    onSuccess: onChanged,
    onError: (error) => setRowError(error instanceof ApiError ? error.message : 'Não foi possível excluir.'),
  })

  if (editing) {
    return (
      <li className={styles.row}>
        <form
          onSubmit={(event) => {
            event.preventDefault()
            setRowError(null)
            updateMutation.mutate()
          }}
        >
          <label htmlFor={`category-${transaction.id}`}>Categoria</label>
          <select id={`category-${transaction.id}`} value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>

          <label htmlFor={`amount-${transaction.id}`}>Valor</label>
          <input id={`amount-${transaction.id}`} type="number" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} />

          <button type="submit">Salvar</button>
          <button type="button" onClick={() => setEditing(false)}>
            Cancelar
          </button>
        </form>
        {rowError && <p role="alert">{rowError}</p>}
      </li>
    )
  }

  return (
    <li className={styles.row}>
      <span>
        {transaction.date} — <strong>{categoryName}</strong> — {TRANSACTION_TYPE_LABELS[transaction.type]} —{' '}
        {formatCurrency(transaction.amount, currency)}
        {transaction.description && ` — ${transaction.description}`}
      </span>
      <span className={styles.actions}>
        <button type="button" onClick={() => setEditing(true)}>
          Editar
        </button>
        <button
          type="button"
          onClick={() => {
            setRowError(null)
            deleteMutation.mutate()
          }}
        >
          Excluir
        </button>
      </span>
      {rowError && <p role="alert">{rowError}</p>}
    </li>
  )
}
