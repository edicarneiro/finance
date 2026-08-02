import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import * as categoriesApi from '../api/categoriesApi'
import type { Category } from '../api/categoriesApi'
import { ApiError } from '../api/httpClient'
import styles from './CategoriesPage.module.css'

const createCategorySchema = z.object({
  name: z.string().min(1, 'Informe o nome da categoria.'),
  parentCategoryId: z.string(),
})

type CreateCategoryFormValues = z.infer<typeof createCategorySchema>

export function CategoriesPage() {
  const queryClient = useQueryClient()
  const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.listCategories })
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateCategoryFormValues>({
    resolver: zodResolver(createCategorySchema),
    defaultValues: { name: '', parentCategoryId: '' },
  })

  const invalidateCategories = () => queryClient.invalidateQueries({ queryKey: ['categories'] })

  const createMutation = useMutation({
    mutationFn: (values: CreateCategoryFormValues) =>
      categoriesApi.createCategory(values.name, values.parentCategoryId === '' ? null : values.parentCategoryId),
    onSuccess: () => {
      invalidateCategories()
      reset()
    },
  })

  const onSubmit = handleSubmit((values) => {
    setFormError(null)
    createMutation.mutate(values, {
      onError: (error) => setFormError(error instanceof ApiError ? error.message : 'Não foi possível criar a categoria.'),
    })
  })

  const categories = categoriesQuery.data ?? []
  // Hierarquia limitada a 2 níveis no backend (InvalidCategoryHierarchyException) — só categorias de
  // topo são oferecidas como pai, evitando um erro previsível sem duplicar a regra em si (o backend
  // continua sendo a autoridade: se o contrato mudar, o erro do servidor ainda aparece normalmente).
  const topLevelCategories = categories.filter((category) => category.parentCategoryId === null)

  return (
    <div>
      <h1>Categorias</h1>

      <form onSubmit={onSubmit} className={styles.form} noValidate>
        <h2>Nova categoria</h2>

        <label htmlFor="name">Nome</label>
        <input id="name" {...register('name')} />
        {errors.name && <p role="alert">{errors.name.message}</p>}

        <label htmlFor="parentCategoryId">Categoria pai (opcional)</label>
        <select id="parentCategoryId" {...register('parentCategoryId')}>
          <option value="">Nenhuma (categoria de topo)</option>
          {topLevelCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>

        {formError && <p role="alert">{formError}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando…' : 'Criar categoria'}
        </button>
      </form>

      <section aria-label="Suas categorias">
        <h2>Suas categorias</h2>
        {categoriesQuery.isLoading && <p>Carregando…</p>}
        {categoriesQuery.isSuccess && categories.length === 0 && <p>Nenhuma categoria cadastrada ainda.</p>}
        <ul className={styles.list}>
          {topLevelCategories.map((category) => (
            <li key={category.id}>
              <CategoryRow category={category} onChanged={invalidateCategories} />
              <ul className={styles.list}>
                {categories
                  .filter((sub) => sub.parentCategoryId === category.id)
                  .map((sub) => (
                    <li key={sub.id}>
                      <CategoryRow category={sub} onChanged={invalidateCategories} />
                    </li>
                  ))}
              </ul>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}

function CategoryRow({ category, onChanged }: { category: Category; onChanged: () => void }) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(category.name)
  const [rowError, setRowError] = useState<string | null>(null)

  const updateMutation = useMutation({
    mutationFn: () => categoriesApi.updateCategory(category.id, name),
    onSuccess: () => {
      setEditing(false)
      onChanged()
    },
    onError: (error) => setRowError(error instanceof ApiError ? error.message : 'Não foi possível salvar.'),
  })

  const deleteMutation = useMutation({
    mutationFn: () => categoriesApi.deleteCategory(category.id),
    onSuccess: onChanged,
    onError: (error) => setRowError(error instanceof ApiError ? error.message : 'Não foi possível excluir.'),
  })

  if (editing) {
    return (
      <form
        onSubmit={(event) => {
          event.preventDefault()
          setRowError(null)
          updateMutation.mutate()
        }}
      >
        <label htmlFor={`category-name-${category.id}`}>Nome</label>
        <input id={`category-name-${category.id}`} value={name} onChange={(event) => setName(event.target.value)} />
        <button type="submit">Salvar</button>
        <button type="button" onClick={() => setEditing(false)}>
          Cancelar
        </button>
      </form>
    )
  }

  return (
    <span className={styles.row}>
      <strong>{category.name}</strong>
      <button type="button" onClick={() => setEditing(true)}>
        Editar
      </button>
      <button
        type="button"
        onClick={() => {
          setRowError(null)
          deleteMutation.mutate()
        }}
      >
        Excluir
      </button>
      {rowError && <p role="alert">{rowError}</p>}
    </span>
  )
}
