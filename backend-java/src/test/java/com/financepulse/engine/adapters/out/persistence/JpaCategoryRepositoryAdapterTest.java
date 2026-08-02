package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.category.Category;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina dos demais adaptadores de persistência (rules.md § 3). */
@DataJpaTest
class JpaCategoryRepositoryAdapterTest {

    @Autowired
    private SpringDataCategoryJpaRepository jpaRepository;

    @Test
    void savesAndReloadsACategoryScopedToItsOwner() {
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(jpaRepository);
        Category category = Category.create("category-1", "user-1", "Alimentação", null);

        adapter.save(category);

        Optional<Category> found = adapter.findByIdAndUserId("category-1", "user-1");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alimentação");
    }

    @Test
    void doesNotFindACategoryBelongingToAnotherUser() {
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(jpaRepository);
        adapter.save(Category.create("category-1", "user-1", "Alimentação", null));

        assertThat(adapter.findByIdAndUserId("category-1", "another-user")).isEmpty();
    }

    @Test
    void listsAllCategoriesForAUser() {
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(jpaRepository);
        adapter.save(Category.create("category-1", "user-1", "Alimentação", null));
        adapter.save(Category.create("category-2", "user-1", "Transporte", null));
        adapter.save(Category.create("category-3", "user-2", "Outra", null));

        List<Category> categories = adapter.findAllByUserId("user-1");

        assertThat(categories).hasSize(2).extracting(Category::getId).containsExactlyInAnyOrder("category-1", "category-2");
    }

    @Test
    void persistsAnUpdateToAnExistingCategory() {
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(jpaRepository);
        Category category = Category.create("category-1", "user-1", "Alimentação", null);
        adapter.save(category);

        adapter.update(category.withName("Comida"));

        assertThat(adapter.findByIdAndUserId("category-1", "user-1").orElseThrow().getName()).isEqualTo("Comida");
    }

    @Test
    void deletesACategoryScopedToItsOwner() {
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(jpaRepository);
        adapter.save(Category.create("category-1", "user-1", "Alimentação", null));

        adapter.deleteByIdAndUserId("category-1", "another-user");
        assertThat(adapter.findByIdAndUserId("category-1", "user-1"))
                .as("delete de outro usuário não deve remover a categoria")
                .isPresent();

        adapter.deleteByIdAndUserId("category-1", "user-1");
        assertThat(adapter.findByIdAndUserId("category-1", "user-1")).isEmpty();
    }

    @Test
    void savesAndReloadsASubcategoryWithItsParentReference() {
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(jpaRepository);
        adapter.save(Category.create("category-1", "user-1", "Alimentação", null));

        adapter.save(Category.create("category-2", "user-1", "Restaurante", "category-1"));

        Category subcategory = adapter.findByIdAndUserId("category-2", "user-1").orElseThrow();
        assertThat(subcategory.getParentCategoryId()).contains("category-1");
        assertThat(subcategory.isSubcategory()).isTrue();
    }

    @Test
    void detectsWhetherACategoryHasSubcategories() {
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(jpaRepository);
        adapter.save(Category.create("category-1", "user-1", "Alimentação", null));
        adapter.save(Category.create("category-2", "user-1", "Restaurante", "category-1"));

        assertThat(adapter.existsByParentCategoryIdAndUserId("category-1", "user-1")).isTrue();
        assertThat(adapter.existsByParentCategoryIdAndUserId("category-2", "user-1")).isFalse();
    }
}
