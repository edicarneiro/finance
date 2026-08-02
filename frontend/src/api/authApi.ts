import { apiRequest } from './httpClient'

export type RegisterResponse = { userId: string }
export type LoginResponse = { token: string }

export function register(email: string, password: string): Promise<RegisterResponse> {
  return apiRequest<RegisterResponse>('/auth/register', {
    method: 'POST',
    body: { email, password },
    authenticated: false,
  })
}

export function login(email: string, password: string): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: { email, password },
    authenticated: false,
  })
}
