import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/authApi'
import { onSessionExpired, setAuthToken } from '../api/httpClient'
import { AuthContext, type AuthContextValue } from './authContextInstance'

const TOKEN_STORAGE_KEY = 'financepulse.token'

/**
 * Sem refresh token no backend-java (ver ADR-0025): o token expira em 15
 * minutos e não há renovação silenciosa possível. sessionStorage (não
 * localStorage) é usado deliberadamente — o token não sobrevive ao
 * fechamento da aba, reduzindo a janela de exposição para dados financeiros.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => sessionStorage.getItem(TOKEN_STORAGE_KEY))
  const [sessionExpired, setSessionExpired] = useState(false)

  useEffect(() => {
    setAuthToken(token)
    if (token) {
      sessionStorage.setItem(TOKEN_STORAGE_KEY, token)
    } else {
      sessionStorage.removeItem(TOKEN_STORAGE_KEY)
    }
  }, [token])

  const logout = useCallback(() => {
    setToken(null)
  }, [])

  useEffect(() => {
    onSessionExpired(() => {
      setToken(null)
      setSessionExpired(true)
    })
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login(email, password)
    setToken(response.token)
    // Um novo login bem-sucedido invalida qualquer aviso de sessão expirada anterior — sem isto, o
    // banner "sua sessão expirou" reaparece na tela recém-autenticada mesmo com uma sessão nova e válida.
    setSessionExpired(false)
  }, [])

  const register = useCallback(async (email: string, password: string) => {
    await authApi.register(email, password)
    const response = await authApi.login(email, password)
    setToken(response.token)
    setSessionExpired(false)
  }, [])

  const dismissSessionExpired = useCallback(() => setSessionExpired(false), [])

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      isAuthenticated: token !== null,
      sessionExpired,
      login,
      register,
      logout,
      dismissSessionExpired,
    }),
    [token, sessionExpired, login, register, logout, dismissSessionExpired],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
