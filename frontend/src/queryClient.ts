import { QueryClient } from '@tanstack/react-query'

// Módulo próprio (não definido inline em App.tsx) para que os testes possam limpar o cache entre
// execuções via queryClient.clear() — sem isto, dados em cache de um teste anterior (ex.: uma conta
// que já não existe mais no MSW após o reset de estado) vazam para o teste seguinte no mesmo arquivo.
export const queryClient = new QueryClient({
  defaultOptions: {
    // TanStack Query tenta de novo (3x, com backoff) qualquer useQuery que falhe, por padrão — ao
    // contrário de useMutation, que não tenta de novo. A maioria dos erros deste app (ApiError vindo
    // de uma validação real do backend, ex.: "data final deve ser posterior à inicial") é
    // determinística: tentar de novo produz o mesmo erro, só atrasa o usuário ver a mensagem.
    queries: { retry: false },
  },
})
