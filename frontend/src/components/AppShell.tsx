import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import styles from './AppShell.module.css'

export function AppShell({ children }: { children: ReactNode }) {
  const { logout, sessionExpired, dismissSessionExpired } = useAuth()

  return (
    <div className={styles.shell}>
      {sessionExpired && (
        <div className={styles.sessionExpiredBanner} role="alert">
          <span>Sua sessão expirou. Entre novamente para continuar.</span>
          <button type="button" onClick={dismissSessionExpired}>
            Fechar
          </button>
        </div>
      )}
      <nav className={styles.nav}>
        <span className={styles.brand}>FinancePulse</span>
        <div className={styles.links}>
          <NavLink to="/">Dashboard</NavLink>
          <NavLink to="/accounts">Contas</NavLink>
          <NavLink to="/categories">Categorias</NavLink>
          <NavLink to="/transactions">Transações</NavLink>
          <NavLink to="/budgets">Orçamentos</NavLink>
          <NavLink to="/goals">Metas</NavLink>
          <NavLink to="/reports">Relatórios</NavLink>
          <NavLink to="/notifications">Notificações</NavLink>
          <NavLink to="/privacy">Privacidade</NavLink>
          <NavLink to="/backoffice">Backoffice</NavLink>
        </div>
        <button type="button" onClick={logout}>
          Sair
        </button>
      </nav>
      <main className={styles.main}>{children}</main>
    </div>
  )
}
