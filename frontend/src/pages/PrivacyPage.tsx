import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import * as privacyApi from '../api/privacyApi'
import { ApiError } from '../api/httpClient'
import { useAuth } from '../auth/useAuth'
import styles from './PrivacyPage.module.css'

export function PrivacyPage() {
  return (
    <div>
      <h1>Privacidade</h1>
      <ExportSection />
      <ConsentSection />
      <DeleteAccountSection />
    </div>
  )
}

function ExportSection() {
  const [exportError, setExportError] = useState<string | null>(null)

  const exportMutation = useMutation({
    mutationFn: privacyApi.exportUserData,
    onError: (error) => setExportError(error instanceof ApiError ? error.message : 'Não foi possível gerar a exportação.'),
  })

  const onDownload = () => {
    if (!exportMutation.data) return
    const blob = new Blob([JSON.stringify(exportMutation.data, null, 2)], { type: 'application/json' })
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `financepulse-dados-${exportMutation.data.profile.id}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
  }

  return (
    <section aria-label="Exportar meus dados" className={styles.section}>
      <h2>Exportar meus dados</h2>
      <p>Gera um documento único com todos os seus dados pessoais e financeiros armazenados.</p>
      <button
        type="button"
        onClick={() => {
          setExportError(null)
          exportMutation.mutate()
        }}
        disabled={exportMutation.isPending}
      >
        {exportMutation.isPending ? 'Gerando…' : 'Gerar exportação'}
      </button>
      {exportError && <p role="alert">{exportError}</p>}
      {exportMutation.data && (
        <div>
          <ul className={styles.summaryList}>
            <li>Contas: {exportMutation.data.accounts.length}</li>
            <li>Transações: {exportMutation.data.transactions.length}</li>
            <li>Categorias: {exportMutation.data.categories.length}</li>
            <li>Orçamentos: {exportMutation.data.budgets.length}</li>
            <li>Metas: {exportMutation.data.goals.length}</li>
            <li>Notificações: {exportMutation.data.notifications.length}</li>
            <li>Registros de consentimento: {exportMutation.data.consentHistory.length}</li>
          </ul>
          <button type="button" onClick={onDownload}>
            Baixar como JSON
          </button>
        </div>
      )}
    </section>
  )
}

function ConsentSection() {
  const queryClient = useQueryClient()
  const consentsQuery = useQuery({ queryKey: ['privacy-consents'], queryFn: privacyApi.listConsents })
  const [version, setVersion] = useState('')
  const [recordError, setRecordError] = useState<string | null>(null)

  const recordMutation = useMutation({
    mutationFn: (version: string) => privacyApi.recordConsent(version),
    onSuccess: () => {
      setVersion('')
      queryClient.invalidateQueries({ queryKey: ['privacy-consents'] })
    },
    onError: (error) => setRecordError(error instanceof ApiError ? error.message : 'Não foi possível registrar o consentimento.'),
  })

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    setRecordError(null)
    recordMutation.mutate(version)
  }

  return (
    <section aria-label="Consentimento" className={styles.section}>
      <h2>Consentimento</h2>
      <form onSubmit={onSubmit}>
        <label htmlFor="consentVersion">Versão dos termos aceitos</label>
        <input id="consentVersion" value={version} onChange={(event) => setVersion(event.target.value)} required />
        {recordError && <p role="alert">{recordError}</p>}
        <button type="submit" disabled={recordMutation.isPending}>
          {recordMutation.isPending ? 'Registrando…' : 'Registrar consentimento'}
        </button>
      </form>

      <h3>Histórico</h3>
      {consentsQuery.isLoading && <p>Carregando…</p>}
      {consentsQuery.isSuccess && consentsQuery.data.length === 0 && <p>Nenhum consentimento registrado.</p>}
      <ul className={styles.list}>
        {consentsQuery.data?.map((consent) => (
          <li key={consent.id}>
            Versão {consent.version} — aceito em {new Date(consent.acceptedAt).toLocaleString('pt-BR')}
          </li>
        ))}
      </ul>
    </section>
  )
}

function DeleteAccountSection() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [confirming, setConfirming] = useState(false)
  const [password, setPassword] = useState('')
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const deleteMutation = useMutation({
    mutationFn: (password: string) => privacyApi.deleteAccount(password),
    onSuccess: () => {
      logout()
      navigate('/login', { replace: true })
    },
    onError: (error) => setDeleteError(error instanceof ApiError ? error.message : 'Não foi possível excluir a conta.'),
  })

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    setDeleteError(null)
    deleteMutation.mutate(password)
  }

  return (
    <section aria-label="Excluir minha conta" className={styles.section}>
      <h2>Excluir minha conta</h2>
      <p>Sua conta será anonimizada e você perderá o acesso. Seus dados financeiros não são apagados nem anonimizados.</p>
      {confirming ? (
        <form onSubmit={onSubmit}>
          <label htmlFor="deletePassword">Confirme sua senha</label>
          <input
            id="deletePassword"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          {deleteError && <p role="alert">{deleteError}</p>}
          <button type="submit" disabled={deleteMutation.isPending}>
            {deleteMutation.isPending ? 'Excluindo…' : 'Confirmar exclusão'}
          </button>
          <button type="button" onClick={() => setConfirming(false)}>
            Cancelar
          </button>
        </form>
      ) : (
        <button type="button" onClick={() => setConfirming(true)}>
          Excluir minha conta
        </button>
      )}
    </section>
  )
}
