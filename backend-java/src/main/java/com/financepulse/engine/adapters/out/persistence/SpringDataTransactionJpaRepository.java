package com.financepulse.engine.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTransactionJpaRepository extends JpaRepository<TransactionJpaEntity, String> {

    Optional<TransactionJpaEntity> findByIdAndUserId(String id, String userId);

    List<TransactionJpaEntity> findAllByUserId(String userId);

    List<TransactionJpaEntity> findAllByAccountIdAndUserId(String accountId, String userId);

    List<TransactionJpaEntity> findAllByCategoryIdAndUserId(String categoryId, String userId);

    long deleteByIdAndUserId(String id, String userId);

    boolean existsByCategoryIdAndUserId(String categoryId, String userId);
}
