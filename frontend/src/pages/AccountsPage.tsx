import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import * as accountsApi from '../api/accountsApi'
import { ACCOUNT_TYPES, type Account } from '../api/accountsApi'
import { ApiError } from '../api/httpClient'
import styles from './AccountsPage.module.css'

const ACCOUNT_TYPE_LABELS: Record<(typeof ACCOUNT_TYPES)[number], string> = {
  CHECKING: 'Conta corrente',
  SAVINGS: 'Poupança',
  CREDIT_CARD: 'Cartão de crédito',
  CASH: 'Dinheiro em espécie',
  DIGITAL_WALLET: 'Carteira digital',
}

const createAccountSchema = z.object({
  type: z.enum(ACCOUNT_TYPES),
  name: z.string().min(1, 'Informe o nome da conta.'),
  currency: z
    .string()
    .length(3, 'Use o código de 3 letras da moeda (ex.: BRL).')
    .transform((value) => value.toUpperCase()),
  initialBalance: z.coerce.number({ message: 'Informe um valor numérico.' }),
})

type CreateAccountFormValues = z.input<typeof createAccountSchema>

function formatCurrency(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency }).format(amount)
  } catch {
    return `${amount.toFixed(2)} ${currency}`
  }
}

export function AccountsPage() {
  const queryClient = useQueryClient()
  const accountsQuery = useQuery({ queryKey: ['accounts'], queryFn: accountsApi.listAccounts })
  const balanceQuery = useQuery({ queryKey: ['accounts', 'consolidated-balance'], queryFn: accountsApi.getConsolidatedBalance })
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateAccountFormValues>({
    resolver: zodResolver(createAccountSchema),
    defaultValues: { type: 'CHECKING', name: '', currency: 'BRL', initialBalance: 0 },
  })

  const invalidateAccounts = () => {
    queryClient.invalidateQueries({ queryKey: ['accounts'] })
  }

  const createMutation = useMutation({
    mutationFn: accountsApi.createAccount,
    onSuccess: () => {
      invalidateAccounts()
      reset()
    },
  })

  const onSubmit = handleSubmit((values) => {
    setFormError(null)
    const parsed = createAccountSchema.parse(values)
    createMutation.mutate(parsed, {
      onError: (error) => setFormError(error instanceof ApiError ? error.message : 'Não foi possível criar a conta.'),
    })
  })

  return (
    <div>
      <h1>Contas</h1>

      {/* GetConsolidatedBalanceUseCase soma sem agrupar por moeda (MVP assume operação em moeda única, ver classe no backend) — BRL aqui só formata a exibição, não reflete uma escolha nova do frontend. */}
      <p>
        Saldo consolidado:{' '}
        {balanceQuery.isSuccess ? formatCurrency(balanceQuery.data.consolidatedBalance, 'BRL') : '—'}
      </p>

      <form onSubmit={onSubmit} className={styles.form} noValidate>
        <h2>Nova conta</h2>

        <label htmlFor="type">Tipo</label>
        <select id="type" {...register('type')}>
          {ACCOUNT_TYPES.map((type) => (
            <option key={type} value={type}>
              {ACCOUNT_TYPE_LABELS[type]}
            </option>
          ))}
        </select>

        <label htmlFor="name">Nome</label>
        <input id="name" {...register('name')} />
        {errors.name && <p role="alert">{errors.name.message}</p>}

        <label htmlFor="currency">Moeda</label>
        <input id="currency" {...register('currency')} maxLength={3} />
        {errors.currency && <p role="alert">{errors.currency.message}</p>}

        <label htmlFor="initialBalance">Saldo inicial</label>
        <input id="initialBalance" type="number" step="0.01" {...register('initialBalance')} />
        {errors.initialBalance && <p role="alert">{errors.initialBalance.message}</p>}

        {formError && <p role="alert">{formError}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando…' : 'Criar conta'}
        </button>
      </form>

      <h2>Suas contas</h2>
      {accountsQuery.isLoading && <p>Carregando…</p>}
      {accountsQuery.isSuccess && accountsQuery.data.length === 0 && <p>Nenhuma conta cadastrada ainda.</p>}
      <ul className={styles.list}>
        {accountsQuery.data?.map((account) => (
          <AccountRow key={account.id} account={account} onChanged={invalidateAccounts} />
        ))}
      </ul>
    </div>
  )
}

function AccountRow({ account, onChanged }: { account: Account; onChanged: () => void }) {
  const [editing, setEditing] = useState(false)
  const [confirmingArchive, setConfirmingArchive] = useState(false)
  const [name, setName] = useState(account.name)
  const [rowError, setRowError] = useState<string | null>(null)

  const updateMutation = useMutation({
    mutationFn: () => accountsApi.updateAccount(account.id, name),
    onSuccess: () => {
      setEditing(false)
      onChanged()
    },
    onError: (error) => setRowError(error instanceof ApiError ? error.message : 'Não foi possível salvar.'),
  })

  const archiveMutation = useMutation({
    mutationFn: () => accountsApi.archiveAccount(account.id),
    onSuccess: () => {
      setConfirmingArchive(false)
      onChanged()
    },
    onError: (error) => setRowError(error instanceof ApiError ? error.message : 'Não foi possível arquivar.'),
  })

  return (
    <li className={styles.row}>
      {editing ? (
        <form
          onSubmit={(event) => {
            event.preventDefault()
            setRowError(null)
            updateMutation.mutate()
          }}
        >
          <label htmlFor={`name-${account.id}`}>Nome</label>
          <input id={`name-${account.id}`} value={name} onChange={(event) => setName(event.target.value)} />
          <button type="submit">Salvar</button>
          <button type="button" onClick={() => setEditing(false)}>
            Cancelar
          </button>
        </form>
      ) : (
        <>
          <span>
            <strong>{account.name}</strong> — {ACCOUNT_TYPE_LABELS[account.type]} — {formatCurrency(account.balance, account.currency)}
            {account.archived && ' (arquivada)'}
          </span>
          {!account.archived && (
            <span className={styles.actions}>
              <button type="button" onClick={() => setEditing(true)}>
                Editar
              </button>
              {confirmingArchive ? (
                <>
                  <button type="button" onClick={() => archiveMutation.mutate()}>
                    Confirmar arquivamento
                  </button>
                  <button type="button" onClick={() => setConfirmingArchive(false)}>
                    Cancelar
                  </button>
                </>
              ) : (
                <button type="button" onClick={() => setConfirmingArchive(true)}>
                  Arquivar
                </button>
              )}
            </span>
          )}
        </>
      )}
      {rowError && <p role="alert">{rowError}</p>}
    </li>
  )
}
