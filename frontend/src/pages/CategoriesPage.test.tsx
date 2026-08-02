import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { seedCategory } from '../test/server'
import { renderApp } from '../test/renderApp'

beforeEach(() => {
  sessionStorage.setItem('financepulse.token', 'fake-jwt-token')
})

function categoriesList() {
  return within(screen.getByRole('region', { name: 'Suas categorias' }))
}

describe('CategoriesPage (fluxo completo via App real)', () => {
  it('lists top-level categories with their subcategories nested', async () => {
    const parent = seedCategory({ name: 'Moradia' })
    seedCategory({ name: 'Aluguel', parentCategoryId: parent.id })

    renderApp('/categories')

    expect(await categoriesList().findByText('Moradia')).toBeInTheDocument()
    expect(await categoriesList().findByText('Aluguel')).toBeInTheDocument()
  })

  it('creates a new top-level category', async () => {
    const user = userEvent.setup()
    renderApp('/categories')
    await screen.findByText('Nenhuma categoria cadastrada ainda.')

    await user.type(screen.getByLabelText('Nome'), 'Transporte')
    await user.click(screen.getByRole('button', { name: 'Criar categoria' }))

    expect(await categoriesList().findByText('Transporte')).toBeInTheDocument()
  })

  it('creates a subcategory under an existing top-level category', async () => {
    seedCategory({ name: 'Alimentação' })
    const user = userEvent.setup()
    renderApp('/categories')
    await categoriesList().findByText('Alimentação')

    await user.type(screen.getByLabelText('Nome'), 'Restaurantes')
    await user.selectOptions(screen.getByLabelText('Categoria pai (opcional)'), 'Alimentação')
    await user.click(screen.getByRole('button', { name: 'Criar categoria' }))

    expect(await categoriesList().findByText('Restaurantes')).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'Restaurantes' })).not.toBeInTheDocument()
  })

  it('shows the backend error when trying to delete a category that still has subcategories', async () => {
    const parent = seedCategory({ name: 'Moradia' })
    seedCategory({ name: 'Aluguel', parentCategoryId: parent.id })
    const user = userEvent.setup()
    renderApp('/categories')

    const parentRow = (await categoriesList().findByText('Moradia')).closest('span')!
    await user.click(within(parentRow).getByRole('button', { name: 'Excluir' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Não é possível excluir uma categoria que possui subcategorias.')
  })

  it('deletes a leaf category successfully', async () => {
    seedCategory({ name: 'Categoria sem uso' })
    const user = userEvent.setup()
    renderApp('/categories')

    const row = (await categoriesList().findByText('Categoria sem uso')).closest('span')!
    await user.click(within(row).getByRole('button', { name: 'Excluir' }))

    await waitFor(() => expect(categoriesList().queryByText('Categoria sem uso')).not.toBeInTheDocument())
  })
})
