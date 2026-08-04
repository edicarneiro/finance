import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import * as budgetsApi from '../api/budgetsApi'
import { BUDGET_PERIOD_TYPES, type Budget, type BudgetPeriodPerformance } from '../api/budgetsApi'
import * as categoriesApi from '../api/categoriesApi'
import { ApiError } from '../api/httpClient'
import { toThresholdList } from '../utils/thresholds'
import styles from './BudgetsPage.module.css'

const PERIOD_TYPE_LABELS: Record<(typeof BUDGET_PERIOD_TYPES)[number], string> = {
  MONTHLY: 'Mensal',
  WEEKLY: 'Semanal',
  CUSTOM: 'Período customizado',
}

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount)
}

const createBudgetSchema = z
  .object({
    categoryId: z.string().min(1, 'Selecione uma categoria.'),
    limitAmount: z.coerce.number({ message: 'Informe um valor numérico.' }).positive('O limite deve ser maior que zero.'),
    periodType: z.enum(BUDGET_PERIOD_TYPES),
    customPeriodStart: z.string(),
    customPeriodEnd: z.string(),
    alertThresholds: z.string(),
  })
  .refine((values) => values.periodType !== 'CUSTOM' || values.customPeriodStart !== '', {
    message: 'Informe a data de início do período customizado.',
    path: ['customPeriodStart'],
  })
  .refine((values) => values.periodType !== 'CUSTOM' || values.customPeriodEnd !== '', {
    message: 'Informe a data de fim do período customizado.',
    path: ['customPeriodEnd'],
  })

type CreateBudgetFormValues = z.input<typeof createBudgetSchema>

export function BudgetsPage() {
  const queryClient = useQueryClient()
  const budgetsQuery = useQuery({ queryKey: ['budgets'], queryFn: budgetsApi.listBudgets })
  const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.listCategories })
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateBudgetFormValues>({
    resolver: zodResolver(createBudgetSchema),
    defaultValues: {
      categoryId: '',
      limitAmount: 0,
      periodType: 'MONTHLY',
      customPeriodStart: '',
      customPeriodEnd: '',
      alertThresholds: '80,100',
    },
  })
  const periodType = watch('periodType')

  const invalidateBudgets = () => queryClient.invalidateQueries({ queryKey: ['budgets'] })

  const createMutation = useMutation({
    mutationFn: budgetsApi.createBudget,
    onSuccess: () => {
      invalidateBudgets()
      reset()
    },
  })

  const onSubmit = handleSubmit((values) => {
    setFormError(null)
    const parsed = createBudgetSchema.parse(values)
    createMutation.mutate(
      {
        categoryId: parsed.categoryId,
        limitAmount: parsed.limitAmount,
        periodType: parsed.periodType,
        customPeriodStart: parsed.periodType === 'CUSTOM' ? parsed.customPeriodStart : null,
        customPeriodEnd: parsed.periodType === 'CUSTOM' ? parsed.customPeriodEnd : null,
        alertThresholds: toThresholdList(parsed.alertThresholds),
      },
      { onError: (error) => setFormError(error instanceof ApiError ? error.message : 'Não foi possível criar o orçamento.') },
    )
  })

  const categoryName = (categoryId: string) => categoriesQuery.data?.find((c) => c.id === categoryId)?.name ?? categoryId

  return (
    <div>
      <h1>Orçamentos</h1>

      <form onSubmit={onSubmit} className={styles.form} noValidate>
        <h2>Novo orçamento</h2>

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

        <label htmlFor="limitAmount">Limite</label>
        <input id="limitAmount" type="number" step="0.01" {...register('limitAmount')} />
        {errors.limitAmount && <p role="alert">{errors.limitAmount.message}</p>}

        <label htmlFor="periodType">Período</label>
        <select id="periodType" {...register('periodType')}>
          {BUDGET_PERIOD_TYPES.map((type) => (
            <option key={type} value={type}>
              {PERIOD_TYPE_LABELS[type]}
            </option>
          ))}
        </select>

        {periodType === 'CUSTOM' && (
          <>
            <label htmlFor="customPeriodStart">Início do período</label>
            <input id="customPeriodStart" type="date" {...register('customPeriodStart')} />
            {errors.customPeriodStart && <p role="alert">{errors.customPeriodStart.message}</p>}

            <label htmlFor="customPeriodEnd">Fim do período</label>
            <input id="customPeriodEnd" type="date" {...register('customPeriodEnd')} />
            {errors.customPeriodEnd && <p role="alert">{errors.customPeriodEnd.message}</p>}
          </>
        )}

        <label htmlFor="alertThresholds">Limiares de alerta (%, separados por vírgula)</label>
        <input id="alertThresholds" {...register('alertThresholds')} />

        {formError && <p role="alert">{formError}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando…' : 'Criar orçamento'}
        </button>
      </form>

      <section aria-label="Seus orçamentos">
        <h2>Seus orçamentos</h2>
        {budgetsQuery.isLoading && <p>Carregando…</p>}
        {budgetsQuery.isSuccess && budgetsQuery.data.length === 0 && <p>Nenhum orçamento cadastrado ainda.</p>}
        <ul className={styles.list}>
          {budgetsQuery.data?.map((budget) => (
            <BudgetRow key={budget.id} budget={budget} categoryName={categoryName(budget.categoryId)} onChanged={invalidateBudgets} />
          ))}
        </ul>
      </section>
    </div>
  )
}

