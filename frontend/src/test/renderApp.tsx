import { render } from '@testing-library/react'
import App from '../App'

/** Renderiza a árvore real da aplicação (App real, rotas reais, AuthProvider real) a partir de um caminho dado. */
export function renderApp(path: string) {
  window.history.pushState({}, '', path)
  return render(<App />)
}
