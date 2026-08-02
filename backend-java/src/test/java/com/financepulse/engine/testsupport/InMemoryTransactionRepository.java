package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.domain.transaction.Transaction;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, Transaction> transactionsById = new LinkedHashMap<>();

    @Override
    public Optional<Transaction> findByIdAndUserId(String id, String userId) {
        return Optional.ofNullable(transactionsById.get(id)).filter(t -> t.getUserId().equals(userId));
    }

    @Override
    public List<Transaction> findAllByUserId(String userId) {
        return transactionsById.values().stream().filter(t -> t.getUserId().equals(userId)).toList();
    }

    @Override
    public List<Transaction> findAllByAccountIdAndUserId(String accountId, String userId) {
        return transactionsById.values().stream()
                .filter(t -> t.getUserId().equals(userId) && t.getAccountId().equals(accountId))
                .toList();
    }

    @Override
    public List<Transaction> findAllByCategoryIdAndUserId(String categoryId, String userId) {
        return transactionsById.values().stream()
                .filter(t -> t.getUserId().equals(userId) && t.getCategoryId().equals(categoryId))
                .toList();
    }

    @Override
    public void save(Transaction transaction) {
        transactionsById.put(transaction.getId(), transaction);
    }

    @Override
    public void update(Transaction transaction) {
        transactionsById.put(transaction.getId(), transaction);
    }

    @Override
    public void deleteByIdAndUserId(String id, String userId) {
        transactionsById.computeIfPresent(id, (key, existing) -> existing.getUserId().equals(userId) ? null : existing);
    }

    @Override
    public boolean existsByCategoryIdAndUserId(String categoryId, String userId) {
        return transactionsById.values().stream()
                .anyMatch(t -> t.getUserId().equals(userId) && t.getCategoryId().equals(categoryId));
    }
}
