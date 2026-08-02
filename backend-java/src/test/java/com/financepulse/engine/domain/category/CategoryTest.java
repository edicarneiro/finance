package com.financepulse.engine.domain.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.category.errors.InvalidCategoryNameException;
import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void createsATopLevelCategoryWithATrimmedName() {
        Category category = Category.create("category-1", "user-1", "  Alimentação  ", null);

        assertThat(category.getId()).isEqualTo("category-1");
        assertThat(category.getUserId()).isEqualTo("user-1");
        assertThat(category.getName()).isEqualTo("Alimentação");
        assertThat(category.getParentCategoryId()).isEmpty();
        assertThat(category.isSubcategory()).isFalse();
    }

    @Test
    void createsASubcategoryWithAParent() {
        Category subcategory = Category.create("category-2", "user-1", "Restaurante", "category-1");

        assertThat(subcategory.getParentCategoryId()).contains("category-1");
        assertThat(subcategory.isSubcategory()).isTrue();
    }

    @Test
    void rejectsCreationWithAnInvalidName() {
        assertThatThrownBy(() -> Category.create("category-1", "user-1", "", null))
                .isInstanceOf(InvalidCategoryNameException.class);
    }

    @Test
    void renamingProducesANewInstanceWithoutChangingIdentityOrParent() {
        Category original = Category.create("category-1", "user-1", "Alimentação", null);

        Category renamed = original.withName("Comida");

        assertThat(renamed.getName()).isEqualTo("Comida");
        assertThat(renamed.getId()).isEqualTo(original.getId());
        assertThat(renamed.getParentCategoryId()).isEqualTo(original.getParentCategoryId());
    }

    @Test
    void rejectsRenamingToAnInvalidName() {
        Category category = Category.create("category-1", "user-1", "Alimentação", null);

        assertThatThrownBy(() -> category.withName("   ")).isInstanceOf(InvalidCategoryNameException.class);
    }
}
