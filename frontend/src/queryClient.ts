import { QueryClient } from '@tanstack/react-query'

// Módulo próprio (não definido inline em App.tsx) para que os testes possam limpar o cache entre
// execuções via queryClient.clear() — sem isto, dados em cache de um teste anterior (ex.: uma conta
// que já não existe mais no MSW após o reset de estado) vazam para o teste seguinte no mesmo arquivo.
export const queryClient = new QueryClient()
