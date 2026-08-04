import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import * as accountsApi from '../api/accountsApi'
import * as categoriesApi from '../api/categoriesApi'
import * as goalsApi from '../api/goalsApi'
import type { Goal } from '../api/goalsApi'
import { ApiError } from '../api/httpClient'
import { toThresholdList } from '../utils/thresholds'
import styles from './GoalsPage.module.css'

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount)
}

// RF-030 / InvalidGoalAssociationException (backend): uma meta é vinculada a exatamente uma conta OU
// uma categoria, nunca ambas nem nenhuma. O seletor abaixo garante isso na origem, sem duplicar a
// validação em si — o backend continua sendo a autoridade.
const createGoalSchema = z
  .object({
    name: z.string().min(1, 'Informe o nome da meta.'),
    targetAmount: z.coerce.number({ message: 'Informe um valor numérico.' }).positive('O valor-alvo deve ser maior que zero.'),
    deadline: z.string().min(1, 'Informe o prazo.'),
    associationType: z.enum(['account', 'category']),
    accountId: z.string(),
    categoryId: z.string(),
    progressAlertThresholds: z.string(),
  })
  .refine((values) => values.associationType !== 'account' || values.accountId !== '', {
    message: 'Selecione uma conta.',
    path: ['accountId'],
  })
  .refine((values) => values.associationType !== 'category' || values.categoryId !== '', {
    message: 'Selecione uma categoria.',
    path: ['categoryId'],
  })

type CreateGoalFormValues = z.input<typeof createGoalSchema>

export function GoalsPage() {
  const queryClient = useQueryClient()
  const goalsQuery = useQuery({ queryKey: ['goals'], queryFn: goalsApi.listGoals })
  const accountsQuery = useQuery({ queryKey: ['accounts'], queryFn: accountsApi.listAccounts })
  const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.listCategories })
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateGoalFormValues>({
    resolver: zodResolver(createGoalSchema),
    defaultValues: {
      name: '',
      targetAmount: 0,
      deadline: '',
      associationType: 'account',
      accountId: '',
      categoryId: '',
      progressAlertThresholds: '50,80,100',
    },
  })
  const associationType = watch('associationType')

  const invalidateGoals = () => queryClient.invalidateQueries({ queryKey: ['goals'] })

  const createMutation = useMutation({
    mutationFn: goalsApi.createGoal,
    onSuccess: () => {
      invalidateGoals()
      reset()
    },
  })

  const onSubmit = handleSubmit((values) => {
    setFormError(null)
    const parsed = createGoalSchema.parse(values)
    createMutation.mutate(
      {
        name: parsed.name,
        targetAmount: parsed.targetAmount,
        deadline: parsed.deadline,
        accountId: parsed.associationType === 'account' ? parsed.accountId : null,
        categoryId: parsed.associationType === 'category' ? parsed.categoryId : null,
        progressAlertThresholds: toThresholdList(parsed.progressAlertThresholds),
      },
      { onError: (error) => setFormError(error instanceof ApiError ? error.message : 'Não foi possível criar a meta.') },
    )
  })

  const accountName = (accountId: string | null) => accountsQuery.data?.find((a) => a.id === accountId)?.name
  const categoryName = (categoryId: string | null) => categoriesQuery.data?.find((c) => c.id === categoryId)?.name

  return (
    <div>
      <h1>Metas Financeiras</h1>

      <form onSubmit={onSubmit} className={styles.form} noValidate>
        <h2>Nova meta</h2>

        <label htmlFor="name">Nome</label>
        <input id="name" {...register('name')} />
        {errors.name && <p role="alert">{errors.name.message}</p>}

        <label htmlFor="targetAmount">Valor-alvo</label>
        <input id="targetAmount" type="number" step="0.01" {...register('targetAmount')} />
        {errors.targetAmount && <p role="alert">{errors.targetAmount.message}</p>}

        <label htmlFor="deadline">Prazo</label>
        <input id="deadline" type="date" {...register('deadline')} />
        {errors.deadline && <p role="alert">{errors.deadline.message}</p>}

        <label htmlFor="associationType">Vincular a</label>
        <select id="associationType" {...register('associationType')}>
          <option value="account">Uma conta</option>
          <option value="category">Uma categoria</option>
        </select>

        {associationType === 'account' ? (
          <>
            <label htmlFor="accountId">Conta</label>
            <select id="accountId" {...register('accountId')}>
              <option value="">Selecione uma conta</option>
              {accountsQuery.data?.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name}
                </option>
              ))}
            </select>
            {errors.accountId && <p role="alert">{errors.accountId.message}</p>}
          </>
        ) : (
          <>
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
          </>
        )}

        <label htmlFor="progressAlertThresholds">Limiares de progresso (%, separados por vírgula)</label>
        <input id="progressAlertThresholds" {...register('progressAlertThresholds')} />

        {formError && <p role="alert">{formError}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando…' : 'Criar meta'}
        </button>
      </form>

      <section aria-label="Suas metas">
        <h2>Suas metas</h2>
        {goalsQuery.isLoading && <p>Carregando…</p>}
        {goalsQuery.isSuccess && goalsQuery.data.length === 0 && <p>Nenhuma meta cadastrada ainda.</p>}
        <ul className={styles.list}>
          {goalsQuery.data?.map((goal) => (
            <GoalRow
              key={goal.id}
              goal={goal}
              associationLabel={goal.accountId ? accountName(goal.accountId) : categoryName(goal.categoryId)}
              onChanged={invalidateGoals}
            />
          ))}
        </ul>
      </section>
    </div>
  )
}

function GoalRow({ goal, associationLabel, onChanged }: { goal: Goal; associationLabel: string | undefined; onChanged: () => void }) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(goal.name)
  const [targetAmount, setTargetAmount] = useState(String(goal.targetAmount))
  const [deadline, setDeadline] = useState(goal.deadline)
  const [rowError, setRowError] = useState<string | null>(null)

  const updateMutation = useMutation({
    mutationFn: () =>
      goalsApi.updateGoal(goal.id, {
        name,
        targetAmount: Number(targetAmount),
        deadline,
        progressAlertThresholds: goal.progressAlertThresholds,
      }),
    onSuccess: () => {
      setEditing(false)
      onChanged()
    },
    onError: (error) => setRowError(error instanceof ApiError ? error.message : 'Não foi possível salvar.'),
  })

  const deleteMutation = useMutation({
    mutationFn: () => goalsApi.deleteGoal(goal.id),
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
          <label htmlFor={`name-${goal.id}`}>Nome</label>
          <input id={`name-${goal.id}`} value={name} onChange={(event) => setName(event.target.value)} />
          <label htmlFor={`target-${goal.id}`}>Valor-alvo</label>
          <input id={`target-${goal.id}`} type="number" step="0.01" value={targetAmount} onChange={(event) => setTargetAmount(event.target.value)} />
          <label htmlFor={`deadline-${goal.id}`}>Prazo</label>
          <input id={`deadline-${goal.id}`} type="date" value={deadline} onChange={(event) => setDeadline(event.target.value)} />
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
        <strong>{goal.name}</strong> — {associationLabel ?? '—'} — {formatCurrency(goal.currentAmount)} de {formatCurrency(goal.targetAmount)} (
        {goal.progressPercentage.toFixed(0)}%) — prazo {goal.deadline}
        {goal.achieved && ' — concluída'}
        {goal.overdue && !goal.achieved && ' — atrasada'}
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
