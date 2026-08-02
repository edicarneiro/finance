package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.category.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findByIdAndUserId(String id, String userId);

    List<Category> findAllByUserId(String userId);

    void save(Category category);

    void update(Category category);

    void deleteByIdAndUserId(String id, String userId);

    boolean existsByParentCategoryIdAndUserId(String parentCategoryId, String userId);
}
