package com.financepulse.engine.application.usecases.category;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.domain.category.Category;
import java.util.List;

/**
 * RF-025: fornece um conjunto de categorias padrão. Em vez de semear no
 * cadastro (RegisterUserUseCase, já aprovado e encerrado na Fase 1/M1), o
 * seed é preguiçoso — na primeira consulta de um usuário sem nenhuma
 * categoria, o conjunto padrão é criado automaticamente (ver ADR-0016).
 */
public class ListCategoriesUseCase {

    public static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
            "Alimentação", "Transporte", "Moradia", "Lazer", "Saúde", "Educação", "Salário", "Outros");

    private final CategoryRepository categoryRepository;
    private final IdGenerator idGenerator;

    public ListCategoriesUseCase(CategoryRepository categoryRepository, IdGenerator idGenerator) {
        this.categoryRepository = categoryRepository;
        this.idGenerator = idGenerator;
    }

    public Output execute(Input input) {
        List<Category> categories = categoryRepository.findAllByUserId(input.userId());

        if (categories.isEmpty()) {
            categories = seedDefaultCategories(input.userId());
        }

        return new Output(categories.stream().map(this::toView).toList());
    }

    private List<Category> seedDefaultCategories(String userId) {
        List<Category> seeded = DEFAULT_CATEGORY_NAMES.stream()
                .map(name -> Category.create(idGenerator.generate(), userId, name, null))
                .toList();

        seeded.forEach(categoryRepository::save);

        return seeded;
    }

    private CategoryView toView(Category category) {
        return new CategoryView(category.getId(), category.getName(), category.getParentCategoryId().orElse(null));
    }

    public record Input(String userId) {
    }

    public record Output(List<CategoryView> categories) {
    }

    public record CategoryView(String id, String name, String parentCategoryId) {
    }
}
