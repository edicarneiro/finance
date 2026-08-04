const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type SessionExpiredListener = () => void

let sessionExpiredListener: SessionExpiredListener | null = null

/** AuthProvider se registra aqui para reagir a um 401 vindo de qualquer chamada autenticada (ver ADR-0025). */
export function onSessionExpired(listener: SessionExpiredListener): void {
  sessionExpiredListener = listener
}

let authToken: string | null = null

/** AuthProvider mantém isto sincronizado com seu próprio estado — única fonte do header Authorization. */
export function setAuthToken(token: string | null): void {
  authToken = token
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  /** false para /auth/register e /auth/login: um 401 ali é uma credencial inválida, não uma sessão expirada. */
  authenticated?: boolean
  /**
   * false para chamadas autenticadas que também exigem senha como reautenticação (ex.: excluir
   * conta) — nelas um 401 significa "senha de confirmação incorreta", não "sessão expirada", e não
   * deve deslogar o usuário silenciosamente no meio do fluxo.
   */
  treatUnauthorizedAsSessionExpired?: boolean
}

async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const data = (await response.json()) as { error?: string }
    return data.error ?? `Erro inesperado (HTTP ${response.status}).`
  } catch {
    return `Erro inesperado (HTTP ${response.status}).`
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, authenticated = true, treatUnauthorizedAsSessionExpired = true } = options

  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (authenticated && authToken) {
    headers['Authorization'] = `Bearer ${authToken}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    const message = await parseErrorMessage(response)

    if (response.status === 401 && authenticated && treatUnauthorizedAsSessionExpired) {
      sessionExpiredListener?.()
    }

    throw new ApiError(message, response.status)
  }

  // Vários endpoints do backend respondem 200 com corpo vazio (ex.: `ResponseEntity.ok().build()` em
  // AccountController/CategoryController), não só 204 — response.json() lança em um corpo vazio, então
  // o corpo é lido como texto primeiro e só parseado se houver conteúdo.
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

const FALLBACK_DOWNLOAD_FILENAME = 'download'

function filenameFromContentDisposition(header: string | null): string | null {
  const match = header?.match(/filename="?([^";]+)"?/)
  return match ? match[1] : null
}

/**
 * Baixa um arquivo autenticado (ex.: exportação CSV) e dispara o download no navegador.
 * `<a href>` simples não funciona aqui — a autenticação é via header `Authorization`, que uma
 * navegação/clique de link comum não envia; por isso a chamada precisa passar por `fetch` com o
 * mesmo token das demais requisições, depois converter a resposta em um Blob local.
 */
export async function downloadFile(path: string): Promise<void> {
  const headers: Record<string, string> = {}
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { headers })

  if (!response.ok) {
    const message = await parseErrorMessage(response)
    if (response.status === 401) {
      sessionExpiredListener?.()
    }
    throw new ApiError(message, response.status)
  }

  const filename = filenameFromContentDisposition(response.headers.get('Content-Disposition')) ?? FALLBACK_DOWNLOAD_FILENAME
  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}
