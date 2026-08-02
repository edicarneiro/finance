package com.financepulse.engine.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCategoryJpaRepository extends JpaRepository<CategoryJpaEntity, String> {

    Optional<CategoryJpaEntity> findByIdAndUserId(String id, String userId);

    List<CategoryJpaEntity> findAllByUserId(String userId);

    void deleteByIdAndUserId(String id, String userId);

    boolean existsByParentCategoryIdAndUserId(String parentCategoryId, String userId);
}
