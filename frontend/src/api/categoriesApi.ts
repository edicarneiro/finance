import { apiRequest } from './httpClient'

export type Category = {
  id: string
  name: string
  parentCategoryId: string | null
}

export function listCategories(): Promise<Category[]> {
  return apiRequest<Category[]>('/categories')
}

export function createCategory(name: string, parentCategoryId: string | null): Promise<{ categoryId: string }> {
  return apiRequest('/categories', { method: 'POST', body: { name, parentCategoryId } })
}

export function updateCategory(id: string, name: string): Promise<void> {
  return apiRequest(`/categories/${id}`, { method: 'PUT', body: { name } })
}

export function deleteCategory(id: string): Promise<void> {
  return apiRequest(`/categories/${id}`, { method: 'DELETE' })
}