function BudgetRow({ budget, categoryName, onChanged }: { budget: Budget; categoryName: string; onChanged: () => void }) {
  const [editing, setEditing] = useState(false)
  const [limitAmount, setLimitAmount] = useState(String(budget.limitAmount))
  const [alertThresholds, setAlertThresholds] = useState(budget.alertThresholds.join(','))
  const [showHistory, setShowHistory] = useState(false)
  const [rowError, setRowError] = useState<string | null>(null)

  const historyQuery = useQuery({
    queryKey: ['budgets', budget.id, 'history'],
    queryFn: () => budgetsApi.getBudgetHistory(budget.id),
    enabled: showHistory,
  })

  const updateMutation = useMutation({
    mutationFn: () => budgetsApi.updateBudget(budget.id, Number(limitAmount), toThresholdList(alertThresholds)),
    onSuccess: () => {
      setEditing(false)
      onChanged()
    },
    onError: (error) => setRowError(error instanceof ApiError ? error.message : 'Não foi possível salvar.'),
  })

  const deleteMutation = useMutation({
    mutationFn: () => budgetsApi.deleteBudget(budget.id),
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
          <label htmlFor={`limit-${budget.id}`}>Limite</label>
          <input id={`limit-${budget.id}`} type="number" step="0.01" value={limitAmount} onChange={(event) => setLimitAmount(event.target.value)} />
          <label htmlFor={`thresholds-${budget.id}`}>Limiares (%)</label>
          <input id={`thresholds-${budget.id}`} value={alertThresholds} onChange={(event) => setAlertThresholds(event.target.value)} />
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
        <strong>{categoryName}</strong> — {PERIOD_TYPE_LABELS[budget.periodType]} ({budget.periodStart} a {budget.periodEnd}) — consumido{' '}
        {formatCurrency(budget.consumedAmount)} de {formatCurrency(budget.limitAmount)} ({budget.consumedPercentage.toFixed(0)}%)
        {budget.thresholdsCrossed.length > 0 && ` — alerta: ${budget.thresholdsCrossed.join('%, ')}%`}
      </span>
      <span className={styles.actions}>
        <button type="button" onClick={() => setEditing(true)}>
          Editar
        </button>
        <button type="button" onClick={() => setShowHistory((prev) => !prev)}>
          {showHistory ? 'Ocultar histórico' : 'Ver histórico'}
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
      {showHistory && (
        <ul className={styles.history}>
          {historyQuery.data?.map((period: BudgetPeriodPerformance) => (
            <li key={period.periodStart}>
              {period.periodStart} a {period.periodEnd}: {formatCurrency(period.consumedAmount)} ({period.consumedPercentage.toFixed(0)}%)
            </li>
          ))}
        </ul>
      )}
      {rowError && <p role="alert">{rowError}</p>}
    </li>
  )
}
