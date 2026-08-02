package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.domain.transaction.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaTransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionJpaRepository jpaRepository;

    public JpaTransactionRepositoryAdapter(SpringDataTransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Transaction> findByIdAndUserId(String id, String userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<Transaction> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Transaction> findAllByAccountIdAndUserId(String accountId, String userId) {
        return jpaRepository.findAllByAccountIdAndUserId(accountId, userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Transaction> findAllByCategoryIdAndUserId(String categoryId, String userId) {
        return jpaRepository.findAllByCategoryIdAndUserId(categoryId, userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(Transaction transaction) {
        jpaRepository.save(toEntity(transaction));
    }

    @Override
    public void update(Transaction transaction) {
        jpaRepository.save(toEntity(transaction));
    }

    @Override
    @Transactional
    public void deleteByIdAndUserId(String id, String userId) {
        jpaRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public boolean existsByCategoryIdAndUserId(String categoryId, String userId) {
        return jpaRepository.existsByCategoryIdAndUserId(categoryId, userId);
    }

    private TransactionJpaEntity toEntity(Transaction transaction) {
        return new TransactionJpaEntity(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getAccountId(),
                transaction.getCategoryId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getDescription(),
                transaction.getTags(),
                transaction.getCreatedAt());
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        return Transaction.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getAccountId(),
                entity.getCategoryId(),
                entity.getType(),
                entity.getAmount(),
                entity.getDate(),
                entity.getDescription(),
                entity.getTags(),
                entity.getCreatedAt());
    }
}
