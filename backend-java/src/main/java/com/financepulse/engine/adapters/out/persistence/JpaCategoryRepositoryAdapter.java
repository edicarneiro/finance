package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.domain.category.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final SpringDataCategoryJpaRepository jpaRepository;

    public JpaCategoryRepositoryAdapter(SpringDataCategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Category> findByIdAndUserId(String id, String userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<Category> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(Category category) {
        jpaRepository.save(toEntity(category));
    }

    @Override
    public void update(Category category) {
        jpaRepository.save(toEntity(category));
    }

    @Override
    @Transactional
    public void deleteByIdAndUserId(String id, String userId) {
        jpaRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public boolean existsByParentCategoryIdAndUserId(String parentCategoryId, String userId) {
        return jpaRepository.existsByParentCategoryIdAndUserId(parentCategoryId, userId);
    }

    private CategoryJpaEntity toEntity(Category category) {
        return new CategoryJpaEntity(
                category.getId(),
                category.getUserId(),
                category.getName(),
                category.getParentCategoryId().orElse(null),
                category.getCreatedAt());
    }

    private Category toDomain(CategoryJpaEntity entity) {
        return Category.reconstitute(entity.getId(), entity.getUserId(), entity.getName(), entity.getParentCategoryId(), entity.getCreatedAt());
    }
}
