import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { queryClient } from '../queryClient'
import { resetTestState, server } from './server'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => {
  server.resetHandlers()
  resetTestState()
  sessionStorage.clear()
  queryClient.clear()
})
afterAll(() => server.close())
