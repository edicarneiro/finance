import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'
import { ApiError, apiRequest, onSessionExpired, setAuthToken } from './httpClient'

const API_BASE_URL = 'http://localhost:8080'

describe('httpClient', () => {
  beforeEach(() => {
    setAuthToken(null)
    onSessionExpired(() => {})
  })

  it('sends the Authorization header when a token is set and the call is authenticated', async () => {
    let receivedAuthHeader: string | null = null
    server.use(
      http.get(`${API_BASE_URL}/probe`, ({ request }) => {
        receivedAuthHeader = request.headers.get('Authorization')
        return HttpResponse.json({ ok: true })
      }),
    )
    setAuthToken('token-123')

    await apiRequest('/probe')

    expect(receivedAuthHeader).toBe('Bearer token-123')
  })

  it('does not send the Authorization header for unauthenticated calls even when a token is set', async () => {
    let receivedAuthHeader: string | null = 'not-checked-yet'
    server.use(
      http.post(`${API_BASE_URL}/auth/login`, ({ request }) => {
        receivedAuthHeader = request.headers.get('Authorization')
        return HttpResponse.json({ token: 'x' })
      }),
    )
    setAuthToken('token-123')

    await apiRequest('/auth/login', { method: 'POST', body: {}, authenticated: false })

    expect(receivedAuthHeader).toBeNull()
  })

  it('throws an ApiError with the backend error message on a non-ok response', async () => {
    server.use(http.get(`${API_BASE_URL}/probe`, () => HttpResponse.json({ error: 'Algo deu errado.' }, { status: 400 })))

    await expect(apiRequest('/probe')).rejects.toMatchObject(
      new ApiError('Algo deu errado.', 400),
    )
  })

  it('notifies the session-expired listener on a 401 from an authenticated call', async () => {
    server.use(http.get(`${API_BASE_URL}/probe`, () => HttpResponse.json({ error: 'Token inválido.' }, { status: 401 })))
    const listener = vi.fn()
    onSessionExpired(listener)

    await expect(apiRequest('/probe')).rejects.toBeInstanceOf(ApiError)

    expect(listener).toHaveBeenCalledOnce()
  })

  it('does not notify the session-expired listener on a 401 from an unauthenticated call (e.g. wrong login credentials)', async () => {
    server.use(http.post(`${API_BASE_URL}/auth/login`, () => HttpResponse.json({ error: 'Credenciais inválidas.' }, { status: 401 })))
    const listener = vi.fn()
    onSessionExpired(listener)

    await expect(apiRequest('/auth/login', { method: 'POST', body: {}, authenticated: false })).rejects.toBeInstanceOf(ApiError)

    expect(listener).not.toHaveBeenCalled()
  })

  // Achado de QA (Fase 13.2): vários endpoints do backend respondem 200 com corpo vazio
  // (ex.: AccountController.update, ResponseEntity.ok().build()), não só 204 — response.json()
  // lança uma exceção de parsing em um corpo vazio.
  it('resolves to undefined for a 200 response with an empty body, without throwing', async () => {
    server.use(http.put(`${API_BASE_URL}/probe`, () => new HttpResponse(null, { status: 200 })))

    await expect(apiRequest('/probe', { method: 'PUT', body: { name: 'x' } })).resolves.toBeUndefined()
  })

  it('still parses the JSON body of a normal 200 response', async () => {
    server.use(http.get(`${API_BASE_URL}/probe`, () => HttpResponse.json({ value: 42 })))

    await expect(apiRequest('/probe')).resolves.toEqual({ value: 42 })
  })
})
