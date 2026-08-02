package com.financepulse.engine.domain.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.category.errors.InvalidCategoryNameException;
import org.junit.jupiter.api.Test;

class CategoryPolicyTest {

    @Test
    void trimsAValidName() {
        assertThat(CategoryPolicy.assertValidName("  Alimentação  ")).isEqualTo("Alimentação");
    }

    @Test
    void rejectsAnEmptyName() {
        assertThatThrownBy(() -> CategoryPolicy.assertValidName("   ")).isInstanceOf(InvalidCategoryNameException.class);
    }

    @Test
    void rejectsANameLongerThanTheMaximum() {
        String tooLong = "a".repeat(CategoryPolicy.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> CategoryPolicy.assertValidName(tooLong)).isInstanceOf(InvalidCategoryNameException.class);
    }
}
