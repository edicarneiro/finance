import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import * as backofficeApi from '../api/backofficeApi'
import { ApiError } from '../api/httpClient'
import styles from './BackofficePage.module.css'

const AUDIT_ACTION_LABELS: Record<(typeof backofficeApi.AUDIT_ACTIONS)[number], string> = {
  VIEWED_USER_DATA: 'Dados do usuário visualizados',
  SUSPENDED_ACCOUNT: 'Conta suspensa',
  REACTIVATED_ACCOUNT: 'Conta reativada',
}

export function BackofficePage() {
  const [userId, setUserId] = useState('')
  const [activeUserId, setActiveUserId] = useState<string | null>(null)

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    setActiveUserId(userId.trim())
  }

  return (
    <div>
      <h1>Backoffice</h1>
      <section aria-label="Buscar usuário" className={styles.section}>
        <h2>Buscar usuário</h2>
        <p>
          Disponível apenas para operadores de suporte (permissão verificada pelo backend a cada chamada). Se sua conta não tiver essa
          permissão, a busca abaixo mostrará o erro real retornado pela API.
        </p>
        <form onSubmit={onSubmit}>
          <label htmlFor="targetUserId">ID do usuário</label>
          <input id="targetUserId" value={userId} onChange={(event) => setUserId(event.target.value)} required />
          <button type="submit">Buscar</button>
        </form>
      </section>
      {activeUserId && <UserLookupSection key={activeUserId} userId={activeUserId} />}
    </div>
  )
}

function UserLookupSection({ userId }: { userId: string }) {
  const lookupQuery = useQuery({ queryKey: ['backoffice-user', userId], queryFn: () => backofficeApi.getUserForSupport(userId) })

  return (
    <section aria-label="Dados do usuário" className={styles.section}>
      <h2>Dados do usuário</h2>
      {lookupQuery.isLoading && <p>Carregando…</p>}
      {lookupQuery.isError && (
        <p role="alert">{lookupQuery.error instanceof ApiError ? lookupQuery.error.message : 'Não foi possível buscar o usuário.'}</p>
      )}
      {lookupQuery.data && (
        <>
          <ul className={styles.summaryList}>
            <li>E-mail: {lookupQuery.data.profile.email}</li>
            <li>Nome: {lookupQuery.data.profile.name ?? '—'}</li>
            <li>Criado em: {new Date(lookupQuery.data.profile.createdAt).toLocaleString('pt-BR')}</li>
            <li>
              Excluído em:{' '}
              {lookupQuery.data.profile.deletedAt ? new Date(lookupQuery.data.profile.deletedAt).toLocaleString('pt-BR') : 'não excluído'}
            </li>
            <li>Contas: {lookupQuery.data.accounts.length}</li>
            <li>Transações: {lookupQuery.data.transactions.length}</li>
          </ul>
          <ActionsSection userId={userId} />
          <AuditLogSection userId={userId} />
        </>
      )}
    </section>
  )
}

function ActionsSection({ userId }: { userId: string }) {
  const queryClient = useQueryClient()
  const [reason, setReason] = useState('')
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionMessage, setActionMessage] = useState<string | null>(null)

  const invalidateAuditLog = () => queryClient.invalidateQueries({ queryKey: ['backoffice-audit-log', userId] })

  const suspendMutation = useMutation({
    mutationFn: () => backofficeApi.suspendAccount(userId, reason),
    onSuccess: () => {
      setActionMessage('Conta suspensa.')
      setReason('')
      invalidateAuditLog()
    },
    onError: (error) => setActionError(error instanceof ApiError ? error.message : 'Não foi possível suspender a conta.'),
  })

  const reactivateMutation = useMutation({
    mutationFn: () => backofficeApi.reactivateAccount(userId, reason),
    onSuccess: () => {
      setActionMessage('Conta reativada.')
      setReason('')
      invalidateAuditLog()
    },
    onError: (error) => setActionError(error instanceof ApiError ? error.message : 'Não foi possível reativar a conta.'),
  })

  return (
    <div>
      <h3>Ações</h3>
      <label htmlFor="reason">Motivo (opcional)</label>
      <input
        id="reason"
        value={reason}
        onChange={(event) => {
          setReason(event.target.value)
          setActionMessage(null)
        }}
      />
      <div className={styles.actions}>
        <button
          type="button"
          onClick={() => {
            setActionError(null)
            setActionMessage(null)
            suspendMutation.mutate()
          }}
          disabled={suspendMutation.isPending}
        >
          {suspendMutation.isPending ? 'Suspendendo…' : 'Suspender conta'}
        </button>
        <button
          type="button"
          onClick={() => {
            setActionError(null)
            setActionMessage(null)
            reactivateMutation.mutate()
          }}
          disabled={reactivateMutation.isPending}
        >
          {reactivateMutation.isPending ? 'Reativando…' : 'Reativar conta'}
        </button>
      </div>
      {actionError && <p role="alert">{actionError}</p>}
      {actionMessage && <p>{actionMessage}</p>}
    </div>
  )
}

function AuditLogSection({ userId }: { userId: string }) {
  const auditLogQuery = useQuery({ queryKey: ['backoffice-audit-log', userId], queryFn: () => backofficeApi.getAuditLog(userId) })

  return (
    <div>
      <h3>Log de auditoria</h3>
      {auditLogQuery.isLoading && <p>Carregando…</p>}
      {auditLogQuery.isSuccess && auditLogQuery.data.length === 0 && <p>Nenhum registro de auditoria.</p>}
      <ul className={styles.list}>
        {auditLogQuery.data?.map((entry, index) => (
          <li key={index}>
            {AUDIT_ACTION_LABELS[entry.action]} por {entry.operatorUserId} em {new Date(entry.createdAt).toLocaleString('pt-BR')}
            {entry.details && ` — ${entry.details}`}
          </li>
        ))}
      </ul>
    </div>
  )
}
