package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.domain.category.Category;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<String, Category> categoriesById = new LinkedHashMap<>();

    @Override
    public Optional<Category> findByIdAndUserId(String id, String userId) {
        return Optional.ofNullable(categoriesById.get(id)).filter(c -> c.getUserId().equals(userId));
    }

    @Override
    public List<Category> findAllByUserId(String userId) {
        return categoriesById.values().stream().filter(c -> c.getUserId().equals(userId)).toList();
    }

    @Override
    public void save(Category category) {
        categoriesById.put(category.getId(), category);
    }

    @Override
    public void update(Category category) {
        categoriesById.put(category.getId(), category);
    }

    @Override
    public void deleteByIdAndUserId(String id, String userId) {
        categoriesById.computeIfPresent(id, (key, existing) -> existing.getUserId().equals(userId) ? null : existing);
    }

    @Override
    public boolean existsByParentCategoryIdAndUserId(String parentCategoryId, String userId) {
        return categoriesById.values().stream()
                .anyMatch(c -> c.getUserId().equals(userId) && c.getParentCategoryId().map(parentCategoryId::equals).orElse(false));
    }
}
